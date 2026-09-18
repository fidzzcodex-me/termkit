package com.termkit.app

import android.app.Application
import com.termkit.app.data.local.HostsDataStore
import com.termkit.app.data.local.SettingsDataStore
import com.termkit.app.data.repo.HostRepository
import com.termkit.app.data.repo.SettingsRepository
import com.termkit.app.data.ssh.SshSessionManager

class TermkitApp : Application() {

    lateinit var hostRepository: HostRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var sessionManager: SshSessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        hostRepository = HostRepository(HostsDataStore(this))
        settingsRepository = SettingsRepository(SettingsDataStore(this))
        sessionManager = SshSessionManager()
    }
}
