package com.reyhoon.kitchen.data

data class Subscription(
    val code: String,
    val customerName: String,
    val address: Address,
    val planName: String,
    val startDate: String,
    val endDate: String,
    val isActive: Boolean
)

data class Address(
    val street: String,
    val city: String,
    val postalCode: String,
    val phone: String,
    val notes: String = ""
) {
    fun fullAddress(): String {
        return listOf(street, city, "کدپستی: $postalCode").filter { it.isNotBlank() }.joinToString(" - ")
    }
}

data class FoodItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Long, // in Toman
    val category: String,
    val isAvailable: Boolean = true
)
