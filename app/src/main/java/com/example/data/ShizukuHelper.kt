package com.example.data

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

object ShizukuHelper {

    val isInstalled: Boolean
        get() = try {
            Shizuku.pingBinder()
            true
        } catch (e: Throwable) {
            false
        }

    val isRunning: Boolean
        get() = try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }

    fun hasPermission(): Boolean {
        return try {
            if (isRunning) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (isRunning && !hasPermission()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Executes command using Shizuku privileged shell (UID 2000 / ADB)
     */
    fun execute(command: String, workingDir: File? = null): CommandResult {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val method = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true

            val process = method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                workingDir?.absolutePath
            )

            val inputStream = process.javaClass.getMethod("getInputStream").invoke(process) as InputStream
            val errorStream = process.javaClass.getMethod("getErrorStream").invoke(process) as InputStream
            val waitForMethod = process.javaClass.getMethod("waitFor")

            val output = BufferedReader(InputStreamReader(inputStream)).readText()
            val error = BufferedReader(InputStreamReader(errorStream)).readText()
            val exitCode = waitForMethod.invoke(process) as Int

            CommandResult(
                output = output.trimEnd(),
                error = error.trimEnd(),
                exitCode = exitCode
            )
        } catch (e: Throwable) {
            CommandResult(
                output = "",
                error = "Shizuku execution failed: ${e.localizedMessage}",
                exitCode = -1
            )
        }
    }
}

data class CommandResult(
    val output: String,
    val error: String,
    val exitCode: Int
)
