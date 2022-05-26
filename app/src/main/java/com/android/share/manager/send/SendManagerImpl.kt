package com.android.share.manager.send

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.android.share.manager.send.SendManagerImpl.RequestState.*
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

class SendManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SendManager {

    private val _requestState: MutableStateFlow<RequestState> = MutableStateFlow(RequestIdle)
    override val requestState = _requestState.asStateFlow()

    private lateinit var clientSocket: Socket

    override suspend fun sendRequest(
        receiver: String,
        documentFile: DocumentFile
    ) = withContext(Dispatchers.IO) {
        try {
            _requestState.value = RequestStarted
            clientSocket = Socket(receiver, 52525)

            clientSocket.getOutputStream().use { socketOutput ->
                val name = documentFile.name ?: UUID.randomUUID().toString()
                socketOutput.writeStringAsStream("${Constants.SOCKET_SHARE}:$name")

                clientSocket.getInputStream().use { socketInput ->
                    val respond = socketInput.readStringFromStream()
                    if (respond == Constants.SOCKET_ACCEPT) sendFile(documentFile, socketOutput)
                    if (respond == Constants.SOCKET_REFUSE) _requestState.value = RequestRefused
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            _requestState.value = RequestFailed
        } finally {
            closeClientSocket()
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
                _requestState.value = RequestProgress(fileName, progress)
            }
        }

        _requestState.value = RequestComplete(fileName)
    }

    override fun closeClientSocket() {
        if (::clientSocket.isInitialized) clientSocket.close()
    }

    sealed class RequestState {
        object RequestIdle : RequestState()
        object RequestFailed : RequestState()
        object RequestStarted : RequestState()
        object RequestRefused : RequestState()
        data class RequestComplete(val name: String) : RequestState()
        data class RequestProgress(val name: String, val progress: Int) : RequestState()
    }
}