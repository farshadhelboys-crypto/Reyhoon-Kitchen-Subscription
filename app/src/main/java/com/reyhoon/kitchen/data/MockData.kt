package com.reyhoon.kitchen.data

object MockData {

    private val subscriptions = mapOf(
        "REYHOON123" to Subscription(
            code = "REYHOON123",
            customerName = "علی محمدی",
            address = Address(
                street = "خیابان ولیعصر، پلاک ۴۵، واحد ۲",
                city = "تهران",
                postalCode = "1435678901",
                phone = "09121234567",
                notes = "زنگ آپارتمان ۲"
            ),
            planName = "اشتراک هفتگی کامل",
            startDate = "۱۴۰۴/۰۶/۰۱",
            endDate = "۱۴۰۴/۰۶/۳۰",
            isActive = true
        ),
        "REYHOON456" to Subscription(
            code = "REYHOON456",
            customerName = "مریم احمدی",
            address = Address(
                street = "بلوار کشاورز، کوچه گلستان، پلاک ۱۲",
                city = "اصفهان",
                postalCode = "8174567890",
                phone = "09134567890",
                notes = ""
            ),
            planName = "اشتراک روزانه ناهار",
            startDate = "۱۴۰۴/۰۶/۱۰",
            endDate = "۱۴۰۴/۰۷/۱۰",
            isActive = true
        ),
        "TEST001" to Subscription(
            code = "TEST001",
            customerName = "کاربر تست",
            address = Address(
                street = "خیابان آزادی، نبش کوچه ۵",
                city = "شیراز",
                postalCode = "7134567890",
                phone = "09170000000",
                notes = "تحویل در لابی"
            ),
            planName = "اشتراک آزمایشی",
            startDate = "۱۴۰۴/۰۱/۰۱",
            endDate = "۱۴۰۴/۱۲/۲۹",
            isActive = true
        )
    )

    val menuItems = listOf(
        FoodItem(1, "چلوکباب کوبیده", "۲ سیخ کباب کوبیده با برنج ایرانی و گوجه کبابی", 280000, "کباب"),
        FoodItem(2, "جوجه کباب", "جوجه کباب زعفرانی با برنج و سالاد فصل", 250000, "کباب"),
        FoodItem(3, "قورمه سبزی", "قورمه سبزی سنتی با گوشت گوسفندی و لوبیا", 180000, "خورشت"),
        FoodItem(4, "قیمه نثار", "قیمه نثار مخصوص با خلال بادام و زرشک", 220000, "خورشت"),
        FoodItem(5, "زرشک پلو با مرغ", "زرشک پلو مجلسی با ران مرغ سرخ شده", 200000, "پلو"),
        FoodItem(6, "عدس پلو", "عدس پلو با گوشت چرخ کرده و کشمش", 150000, "پلو"),
        FoodItem(7, "سالاد فصل", "سالاد تازه فصل با سس مخصوص ریحون", 80000, "پیش‌غذا"),
        FoodItem(8, "ماست و خیار", "ماست چکیده با خیار و نعنا", 50000, "پیش‌غذا"),
        FoodItem(9, "دوغ سنتی", "دوغ خانگی گازدار", 40000, "نوشیدنی"),
        FoodItem(10, "شربت بهارنارنج", "شربت طبیعی بهارنارنج", 45000, "نوشیدنی")
    )

    fun findSubscription(code: String): Subscription? {
        return subscriptions[code.trim().uppercase()]
    }

    fun getMenuByCategory(): Map<String, List<FoodItem>> {
        return menuItems.groupBy { it.category }
    }
}
