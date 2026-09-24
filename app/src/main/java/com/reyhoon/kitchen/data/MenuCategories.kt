package com.reyhoon.kitchen.data

/**
 * دسته‌بندی‌های ثابت منوی ریحون — با ایموجی برای ظاهر یکدست و شیک
 */
object MenuCategories {
    data class Cat(val name: String, val emoji: String, val colorHex: Long)

    val DEFS = listOf(
        Cat("چلوها", "🍚", 0xFF2E7D32),
        Cat("کباب‌ها", "🍢", 0xFFBF360C),
        Cat("خورشت‌ها", "🍲", 0xFFE65100),
        Cat("نوشیدنی‌ها", "🥤", 0xFF1565C0),
        Cat("مخلفات", "🥗", 0xFF6A1B9A),
        Cat("پیش‌غذا", "🥙", 0xFF00838F),
        Cat("دسر", "🍮", 0xFFC62828),
        Cat("سرویس ویژه", "⭐", 0xFFF9A825),
        Cat("عمومی", "🍽️", 0xFF546E7A)
    )

    val ALL = DEFS.map { it.name }

    fun emoji(category: String): String {
        val c = category.trim()
        return DEFS.find { it.name == c }?.emoji
            ?: when {
                c.contains("چلو") -> "🍚"
                c.contains("کباب") -> "🍢"
                c.contains("خورشت") -> "🍲"
                c.contains("نوشید") -> "🥤"
                c.contains("مخلف") -> "🥗"
                c.contains("پیش") -> "🥙"
                c.contains("دسر") -> "🍮"
                c.contains("ویژه") -> "⭐"
                else -> "🍽️"
            }
    }

    fun label(category: String): String {
        val e = emoji(category)
        val name = category.ifBlank { "عمومی" }
        return "$e  $name"
    }
}
