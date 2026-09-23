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

    // ---------- Menu ----------
    fun addFood(item: FoodItem) { menuItems.add(item) }

    fun updateFood(item: FoodItem) {
        val idx = menuItems.indexOfFirst { it.id == item.id }
        if (idx >= 0) menuItems[idx] = item
    }

    fun deleteFood(id: String) { menuItems.removeAll { it.id == id } }

    fun getMenuByCategory(): Map<String, List<FoodItem>> {
        return menuItems.filter { it.isAvailable }.groupBy { it.category }
    }

    // ---------- Customers ----------
    fun addCustomer(customer: Customer) { customers.add(customer) }

    fun updateCustomer(customer: Customer) {
        val idx = customers.indexOfFirst { it.id == customer.id }
        if (idx >= 0) customers[idx] = customer
    }

    fun deleteCustomer(id: String) { customers.removeAll { it.id == id } }

    fun findByCode(code: String): Customer? {
        return customers.find { it.subscriptionCode?.equals(code.trim(), ignoreCase = true) == true }
    }

    fun findCustomer(id: String): Customer? = customers.find { it.id == id }

    private fun syncCurrentCustomer(id: String) {
        if (currentCustomer.value?.id == id) {
            currentCustomer.value = findCustomer(id)
        }
    }

    /** محاسبه مجدد بدهی مشتری از روی سفارش‌های پرداخت‌نشده */
    fun recalculateDebt(customerId: String): Long {
        return orders
            .filter { it.customerId == customerId }
            .sumOf { it.remaining }
    }

    // ---------- Orders & Accounting ----------
    /**
     * ثبت سفارش با پشتیبانی از:
     * - کسر اعتبار قبلی مشتری از مبلغ غذا
     * - پرداخت بیشتر از مبلغ → تبدیل به اعتبار برای سفارش بعدی
     * - پرداخت کمتر → بدهی
     */
    fun createOrder(
        customer: Customer,
        items: List<OrderItem>,
        paidNow: Long = 0L,
        note: String = ""
    ): OrderResult {
        val total = items.sumOf { it.total }
        var credit = customer.credit
        var debt = customer.debt

        // ۱) اعتبار قبلی را از مبلغ سفارش کسر کن
        val creditApplied = minOf(credit, total)
        credit -= creditApplied
        val afterCredit = total - creditApplied

        // ۲) پرداخت نقدی
        val cashUsed = minOf(paidNow.coerceAtLeast(0), afterCredit)
        val overpay = (paidNow.coerceAtLeast(0) - afterCredit).coerceAtLeast(0)

        // اضافه‌پرداخت → اعتبار جدید
        if (overpay > 0) {
            credit += overpay
        }

        // کمبود پرداخت → بدهی
        val shortfall = afterCredit - cashUsed
        if (shortfall > 0) {
            debt += shortfall
        }

        val paidOnOrder = creditApplied + cashUsed

        val order = Order(
            customerId = customer.id,
            customerName = customer.name,
            items = items,
            totalAmount = total,
            paidAmount = paidOnOrder,
            creditApplied = creditApplied,
            note = buildString {
                if (note.isNotBlank()) append(note)
                if (creditApplied > 0) {
                    if (isNotEmpty()) append(" | ")
                    append("کسر از اعتبار قبلی: ${formatPrice(creditApplied)} تومان")
                }
                if (overpay > 0) {
                    if (isNotEmpty()) append(" | ")
                    append("اعتبار جدید بابت اضافه‌پرداخت: ${formatPrice(overpay)} تومان")
                }
            }
        )
        orders.add(0, order)

        if (cashUsed > 0 || overpay > 0) {
            payments.add(
                Payment(
                    customerId = customer.id,
                    orderId = order.id,
                    amount = cashUsed + overpay,
                    note = when {
                        overpay > 0 && cashUsed > 0 -> "پرداخت سفارش + اعتبار اضافه‌پرداخت"
                        overpay > 0 -> "فقط اعتبار (اضافه‌پرداخت)"
                        else -> "پرداخت هنگام ثبت سفارش"
                    }
                )
            )
        }

        val updated = customer.copy(debt = debt, credit = credit)
        updateCustomer(updated)
        syncCurrentCustomer(customer.id)

        val message = buildString {
            append("سفارش ثبت شد. جمع غذا: ${formatPrice(total)} تومان")
            if (creditApplied > 0) {
                append("\n✓ مبلغ ${formatPrice(creditApplied)} تومان بابت اعتبار قبلی شما از مبلغ غذا کسر شد.")
            }
            if (overpay > 0) {
                append("\n✓ مبلغ ${formatPrice(overpay)} تومان اضافه‌پرداخت به‌عنوان اعتبار برای سفارش بعدی ذخیره شد.")
            }
            if (shortfall > 0) {
                append("\n⚠ بدهی جدید: ${formatPrice(shortfall)} تومان")
            }
            if (credit > 0) {
                append("\nاعتبار باقی‌مانده مشتری: ${formatPrice(credit)} تومان")
            }
            if (debt > 0) {
                append("\nبدهی کل مشتری: ${formatPrice(debt)} تومان")
            }
        }

        return OrderResult(order, creditApplied, credit, debt, message)
    }

    /**
     * تسویه بدهی:
     * - ابتدا روی سفارش‌های پرداخت‌نشده (قدیمی‌تر اول) اعمال می‌شود
     * - در نتیجه «بدهی دوره» هم کم می‌شود
     * - اگر مبلغ بیشتر از بدهی باشد → اعتبار مشتری افزایش می‌یابد
     */
    fun recordPayment(customerId: String, amount: Long, orderId: String? = null, note: String = "") {
        if (amount <= 0) return
        val customer = findCustomer(customerId) ?: return
        var remainingPay = amount

        payments.add(
            Payment(
                customerId = customerId,
                orderId = orderId,
                amount = amount,
                note = note.ifBlank { "تسویه بدهی" }
            )
        )

        if (orderId != null) {
            val idx = orders.indexOfFirst { it.id == orderId }
            if (idx >= 0) {
                val o = orders[idx]
                val canPay = minOf(remainingPay, o.remaining)
                if (canPay > 0) {
                    orders[idx] = o.copy(paidAmount = o.paidAmount + canPay)
                    remainingPay -= canPay
                }
            }
        } else {
            // اعمال روی همه سفارش‌های باز (از قدیمی به جدید)
            val openOrders = orders
                .mapIndexed { index, order -> index to order }
                .filter { it.second.customerId == customerId && it.second.remaining > 0 }
                .sortedBy { it.second.createdAt }

            for ((idx, o) in openOrders) {
                if (remainingPay <= 0) break
                val canPay = minOf(remainingPay, o.remaining)
                orders[idx] = o.copy(paidAmount = o.paidAmount + canPay)
                remainingPay -= canPay
            }
        }

        // بدهی = مجموع remaining سفارش‌ها
        val newDebt = recalculateDebt(customerId)
        // باقی‌مانده پرداخت → اعتبار
        val newCredit = customer.credit + remainingPay

        updateCustomer(customer.copy(debt = newDebt, credit = newCredit))
        syncCurrentCustomer(customerId)
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
                orders.filter { it.createdAt >= cal.timeInMillis }
            }
            "هفتگی" -> {
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_YEAR, -7)
                orders.filter { it.createdAt >= cal.timeInMillis }
            }
            "ماهانه" -> {
                cal.timeInMillis = now
                cal.add(Calendar.MONTH, -1)
                orders.filter { it.createdAt >= cal.timeInMillis }
            }
            else -> orders.toList()
        }

        return SalesSummary(
            period = period,
            totalSales = filtered.sumOf { it.totalAmount },
            totalPaid = filtered.sumOf { it.paidAmount },
            totalDebt = filtered.sumOf { it.remaining },
            orderCount = filtered.size
        )
    }

    fun totalCustomerDebt(): Long = customers.sumOf { it.debt }
    fun totalCustomerCredit(): Long = customers.sumOf { it.credit }

    fun formatPrice(price: Long): String {
        return "%,d".format(price).replace(',', '٬')
    }
}
