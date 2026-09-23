package com.reyhoon.customer

object ApiConfig {
    /** آدرس Worker شما */
    var baseUrl: String = "https://reyhoon-api.farshadhelboys.workers.dev"
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && baseUrl.startsWith("http")
}
