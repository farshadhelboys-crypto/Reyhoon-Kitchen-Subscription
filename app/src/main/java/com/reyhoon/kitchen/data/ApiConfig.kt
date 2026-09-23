package com.reyhoon.kitchen.data

object ApiConfig {
    /** آدرس Worker — آنلاین */
    var baseUrl: String = "https://reyhoon-api.farshadhelboys.workers.dev"

    const val ADMIN_KEY = "reyhoon-admin-2024"

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && baseUrl.startsWith("http")
}
