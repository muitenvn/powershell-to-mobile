package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.TerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCopilotScreen(viewModel: TerminalViewModel) {
    var prompt by remember { mutableStateOf("") }
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PowerShell AI Copilot (Gemini)") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Ask Gemini to write or explain PowerShell scripts...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (prompt.isNotBlank()) {
                        viewModel.askAi(prompt)
                    }
                })
            )

            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        viewModel.askAi(prompt)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Ask AI Copilot")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("AI Response & Script Suggestions:", style = MaterialTheme.typography.titleMedium)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    item {
                        if (isLoading) {
                            Text("Thinking...", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        } else if (aiResponse.isBlank()) {
                            Text("Type a prompt above to generate PowerShell commands or ask questions about scripting.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(
                                text = aiResponse,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
