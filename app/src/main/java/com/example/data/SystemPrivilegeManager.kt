package com.example.data

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.example.service.PowerShellAccessibilityService
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

enum class ExecutionMode {
    ANDROID_SHELL, // Real Linux shell (/system/bin/sh)
    SHIZUKU,       // Privileged ADB shell via Shizuku
    ROOT,          // Superuser shell via su
    AUTO           // Intelligent fallback (Root -> Shizuku -> Shell)
}

object SystemPrivilegeManager {

    /**
     * Checks if real root (su) binary is executable
     */
    fun isRootAvailable(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            !line.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if Accessibility service is running
     */
    fun isAccessibilityEnabled(): Boolean {
        return PowerShellAccessibilityService.isRunning
    }

    /**
     * Checks if All Files Access (MANAGE_EXTERNAL_STORAGE) is granted
     */
    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * Checks if Overlay permission is granted
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Executes real Linux process on Android
     */
    fun executeRealCommand(
        command: String,
        mode: ExecutionMode,
        workingDir: File = Environment.getExternalStorageDirectory()
    ): CommandResult {
        return when (mode) {
            ExecutionMode.ROOT -> {
                executeWithRoot(command, workingDir)
            }
            ExecutionMode.SHIZUKU -> {
                if (ShizukuHelper.isRunning && ShizukuHelper.hasPermission()) {
                    ShizukuHelper.execute(command, workingDir)
                } else {
                    CommandResult("", "Shizuku is not running or permission is not granted. Please open Shizuku to grant permission.", -1)
                }
            }
            ExecutionMode.AUTO -> {
                if (isRootAvailable()) {
                    executeWithRoot(command, workingDir)
                } else if (ShizukuHelper.isRunning && ShizukuHelper.hasPermission()) {
                    ShizukuHelper.execute(command, workingDir)
                } else {
                    executeWithShell(command, workingDir)
                }
            }
            ExecutionMode.ANDROID_SHELL -> {
                executeWithShell(command, workingDir)
            }
        }
    }

    private fun executeWithShell(command: String, workingDir: File): CommandResult {
        return try {
            val pb = ProcessBuilder("/system/bin/sh", "-c", command)
            if (workingDir.exists() && workingDir.isDirectory) {
                pb.directory(workingDir)
            }
            // Add standard paths to environment
            val env = pb.environment()
            val currentPath = env["PATH"] ?: "/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin"
            env["PATH"] = "$currentPath:/data/local/tmp"

            val process = pb.start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val error = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()

            CommandResult(output.trimEnd(), error.trimEnd(), exitCode)
        } catch (e: Exception) {
            CommandResult("", "Shell execution failed: ${e.localizedMessage}", -1)
        }
    }

    private fun executeWithRoot(command: String, workingDir: File): CommandResult {
        return try {
            val pb = ProcessBuilder("su", "-c", command)
            if (workingDir.exists() && workingDir.isDirectory) {
                pb.directory(workingDir)
            }
            val process = pb.start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val error = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()

            CommandResult(output.trimEnd(), error.trimEnd(), exitCode)
        } catch (e: Exception) {
            CommandResult("", "Root (su) execution failed: ${e.localizedMessage}", -1)
        }
    }

    // Intents to open system settings for permissions
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openAllFilesAccessSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            }
        }
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
