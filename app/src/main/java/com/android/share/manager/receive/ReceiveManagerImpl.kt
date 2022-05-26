package com.android.share.manager.receive

import android.content.Context
import com.android.share.manager.receive.ReceiveManagerImpl.ReceiveState.*
import com.android.share.manager.receive.ReceiveManagerImpl.RequestState.*
import com.android.share.model.network.NetworkModel
import com.android.share.util.Constants
import com.android.share.util.readStringFromStream
import com.android.share.util.writeStringAsStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

class ReceiveManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReceiveManager {

    private val _receiveState: MutableStateFlow<ReceiveState> = MutableStateFlow(ReceiveIdle)
    override val receiveState = _receiveState.asStateFlow()

    private val _requestState: MutableStateFlow<RequestState> = MutableStateFlow(RequestIdle)
    override val requestState = _requestState.asStateFlow()

    override var receiveCallback: ReceiveCallback? = null

    private lateinit var serverSocket: ServerSocket
    private lateinit var clientSocket: Socket
    private lateinit var clientInputStream: InputStream
    private lateinit var clientOutputStream: OutputStream

    override suspend fun startReceiving() = withContext(Dispatchers.IO) {
        try {
            val network = getDeviceAddress()
            _receiveState.value = ReceiveInitializing
            serverSocket = ServerSocket(52525, 0, network.address)
            val uniqueNumber = network.address.canonicalHostName.substringAfterLast(".")
            _receiveState.value = ReceiveStarted(uniqueNumber)
            while (!serverSocket.isClosed) waitForClient()
        } catch (exception: Exception) {
            exception.printStackTrace()
            _receiveState.value = ReceiveClosed
            _requestState.value = RequestFailed
        }
    }

    private fun getDeviceAddress(): NetworkModel {
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
        }.first()
    }

    private suspend fun waitForClient() {
        clientSocket = serverSocket.accept()

        clientSocket.getInputStream().use { socketInput ->
            val clientRequest = socketInput.readStringFromStream()

            if (clientRequest != Constants.SOCKET_SCAN) {
                val (connect, name) = clientRequest.split(":")
                if (connect == Constants.SOCKET_SHARE) receiveRequest(socketInput, name)
            } else return
        }
    }

    private suspend fun receiveRequest(
        socketInput: InputStream,
        name: String
    ): Unit = clientSocket.getOutputStream().use { socketOutput ->

        delay(500)
        val sender = clientSocket.inetAddress.hostName.substringAfterLast(".")
        _requestState.value = RequestConnect(sender, name)

        suspendCoroutine<Boolean> { continuation ->
            receiveCallback = object : ReceiveCallback {
                override fun accept() {
                    socketOutput.writeStringAsStream(Constants.SOCKET_ACCEPT)
                    receiveFile(socketInput)
                    continuation.resume(true)
                }

                override fun refuse() {
                    socketOutput.writeStringAsStream(Constants.SOCKET_REFUSE)
                    _requestState.value = RequestIdle
                    continuation.resume(true)
                }
            }
        }
    }

    private fun receiveFile(socketInput: InputStream) {
        val metadataSize = socketInput.read()
        val metadata = socketInput.readStringFromStream(metadataSize)
        val (name, length) = metadata.split(":")
        val fileSize = length.toDouble()
        val file = File(context.filesDir, name)

        file.outputStream().use { fileOutput ->
            val bufferSize = ByteArray(DEFAULT_BUFFER_SIZE)
            var downloadProgress = 0L
            var bytesRead: Int

            while (socketInput.read(bufferSize).also {
                    bytesRead = it
                    downloadProgress += it
                } != -1) {
                fileOutput.write(bufferSize, 0, bytesRead)
                val progress = ((downloadProgress.toDouble() / fileSize) * 100).roundToInt()
                _requestState.value = RequestProgress(name, progress)
            }
        }

        _requestState.value = RequestComplete(name)
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

    sealed class ReceiveState {
        object ReceiveIdle : ReceiveState()
        object ReceiveClosed : ReceiveState()
        object ReceiveInitializing : ReceiveState()
        data class ReceiveStarted(val uniqueNumber: String) : ReceiveState()
    }

    sealed class RequestState {
        object RequestIdle : RequestState()
        object RequestFailed : RequestState()
        data class RequestComplete(val name: String) : RequestState()
        data class RequestProgress(val name: String, val progress: Int) : RequestState()
        data class RequestConnect(val uniqueNumber: String, val name: String) : RequestState()
    }
}