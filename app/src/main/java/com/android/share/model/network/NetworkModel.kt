package com.android.share.model.network

import java.net.Inet4Address

data class NetworkModel(
    val address: Inet4Address,
    val prefix: Short,
    val interfaceName: String,
    val displayName: String
)