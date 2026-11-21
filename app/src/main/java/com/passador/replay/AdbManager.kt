package com.passador.replay

import android.content.Context
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.File

class AdbManager(private val context: Context) {
    private var connection: Any? = null

    // O Shizuku precisa que o app tenha permissão para usar o ADB
    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(1001)
        } catch (e: Exception) {
            Log.e("AdbManager", "Erro ao pedir permissão: ${e.message}")
        }
    }

    fun connect(): Boolean {
        return try {
            // Verifica se o Shizuku está rodando e se temos permissão
            if (!Shizuku.pingBinder()) {
                Log.e("AdbManager", "Shizuku não está rodando")
                return false
            }
            if (!hasPermission()) {
                Log.e("AdbManager", "Sem permissão do Shizuku")
                return false
            }
            true
        } catch (e: Exception) {
            Log.e("AdbManager", "Falha ao conectar: ${e.message}")
            false
        }
    }

    fun executeCommand(command: String): String? {
        return try {
            // Executa o comando shell via Shizuku
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = process.inputStream.readBytes().toString(Charsets.UTF_8)
            process.waitFor()
            output
        } catch (e: Exception) {
            Log.e("AdbManager", "Erro ao executar: ${e.message}")
            null
        }
    }

    fun disconnect() {
        connection = null
    }
}
