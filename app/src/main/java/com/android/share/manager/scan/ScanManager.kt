package com.android.share.manager.scan

import kotlinx.coroutines.flow.StateFlow

interface ScanManager {

    val scanState: StateFlow<ScanManagerImpl.ScanState>

    suspend fun startScanning()
}