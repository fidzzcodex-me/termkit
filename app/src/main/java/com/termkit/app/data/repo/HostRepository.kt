package com.termkit.app.data.repo

import com.termkit.app.data.local.HostsDataStore
import com.termkit.app.data.model.Host
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HostRepository(private val dataStore: HostsDataStore) {

    val hosts: Flow<List<Host>> = dataStore.hosts

    suspend fun upsert(host: Host) {
        val current = dataStore.hosts.first().toMutableList()
        val index = current.indexOfFirst { it.id == host.id }
        if (index >= 0) current[index] = host else current.add(host)
        dataStore.save(current)
    }

    suspend fun delete(hostId: String) {
        val current = dataStore.hosts.first().filterNot { it.id == hostId }
        dataStore.save(current)
    }

    suspend fun toggleFavorite(hostId: String) {
        val current = dataStore.hosts.first().map {
            if (it.id == hostId) it.copy(isFavorite = !it.isFavorite) else it
        }
        dataStore.save(current)
    }

    suspend fun markConnected(hostId: String) {
        val current = dataStore.hosts.first().map {
            if (it.id == hostId) it.copy(lastConnectedAt = System.currentTimeMillis()) else it
        }
        dataStore.save(current)
    }

    suspend fun clearAll() {
        dataStore.save(emptyList())
    }
}
