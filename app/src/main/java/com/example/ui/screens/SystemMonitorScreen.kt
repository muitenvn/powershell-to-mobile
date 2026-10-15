package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMonitorScreen(viewModel: TerminalViewModel) {
    val executionMode by viewModel.executionMode.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isShizukuGranted by viewModel.isShizukuGranted.collectAsState()
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PowerShell System & Device Monitor") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Privileges & Automation State Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Hệ thống Quyền & Tự Động Hóa", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider()
                        Text("Chế độ thực thi hiện tại: $executionMode", fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isAccessibilityActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                            Text("Quyền Chạm màn hình (Accessibility): ${if (isAccessibilityActive) "ĐÃ KÍCH HOẠT" else "CHƯA BẬT"}", fontSize = 13.sp)
                        }
                        Text("Quyền Shizuku (ADB API): ${if (isShizukuGranted) "ĐÃ CẤP QUYỀN (UID 2000)" else "CHƯA CẤP"}", fontSize = 13.sp)
                        Text("Quyền Root (su binary): ${if (isRootAvailable) "CÓ SẴN (UID 0)" else "CHƯA ROOT"}", fontSize = 13.sp)
                    }
                }
            }

            // Hardware & OS Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Phần cứng Thiết bị & Linux Kernel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider()
                        Text("Thiết bị: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                        Text("Phiên bản OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        Text("Bản vá bảo mật: ${Build.VERSION.SECURITY_PATCH}")
                        Text("Kiến trúc CPU: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
                        Text("Phần cứng Bo mạch: ${Build.BOARD} / ${Build.HARDWARE}")
                        Text("Bootloader: ${Build.BOOTLOADER}")
                    }
                }
            }

            // Memory Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Bộ nhớ & Tài nguyên", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider()
                        val runtime = Runtime.getRuntime()
                        val maxMemory = runtime.maxMemory() / (1024 * 1024)
                        val totalMemory = runtime.totalMemory() / (1024 * 1024)
                        val freeMemory = runtime.freeMemory() / (1024 * 1024)

                        Text("RAM Tối đa cấp phát: $maxMemory MB")
                        Text("RAM Đang sử dụng: ${totalMemory - freeMemory} MB / $totalMemory MB")
                        Text("Số nhân CPU khả dụng: ${runtime.availableProcessors()}")
                    }
                }
            }

            // PowerShell Core Environment Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Môi trường PowerShell Engine", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Divider()
                        Text("PowerShell Version: 7.4.2-Android.RealEngine", fontFamily = FontFamily.Monospace)
                        Text("Loại Engine: Thực thi tiến trình thật trên Linux Kernel", fontSize = 13.sp)
                        Text("Tự động hóa: Hỗ trợ cử chỉ chạm, vuốt, click phần tử UI", fontSize = 13.sp)
                        Text("Quyền truy cập: Root su / Shizuku ADB / Android Shell", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
