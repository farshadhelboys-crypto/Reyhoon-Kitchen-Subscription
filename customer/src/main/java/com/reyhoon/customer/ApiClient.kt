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

data class FoodItem(val id: String, val name: String, val description: String, val price: Long, val category: String, val extraSkewerPrice: Long = 0L)
data class Address(val street: String, val city: String) {
    fun full(): String = listOf(street, city).filter { it.isNotBlank() }.joinToString(" - ")
}
data class Customer(
    val id: String, val name: String, val phone: String,
    val subscriptionCode: String?, val debt: Long, val credit: Long,
    val address: Address
)
data class OrderItem(val foodId: String, val foodName: String, val unitPrice: Long, val quantity: Int)
data class Order(
    val id: String, val items: List<OrderItem>, val totalAmount: Long, val paidAmount: Long,
    val status: String, val createdAt: Long, val deliveredAt: Long?, val rated: Boolean = false
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
            useCaches = false
        }
    }

    private fun read(c: HttpURLConnection): String {
        val stream = try {
            if (c.responseCode in 200..299) c.inputStream else c.errorStream
        } catch (_: Exception) { c.inputStream } ?: return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun write(c: HttpURLConnection, body: JSONObject) {
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        OutputStreamWriter(c.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
    }

    suspend fun login(code: String): Customer? = loginByCode(code)

    suspend fun loginByCode(code: String): Customer? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext null
        try {
            val enc = java.net.URLEncoder.encode(code, "UTF-8")
            val c = conn("/api/customers/code/$enc", "GET")
            val body = read(c)
            val codeResp = c.responseCode
            c.disconnect()
            if (codeResp != 200) return@withContext null
            val o = JSONObject(body)
            val a = o.optJSONObject("address")
            Customer(
                o.optString("id"), o.optString("name"), o.optString("phone"),
                o.optString("subscriptionCode").ifBlank { null },
                o.optLong("debt"), o.optLong("credit"),
                Address(a?.optString("street") ?: "", a?.optString("city") ?: "")
            )
        } catch (_: Exception) { null }
    }

    suspend fun register(name: String, phone: String, street: String, city: String): Customer? =
        withContext(Dispatchers.IO) {
            if (!ApiConfig.isConfigured) return@withContext null
            try {
                val c = conn("/api/customers/register", "POST")
                write(c, JSONObject()
                    .put("name", name).put("phone", phone)
                    .put("address", JSONObject().put("street", street).put("city", city)))
                val body = read(c)
                val code = c.responseCode
                c.disconnect()
                if (code !in 200..299) return@withContext null
                val root = JSONObject(body)
                val o = root.optJSONObject("customer") ?: root
                val a = o.optJSONObject("address")
                Customer(
                    o.optString("id"), o.optString("name"), o.optString("phone"),
                    o.optString("subscriptionCode").ifBlank { null },
                    o.optLong("debt"), o.optLong("credit"),
                    Address(a?.optString("street") ?: "", a?.optString("city") ?: "")
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
            (0 until arr.length()).mapNotNull {
                val o = arr.getJSONObject(it)
                if (o.optString("name") == "__ping__") null
                else FoodItem(
                    o.optString("id"), o.optString("name"), o.optString("description"),
                    o.optLong("price"), o.optString("category", "عمومی"),
                    o.optLong("extraSkewerPrice", 0)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun placeOrder(customerId: String, items: List<OrderItem>): Boolean =
        withContext(Dispatchers.IO) {
            if (!ApiConfig.isConfigured) return@withContext false
            try {
                val arr = JSONArray()
                items.forEach {
                    arr.put(
                        JSONObject()
                            .put("foodId", it.foodId)
                            .put("foodName", it.foodName)
                            .put("unitPrice", it.unitPrice)
                            .put("quantity", it.quantity)
                    )
                }
                val c = conn("/api/orders", "POST")
                write(
                    c,
                    JSONObject()
                        .put("customerId", customerId)
                        .put("items", arr)
                        .put("paidNow", 0)
                        .put("source", "online")
                )
                val code = c.responseCode
                c.disconnect()
                code in 200..299
            } catch (_: Exception) { false }
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
                    OrderItem(
                        it.optString("foodId"), it.optString("foodName"),
                        it.optLong("unitPrice"), it.optInt("quantity", 1)
                    )
                }
                Order(
                    id = o.optString("id"),
                    items = items,
                    totalAmount = o.optLong("totalAmount"),
                    paidAmount = o.optLong("paidAmount"),
                    status = o.optString("status", "registered"),
                    createdAt = o.optLong("createdAt"),
                    deliveredAt = o.optLong("deliveredAt").takeIf { it > 0 },
                    rated = o.optBoolean("rated", false)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun confirmDelivered(orderId: String): Boolean = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext false
        try {
            val c = conn("/api/orders/$orderId/status", "PATCH")
            write(c, JSONObject().put("status", "delivered").put("byCustomer", true))
            val code = c.responseCode
            c.disconnect()
            code in 200..299
        } catch (_: Exception) { false }
    }

    suspend fun rateOrder(orderId: String, rating: Int, comment: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!ApiConfig.isConfigured) return@withContext false
            try {
                val c = conn("/api/ratings", "POST")
                write(c, JSONObject().put("orderId", orderId).put("rating", rating).put("comment", comment))
                val code = c.responseCode
                c.disconnect()
                code in 200..299
            } catch (_: Exception) { false }
        }
}
