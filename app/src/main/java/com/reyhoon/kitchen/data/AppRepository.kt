package com.reyhoon.kitchen.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.Calendar

object AppRepository {

    val customers: SnapshotStateList<Customer> = mutableStateListOf()
    val menuItems: SnapshotStateList<FoodItem> = mutableStateListOf()
    val orders: SnapshotStateList<Order> = mutableStateListOf()
    val payments: SnapshotStateList<Payment> = mutableStateListOf()

    val currentCustomer = mutableStateOf<Customer?>(null)
    val isAdmin = mutableStateOf(false)

    fun addFood(item: FoodItem) { menuItems.add(item) }

    fun updateFood(item: FoodItem) {
        val idx = menuItems.indexOfFirst { it.id == item.id }
        if (idx >= 0) menuItems[idx] = item
    }

    fun deleteFood(id: String) { menuItems.removeAll { it.id == id } }

    fun getMenuByCategory(): Map<String, List<FoodItem>> {
        return menuItems.filter { it.isAvailable }.groupBy { it.category }
    }

    fun addCustomer(customer: Customer) {
        val idx = customers.indexOfFirst { it.id == customer.id }
        if (idx >= 0) customers[idx] = customer else customers.add(customer)
    }

    /** سفارش مجازی برای بدهی قبلی مشتری (قابل تسویه مثل بقیه) */
    fun ensurePriorDebtOrder(customer: Customer) {
        if (customer.debt <= 0) return
        if (orders.any { it.customerId == customer.id && it.source == "prior_debt" }) return
        val amount = customer.debt
        val order = Order(
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            customerAddress = customer.address.fullAddress(),
            items = listOf(
                OrderItem(
                    foodId = "prior_debt",
                    foodName = "بدهی قبلی",
                    unitPrice = amount,
                    quantity = 1
                )
            ),
            totalAmount = amount,
            paidAmount = 0L,
            status = "delivered",
            note = "بدهی قبلی هنگام ثبت مشتری",
            source = "prior_debt"
        )
        orders.add(0, order)
    }

    fun updateCustomer(customer: Customer) {
        val idx = customers.indexOfFirst { it.id == customer.id }
        if (idx >= 0) customers[idx] = customer
    }

    fun deleteCustomer(id: String) {
        customers.removeAll { it.id == id }
        if (currentCustomer.value?.id == id) currentCustomer.value = null
    }

    fun findByCode(code: String): Customer? {
        return customers.find { it.subscriptionCode?.equals(code.trim(), ignoreCase = true) == true }
    }

    fun findCustomer(id: String): Customer? = customers.find { it.id == id }

    private fun syncCurrentCustomer(id: String) {
        if (currentCustomer.value?.id == id) {
            currentCustomer.value = findCustomer(id)
        }
    }

    /** بدهی = فقط جمع باقیمانده سفارش‌ها */
    fun recalculateDebt(customerId: String): Long {
        return orders
            .filter { it.customerId == customerId }
            .sumOf { it.remaining }
    }

