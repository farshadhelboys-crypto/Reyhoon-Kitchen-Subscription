package com.reyhoon.kitchen.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * کلاینت ساده HTTP برای Cloudflare Worker (بدون کتابخانه اضافه)
 */
object ApiClient {

    private fun conn(path: String, method: String, admin: Boolean = false): HttpURLConnection {
        val base = ApiConfig.baseUrl.trimEnd('/')
        val c = (URL("$base$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/json")
            if (admin) setRequestProperty("X-Admin-Key", ApiConfig.ADMIN_KEY)
            useCaches = false
        }
        return c
    }

    private fun readBody(c: HttpURLConnection): String {
        val stream = try {
            if (c.responseCode in 200..299) c.inputStream else c.errorStream
        } catch (_: Exception) {
            c.inputStream
        } ?: return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun writeJson(c: HttpURLConnection, body: JSONObject) {
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        OutputStreamWriter(c.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
    }

    suspend fun health(): Boolean = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext false
        try {
            val c = conn("/api/health", "GET")
            val ok = c.responseCode == 200
            c.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchMenu(): List<FoodItem> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val c = conn("/api/menu", "GET")
            val body = readBody(c)
            c.disconnect()
            val arr = JSONArray(body)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                FoodItem(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    description = o.optString("description", ""),
                    price = o.optLong("price"),
                    category = o.optString("category", "عمومی"),
                    isAvailable = o.optBoolean("isAvailable", true)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchOrders(customerId: String? = null): List<Order> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val q = if (customerId != null) "?customerId=$customerId" else ""
            val c = conn("/api/orders$q", "GET")
            val body = readBody(c)
            c.disconnect()
            parseOrders(JSONArray(body))
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchNewOrders(since: Long): List<Order> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val c = conn("/api/orders/new-count?since=$since", "GET")
            val body = readBody(c)
            c.disconnect()
            val obj = JSONObject(body)
            parseOrders(obj.optJSONArray("orders") ?: JSONArray())
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        status: String,
        byKitchen: Boolean = false,
        byCustomer: Boolean = false
    ): Order? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext null
        try {
            val c = conn("/api/orders/$orderId/status", "PATCH", admin = byKitchen)
            writeJson(
                c,
                JSONObject()
                    .put("status", status)
                    .put("byKitchen", byKitchen)
                    .put("byCustomer", byCustomer)
            )
            val body = readBody(c)
            val code = c.responseCode
            c.disconnect()
            if (code !in 200..299) return@withContext null
            parseOrder(JSONObject(body))
        } catch (_: Exception) {
            null
        }
    }

    suspend fun createOrder(
        customerId: String,
        items: List<OrderItem>,
        paidNow: Long = 0,
        note: String = ""
    ): Order? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext null
        try {
            val arr = JSONArray()
            items.forEach { it ->
                arr.put(
                    JSONObject()
                        .put("foodId", it.foodId)
                        .put("foodName", it.foodName)
                        .put("unitPrice", it.unitPrice)
                        .put("quantity", it.quantity)
                )
            }
            val c = conn("/api/orders", "POST")
            writeJson(
                c,
                JSONObject()
                    .put("customerId", customerId)
                    .put("items", arr)
                    .put("paidNow", paidNow)
                    .put("note", note)
            )
            val body = readBody(c)
            val code = c.responseCode
            c.disconnect()
            if (code !in 200..299) return@withContext null
            val obj = JSONObject(body)
            parseOrder(obj.getJSONObject("order"))
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchCustomerByCode(code: String): Customer? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext null
        try {
            val enc = java.net.URLEncoder.encode(code, "UTF-8")
            val c = conn("/api/customers/code/$enc", "GET")
            val body = readBody(c)
            val codeResp = c.responseCode
            c.disconnect()
            if (codeResp != 200) return@withContext null
            parseCustomer(JSONObject(body))
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchCustomers(): List<Customer> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val c = conn("/api/customers", "GET", admin = true)
            val body = readBody(c)
            c.disconnect()
            val arr = JSONArray(body)
            (0 until arr.length()).map { parseCustomer(arr.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createCustomer(customer: Customer): Customer? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext null
        try {
            val addr = JSONObject()
                .put("street", customer.address.street)
                .put("city", customer.address.city)
                .put("postalCode", customer.address.postalCode)
                .put("notes", customer.address.notes)
            val c = conn("/api/customers", "POST", admin = true)
            writeJson(
                c,
                JSONObject()
                    .put("name", customer.name)
                    .put("phone", customer.phone)
                    .put("subscriptionCode", customer.subscriptionCode)
                    .put("address", addr)
                    .put("debt", customer.debt)
                    .put("credit", customer.credit)
            )
            val body = readBody(c)
            val code = c.responseCode
            c.disconnect()
            if (code !in 200..299) return@withContext null
            parseCustomer(JSONObject(body))
        } catch (_: Exception) {
            null
        }
    }

    private fun parseOrders(arr: JSONArray): List<Order> =
        (0 until arr.length()).map { parseOrder(arr.getJSONObject(it)) }

    private fun parseOrder(o: JSONObject): Order {
        val itemsArr = o.optJSONArray("items") ?: JSONArray()
        val items = (0 until itemsArr.length()).map { i ->
            val it = itemsArr.getJSONObject(i)
            OrderItem(
                foodId = it.optString("foodId"),
                foodName = it.optString("foodName"),
                unitPrice = it.optLong("unitPrice"),
                quantity = it.optInt("quantity", 1)
            )
        }
        return Order(
            id = o.optString("id"),
            customerId = o.optString("customerId"),
            customerName = o.optString("customerName"),
            customerPhone = o.optString("customerPhone", ""),
            items = items,
            totalAmount = o.optLong("totalAmount"),
            paidAmount = o.optLong("paidAmount"),
            creditApplied = o.optLong("creditApplied"),
            status = o.optString("status", "registered"),
            createdAt = o.optLong("createdAt"),
            preparingAt = o.optLong("preparingAt").takeIf { it > 0 },
            shippedAt = o.optLong("shippedAt").takeIf { it > 0 },
            deliveredAt = o.optLong("deliveredAt").takeIf { it > 0 },
            deliveredByCustomer = o.optBoolean("deliveredByCustomer"),
            deliveredByKitchen = o.optBoolean("deliveredByKitchen"),
            note = o.optString("note", "")
        )
    }

    private fun parseCustomer(o: JSONObject): Customer {
        val a = o.optJSONObject("address")
        return Customer(
            id = o.optString("id"),
            name = o.optString("name"),
            phone = o.optString("phone"),
            subscriptionCode = o.optString("subscriptionCode").ifBlank { null },
            debt = o.optLong("debt"),
            credit = o.optLong("credit"),
            notes = o.optString("notes", ""),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            address = Address(
                street = a?.optString("street") ?: "",
                city = a?.optString("city") ?: "",
                postalCode = a?.optString("postalCode") ?: "",
                notes = a?.optString("notes") ?: ""
            )
        )
    }
}
