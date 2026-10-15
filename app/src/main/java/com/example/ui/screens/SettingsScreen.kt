package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExecutionMode
import com.example.model.TerminalTheme
import com.example.ui.TerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TerminalViewModel) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val executionMode by viewModel.executionMode.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isShizukuActive by viewModel.isShizukuActive.collectAsState()
    val isShizukuGranted by viewModel.isShizukuGranted.collectAsState()
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()
    val hasAllFilesAccess by viewModel.hasAllFilesAccess.collectAsState()

    val themes = listOf(
        TerminalTheme("PowerShell Blue", "#012456", "#FFFFFF", "#569CD6", "#007ACC"),
        TerminalTheme("Hacker Matrix Green", "#0D0208", "#00FF66", "#008F11", "#00FF66"),
        TerminalTheme("Dracula Dark", "#282A36", "#F8F8F2", "#50FA7B", "#BD93F9"),
        TerminalTheme("Solarized Dark", "#002B36", "#839496", "#2AA198", "#B58900"),
        TerminalTheme("Classic Light", "#FFFFFF", "#000000", "#0000FF", "#007ACC")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Quyền & Cài đặt") },
                actions = {
                    IconButton(onClick = { viewModel.refreshPrivileges() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới quyền")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Android System Privileges (Quyền Hệ Thống Thực Tế)
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Trung tâm Quyền Hệ thống Android",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "Cấp các quyền đặc biệt để PowerShell truy cập sâu vào hệ thống và điều khiển thiết bị thực tế.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Accessibility Service (Quyền Chạm Màn Hình & Cử Chỉ)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAccessibilityActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Quyền Chạm Màn hình & Cử chỉ", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (isAccessibilityActive) "Đã kích hoạt (Sẵn sàng thao tác)" else "Chưa kích hoạt dịch vụ trợ năng",
                                        fontSize = 12.sp,
                                        color = if (isAccessibilityActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            FilledTonalButton(onClick = { viewModel.openAccessibilitySettings() }) {
                                Text(if (isAccessibilityActive) "Quản lý" else "Kích hoạt")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Cho phép thực hiện các lệnh: tap <X> <Y> (chạm tọa độ), swipe (vuốt), click \"Text\" (click nút giao diện), dumpscreen (quét toàn bộ nút trên màn hình).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Shizuku Privileges
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isShizukuGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Quyền Shizuku (ADB API Không Cần Root)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    when {
                                        isShizukuGranted -> "Đã cấp quyền Shizuku thành công"
                                        isShizukuActive -> "Shizuku đang chạy (Cần cấp quyền)"
                                        else -> "Shizuku chưa chạy hoặc chưa cài đặt"
                                    },
                                    fontSize = 12.sp,
                                    color = if (isShizukuGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                            Button(onClick = { viewModel.requestShizukuPermission() }) {
                                Text("Cấp quyền")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Quyền Shizuku cho phép chạy tiến trình với cấp bậc ADB (UID 2000), quản lý package (pm), cài đặt/gỡ ứng dụng, điều khiển hệ thống.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Superuser (Root - su)
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Quyền Root (Superuser - su)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (isRootAvailable) "Phát hiện nhị phân su (Root có sẵn)" else "Không tìm thấy quyền Root (Thiết bị chưa root)",
                                    fontSize = 12.sp,
                                    color = if (isRootAvailable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(onClick = { viewModel.executeCommand("su id") }) {
                                Text("Test su")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Quyền Root cho phép truy cập toàn bộ hệ thống file /data, /system, sửa file hệ thống và điều khiển kernel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. All Files Access (MANAGE_EXTERNAL_STORAGE)
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Truy cập Toàn bộ Tệp tin", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (hasAllFilesAccess) "Đã cấp quyền All Files Access" else "Chưa cấp quyền quản lý toàn bộ tệp tin",
                                        fontSize = 12.sp,
                                        color = if (hasAllFilesAccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            FilledTonalButton(onClick = { viewModel.openAllFilesAccessSettings() }) {
                                Text("Cấp quyền")
                            }
                        }
                    }
                }
            }

            // 5. Overlay Permission (Cửa sổ nổi)
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Vẽ lên Ứng dụng Khác (Overlay)", fontWeight = FontWeight.SemiBold)
                                    Text("Cho phép mở terminal nổi khi đang dùng app khác", fontSize = 12.sp)
                                }
                            }
                            FilledTonalButton(onClick = { viewModel.openOverlaySettings() }) {
                                Text("Cấp quyền")
                            }
                        }
                    }
                }
            }

            // Section 2: Chế độ Thực thi (Execution Mode)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Chế độ Thực thi Lệnh (Execution Mode)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExecutionMode.values().forEach { mode ->
                        val isSelected = executionMode == mode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setExecutionMode(mode) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(mode.name, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(
                                        when (mode) {
                                            ExecutionMode.AUTO -> "Tự động ưu tiên quyền cao nhất có sẵn (Root -> Shizuku -> Shell)"
                                            ExecutionMode.ROOT -> "Thực thi trực tiếp qua nhị phân su (Quyền root cao nhất)"
                                            ExecutionMode.SHIZUKU -> "Thực thi với tiến trình Shizuku (ADB cấp cao UID 2000)"
                                            ExecutionMode.ANDROID_SHELL -> "Thực thi trong môi trường Linux Shell tiêu chuẩn (/system/bin/sh)"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                RadioButton(selected = isSelected, onClick = { viewModel.setExecutionMode(mode) })
                            }
                        }
                    }
                }
            }

            // Section 3: Color Themes
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Giao diện & Màu sắc Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            items(themes) { theme ->
                val isSelected = currentTheme.name == theme.name
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTheme(theme) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(theme.name, style = MaterialTheme.typography.titleSmall)
                            Text("Nền: ${theme.backgroundHex} | Chữ: ${theme.textHex}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (isSelected) {
                            Badge { Text("Đang chọn") }
                        }
                    }
                }
            }
        }
    }
}
