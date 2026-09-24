package com.reyhoon.kitchen.data

import com.reyhoon.kitchen.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private fun conn(path: String, method: String, admin: Boolean = false): HttpURLConnection {
        val base = ApiConfig.baseUrl.trimEnd('/')
        return (URL("$base$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/json")
            if (admin) {
                setRequestProperty("X-Admin-Key", ApiConfig.ADMIN_KEY)
                AppLog.d("HTTP", "$method $path adminKeyLen=${ApiConfig.ADMIN_KEY.length}")
            }
            useCaches = false
        }
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
        } catch (_: Exception) { false }
    }

    suspend fun fetchMenu(): List<FoodItem> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val c = conn("/api/menu", "GET")
            val body = readBody(c)
            c.disconnect()
            val arr = JSONArray(body)
            val list = (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                if (o.optString("name") == "__ping__") return@mapNotNull null
                FoodItem(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    description = o.optString("description", ""),
                    price = o.optLong("price"),
                    category = o.optString("category", "عمومی"),
                    isAvailable = o.optBoolean("isAvailable", true),
                    extraSkewerPrice = o.optLong("extraSkewerPrice", 0),
                    priceTier = o.optString("priceTier", "regular").ifBlank { "regular" }
                )
            }
            AppLog.i("MenuAPI", "fetchMenu count=${list.size}")
            list
        } catch (e: Exception) {
            AppLog.e("MenuAPI", "fetchMenu failed", e)
            emptyList()
        }
    }

    data class MenuSaveResult(val item: FoodItem?, val httpCode: Int, val error: String?)

    suspend fun createMenuItem(item: FoodItem): FoodItem? = createMenuItemDetailed(item).item

    suspend fun createMenuItemDetailed(item: FoodItem): MenuSaveResult = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) {
            AppLog.w("MenuAPI", "create: API تنظیم نشده")
            return@withContext MenuSaveResult(null, 0, "آدرس سرور تنظیم نشده")
        }
        try {
            AppLog.i("MenuAPI", "POST /api/menu name=${item.name} price=${item.price} tier=${item.priceTier}")
            val c = conn("/api/menu", "POST", admin = true)
            writeJson(c, JSONObject()
                .put("name", item.name).put("description", item.description)
                .put("price", item.price).put("category", item.category)
                .put("extraSkewerPrice", item.extraSkewerPrice)
                .put("priceTier", item.priceTier)
                .put("isAvailable", item.isAvailable))
            val body = readBody(c)
            val code = c.responseCode
            c.disconnect()
            AppLog.i("MenuAPI", "POST response code=$code body=${body.take(200)}")
            if (code !in 200..299) {
                val err = try { JSONObject(body).optString("error", body) } catch (_: Exception) { body }
                val msg = when (code) {
                    401 -> "دسترسی ادمین رد شد (کلید ادمین اشتباه یا ست نشده روی سرور)"
                    404 -> "مسیر /api/menu پیدا نشد — Worker را دوباره دیپلوی کنید"
                    else -> "خطای سرور $code: $err"
                }
                return@withContext MenuSaveResult(null, code, msg)
            }
            val o = JSONObject(body)
            val saved = FoodItem(
                id = o.optString("id", item.id),
                name = o.optString("name", item.name),
                description = o.optString("description", item.description),
                price = o.optLong("price", item.price),
                category = o.optString("category", item.category),
                isAvailable = o.optBoolean("isAvailable", true),
                extraSkewerPrice = o.optLong("extraSkewerPrice", item.extraSkewerPrice),
                priceTier = o.optString("priceTier", item.priceTier).ifBlank { item.priceTier }
            )
            AppLog.i("MenuAPI", "create OK id=${saved.id}")
            MenuSaveResult(saved, code, null)
        } catch (e: Exception) {
            AppLog.e("MenuAPI", "create exception", e)
            MenuSaveResult(null, -1, "خطای شبکه: ${e.message}")
        }
    }

    suspend fun updateMenuItem(item: FoodItem): FoodItem? = updateMenuItemDetailed(item).item

    suspend fun updateMenuItemDetailed(item: FoodItem): MenuSaveResult = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) {
            return@withContext MenuSaveResult(null, 0, "آدرس سرور تنظیم نشده")
        }
        try {
            AppLog.i("MenuAPI", "PUT /api/menu/${item.id} name=${item.name}")
            val c = conn("/api/menu/${item.id}", "PUT", admin = true)
            writeJson(c, JSONObject()
                .put("name", item.name).put("description", item.description)
                .put("price", item.price).put("category", item.category)
                .put("extraSkewerPrice", item.extraSkewerPrice)
                .put("priceTier", item.priceTier)
                .put("isAvailable", item.isAvailable))
            val body = readBody(c)
            val code = c.responseCode
            c.disconnect()
            AppLog.i("MenuAPI", "PUT response code=$code body=${body.take(200)}")
            if (code !in 200..299) {
                val err = try { JSONObject(body).optString("error", body) } catch (_: Exception) { body }
                val msg = when (code) {
                    401 -> "دسترسی ادمین رد شد (کلید ادمین)"
                    404 -> "غذا روی سرور پیدا نشد"
                    else -> "خطای سرور $code: $err"
                }
                return@withContext MenuSaveResult(null, code, msg)
            }
            val o = JSONObject(body)
            val saved = FoodItem(
                id = o.optString("id", item.id),
                name = o.optString("name", item.name),
                description = o.optString("description", item.description),
                price = o.optLong("price", item.price),
                category = o.optString("category", item.category),
                isAvailable = o.optBoolean("isAvailable", true),
                extraSkewerPrice = o.optLong("extraSkewerPrice", item.extraSkewerPrice),
                priceTier = o.optString("priceTier", item.priceTier).ifBlank { item.priceTier }
            )
            MenuSaveResult(saved, code, null)
        } catch (e: Exception) {
            AppLog.e("MenuAPI", "update exception", e)
            MenuSaveResult(null, -1, "خطای شبکه: ${e.message}")
        }
    }

    suspend fun deleteMenuItem(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext false
        try {
            val c = conn("/api/menu/$id", "DELETE", admin = true)
            val ok = c.responseCode in 200..299
            c.disconnect()
            ok
        } catch (_: Exception) { false }
    }

    suspend fun fetchOrders(customerId: String? = null): List<Order> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val q = if (customerId != null) "?customerId=$customerId" else ""
            val c = conn("/api/orders$q", "GET")
            val body = readBody(c)
            c.disconnect()
            parseOrders(JSONArray(body))
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchNewOrders(since: Long): List<Order> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val c = conn("/api/orders/new-count?since=$since", "GET")
            val body = readBody(c)
            c.disconnect()
            parseOrders(JSONObject(body).optJSONArray("orders") ?: JSONArray())
        } catch (_: Exception) { emptyList() }
    }

    suspend fun updateOrderStatus(
        orderId: String, status: String,
        byKitchen: Boolean = false, byCustomer: Boolean = false
    ): Order? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext null
        try {
            val c = conn("/api/orders/$orderId/status", "PATCH", admin = byKitchen)
            writeJson(c, JSONObject().put("status", status)
                .put("byKitchen", byKitchen).put("byCustomer", byCustomer))
            val body = readBody(c)
            val code = c.responseCode
            c.disconnect()
            if (code !in 200..299) return@withContext null
            parseOrder(JSONObject(body))
        } catch (_: Exception) { null }
    }

    suspend fun createOrder(
        customerId: String, items: List<OrderItem>,
        paidNow: Long = 0, note: String = "", source: String = "kitchen"
    ): Order? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext null
        try {
            val arr = JSONArray()
            items.forEach {
                arr.put(JSONObject().put("foodId", it.foodId).put("foodName", it.foodName)
                    .put("unitPrice", it.unitPrice).put("quantity", it.quantity))
            }
            val c = conn("/api/orders", "POST")
            writeJson(c, JSONObject().put("customerId", customerId).put("items", arr)
                .put("paidNow", paidNow).put("note", note).put("source", source))
            val body = readBody(c)
            val code = c.responseCode
            c.disconnect()
            if (code !in 200..299) return@withContext null
            parseOrder(JSONObject(body).getJSONObject("order"))
        } catch (_: Exception) { null }
    }

    suspend fun recordPayment(customerId: String, amount: Long, note: String = ""): Boolean =
        withContext(Dispatchers.IO) {
            if (!ApiConfig.isConfigured || amount <= 0) return@withContext false
            try {
                val c = conn("/api/payments", "POST", admin = true)
                writeJson(c, JSONObject()
                    .put("customerId", customerId)
                    .put("amount", amount)
                    .put("note", note.ifBlank { "دریافت از آشپزخانه" }))
                val ok = c.responseCode in 200..299
                c.disconnect()
                ok
            } catch (_: Exception) { false }
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
        } catch (_: Exception) { null }
    }

    suspend fun fetchCustomers(): List<Customer> = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext emptyList()
        try {
            val c = conn("/api/customers", "GET", admin = true)
            val body = readBody(c)
            c.disconnect()
            val arr = JSONArray(body)
            (0 until arr.length()).map { parseCustomer(arr.getJSONObject(it)) }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun createCustomer(customer: Customer): Customer? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext null
        try {
            val addr = JSONObject().put("street", customer.address.street)
                .put("city", customer.address.city)
                .put("postalCode", customer.address.postalCode)
                .put("notes", customer.address.notes)
            val c = conn("/api/customers", "POST", admin = true)
            writeJson(c, JSONObject().put("name", customer.name).put("phone", customer.phone)
                .put("subscriptionCode", customer.subscriptionCode).put("address", addr)
                .put("debt", customer.debt).put("credit", customer.credit))
            val body = readBody(c)
            val code = c.responseCode
            c.disconnect()
            if (code !in 200..299) return@withContext null
            parseCustomer(JSONObject(body))
        } catch (_: Exception) { null }
    }

    suspend fun deleteCustomer(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext false
        try {
            val c = conn("/api/customers/$id", "DELETE", admin = true)
            val ok = c.responseCode in 200..299
            c.disconnect()
            ok
        } catch (_: Exception) { false }
    }

    suspend fun resetAllData(): Boolean = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext false
        try {
            val c = conn("/api/admin/reset", "POST", admin = true)
            writeJson(c, JSONObject().put("confirm", true))
            val ok = c.responseCode in 200..299
            c.disconnect()
            ok
        } catch (_: Exception) { false }
    }

    data class DeliveryRating(
        val id: String, val orderId: String, val customerName: String,
        val rating: Int, val comment: String, val createdAt: Long
    )
    data class RatingsResult(val ratings: List<DeliveryRating>, val average: Double, val count: Int)

    suspend fun fetchRatings(): RatingsResult = withContext(Dispatchers.IO) {
        if (!ApiConfig.isConfigured) return@withContext RatingsResult(emptyList(), 0.0, 0)
        try {
            val c = conn("/api/ratings", "GET")
            val body = readBody(c)
            c.disconnect()
            val obj = JSONObject(body)
            val arr = obj.optJSONArray("ratings") ?: JSONArray()
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DeliveryRating(o.optString("id"), o.optString("orderId"),
                    o.optString("customerName"), o.optInt("rating"),
                    o.optString("comment", ""), o.optLong("createdAt"))
            }
            RatingsResult(list, obj.optDouble("average", 0.0), obj.optInt("count", list.size))
        } catch (_: Exception) { RatingsResult(emptyList(), 0.0, 0) }
    }

    private fun parseOrders(arr: JSONArray): List<Order> =
        (0 until arr.length()).map { parseOrder(arr.getJSONObject(it)) }

    private fun parseOrder(o: JSONObject): Order {
        val itemsArr = o.optJSONArray("items") ?: JSONArray()
        val items = (0 until itemsArr.length()).map { i ->
            val it = itemsArr.getJSONObject(i)
            OrderItem(it.optString("foodId"), it.optString("foodName"),
                it.optLong("unitPrice"), it.optInt("quantity", 1))
        }
        val addrObj = o.optJSONObject("customerAddressObj")
        val addressStr = o.optString("customerAddress").ifBlank {
            if (addrObj != null) {
                listOf(addrObj.optString("street"), addrObj.optString("city"))
                    .filter { it.isNotBlank() }.joinToString(" - ")
            } else ""
        }
        return Order(
            id = o.optString("id"),
            customerId = o.optString("customerId"),
            customerName = o.optString("customerName"),
            customerPhone = o.optString("customerPhone", ""),
            customerAddress = addressStr,
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
            note = o.optString("note", ""),
            source = o.optString("source", "")
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
