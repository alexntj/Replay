package com.passador.replay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val PKG_NORMAL = "com.dts.freefireth"
    private val PKG_MAX = "com.dts.freefiremax"
    private val PATH_NORMAL = "/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays/"
    private val PATH_MAX = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays/"
    private val REQ_NOTIFICATION = 1001

    private lateinit var screenConnect: View
    private lateinit var screenDirection: View
    private lateinit var screenTransfer: View
    private lateinit var logBox: TextView
    private lateinit var btnPassar: Button
    private lateinit var btnBypass: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvVersionNormal: TextView
    private lateinit var tvVersionMax: TextView
    private val adbManager by lazy { AdbManager(this) }

    private var directionMaxToNormal = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        screenConnect = findViewById(R.id.screenConnect)
        screenDirection = findViewById(R.id.screenDirection)
        screenTransfer = findViewById(R.id.screenTransfer)
        logBox = findViewById(R.id.logBox)
        btnPassar = findViewById(R.id.btnPassar)
        btnBypass = findViewById(R.id.btnBypass)
        tvStatus = findViewById(R.id.tvStatus)
        tvVersionNormal = findViewById(R.id.tvVersionNormal)
        tvVersionMax = findViewById(R.id.tvVersionMax)

        // Pede as duas permissões assim que abre
        requestNotificationPermission()
        requestAllFilesPermission()

        findViewById<Button>(R.id.btnConectar).setOnClickListener {
            openDeveloperOptions()
        }

        findViewById<Button>(R.id.btnJaConectei).setOnClickListener {
            fadeOut(screenConnect)
            fadeIn(screenDirection)
            detectVersions()
        }

        findViewById<Button>(R.id.btnMaxToNormal).setOnClickListener {
            directionMaxToNormal = true
            fadeOut(screenDirection)
            fadeIn(screenTransfer)
            updateTransferUI()
        }

        findViewById<Button>(R.id.btnNormalToMax).setOnClickListener {
            directionMaxToNormal = false
            fadeOut(screenDirection)
            fadeIn(screenTransfer)
            updateTransferUI()
        }

        btnPassar.setOnClickListener { startTransfer() }
        btnBypass.setOnClickListener { doBypass() }


        findViewById<Button>(R.id.btnParear).setOnClickListener {
            val code = findViewById<android.widget.EditText>(R.id.etPairingCode).text.toString()
            val port = findViewById<android.widget.EditText>(R.id.etPairingPort).text.toString().toIntOrNull() ?: 0

            if (code.length == 6 && port > 0) {
                log("Pareando ADB...")
                Thread {
                    val ok = adbManager.pair("localhost", port, code)
                    runOnUiThread {
                        if (ok) log("Pareamento concluído!")
                        else log("Falha no pareamento. Verifique o código e a porta.")
                    }
                }.start()
            } else {
                log("Preencha o código (6 dígitos) e a porta corretamente.")
            }
        }

        screenConnect.alpha = 0f
        screenConnect.animate().alpha(1f).setDuration(500).start()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIFICATION
                )
            }
        }
    }

    private fun requestAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun openDeveloperOptions() {
        try {
            val intent = Intent("com.android.settings.WIRELESS_DEBUGGING")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                return
            } catch (e2: Exception) {}
        }
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {}
    }

    private fun detectVersions() {
        val pm = packageManager
        var normalOk = false
        var maxOk = false

        try {
            val infoN = pm.getPackageInfo(PKG_NORMAL, 0)
            tvVersionNormal.text = "Normal: ${infoN.versionName}"
            normalOk = true
        } catch (e: PackageManager.NameNotFoundException) {
            tvVersionNormal.text = "Normal: NÃO INSTALADO"
        }

        try {
            val infoM = pm.getPackageInfo(PKG_MAX, 0)
            tvVersionMax.text = "Max: ${infoM.versionName}"
            maxOk = true
        } catch (e: PackageManager.NameNotFoundException) {
            tvVersionMax.text = "Max: NÃO INSTALADO"
        }

        if (!normalOk || !maxOk) {
            tvStatus.text = "⚠ Os dois Free Fire precisam estar instalados"
            tvStatus.setTextColor(0xFFFF5252.toInt())
            btnPassar.isEnabled = false
            btnPassar.alpha = 0.4f
        } else {
            tvStatus.text = "Tudo pronto"
            tvStatus.setTextColor(0xFFFF1A1A.toInt())
            btnPassar.isEnabled = true
            btnPassar.alpha = 1f
        }
    }

    private fun updateTransferUI() {
        val title = findViewById<TextView>(R.id.tvTransferTitle)
        title.text = if (directionMaxToNormal) "FF MAX  →  FF NORMAL" else "FF NORMAL  →  FF MAX"
        logBox.text = ""
        btnBypass.visibility = View.GONE
        btnPassar.visibility = View.VISIBLE
        btnPassar.isEnabled = true
    }

    private fun startTransfer() {
        btnPassar.isEnabled = false
        log("Iniciando transferência via ADB...")

        Thread {
            if (!adbManager.connect()) {
                runOnUiThread {
                    log("ERRO: Não foi possível conectar ao ADB. Ative a Depuração sem fio nas configurações.")
                    btnPassar.isEnabled = true
                }
                return@Thread
            }

            val srcDir = if (directionMaxToNormal) PATH_MAX else PATH_NORMAL
            val dstDir = if (directionMaxToNormal) PATH_NORMAL else PATH_MAX

            val cmd = "mkdir -p $dstDir && cp -r $srcDir* $dstDir/"
            val result = adbManager.executeCommand(cmd)

            runOnUiThread {
                if (result != null) {
                    log("Comando ADB executado com sucesso!")
                    if (result.isNotEmpty()) log(result)
                } else {
                    log("Falha no comando ADB.")
                }
                adbManager.disconnect()
                btnPassar.visibility = View.GONE
                btnBypass.visibility = View.VISIBLE
                btnBypass.alpha = 0f
                btnBypass.animate().alpha(1f).setDuration(350).start()
            }
        }.start()
    }

    private fun doBypass() {
        log("BYPASS iniciado...")
        val targetPkg = if (directionMaxToNormal) PKG_NORMAL else PKG_MAX
        try {
            val launch = packageManager.getLaunchIntentForPackage(targetPkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
                log("Abrindo $targetPkg")
            }
        } catch (e: Exception) {
            log("Falha ao abrir o jogo: ${e.message}")
        }

        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            log("Falha ao desinstalar: ${e.message}")
        }

        try {
            cacheDir.deleteRecursively()
            codeCacheDir.deleteRecursively()
        } catch (_: Exception) {}

        finishAffinity()
    }

    private fun log(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        runOnUiThread {
            logBox.append("[$time] $msg\n")
            val scroll = findViewById<ScrollView>(R.id.logScroll)
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun fadeOut(v: View) {
        v.animate().alpha(0f).setDuration(260).withEndAction { v.visibility = View.GONE }.start()
    }

    private fun fadeIn(v: View) {
        v.alpha = 0f
        v.visibility = View.VISIBLE
        v.animate().alpha(1f).setDuration(300).start()
    }
}
