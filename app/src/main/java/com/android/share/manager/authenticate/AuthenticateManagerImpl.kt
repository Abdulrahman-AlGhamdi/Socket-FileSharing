package com.android.share.manager.authenticate

import com.android.share.manager.authenticate.AuthenticateManagerImpl.AuthenticateState.*
import com.android.share.model.network.NetworkModel
import com.android.share.util.readStringFromStream
import com.android.share.util.writeStringAsStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import kotlin.random.Random

class AuthenticateManagerImpl @Inject constructor() : AuthenticateManager {

    private val _authenticateState: MutableStateFlow<AuthenticateState> = MutableStateFlow(Idle)
    override val authenticateState = _authenticateState.asStateFlow()

    private lateinit var serverSocket: ServerSocket
    private lateinit var clientSocket: Socket
    private lateinit var clientInputStream: InputStream
    private lateinit var clientOutputStream: OutputStream

    override suspend fun startAuthentication() = withContext(Dispatchers.IO) {
        val network = getDeviceAddress()

        if (network == null) {
            _authenticateState.value = NoInternet
            return@withContext
        }

        val uniqueNumber = network.address.canonicalHostName.substringAfterLast(".")
        _authenticateState.value = StartReceiving(uniqueNumber)
        startServerSocket(network.address)
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

    private fun startServerSocket(address: Inet4Address) = try {
        serverSocket = ServerSocket(52525, 0, address)
        while (!serverSocket.isClosed) receiveFile()
    } catch (exception: Exception) {
        exception.printStackTrace()
        _authenticateState.value = Failed
    }

    private fun receiveFile() {
        clientSocket = serverSocket.accept()
        clientInputStream = clientSocket.getInputStream()
        val request = clientInputStream.readStringFromStream()

        if (request == "scan") return

        clientOutputStream = clientSocket.getOutputStream()
        val uniqueNumber = clientSocket.inetAddress.hostName.substringAfterLast(".")
        if (request == "connect") _authenticateState.value = Connect(uniqueNumber)
    }

    override suspend fun acceptConnection(accept: Boolean) = withContext(Dispatchers.IO) {
        try {
            if (accept) clientOutputStream.writeStringAsStream("accept")
            else clientOutputStream.writeStringAsStream("refuse")
        } catch (exception: Exception) {
            exception.printStackTrace()
            _authenticateState.value = Failed
        }
    }

    override fun closeServerSocket() {
        if (::clientOutputStream.isInitialized) {
            clientOutputStream.flush()
            clientOutputStream.close()
        }
        if (::clientInputStream.isInitialized) clientInputStream.close()
        if (::clientSocket.isInitialized) clientSocket.close()
        if (::serverSocket.isInitialized) serverSocket.close()
    }

    sealed class AuthenticateState {
        object Idle : AuthenticateState()
        object Failed : AuthenticateState()
        object NoInternet : AuthenticateState()
        data class StartReceiving(val uniqueNumber: String) : AuthenticateState()
        data class Connect(val uniqueNumber: String, val number: Int = Random.nextInt()) :
            AuthenticateState()
    }
}