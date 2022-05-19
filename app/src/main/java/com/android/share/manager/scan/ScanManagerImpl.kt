package com.android.share.manager.scan

import com.android.share.manager.scan.ScanManagerImpl.ScanState.*
import com.android.share.model.network.NetworkModel
import com.android.share.util.writeStringAsStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import javax.inject.Inject
import kotlin.math.abs

class ScanManagerImpl @Inject constructor() : ScanManager {

    private val _scanState: MutableStateFlow<ScanState> = MutableStateFlow(Idle)
    override val scanState = _scanState.asStateFlow()

    override suspend fun startScanning() = withContext(Dispatchers.IO) {
        val network = getDeviceAddress()

        if (network == null) {
            _scanState.value = NoInternet
            return@withContext
        }

        val uniqueNumber = network.address.hostName.substringAfterLast(".")
        val (baseIp, networkSize) = network.address.maskWith(network.prefix)
        val addresses = allAddresses(baseIp, networkSize)
        var counter = 0

        val reachableAddresses = addresses.chunked(2).mapNotNull { ipAddresses ->
            async {
                ipAddresses.mapNotNull { ipAddress ->
                    _scanState.value = Progress(uniqueNumber, networkSize, counter++)
                    val isReachable = ipAddress.isReachable(500)
                    if (isReachable && isTcpPortOpen(ipAddress)) ipAddress.canonicalHostName else null
                }
            }
        }.toList().awaitAll().flatten()

        if (reachableAddresses.isEmpty()) _scanState.value = Empty
        else _scanState.value = Complete(reachableAddresses)
    }

    private fun getDeviceAddress(): NetworkModel? {
        return NetworkInterface.getNetworkInterfaces().asSequence().flatMap { interfaces ->
            interfaces.interfaceAddresses.asSequence().mapNotNull { addresses ->
                val address = addresses.address
                if (!address.isLoopbackAddress && address is Inet4Address) NetworkModel(
                    address = address,
                    prefix = addresses.networkPrefixLength,
                    interfaceName = interfaces.name,
                    displayName = interfaces.displayName
                ) else null
            }
        }.firstOrNull()
    }

    private fun Inet4Address.maskWith(maskLength: Short): Pair<Inet4Address, Int> {
        val totalSubMask = (2.shl(maskLength.toInt()) - 1).shl(32 - maskLength)
        val masked = this.hashCode().and(totalSubMask)
        val address = inet4AddressFromInt(masked)
        return Pair(address, abs(totalSubMask))
    }

    private fun allAddresses(baseIp: Inet4Address, networkSize: Int): Sequence<Inet4Address> {
        return generateSequence(0) { if (it + 1 < networkSize) it + 1 else null }
            .map { baseIp.hashCode() + it }
            .map { inet4AddressFromInt(it) }
    }

    private fun inet4AddressFromInt(ip: Int): Inet4Address {
        return Inet4Address.getByAddress(
            "", byteArrayOf(
                (ip ushr 24 and 0xFF).toByte(),
                (ip ushr 16 and 0xFF).toByte(),
                (ip ushr 8 and 0xFF).toByte(),
                (ip and 0xFF).toByte()
            )
        ) as Inet4Address
    }

    private fun isTcpPortOpen(ipAddress: Inet4Address): Boolean {
        val socket = Socket()

        return try {
            socket.connect(InetSocketAddress(ipAddress, 52525), 500)
            socket.getOutputStream().use { it.writeStringAsStream("scan") }
            true
        } catch (ex: Exception) {
            false
        } finally {
            socket.close()
        }
    }

    sealed class ScanState {
        object Idle : ScanState()
        object Empty : ScanState()
        object NoInternet : ScanState()
        data class Complete(val receivers: List<String>) : ScanState()
        data class Progress(val uniqueNumber: String, val max: Int, val progress: Int) : ScanState()
    }
}