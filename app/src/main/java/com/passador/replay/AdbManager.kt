package com.passador.replay

import android.content.Context
import android.util.Log
import com.muntashirakon.adb.AadbKeyPair
import com.muntashirakon.adb.AdbClient
import com.muntashirakon.adb.AdbConnection
import com.muntashirakon.adb.AdbPairingClient
import java.io.File

class AdbManager(private val context: Context) {
    private var connection: AdbConnection? = null
    private val keyFile = File(context.filesDir, "adbkey")

    private fun getKeyPair(): AadbKeyPair {
        return if (keyFile.exists()) {
            AadbKeyPair.fromFile(keyFile)
        } else {
            val kp = AadbKeyPair.generate()
            kp.writeToFile(keyFile)
            kp
        }
    }

    fun pair(host: String, port: Int, code: String): Boolean {
        return try {
            val keyPair = getKeyPair()
            val pairingClient = AdbPairingClient(host, port, code, keyPair)
            pairingClient.start()
            true
        } catch (e: Exception) {
            Log.e("AdbManager", "Falha no pareamento: ${e.message}")
            false
        }
    }

    fun connect(): Boolean {
        return try {
            val keyPair = getKeyPair()
            val client = AdbClient(keyPair, null)
            connection = client.connect()
            true
        } catch (e: Exception) {
            Log.e("AdbManager", "Falha ao conectar: ${e.message}")
            false
        }
    }

    fun executeCommand(command: String): String? {
        return try {
            val stream = connection?.open("shell:$command")
            val output = stream?.inputStream?.readBytes()?.toString(Charsets.UTF_8)
            stream?.close()
            output
        } catch (e: Exception) {
            Log.e("AdbManager", "Erro ao executar: ${e.message}")
            null
        }
    }

    fun disconnect() {
        try {
            connection?.close()
        } catch (_: Exception) {}
        connection = null
    }
}
