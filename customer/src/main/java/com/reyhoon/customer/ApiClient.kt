package com.reyhoon.customer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class FoodItem(val id: String, val name: String, val description: String, val price: Long, val category: String)
data class Address(val street: String, val city: String) {
    fun full() = listOf(street, city).filter { it.isNotBlank() }.joinToString(" - ")
}
data class Customer(
    val id: String, val name: String, val phone: String,
    val address: Address, val subscriptionCode: String?,
    val debt: Long, val credit: Long
)
data class OrderItem(val foodId: String, val foodName: String, val unitPrice: Long, val quantity: Int)
data class Order(
    val id: String, val items: List<OrderItem>, val totalAmount: Long, val paidAmount: Long,
    val status: String, val createdAt: Long, val deliveredAt: Long?
) {
    val statusFa: String get() = when (status) {
        "registered" -> "سفارش ثبت شد"
        "preparing" -> "در حال آماده‌سازی"
        "shipped" -> "ارسال شده"
        "delivered" -> "تحویل داده شد"
        else -> status
    }
}

object ApiClient {
    private fun conn(path: String, method: String): HttpURLConnection {
        val base = ApiConfig.baseUrl.trimEnd('/')
        return (URL("$base$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun read(c: HttpURLConnection): String {
        val s = try {
            if (c.responseCode in 200..299) c.inputStream else c.errorStream
        } catch (_: Exception) { c.inputStream }
        return BufferedReader(InputStreamReader(s ?: return "", Charsets.UTF_8)).use { it.readText() }
    }

    suspend fun login(code: String): Customer? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext null
        try {
            val enc = java.net.URLEncoder.encode(code, "UTF-8")
            val c = conn("/api/customers/code/$enc", "GET")
            val body = read(c)
            val ok = c.responseCode == 200
            c.disconnect()
            if (!ok) return@withContext null
            val o = JSONObject(body)
            val a = o.optJSONObject("address")
            Customer(
                id = o.optString("id"),
                name = o.optString("name"),
                phone = o.optString("phone"),
                subscriptionCode = o.optString("subscriptionCode").ifBlank { null },
                debt = o.optLong("debt"),
                credit = o.optLong("credit"),
                address = Address(a?.optString("street") ?: "", a?.optString("city") ?: "")
            )
        } catch (_: Exception) { null }
    }

    suspend fun menu(): List<FoodItem> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val c = conn("/api/menu", "GET")
            val body = read(c)
            c.disconnect()
            val arr = JSONArray(body)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                FoodItem(o.optString("id"), o.optString("name"), o.optString("description"), o.optLong("price"), o.optString("category", "عمومی"))
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun orders(customerId: String): List<Order> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val c = conn("/api/orders?customerId=$customerId", "GET")
            val body = read(c)
            c.disconnect()
            val arr = JSONArray(body)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val itemsArr = o.optJSONArray("items") ?: JSONArray()
                val items = (0 until itemsArr.length()).map { j ->
                    val it = itemsArr.getJSONObject(j)
                    OrderItem(it.optString("foodId"), it.optString("foodName"), it.optLong("unitPrice"), it.optInt("quantity", 1))
                }
                Order(
                    id = o.optString("id"),
                    items = items,
                    totalAmount = o.optLong("totalAmount"),
                    paidAmount = o.optLong("paidAmount"),
                    status = o.optString("status"),
                    createdAt = o.optLong("createdAt"),
                    deliveredAt = o.optLong("deliveredAt").takeIf { it > 0 }
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun placeOrder(customerId: String, items: List<OrderItem>): Boolean = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext false
        try {
            val arr = JSONArray()
            items.forEach {
                arr.put(JSONObject().put("foodId", it.foodId).put("foodName", it.foodName).put("unitPrice", it.unitPrice).put("quantity", it.quantity))
            }
            val c = conn("/api/orders", "POST")
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            OutputStreamWriter(c.outputStream, Charsets.UTF_8).use {
                it.write(JSONObject().put("customerId", customerId).put("items", arr).put("paidNow", 0).toString())
            }
            val ok = c.responseCode in 200..299
            c.disconnect()
            ok
        } catch (_: Exception) { false }
    }

    suspend fun confirmDelivered(orderId: String): Boolean = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext false
        try {
            val c = conn("/api/orders/$orderId/status", "PATCH")
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            OutputStreamWriter(c.outputStream, Charsets.UTF_8).use {
                it.write(JSONObject().put("status", "delivered").put("byCustomer", true).toString())
            }
            val ok = c.responseCode in 200..299
            c.disconnect()
            ok
        } catch (_: Exception) { false }
    }
}
