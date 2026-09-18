package com.termkit.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.termkit.app.data.model.AuthMethod
import com.termkit.app.data.model.Host
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.hostsStore by preferencesDataStore(name = "hosts_store")

class HostsDataStore(private val context: Context) {

    private val hostsKey = stringPreferencesKey("hosts_json")

    val hosts: Flow<List<Host>> = context.hostsStore.data.map { prefs ->
        parseHosts(prefs[hostsKey] ?: "[]")
    }

    suspend fun save(hosts: List<Host>) {
        context.hostsStore.edit { prefs ->
            prefs[hostsKey] = serializeHosts(hosts)
        }
    }

    private fun serializeHosts(hosts: List<Host>): String {
        val array = JSONArray()
        hosts.forEach { host ->
            val obj = JSONObject()
            obj.put("id", host.id)
            obj.put("name", host.name)
            obj.put("hostname", host.hostname)
            obj.put("port", host.port)
            obj.put("username", host.username)
            obj.put("authMethod", host.authMethod.name)
            obj.put("password", host.password)
            obj.put("privateKey", host.privateKey)
            obj.put("passphrase", host.passphrase)
            obj.put("isFavorite", host.isFavorite)
            obj.put("lastConnectedAt", host.lastConnectedAt)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseHosts(raw: String): List<Host> {
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                Host(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    hostname = obj.getString("hostname"),
                    port = obj.optInt("port", 22),
                    username = obj.getString("username"),
                    authMethod = AuthMethod.valueOf(obj.getString("authMethod")),
                    password = obj.optString("password", ""),
                    privateKey = obj.optString("privateKey", ""),
                    passphrase = obj.optString("passphrase", ""),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    lastConnectedAt = obj.optLong("lastConnectedAt", 0L)
                )
            }
        } catch (error: Exception) {
            emptyList()
        }
    }
}
