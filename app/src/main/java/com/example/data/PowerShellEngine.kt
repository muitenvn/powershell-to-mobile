package com.example.data

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.Environment
import com.example.model.LineType
import com.example.model.OutputLine
import com.example.service.PowerShellAccessibilityService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PowerShellEngine(private val context: Context) {
    // Default to real External Storage (e.g. /storage/emulated/0) or filesDir
    private var currentDir: File = try {
        val ext = Environment.getExternalStorageDirectory()
        if (ext != null && ext.exists()) ext else context.filesDir
    } catch (e: Exception) {
        context.filesDir
    }

    var executionMode: ExecutionMode = ExecutionMode.AUTO

    fun getCurrentPath(): String {
        return currentDir.absolutePath
    }

    fun setWorkingDir(path: String): Boolean {
        val newDir = when (path) {
            "~" -> Environment.getExternalStorageDirectory() ?: context.filesDir
            ".." -> currentDir.parentFile ?: currentDir
            "/" -> File("/")
            else -> {
                val f = File(path)
                if (f.isAbsolute) f else File(currentDir, path)
            }
        }
        return if (newDir.exists() && newDir.isDirectory) {
            currentDir = newDir.canonicalFile
            true
        } else {
            false
        }
    }

    fun execute(command: String): List<OutputLine> {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lines = mutableListOf<OutputLine>()
        val promptPrefix = when (executionMode) {
            ExecutionMode.ROOT -> "PS [ROOT #] ${currentDir.absolutePath}>"
            ExecutionMode.SHIZUKU -> "PS [SHIZUKU $] ${currentDir.absolutePath}>"
            ExecutionMode.ANDROID_SHELL -> "PS [SHELL] ${currentDir.absolutePath}>"
            ExecutionMode.AUTO -> "PS ${currentDir.absolutePath}>"
        }
        lines.add(OutputLine("$promptPrefix $trimmed", LineType.COMMAND))

        try {
            val parts = parseCommand(trimmed)
            val cmd = parts.firstOrNull()?.lowercase() ?: ""
            val args = parts.drop(1)

            when (cmd) {
                "cls", "clear" -> {
                    return listOf(OutputLine("CLEAR_SCREEN", LineType.SYSTEM))
                }

                // Mode switching
                "su", "sudo" -> {
                    if (args.isEmpty()) {
                        executionMode = ExecutionMode.ROOT
                        lines.add(OutputLine("Switched execution mode to: ROOT (Superuser)", LineType.WARNING))
                        lines.add(OutputLine(SystemPrivilegeManager.executeRealCommand("id", ExecutionMode.ROOT, currentDir).output, LineType.INFO))
                    } else {
                        val subCmd = args.joinToString(" ")
                        val result = SystemPrivilegeManager.executeRealCommand(subCmd, ExecutionMode.ROOT, currentDir)
                        if (result.output.isNotBlank()) lines.add(OutputLine(result.output, LineType.SUCCESS))
                        if (result.error.isNotBlank()) lines.add(OutputLine(result.error, LineType.ERROR))
                    }
                    return lines
                }

                "shizuku" -> {
                    if (args.isEmpty()) {
                        executionMode = ExecutionMode.SHIZUKU
                        val status = if (ShizukuHelper.isRunning) {
                            if (ShizukuHelper.hasPermission()) "Running & Granted" else "Running (Permission required)"
                        } else "Not running"
                        lines.add(OutputLine("Switched execution mode to: SHIZUKU (ADB Privileges). Status: $status", LineType.WARNING))
                    } else {
                        val subCmd = args.joinToString(" ")
                        val result = SystemPrivilegeManager.executeRealCommand(subCmd, ExecutionMode.SHIZUKU, currentDir)
                        if (result.output.isNotBlank()) lines.add(OutputLine(result.output, LineType.SUCCESS))
                        if (result.error.isNotBlank()) lines.add(OutputLine(result.error, LineType.ERROR))
                    }
                    return lines
                }

                "shell", "sh" -> {
                    executionMode = ExecutionMode.ANDROID_SHELL
                    lines.add(OutputLine("Switched execution mode to: ANDROID_SHELL (/system/bin/sh)", LineType.INFO))
                    return lines
                }

                // Real Screen Touch & Gesture Automation
                "tap", "invoke-tap" -> {
                    if (args.size < 2) {
                        lines.add(OutputLine("Usage: tap <X> <Y> (e.g. tap 500 800)", LineType.ERROR))
                    } else {
                        val x = args[0].toFloatOrNull() ?: 0f
                        val y = args[1].toFloatOrNull() ?: 0f
                        val service = PowerShellAccessibilityService.instance
                        if (service != null) {
                            val success = service.tap(x, y)
                            lines.add(OutputLine("[Accessibility] Tap dispatched to coordinate ($x, $y) - Success: $success", LineType.SUCCESS))
                        } else {
                            // Fallback to real input tap via Shell / Shizuku / Root
                            lines.add(OutputLine("[Service Inactive] Falling back to system 'input tap $x $y'...", LineType.INFO))
                            val result = SystemPrivilegeManager.executeRealCommand("input tap $x $y", executionMode, currentDir)
                            if (result.error.isNotBlank()) lines.add(OutputLine(result.error, LineType.ERROR))
                            else lines.add(OutputLine("Input tap executed at ($x, $y)", LineType.SUCCESS))
                        }
                    }
                    return lines
                }

                "swipe", "invoke-swipe" -> {
                    if (args.size < 4) {
                        lines.add(OutputLine("Usage: swipe <startX> <startY> <endX> <endY> [durationMs]", LineType.ERROR))
                    } else {
                        val x1 = args[0].toFloatOrNull() ?: 0f
                        val y1 = args[1].toFloatOrNull() ?: 0f
                        val x2 = args[2].toFloatOrNull() ?: 0f
                        val y2 = args[3].toFloatOrNull() ?: 0f
                        val duration = if (args.size >= 5) args[4].toLongOrNull() ?: 300L else 300L

                        val service = PowerShellAccessibilityService.instance
                        if (service != null) {
                            val success = service.swipe(x1, y1, x2, y2, duration)
                            lines.add(OutputLine("[Accessibility] Swipe dispatched ($x1,$y1) -> ($x2,$y2) in ${duration}ms - Success: $success", LineType.SUCCESS))
                        } else {
                            val result = SystemPrivilegeManager.executeRealCommand("input swipe $x1 $y1 $x2 $y2 $duration", executionMode, currentDir)
                            if (result.error.isNotBlank()) lines.add(OutputLine(result.error, LineType.ERROR))
                            else lines.add(OutputLine("Input swipe executed", LineType.SUCCESS))
                        }
                    }
                    return lines
                }

                "click", "invoke-click" -> {
                    val textToClick = args.joinToString(" ").removeSurrounding("\"").removeSurrounding("'")
                    if (textToClick.isBlank()) {
                        lines.add(OutputLine("Usage: click \"Button Text\" or click \"Search\"", LineType.ERROR))
                    } else {
                        val service = PowerShellAccessibilityService.instance
                        if (service != null) {
                            val success = service.clickByText(textToClick)
                            lines.add(OutputLine("[Accessibility] Click element with text '$textToClick' - Found and clicked: $success", if (success) LineType.SUCCESS else LineType.WARNING))
                        } else {
                            lines.add(OutputLine("Accessibility Service is not enabled. Go to Settings tab to enable PowerShell Accessibility Service.", LineType.ERROR))
                        }
                    }
                    return lines
                }

                "dumpscreen", "get-screenhierarchy" -> {
                    val service = PowerShellAccessibilityService.instance
                    if (service != null) {
                        val hierarchy = service.dumpScreen()
                        lines.add(OutputLine("=== CURRENT SCREEN VIEW HIERARCHY ===", LineType.INFO))
                        lines.add(OutputLine(hierarchy, LineType.SUCCESS))
                    } else {
                        lines.add(OutputLine("Accessibility Service is not active. Please grant accessibility permission in Settings.", LineType.ERROR))
                    }
                    return lines
                }

                "back", "invoke-back" -> {
                    val service = PowerShellAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                        lines.add(OutputLine("[Accessibility] Performed Global Action: BACK", LineType.SUCCESS))
                    } else {
                        SystemPrivilegeManager.executeRealCommand("input keyevent 4", executionMode, currentDir)
                        lines.add(OutputLine("[Shell] Dispatched keyevent 4 (BACK)", LineType.SUCCESS))
                    }
                    return lines
                }

                "home", "invoke-home" -> {
                    val service = PowerShellAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                        lines.add(OutputLine("[Accessibility] Performed Global Action: HOME", LineType.SUCCESS))
                    } else {
                        SystemPrivilegeManager.executeRealCommand("input keyevent 3", executionMode, currentDir)
                        lines.add(OutputLine("[Shell] Dispatched keyevent 3 (HOME)", LineType.SUCCESS))
                    }
                    return lines
                }

                "recents", "invoke-recents" -> {
                    val service = PowerShellAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                        lines.add(OutputLine("[Accessibility] Performed Global Action: RECENTS", LineType.SUCCESS))
                    } else {
                        SystemPrivilegeManager.executeRealCommand("input keyevent 187", executionMode, currentDir)
                        lines.add(OutputLine("[Shell] Dispatched keyevent 187 (RECENTS)", LineType.SUCCESS))
                    }
                    return lines
                }

                "notifications", "invoke-notifications" -> {
                    val service = PowerShellAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                        lines.add(OutputLine("[Accessibility] Opened Notifications Panel", LineType.SUCCESS))
                    } else {
                        SystemPrivilegeManager.executeRealCommand("cmd statusbar expand-notifications", executionMode, currentDir)
                        lines.add(OutputLine("[Shell] Expanded notifications", LineType.SUCCESS))
                    }
                    return lines
                }

                // File system navigation
                "pwd", "get-location" -> {
                    lines.add(OutputLine(currentDir.absolutePath, LineType.SUCCESS))
                    return lines
                }

                "cd", "set-location" -> {
                    val target = args.firstOrNull() ?: "~"
                    if (setWorkingDir(target)) {
                        lines.add(OutputLine(currentDir.absolutePath, LineType.SUCCESS))
                    } else {
                        lines.add(OutputLine("Set-Location: Cannot find path '$target' because it does not exist or access is restricted.", LineType.ERROR))
                    }
                    return lines
                }

                "ls", "dir", "get-childitem" -> {
                    // Execute real ls -la or native directory listing
                    val files = currentDir.listFiles()
                    if (files != null) {
                        lines.add(OutputLine("    Directory: ${currentDir.absolutePath}\n", LineType.INFO))
                        lines.add(OutputLine(String.format("%-15s %-10s %-20s %s", "Mode", "LastWriteTime", "Length", "Name"), LineType.INFO))
                        lines.add(OutputLine("----             ------------          ------      ----", LineType.INFO))
                        for (f in files.sortedBy { !it.isDirectory }) {
                            val mode = if (f.isDirectory) "d-----" else "-a----"
                            val dateStr = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()).format(Date(f.lastModified()))
                            val len = if (f.isDirectory) "" else "${f.length()}b"
                            lines.add(OutputLine(String.format("%-15s %-22s %-11s %s", mode, dateStr, len, f.name), LineType.SUCCESS))
                        }
                    } else {
                        // Fallback to real system shell ls -la
                        val realResult = SystemPrivilegeManager.executeRealCommand("ls -la", executionMode, currentDir)
                        if (realResult.output.isNotBlank()) lines.add(OutputLine(realResult.output, LineType.SUCCESS))
                        if (realResult.error.isNotBlank()) lines.add(OutputLine(realResult.error, LineType.ERROR))
                    }
                    return lines
                }

                "cat", "get-content" -> {
                    val target = args.firstOrNull()
                    if (target == null) {
                        lines.add(OutputLine("Get-Content: Missing path argument.", LineType.ERROR))
                    } else {
                        val file = if (File(target).isAbsolute) File(target) else File(currentDir, target)
                        if (file.exists() && file.canRead()) {
                            lines.add(OutputLine(file.readText(), LineType.SUCCESS))
                        } else {
                            // Try reading via real shell (works for system files when rooted/shizuku)
                            val res = SystemPrivilegeManager.executeRealCommand("cat \"$target\"", executionMode, currentDir)
                            if (res.output.isNotBlank()) lines.add(OutputLine(res.output, LineType.SUCCESS))
                            if (res.error.isNotBlank()) lines.add(OutputLine(res.error, LineType.ERROR))
                        }
                    }
                    return lines
                }

                "get-process", "ps" -> {
                    val realResult = SystemPrivilegeManager.executeRealCommand("ps -A -o PID,USER,VSZ,RSS,NAME || ps -A || ps", executionMode, currentDir)
                    if (realResult.output.isNotBlank()) {
                        lines.add(OutputLine(realResult.output, LineType.SUCCESS))
                    } else {
                        lines.add(OutputLine(realResult.error, LineType.ERROR))
                    }
                    return lines
                }

                "get-computerinfo", "get-ciminstance" -> {
                    lines.add(OutputLine("=== REAL ANDROID DEVICE & SYSTEM SPECS ===", LineType.INFO))
                    lines.add(OutputLine("Device Model:       ${Build.MANUFACTURER} ${Build.MODEL}", LineType.SUCCESS))
                    lines.add(OutputLine("Android OS:         Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})", LineType.SUCCESS))
                    lines.add(OutputLine("Security Patch:     ${Build.VERSION.SECURITY_PATCH}", LineType.SUCCESS))
                    lines.add(OutputLine("Architecture:       ${Build.SUPPORTED_ABIS.joinToString(", ")}", LineType.SUCCESS))
                    lines.add(OutputLine("Root (su):          ${if (SystemPrivilegeManager.isRootAvailable()) "Available" else "Not detected"}", LineType.SUCCESS))
                    lines.add(OutputLine("Shizuku Status:     ${if (ShizukuHelper.isRunning) (if (ShizukuHelper.hasPermission()) "Running (Granted)" else "Running (Needs Permission)") else "Inactive / Not Installed"}", LineType.SUCCESS))
                    lines.add(OutputLine("Accessibility:      ${if (PowerShellAccessibilityService.isRunning) "ACTIVE (Screen touch ready)" else "INACTIVE"}", LineType.SUCCESS))
                    
                    val storageRes = SystemPrivilegeManager.executeRealCommand("df -h /storage/emulated/0 || df -h", executionMode, currentDir)
                    if (storageRes.output.isNotBlank()) {
                        lines.add(OutputLine("\n--- Real Storage Partitions ---", LineType.INFO))
                        lines.add(OutputLine(storageRes.output, LineType.SUCCESS))
                    }
                    return lines
                }

                "help", "get-command" -> {
                    lines.add(OutputLine("POWERSHELL MOBILE - REAL ANDROID SYSTEM ACCESS:", LineType.INFO))
                    lines.add(OutputLine("REAL SCREEN TOUCH & GESTURE COMMANDS:", LineType.INFO))
                    lines.add(OutputLine("  tap <X> <Y>             - Touch screen at exact (X, Y) pixel coordinates", LineType.SUCCESS))
                    lines.add(OutputLine("  swipe <X1> <Y1> <X2> <Y2> [ms] - Perform swipe gesture on screen", LineType.SUCCESS))
                    lines.add(OutputLine("  click \"<text>\"          - Find visible UI button with text and click it", LineType.SUCCESS))
                    lines.add(OutputLine("  dumpscreen              - Inspect and dump UI hierarchy of active screen", LineType.SUCCESS))
                    lines.add(OutputLine("  back / home / recents   - Perform Android system navigation", LineType.SUCCESS))
                    lines.add(OutputLine("  notifications           - Open notifications drawer", LineType.SUCCESS))
                    lines.add(OutputLine("\nPRIVILEGE CONTROL & MODES:", LineType.INFO))
                    lines.add(OutputLine("  su [cmd] / sudo [cmd]   - Switch to or execute with Superuser (Root)", LineType.SUCCESS))
                    lines.add(OutputLine("  shizuku [cmd]           - Switch to or execute with Shizuku (ADB UID 2000)", LineType.SUCCESS))
                    lines.add(OutputLine("  shell / sh              - Switch back to standard Android Linux shell", LineType.SUCCESS))
                    lines.add(OutputLine("\nREAL SYSTEM & DIAGNOSTICS:", LineType.INFO))
                    lines.add(OutputLine("  pm list packages        - Query real installed apps on device", LineType.SUCCESS))
                    lines.add(OutputLine("  getprop [key]           - Read Android system properties", LineType.SUCCESS))
                    lines.add(OutputLine("  ps / Get-Process        - List real Android system processes", LineType.SUCCESS))
                    lines.add(OutputLine("  ls / dir / cd / cat     - Real filesystem navigation & inspection", LineType.SUCCESS))
                    lines.add(OutputLine("  ping [host]             - Real network ICMP ping execution", LineType.SUCCESS))
                    lines.add(OutputLine("  get-computerinfo        - Detailed real device specs & privileges", LineType.SUCCESS))
                    lines.add(OutputLine("  cls / clear             - Clear terminal screen", LineType.SUCCESS))
                    lines.add(OutputLine("  <any bash/sh command>   - Execute ANY real shell command directly!", LineType.SUCCESS))
                    return lines
                }

                "\$psversiontable" -> {
                    lines.add(OutputLine("Name                           Value", LineType.INFO))
                    lines.add(OutputLine("----                           -----", LineType.INFO))
                    lines.add(OutputLine("PSVersion                      7.4.2-Android.RealEngine", LineType.SUCCESS))
                    lines.add(OutputLine("PSEdition                      Core", LineType.SUCCESS))
                    lines.add(OutputLine("Platform                       Android Linux Kernel", LineType.SUCCESS))
                    lines.add(OutputLine("ExecutionMode                  $executionMode", LineType.SUCCESS))
                    lines.add(OutputLine("AccessibilityService           ${if (PowerShellAccessibilityService.isRunning) "Connected" else "Disconnected"}", LineType.SUCCESS))
                    lines.add(OutputLine("ShizukuPrivileges              ${if (ShizukuHelper.hasPermission()) "Granted" else "None"}", LineType.SUCCESS))
                    lines.add(OutputLine("RootPrivileges                 ${if (SystemPrivilegeManager.isRootAvailable()) "Available" else "None"}", LineType.SUCCESS))
                    return lines
                }

                else -> {
                    // Execute REAL system command on Android!
                    val realResult = SystemPrivilegeManager.executeRealCommand(trimmed, executionMode, currentDir)
                    if (realResult.output.isNotBlank()) {
                        lines.add(OutputLine(realResult.output, LineType.SUCCESS))
                    }
                    if (realResult.error.isNotBlank()) {
                        lines.add(OutputLine(realResult.error, LineType.ERROR))
                    }
                    if (realResult.output.isBlank() && realResult.error.isBlank()) {
                        if (realResult.exitCode == 0) {
                            lines.add(OutputLine("Command completed successfully (Exit code 0)", LineType.INFO))
                        } else {
                            lines.add(OutputLine("Process terminated with exit code: ${realResult.exitCode}", LineType.WARNING))
                        }
                    }
                    return lines
                }
            }
        } catch (e: Exception) {
            lines.add(OutputLine("System execution error: ${e.localizedMessage}", LineType.ERROR))
        }

        return lines
    }

    private fun parseCommand(command: String): List<String> {
        val list = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        for (c in command) {
            when {
                c == '"' || c == '\'' -> inQuotes = !inQuotes
                c == ' ' && !inQuotes -> {
                    if (sb.isNotEmpty()) {
                        list.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> sb.append(c)
            }
        }
        if (sb.isNotEmpty()) {
            list.add(sb.toString())
        }
        return list
    }
}
