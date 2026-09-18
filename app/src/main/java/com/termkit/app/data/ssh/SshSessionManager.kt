package com.termkit.app.data.ssh

import android.content.Context
import android.net.Uri
import com.termkit.app.data.model.AuthMethod
import com.termkit.app.data.model.ConnectionStatus
import com.termkit.app.data.model.Host
import com.termkit.app.data.model.SftpEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import java.io.IOException
import java.io.StringReader
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SshSessionManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Offline)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _terminalOutput = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val terminalOutput = _terminalOutput.asSharedFlow()

    private var client: SSHClient? = null
    private var shellSession: Session? = null
    private var shell: Session.Shell? = null
    private var readerJob: Job? = null
    private var sftpClient: SFTPClient? = null

    val activeHost: Host?
        get() = (_status.value as? ConnectionStatus.Connected)?.host

    suspend fun connect(host: Host) = withContext(Dispatchers.IO) {
        cleanup()
        _status.value = ConnectionStatus.Connecting
        try {
            val sshClient = SSHClient()
            sshClient.addHostKeyVerifier(PromiscuousVerifier())
            sshClient.connectTimeout = 10000
            sshClient.timeout = 10000
            sshClient.connect(host.hostname, host.port)

            when (host.authMethod) {
                AuthMethod.PASSWORD -> sshClient.authPassword(host.username, host.password)
                AuthMethod.PRIVATE_KEY -> {
                    val keyProvider = buildKeyProvider(host.privateKey, host.passphrase)
                    sshClient.authPublickey(host.username, keyProvider)
                }
            }

            client = sshClient
            openShell(sshClient)
            _status.value = ConnectionStatus.Connected(host)
        } catch (error: Exception) {
            cleanup()
            _status.value = ConnectionStatus.Error(mapError(error))
        }
    }

    private fun buildKeyProvider(pem: String, passphrase: String): KeyProvider {
        val keyFile = PKCS8KeyFile()
        val finder: PasswordFinder? = if (passphrase.isBlank()) {
            null
        } else {
            object : PasswordFinder {
                override fun reqPassword(resource: Resource<*>?): CharArray = passphrase.toCharArray()
                override fun shouldRetry(resource: Resource<*>?): Boolean = false
            }
        }
        keyFile.init(StringReader(pem), null, finder)
        return keyFile
    }

    private fun openShell(sshClient: SSHClient) {
        val session = sshClient.startSession()
        session.allocateDefaultPTY()
        val shellChannel = session.startShell()
        shellSession = session
        shell = shellChannel
        readerJob = scope.launch {
            val buffer = ByteArray(4096)
            val input = shellChannel.inputStream
            try {
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    _terminalOutput.emit(String(buffer, 0, read, Charsets.UTF_8))
                }
                resetConnectionState()
                _status.value = ConnectionStatus.Offline
            } catch (error: IOException) {
                _terminalOutput.emit("\n[koneksi terputus]\n")
                resetConnectionState()
                _status.value = ConnectionStatus.Error("Koneksi ke server terputus")
            }
        }
    }

    suspend fun sendCommand(command: String) = withContext(Dispatchers.IO) {
        val activeShell = shell
        if (activeShell == null) {
            _status.value = ConnectionStatus.Error("Tidak ada sesi aktif")
            return@withContext
        }
        try {
            activeShell.outputStream.write((command + "\n").toByteArray())
            activeShell.outputStream.flush()
        } catch (error: IOException) {
            _status.value = ConnectionStatus.Error("Gagal mengirim perintah")
        }
    }

    suspend fun listDirectory(path: String): List<SftpEntry> = withContext(Dispatchers.IO) {
        val sftp = ensureSftp()
        sftp.ls(path)
            .filterNot { it.name == "." || it.name == ".." }
            .map { info ->
                SftpEntry(
                    name = info.name,
                    path = if (path.endsWith("/")) path + info.name else "$path/${info.name}",
                    isDirectory = info.isDirectory,
                    size = info.attributes.size,
                    modifiedAt = info.attributes.mtime * 1000L,
                    permissions = info.attributes.permissions.toString()
                )
            }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun createDirectory(path: String) = withContext(Dispatchers.IO) {
        ensureSftp().mkdir(path)
    }

    suspend fun delete(path: String, isDirectory: Boolean) = withContext(Dispatchers.IO) {
        val sftp = ensureSftp()
        if (isDirectory) sftp.rmdir(path) else sftp.rm(path)
    }

    suspend fun rename(oldPath: String, newPath: String) = withContext(Dispatchers.IO) {
        ensureSftp().rename(oldPath, newPath)
    }

    suspend fun uploadFromUri(context: Context, uri: Uri, remotePath: String) = withContext(Dispatchers.IO) {
        val sftp = ensureSftp()
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Tidak bisa membaca file yang dipilih")
        input.use { source ->
            sftp.open(remotePath, setOf(OpenMode.CREAT, OpenMode.WRITE, OpenMode.TRUNC)).use { remoteFile ->
                remoteFile.RemoteFileOutputStream().use { output ->
                    source.copyTo(output)
                }
            }
        }
    }

    suspend fun downloadToUri(context: Context, remotePath: String, uri: Uri) = withContext(Dispatchers.IO) {
        val sftp = ensureSftp()
        sftp.open(remotePath, setOf(OpenMode.READ)).use { remoteFile ->
            remoteFile.RemoteFileInputStream().use { input ->
                val output = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Tidak bisa menulis ke tujuan yang dipilih")
                output.use { input.copyTo(it) }
            }
        }
    }

    private fun ensureSftp(): SFTPClient {
        val sshClient = client ?: throw IllegalStateException("Tidak terhubung ke server")
        var current = sftpClient
        if (current == null) {
            current = sshClient.newSFTPClient()
            sftpClient = current
        }
        return current
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        cleanup()
        _status.value = ConnectionStatus.Offline
    }

    private fun cleanup() {
        readerJob?.cancel()
        readerJob = null
        resetConnectionState()
    }

    private fun resetConnectionState() {
        runCatching { shell?.close() }
        runCatching { shellSession?.close() }
        runCatching { sftpClient?.close() }
        runCatching { client?.disconnect() }
        shell = null
        shellSession = null
        sftpClient = null
        client = null
    }

    private fun mapError(error: Exception): String {
        val message = error.message.orEmpty()
        return when {
            error is UnknownHostException -> "Host tidak ditemukan, periksa alamat server"
            error is SocketTimeoutException -> "Koneksi timeout, periksa jaringan atau alamat host"
            message.contains("Auth fail", ignoreCase = true) -> "Autentikasi gagal, periksa username, password, atau key"
            message.contains("connection refused", ignoreCase = true) -> "Koneksi ditolak oleh server"
            message.contains("key", ignoreCase = true) -> "Private key tidak valid atau passphrase salah"
            message.isBlank() -> "Gagal terhubung ke server"
            else -> "Gagal terhubung: $message"
        }
    }
}
