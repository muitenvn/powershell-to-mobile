package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExecutionMode
import com.example.data.GeminiService
import com.example.data.PowerShellEngine
import com.example.data.ShizukuHelper
import com.example.data.SystemPrivilegeManager
import com.example.model.LineType
import com.example.model.OutputLine
import com.example.model.ScriptItem
import com.example.model.TerminalTheme
import com.example.service.PowerShellAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = PowerShellEngine(application)
    private val geminiService = GeminiService()

    private val _executionMode = MutableStateFlow(ExecutionMode.AUTO)
    val executionMode: StateFlow<ExecutionMode> = _executionMode.asStateFlow()

    private val _isAccessibilityActive = MutableStateFlow(PowerShellAccessibilityService.isRunning)
    val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

    private val _isShizukuActive = MutableStateFlow(ShizukuHelper.isRunning)
    val isShizukuActive: StateFlow<Boolean> = _isShizukuActive.asStateFlow()

    private val _isShizukuGranted = MutableStateFlow(ShizukuHelper.hasPermission())
    val isShizukuGranted: StateFlow<Boolean> = _isShizukuGranted.asStateFlow()

    private val _isRootAvailable = MutableStateFlow(SystemPrivilegeManager.isRootAvailable())
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    private val _hasAllFilesAccess = MutableStateFlow(SystemPrivilegeManager.hasAllFilesAccess(application))
    val hasAllFilesAccess: StateFlow<Boolean> = _hasAllFilesAccess.asStateFlow()

    private val _outputLines = MutableStateFlow<List<OutputLine>>(
        listOf(
            OutputLine("PowerShell Core 7.4.2 [Real Android System Engine]", LineType.INFO),
            OutputLine("Touch Automation: ${if (PowerShellAccessibilityService.isRunning) "READY" else "DISABLED (Enable in Settings)"}", LineType.INFO),
            OutputLine("Privileges: Root=${SystemPrivilegeManager.isRootAvailable()} | Shizuku=${ShizukuHelper.isRunning}", LineType.INFO),
            OutputLine("\nType 'help' to see system & screen touch cmdlets.\nCurrent Path: ${engine.getCurrentPath()}\n", LineType.SUCCESS)
        )
    )
    val outputLines: StateFlow<List<OutputLine>> = _outputLines.asStateFlow()

    private val _currentPath = MutableStateFlow(engine.getCurrentPath())
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    private val _scripts = MutableStateFlow<List<ScriptItem>>(
        listOf(
            ScriptItem(
                name = "ScreenAutomationDemo.ps1",
                description = "Demonstrates real screen touches, swipes and navigation",
                code = "# Test screen touch and navigation\nWrite-Output 'Testing Touch Automation...'\ntap 500 800\nswipe 300 800 300 300 250\nWrite-Output 'Gesture dispatched successfully!'"
            ),
            ScriptItem(
                name = "SystemPrivilegeAudit.ps1",
                description = "Audits all Android privileges, Root, Shizuku & Storage",
                code = "Write-Output '=== ANDROID SYSTEM PRIVILEGE AUDIT ==='\n\$psversiontable\nget-computerinfo\npm list packages -3"
            ),
            ScriptItem(
                name = "NetworkDiagnostic.ps1",
                description = "Tests real ICMP ping and network IP",
                code = "Write-Output 'Testing real network connectivity...'\nping 8.8.8.8\nip addr"
            )
        )
    )
    val scripts: StateFlow<List<ScriptItem>> = _scripts.asStateFlow()

    private val _aiResponse = MutableStateFlow<String>("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Terminal, 1: Scripts, 2: Files, 3: Monitor, 4: AI Copilot, 5: Privileges & Settings
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _currentTheme = MutableStateFlow(
        TerminalTheme("PowerShell Blue", "#012456", "#FFFFFF", "#569CD6", "#007ACC")
    )
    val currentTheme: StateFlow<TerminalTheme> = _currentTheme.asStateFlow()

    fun refreshPrivileges() {
        _isAccessibilityActive.value = PowerShellAccessibilityService.isRunning
        _isShizukuActive.value = ShizukuHelper.isRunning
        _isShizukuGranted.value = ShizukuHelper.hasPermission()
        _isRootAvailable.value = SystemPrivilegeManager.isRootAvailable()
        _hasAllFilesAccess.value = SystemPrivilegeManager.hasAllFilesAccess(getApplication())
    }

    fun setExecutionMode(mode: ExecutionMode) {
        _executionMode.value = mode
        engine.executionMode = mode
        _outputLines.value = _outputLines.value + listOf(
            OutputLine("[System] Switched execution mode to: $mode", LineType.WARNING)
        )
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
        refreshPrivileges()
    }

    fun setTheme(theme: TerminalTheme) {
        _currentTheme.value = theme
    }

    fun executeCommand(cmd: String) {
        if (cmd.isBlank()) return
        _commandHistory.add(cmd)
        historyIndex = _commandHistory.size

        if (cmd.trim().lowercase() in listOf("cls", "clear")) {
            _outputLines.value = emptyList()
            return
        }

        val results = engine.execute(cmd)
        _outputLines.value = _outputLines.value + results
        _currentPath.value = engine.getCurrentPath()
        refreshPrivileges()
    }

    fun runScript(script: ScriptItem) {
        _selectedTab.value = 0 // Switch to terminal
        val lines = mutableListOf<OutputLine>()
        lines.add(OutputLine("=== EXECUTING REAL SCRIPT: ${script.name} ===", LineType.WARNING))
        _outputLines.value = _outputLines.value + lines

        // Execute line by line
        for (line in script.code.split("\n")) {
            val t = line.trim()
            if (t.isNotBlank() && !t.startsWith("#")) {
                val res = engine.execute(t)
                _outputLines.value = _outputLines.value + res
            }
        }
        _outputLines.value = _outputLines.value + listOf(OutputLine("=== SCRIPT EXECUTION FINISHED ===", LineType.WARNING))
        refreshPrivileges()
    }

    fun addScript(name: String, description: String, code: String) {
        val newItem = ScriptItem(name = name, description = description, code = code)
        _scripts.value = _scripts.value + newItem
    }

    fun deleteScript(id: String) {
        _scripts.value = _scripts.value.filter { it.id != id }
    }

    fun askAi(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            val answer = geminiService.askPowerShellCopilot(prompt)
            _aiResponse.value = answer
            _isAiLoading.value = false
        }
    }

    // Permission actions
    fun openAccessibilitySettings() {
        SystemPrivilegeManager.openAccessibilitySettings(getApplication())
    }

    fun openAllFilesAccessSettings() {
        SystemPrivilegeManager.openAllFilesAccessSettings(getApplication())
    }

    fun openOverlaySettings() {
        SystemPrivilegeManager.openOverlaySettings(getApplication())
    }

    fun requestShizukuPermission() {
        ShizukuHelper.requestPermission()
        refreshPrivileges()
    }
}
