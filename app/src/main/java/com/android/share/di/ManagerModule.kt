package com.android.share.di

import com.android.share.manager.authenticate.ReceiverManager
import com.android.share.manager.authenticate.ReceiverManagerImpl
import com.android.share.manager.sender.SenderManager
import com.android.share.manager.sender.SenderManagerImpl
import com.android.share.manager.scan.ScanManager
import com.android.share.manager.scan.ScanManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ManagerModule {

    @Binds
    abstract fun bindScanManager(
        scanManagerImpl: ScanManagerImpl
    ): ScanManager

    @Binds
    abstract fun bindSenderManager(
        senderManagerImpl: SenderManagerImpl
    ): SenderManager

    @Binds
    abstract fun bindReceiverManager(
        receiverManagerImpl: ReceiverManagerImpl
    ): ReceiverManager
}