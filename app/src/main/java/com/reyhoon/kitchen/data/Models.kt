package com.reyhoon.kitchen.data

import java.util.UUID

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val address: Address,
    val subscriptionCode: String? = null,
    /** مبلغی که مشتری به آشپزخانه بدهکار است */
    val debt: Long = 0L,
    /** اعتبار مشتری: پولی که بیشتر پرداخت کرده و در سفارش بعدی کسر می‌شود */
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
    val isAvailable: Boolean = true
)

data class OrderItem(
    val foodId: String,
    val foodName: String,
    val unitPrice: Long,
    val quantity: Int = 1
) {
    val total: Long get() = unitPrice * quantity
}

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val customerName: String,
    val items: List<OrderItem>,
    val totalAmount: Long,
    val paidAmount: Long = 0L,
    /** مبلغ کسر شده از اعتبار قبلی مشتری */
    val creditApplied: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = ""
) {
    val remaining: Long get() = (totalAmount - paidAmount).coerceAtLeast(0)
    val isFullyPaid: Boolean get() = remaining == 0L
    val isPartial: Boolean get() = paidAmount > 0 && remaining > 0
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

/** نتیجه ثبت سفارش برای نمایش پیام به ادمین */
data class OrderResult(
    val order: Order,
    val creditApplied: Long,
    val newCredit: Long,
    val newDebt: Long,
    val message: String
)
