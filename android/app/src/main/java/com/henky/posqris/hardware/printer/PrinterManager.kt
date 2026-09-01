package com.henky.posqris.hardware.printer

import kotlinx.coroutines.flow.StateFlow

sealed interface PrinterConnection {
    data object Disconnected : PrinterConnection
    data class Connected(val name: String, val type: PrinterType) : PrinterConnection
    data class Error(val message: String) : PrinterConnection
}

enum class PrinterType { BLUETOOTH, USB }

data class PrinterSettings(
    val type: PrinterType,
    val paperWidthMm: Int = 80,
    val autoPrint: Boolean = true,
    val copies: Int = 1
)

interface PrinterManager {
    val connection: StateFlow<PrinterConnection>
    suspend fun connect(deviceId: String, settings: PrinterSettings): Result<Unit>
    suspend fun disconnect()
    suspend fun print(receipt: ByteArray): Result<Unit>
    suspend fun testPrint(settings: PrinterSettings): Result<Unit>
}
