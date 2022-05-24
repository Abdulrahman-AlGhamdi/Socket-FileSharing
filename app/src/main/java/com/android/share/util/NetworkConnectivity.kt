package com.android.share.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.LiveData

class NetworkConnectivity(context: Context) : LiveData<Boolean>() {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        if (cm.activeNetwork == null) postValue(false)
    }

    override fun onActive() {
        cm.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onInactive() {
        cm.unregisterNetworkCallback(networkCallback)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (value != true) postValue(true)
        }

        override fun onLost(network: Network) {
            if (value != false) postValue(false)
        }
    }
}