    fun createOrder(
        customer: Customer,
        items: List<OrderItem>,
        paidNow: Long = 0L,
        note: String = ""
    ): OrderResult {
        val total = items.sumOf { it.total }
        var credit = customer.credit

        val creditApplied = minOf(credit, total)
        credit -= creditApplied
        val afterCredit = total - creditApplied

        val cash = paidNow.coerceAtLeast(0)
        val cashUsed = minOf(cash, afterCredit)
        val overpay = (cash - afterCredit).coerceAtLeast(0)
        if (overpay > 0) credit += overpay

        // paidAmount = اعتبار + نقد (برای محاسبه باقیمانده بدهی)
        // درآمد واقعی فقط از cashUsed است (cashReceived)
        val paidOnOrder = creditApplied + cashUsed
        val order = Order(
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            customerAddress = customer.address.fullAddress(),
            items = items,
            totalAmount = total,
            paidAmount = paidOnOrder,
            creditApplied = creditApplied,
            note = note,
            source = "kitchen"
        )
        orders.add(0, order)

        // فقط پول نقد واقعی در لیست پرداخت‌ها
        if (cashUsed > 0 || overpay > 0) {
            payments.add(
                0,
                Payment(
                    customerId = customer.id,
                    orderId = order.id,
                    amount = cashUsed + overpay,
                    note = note.ifBlank { "پرداخت هنگام سفارش" }
                )
            )
        }

        val debt = recalculateDebt(customer.id)
        val updated = customer.copy(debt = debt, credit = credit)
        updateCustomer(updated)
        syncCurrentCustomer(customer.id)

        val message = buildString {
            append("سفارش ثبت شد — جمع: ${formatPrice(total)}")
            if (creditApplied > 0) append("\nاعتبار کسرشده: ${formatPrice(creditApplied)}")
            if (cashUsed > 0) append("\nدریافتی نقد: ${formatPrice(cashUsed)}")
            if (overpay > 0) append("\nمازاد به اعتبار: ${formatPrice(overpay)}")
            if (debt > 0) append("\nبدهی کل: ${formatPrice(debt)}")
            else if (credit > 0) append("\nاعتبار مشتری: ${formatPrice(credit)}")
        }
        return OrderResult(order, creditApplied, credit, debt, message)
    }

    fun recordPayment(customerId: String, amount: Long, orderId: String? = null, note: String = "") {
        if (amount <= 0) return
        val customer = findCustomer(customerId) ?: return
        var remainingPay = amount

        if (orderId != null) {
            val idx = orders.indexOfFirst { it.id == orderId && it.customerId == customerId }
            if (idx >= 0) {
                val o = orders[idx]
                val pay = minOf(remainingPay, o.remaining)
                orders[idx] = o.copy(paidAmount = o.paidAmount + pay)
                remainingPay -= pay
            }
        }

        if (remainingPay > 0) {
            remainingPay = applyToOpenOrders(customerId, remainingPay)
        }

        payments.add(
            0,
            Payment(
                customerId = customerId,
                orderId = orderId,
                amount = amount,
                note = note.ifBlank { "تسویه بدهی" }
            )
        )

        val newDebt = recalculateDebt(customerId)
        val newCredit = customer.credit + remainingPay
        updateCustomer(customer.copy(debt = newDebt, credit = newCredit))
        syncCurrentCustomer(customerId)
    }

    private fun applyToOpenOrders(customerId: String, payAmount: Long): Long {
        var left = payAmount
        val open = orders
            .mapIndexed { index, order -> index to order }
            .filter { it.second.customerId == customerId && it.second.remaining > 0 }
            .sortedBy { it.second.createdAt }
        for ((idx, order) in open) {
            if (left <= 0) break
            val pay = minOf(left, order.remaining)
            orders[idx] = order.copy(paidAmount = order.paidAmount + pay)
            left -= pay
        }
        return left
    }

    fun clearAllLocal() {
        customers.clear()
        menuItems.clear()
        orders.clear()
        payments.clear()
        currentCustomer.value = null
    }

    fun getSalesSummary(period: String): SalesSummary {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val start: Long = when (period) {
            "day" -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "week" -> now - 7L * 24 * 60 * 60 * 1000
            "month" -> now - 30L * 24 * 60 * 60 * 1000
            else -> 0L
        }
        val filtered = orders.filter { it.createdAt >= start && it.source != "prior_debt" }
        val totalSales = filtered.sumOf { it.totalAmount }
        // درآمد واقعی = فقط نقد؛ اعتبار مشتری درآمد نیست
        val totalPaid = filtered.sumOf { it.cashReceived }
        return SalesSummary(
            period = period,
            totalSales = totalSales,
            totalPaid = totalPaid,
            totalDebt = totalCustomerDebt(),
            orderCount = filtered.size
        )
    }

    fun totalCustomerDebt(): Long = customers.sumOf { recalculateDebt(it.id) }
    fun totalCustomerCredit(): Long = customers.sumOf { it.credit }

    fun formatPrice(price: Long): String {
        return "%,d".format(price).replace(',', '،')
    }

    fun generateLocalCode(): String {
        return (100000..999999).random().toString()
    }
}
