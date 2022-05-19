package com.android.share.di

import com.android.share.manager.authenticate.AuthenticateManager
import com.android.share.manager.authenticate.AuthenticateManagerImpl
import com.android.share.manager.request.RequestManager
import com.android.share.manager.request.RequestManagerImpl
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
    abstract fun bindRequestManager(
        requestManagerImpl: RequestManagerImpl
    ): RequestManager

    @Binds
    abstract fun bindAuthenticateManager(
        authenticateManagerImpl: AuthenticateManagerImpl
    ): AuthenticateManager
}