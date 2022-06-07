package com.android.share.di

import com.android.share.manager.broadcast.BroadcastManager
import com.android.share.manager.broadcast.BroadcastManagerImpl
import com.android.share.manager.imported.ImportedFilesManager
import com.android.share.manager.imported.ImportedFilesManagerImpl
import com.android.share.manager.scan.ScanManager
import com.android.share.manager.scan.ScanManagerImpl
import com.android.share.manager.send.SendManager
import com.android.share.manager.send.SendManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ManagerModule {

    @Binds
    abstract fun bindScanManager(scanManagerImpl: ScanManagerImpl): ScanManager

    @Binds
    abstract fun bindSendManager(sendManagerImpl: SendManagerImpl): SendManager

    @Binds
    abstract fun bindBroadcastManager(broadcastManagerImpl: BroadcastManagerImpl): BroadcastManager

    @Binds
    abstract fun bindImportedFilesManager(importedFilesManagerImpl: ImportedFilesManagerImpl): ImportedFilesManager
}