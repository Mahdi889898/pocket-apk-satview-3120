package com.example.data.model

enum class ChannelCategory(val labelFa: String) {
    ALL("همه"),
    GENERAL("عمومی"),
    NEWS("خبری"),
    SPORTS("ورزشی"),
    MOVIES("فیلم و سریال"),
    DOCUMENTARY("مستند"),
    KIDS("کودک و نوجوان"),
    MUSIC("موسیقی"),
    DEMO("پخش آزمایشی آزاد");

    companion object {
        val editableCategories: List<ChannelCategory> = entries.filter { it != ALL }

        fun fromRawGroup(raw: String?): ChannelCategory {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return when {
                normalized.isBlank() -> GENERAL
                normalized.contains("news") || normalized.contains("خبر") -> NEWS
                normalized.contains("sport") || normalized.contains("ورزش") -> SPORTS
                normalized.contains("movie") || normalized.contains("cinema") ||
                    normalized.contains("series") || normalized.contains("entertainment") ||
                    normalized.contains("فیلم") || normalized.contains("سریال") -> MOVIES
                normalized.contains("doc") || normalized.contains("science") ||
                    normalized.contains("nature") || normalized.contains("history") ||
                    normalized.contains("مستند") -> DOCUMENTARY
                normalized.contains("kid") || normalized.contains("cart") ||
                    normalized.contains("anim") || normalized.contains("family") ||
                    normalized.contains("کودک") -> KIDS
                normalized.contains("music") || normalized.contains("موسیقی") -> MUSIC
                normalized.contains("demo") || normalized.contains("آزمایش") -> DEMO
                else -> GENERAL
            }
        }
    }
}
