package com.example.model

enum class LineType {
    COMMAND,
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    SYSTEM
}

data class OutputLine(
    val text: String,
    val type: LineType = LineType.SUCCESS,
    val id: String = java.util.UUID.randomUUID().toString()
)

data class ScriptItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val code: String
)

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = System.currentTimeMillis()
)

data class TerminalTheme(
    val name: String,
    val backgroundHex: String,
    val textHex: String,
    val promptHex: String,
    val accentHex: String
)
