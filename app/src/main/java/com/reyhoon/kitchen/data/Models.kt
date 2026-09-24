package com.reyhoon.kitchen.data

import java.util.UUID

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val address: Address,
    val subscriptionCode: String? = null,
    val debt: Long = 0L,
    val credit: Long = 0L,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Address(
    val street: String,
    val city: String,
    val postalCode: String = "",
    val notes: String = ""
) {
    fun fullAddress(): String {
        return listOf(street, city, if (postalCode.isNotBlank()) "کدپستی: $postalCode" else "")
            .filter { it.isNotBlank() }
            .joinToString(" - ")
    }
}

data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val price: Long,
    val category: String = "عمومی",
    val isAvailable: Boolean = true,
    /** قیمت هر سیخ اضافه؛ ۰ یعنی این گزینه برای این غذا فعال نیست */
    val extraSkewerPrice: Long = 0L,
    /** economy = اقتصادی | regular = غیر اقتصادی */
    val priceTier: String = "regular"
) {
    val isEconomy: Boolean get() = priceTier == "economy"
    val priceTierLabel: String get() = if (isEconomy) "اقتصادی" else "غیر اقتصادی"
}

object PriceTiers {
    const val ECONOMY = "economy"
    const val REGULAR = "regular"
    val ALL = listOf(ECONOMY, REGULAR)
    fun label(tier: String): String = when (tier) {
        ECONOMY -> "اقتصادی"
        else -> "غیر اقتصادی"
    }
}

data class OrderItem(
    val foodId: String,
    val foodName: String,
    val unitPrice: Long,
    val quantity: Int = 1
) {
    val total: Long get() = unitPrice * quantity
}

enum class OrderStatus(val key: String, val labelFa: String) {
    REGISTERED("registered", "سفارش ثبت شد"),
    PREPARING("preparing", "در حال آماده‌سازی"),
    SHIPPED("shipped", "ارسال شده"),
    DELIVERED("delivered", "تحویل داده شد"),
    CANCELLED("cancelled", "لغو شده");

    companion object {
        fun fromKey(key: String): OrderStatus =
            entries.find { it.key == key } ?: REGISTERED
    }
}

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val items: List<OrderItem>,
    val totalAmount: Long,
    val paidAmount: Long = 0L,
    val creditApplied: Long = 0L,
    val status: String = OrderStatus.REGISTERED.key,
    val createdAt: Long = System.currentTimeMillis(),
    val preparingAt: Long? = null,
    val shippedAt: Long? = null,
    val deliveredAt: Long? = null,
    val deliveredByCustomer: Boolean = false,
    val deliveredByKitchen: Boolean = false,
    val note: String = "",
    val source: String = ""
) {
    val remaining: Long get() = (totalAmount - paidAmount).coerceAtLeast(0)
    val cashReceived: Long get() = (paidAmount - creditApplied).coerceAtLeast(0)
    val isFullyPaid: Boolean get() = remaining == 0L
    val statusEnum: OrderStatus get() = OrderStatus.fromKey(status)
}

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val orderId: String? = null,
    val amount: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class SalesSummary(
    val period: String,
    val totalSales: Long,
    val totalPaid: Long,
    val totalDebt: Long,
    val orderCount: Int
)

data class OrderResult(
    val order: Order,
    val creditApplied: Long,
    val newCredit: Long,
    val newDebt: Long,
    val message: String
)
