package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TerminalViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: TerminalViewModel = viewModel()
                val selectedTab by viewModel.selectedTab.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal") },
                                label = { Text("Terminal") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                icon = { Icon(Icons.Default.Code, contentDescription = "Scripts") },
                                label = { Text("Scripts") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { viewModel.selectTab(2) },
                                icon = { Icon(Icons.Default.Folder, contentDescription = "Files") },
                                label = { Text("Files") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { viewModel.selectTab(3) },
                                icon = { Icon(Icons.Default.Memory, contentDescription = "Monitor") },
                                label = { Text("Monitor") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { viewModel.selectTab(4) },
                                icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Copilot") },
                                label = { Text("AI Copilot") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 5,
                                onClick = { viewModel.selectTab(5) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Quyền & Cài đặt") },
                                label = { Text("Quyền & Cài đặt") }
                            )
                        }
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> TerminalScreen(viewModel)
                            1 -> ScriptEditorScreen(viewModel)
                            2 -> FileExplorerScreen(viewModel)
                            3 -> SystemMonitorScreen(viewModel)
                            4 -> AiCopilotScreen(viewModel)
                            5 -> SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
