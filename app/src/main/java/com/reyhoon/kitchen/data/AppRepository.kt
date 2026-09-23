package com.reyhoon.kitchen.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.Calendar

/**
 * In-memory repository for Reyhoon Kitchen.
 * Handles customers, menu, orders, payments and accounting reports.
 */
object AppRepository {

    val customers: SnapshotStateList<Customer> = mutableStateListOf()
    val menuItems: SnapshotStateList<FoodItem> = mutableStateListOf()
    val orders: SnapshotStateList<Order> = mutableStateListOf()
    val payments: SnapshotStateList<Payment> = mutableStateListOf()

    val currentCustomer = mutableStateOf<Customer?>(null)
    val isAdmin = mutableStateOf(false)

    // ---------- Menu ----------
    fun addFood(item: FoodItem) {
        menuItems.add(item)
    }

    fun updateFood(item: FoodItem) {
        val idx = menuItems.indexOfFirst { it.id == item.id }
        if (idx >= 0) menuItems[idx] = item
    }

    fun deleteFood(id: String) {
        menuItems.removeAll { it.id == id }
    }

    fun getMenuByCategory(): Map<String, List<FoodItem>> {
        return menuItems.filter { it.isAvailable }.groupBy { it.category }
    }

    // ---------- Customers ----------
    fun addCustomer(customer: Customer) {
        customers.add(customer)
    }

    fun updateCustomer(customer: Customer) {
        val idx = customers.indexOfFirst { it.id == customer.id }
        if (idx >= 0) customers[idx] = customer
    }

    fun deleteCustomer(id: String) {
        customers.removeAll { it.id == id }
    }

    fun findByCode(code: String): Customer? {
        return customers.find { it.subscriptionCode?.equals(code.trim(), ignoreCase = true) == true }
    }

    fun findCustomer(id: String): Customer? = customers.find { it.id == id }

    // ---------- Orders & Accounting ----------
    fun createOrder(
        customer: Customer,
        items: List<OrderItem>,
        paidNow: Long = 0L,
        note: String = ""
    ): Order {
        val total = items.sumOf { it.total }
        val order = Order(
            customerId = customer.id,
            customerName = customer.name,
            items = items,
            totalAmount = total,
            paidAmount = paidNow.coerceIn(0, total),
            note = note
        )
        orders.add(0, order)

        // Update customer debt
        val remaining = order.remaining
        if (remaining > 0) {
            val updated = customer.copy(debt = customer.debt + remaining)
            updateCustomer(updated)
            if (currentCustomer.value?.id == customer.id) {
                currentCustomer.value = updated
            }
        }

        if (paidNow > 0) {
            payments.add(
                Payment(
                    customerId = customer.id,
                    orderId = order.id,
                    amount = paidNow,
                    note = "پرداخت هنگام ثبت سفارش"
                )
            )
        }
        return order
    }

    fun recordPayment(customerId: String, amount: Long, orderId: String? = null, note: String = "") {
        if (amount <= 0) return
        val customer = findCustomer(customerId) ?: return

        payments.add(
            Payment(
                customerId = customerId,
                orderId = orderId,
                amount = amount,
                note = note.ifBlank { "پرداخت بدهی" }
            )
        )

        // Reduce debt
        val newDebt = (customer.debt - amount).coerceAtLeast(0)
        updateCustomer(customer.copy(debt = newDebt))
        if (currentCustomer.value?.id == customerId) {
            currentCustomer.value = findCustomer(customerId)
        }

        // If linked to order, update paid amount
        if (orderId != null) {
            val idx = orders.indexOfFirst { it.id == orderId }
            if (idx >= 0) {
                val o = orders[idx]
                orders[idx] = o.copy(paidAmount = (o.paidAmount + amount).coerceAtMost(o.totalAmount))
            }
        }
    }

    // ---------- Reports ----------
    fun getSalesSummary(period: String): SalesSummary {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        val filtered = when (period) {
            "روزانه" -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                orders.filter { it.createdAt >= start }
            }
            "هفتگی" -> {
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val start = cal.timeInMillis
                orders.filter { it.createdAt >= start }
            }
            "ماهانه" -> {
                cal.timeInMillis = now
                cal.add(Calendar.MONTH, -1)
                val start = cal.timeInMillis
                orders.filter { it.createdAt >= start }
            }
            else -> orders.toList()
        }

        val totalSales = filtered.sumOf { it.totalAmount }
        val totalPaid = filtered.sumOf { it.paidAmount }
        val totalDebt = filtered.sumOf { it.remaining }

        return SalesSummary(
            period = period,
            totalSales = totalSales,
            totalPaid = totalPaid,
            totalDebt = totalDebt,
            orderCount = filtered.size
        )
    }

    fun totalCustomerDebt(): Long = customers.sumOf { it.debt }

    fun formatPrice(price: Long): String {
        return "%,d".format(price).replace(',', '٬')
    }

    // Seed minimal empty state - no sample codes shown to user
    fun ensureSeeded() {
        // Intentionally empty start. Admin adds everything.
        // Optional: one default category food can be added by admin.
    }
}
