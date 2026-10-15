package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExecutionMode
import com.example.model.LineType
import com.example.ui.TerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: TerminalViewModel) {
    val outputLines by viewModel.outputLines.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val theme by viewModel.currentTheme.collectAsState()
    val executionMode by viewModel.executionMode.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isShizukuActive by viewModel.isShizukuActive.collectAsState()
    val isShizukuGranted by viewModel.isShizukuGranted.collectAsState()
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()

    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(outputLines.size) {
        if (outputLines.isNotEmpty()) {
            listState.animateScrollToItem(outputLines.size - 1)
        }
    }

    val backgroundColor = Color(android.graphics.Color.parseColor(theme.backgroundHex))
    val textColor = Color(android.graphics.Color.parseColor(theme.textHex))
    val promptColor = Color(android.graphics.Color.parseColor(theme.promptHex))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        // Mode & Privilege Status Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Mode: $executionMode",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Mode Switcher Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExecutionMode.values().forEach { mode ->
                            val isSelected = executionMode == mode
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (isSelected) promptColor else Color.DarkGray,
                                modifier = Modifier.clickable { viewModel.setExecutionMode(mode) }
                            ) {
                                Text(
                                    text = mode.name.take(4),
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.Black else Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Privilege status pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Touch / Accessibility Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.clickable { viewModel.openAccessibilitySettings() }
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isAccessibilityActive) Color(0xFF50FA7B) else Color(0xFFFF5555),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isAccessibilityActive) "Touch Active" else "Touch Off (Tap)",
                            fontSize = 10.sp,
                            color = if (isAccessibilityActive) Color(0xFF50FA7B) else Color(0xFFFF5555),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Shizuku Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.clickable { viewModel.requestShizukuPermission() }
                    ) {
                        Icon(
                            imageVector = if (isShizukuGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isShizukuGranted) Color(0xFF50FA7B) else Color(0xFF8BE9FD),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isShizukuGranted) "Shizuku Ready" else "Shizuku (Tap)",
                            fontSize = 10.sp,
                            color = if (isShizukuGranted) Color(0xFF50FA7B) else Color(0xFF8BE9FD),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Root Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isRootAvailable) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isRootAvailable) Color(0xFF50FA7B) else Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isRootAvailable) "Root Ready" else "No Root",
                            fontSize = 10.sp,
                            color = if (isRootAvailable) Color(0xFF50FA7B) else Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Quick Action Command Chips (Real & Touch Commands)
        val quickCmds = listOf(
            "tap 500 800",
            "swipe 300 800 300 200",
            "dumpscreen",
            "back",
            "home",
            "pm list packages -3",
            "getprop ro.build.version.release",
            "ps",
            "df -h",
            "ip addr",
            "su id",
            "help",
            "cls"
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            items(quickCmds) { cmd ->
                FilledTonalButton(
                    onClick = { viewModel.executeCommand(cmd) },
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("quick_cmd_$cmd"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    if (cmd.startsWith("tap") || cmd.startsWith("swipe") || cmd == "dumpscreen") {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(cmd, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Terminal Output Log
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(backgroundColor.copy(alpha = 0.95f))
                .padding(4.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(outputLines) { line ->
                    val color = when (line.type) {
                        LineType.COMMAND -> promptColor
                        LineType.SUCCESS -> textColor
                        LineType.ERROR -> Color(0xFFFF5555)
                        LineType.WARNING -> Color(0xFFFFB86C)
                        LineType.INFO -> Color(0xFF8BE9FD)
                        LineType.SYSTEM -> Color(0xFFBD93F9)
                    }
                    Text(
                        text = line.text,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Command Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f), shape = MaterialTheme.shapes.medium)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val prefix = when (executionMode) {
                ExecutionMode.ROOT -> "PS #>"
                ExecutionMode.SHIZUKU -> "PS $>"
                ExecutionMode.ANDROID_SHELL -> "PS %>"
                ExecutionMode.AUTO -> "PS>"
            }
            Text(
                text = prefix,
                color = promptColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            OutlinedTextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input"),
                textStyle = LocalTextStyle.current.copy(
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputCommand.isNotBlank()) {
                            viewModel.executeCommand(inputCommand)
                            inputCommand = ""
                        }
                    }
                ),
                placeholder = {
                    Text("e.g. tap 500 800, pm list, su, ls, help...", color = textColor.copy(alpha = 0.45f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            )

            IconButton(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        viewModel.executeCommand(inputCommand)
                        inputCommand = ""
                    }
                },
                modifier = Modifier.testTag("send_command_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Execute Command", tint = promptColor)
            }
        }
    }
}
