package com.android.share.manager.sender

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.android.share.manager.sender.SenderManagerImpl.SendState.*
import com.android.share.util.Constants
import com.android.share.util.readStringFromStream
import com.android.share.util.writeStringAsStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.Socket
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt

class SenderManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SenderManager {

    private val _sendState: MutableStateFlow<SendState> = MutableStateFlow(Idle)
    override val sendState = _sendState.asStateFlow()

    private lateinit var clientSocket: Socket

    override suspend fun sendRequest(
        receiver: String,
        documentFile: DocumentFile
    ) = withContext(Dispatchers.IO) {
        try {
            _sendState.value = SendStarted
            clientSocket = Socket(receiver, 52525)

            clientSocket.getOutputStream().use { socketOutput ->
                val name = documentFile.name ?: UUID.randomUUID().toString()
                socketOutput.writeStringAsStream("${Constants.SOCKET_SHARE}:$name")

                clientSocket.getInputStream().use { socketInput ->
                    val respond = socketInput.readStringFromStream()
                    if (respond == Constants.SOCKET_ACCEPT) sendFile(documentFile, socketOutput)
                    if (respond == Constants.SOCKET_REFUSE) _sendState.value = SendRefused
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            _sendState.value = SendFailed
        } finally {
            clientSocket.close()
        }
    }

    private fun sendFile(documentFile: DocumentFile, socketOutput: OutputStream) {
        val fileName = documentFile.name ?: UUID.randomUUID().toString()
        val fileSize = documentFile.length().toDouble()
        val metadata = "$fileName:$fileSize".toByteArray()
        val bufferSize = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploadProgress = 0L
        var bytesRead = 0

        socketOutput.write(metadata.size)
        socketOutput.write(metadata)

        context.contentResolver.openInputStream(documentFile.uri)?.use { fileInputStream ->
            while (fileInputStream.read(bufferSize).also {
                    bytesRead = it
                    uploadProgress += it
                } != -1) {
                socketOutput.write(bufferSize, 0, bytesRead)
                val progress = ((uploadProgress.toDouble() / fileSize) * 100).roundToInt()
                _sendState.value = SendProgress(fileName, progress)
            }
        }

        _sendState.value = SendComplete(fileName)
    }

    override fun closeClientSocket() {
        if (::clientSocket.isInitialized) clientSocket.close()
    }

    sealed class SendState {
        object Idle : SendState()
        object SendFailed : SendState()
        object SendStarted : SendState()
        object SendRefused : SendState()
        object SendAccepted : SendState()
        data class SendComplete(val name: String) : SendState()
        data class SendProgress(val name: String, val progress: Int) : SendState()
    }
}