package cn.super12138.todo.logic

/** Semantic tag accents; text always uses the current Material foreground role. */
val tagColorPalette = listOf(
    0xFF3877B8.toInt(), 0xFFB15B35.toInt(), 0xFF39846A.toInt(), 0xFF945EB5.toInt(),
    0xFFBD526F.toInt(), 0xFF827326.toInt(), 0xFF367F91.toInt(), 0xFF7271C1.toInt(),
    0xFFB86C96.toInt(), 0xFF66803F.toInt(), 0xFFA26E43.toInt(), 0xFF687B91.toInt()
)

internal fun assignTagColors(tags: List<String>, existing: Map<String, Int>): Map<String, Int> {
    val colors = existing.toMutableMap()
    tags.forEach { tag ->
        if (tag !in colors) colors[tag] = nextTagColor(colors.values.toSet())
    }
    return colors
}

fun nextTagColor(used: Set<Int>): Int {
    tagColorPalette.firstOrNull { it !in used }?.let { return it }
    // Once the curated palette is full, continue with unused opaque RGB values.
    var rgb = 0x527A91
    while ((0xFF000000.toInt() or rgb) in used) rgb = (rgb + 0x314159) and 0xFFFFFF
    return 0xFF000000.toInt() or rgb
}
