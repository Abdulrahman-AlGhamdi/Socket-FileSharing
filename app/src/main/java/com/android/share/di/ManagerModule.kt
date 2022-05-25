package com.android.share.di

import com.android.share.manager.receiver.ReceiverManager
import com.android.share.manager.receiver.ReceiverManagerImpl
import com.android.share.manager.scan.ScanManager
import com.android.share.manager.scan.ScanManagerImpl
import com.android.share.manager.sender.RequestManager
import com.android.share.manager.sender.RequestManagerImpl
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
    abstract fun bindRequestManager(requestManagerImpl: RequestManagerImpl): RequestManager

    @Binds
    abstract fun bindReceiverManager(receiverManagerImpl: ReceiverManagerImpl): ReceiverManager
}