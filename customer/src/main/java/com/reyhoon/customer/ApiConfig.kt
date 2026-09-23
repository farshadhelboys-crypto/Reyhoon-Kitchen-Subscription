package com.reyhoon.customer

object ApiConfig {
    /** همان آدرس Worker اپ آشپزخانه */
    var baseUrl: String = ""
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && baseUrl.startsWith("http")
}
