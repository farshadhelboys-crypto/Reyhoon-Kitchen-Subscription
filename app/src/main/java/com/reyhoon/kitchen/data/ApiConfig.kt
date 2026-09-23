package com.reyhoon.kitchen.data

/**
 * بعد از دیپلوی Worker، آدرس را اینجا بگذارید.
 * مثال: https://reyhoon-api.your-subdomain.workers.dev
 *
 * اگر خالی باشد اپ در حالت آفلاین (حافظه محلی) کار می‌کند.
 */
object ApiConfig {
    /** آدرس Worker — حتماً بدون اسلش پایانی */
    var baseUrl: String = ""

    const val ADMIN_KEY = "reyhoon-admin-2024"

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && baseUrl.startsWith("http")
}
