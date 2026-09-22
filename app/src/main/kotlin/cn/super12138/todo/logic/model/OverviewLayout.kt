package cn.super12138.todo.logic.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class OverviewCard { Overdue, Today, Upcoming, All, Pending, Completed }

@Serializable
enum class OverviewCardSize { Compact, Expanded }

@Serializable
data class OverviewCardConfig(
    val card: OverviewCard,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val size: OverviewCardSize = OverviewCardSize.Compact
)

@Serializable
data class OverviewLayout(val cards: List<OverviewCardConfig> = defaultOverviewCards()) {
    fun normalized(): OverviewLayout {
        val unique = cards.distinctBy { it.card }
        return copy(cards = unique + defaultOverviewCards().filter { default ->
            unique.none { it.card == default.card }
        })
    }

    fun configure(card: OverviewCard, change: (OverviewCardConfig) -> OverviewCardConfig) =
        copy(cards = cards.map { if (it.card == card) change(it) else it })

    /** Locked cards keep their absolute slots, even when other cards move past them. */
    fun move(card: OverviewCard, target: OverviewCard): OverviewLayout {
        val movable = cards.filterNot { it.locked }.toMutableList()
        val from = movable.indexOfFirst { it.card == card }
        val to = movable.indexOfFirst { it.card == target }
        if (from < 0 || to < 0 || from == to) return this
        movable.add(to, movable.removeAt(from))
        var index = 0
        return copy(cards = cards.map { if (it.locked) it else movable[index++] })
    }

    fun neighbor(card: OverviewCard, direction: Int): OverviewCard? {
        val movable = cards.filterNot { it.locked }
        val index = movable.indexOfFirst { it.card == card }
        return if (index < 0) null else movable.getOrNull(index + direction)?.card
    }

    fun encode(): String = json.encodeToString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun decode(value: String?): OverviewLayout = value?.let {
            runCatching { json.decodeFromString<OverviewLayout>(it).normalized() }.getOrNull()
        } ?: OverviewLayout()
    }
}

private fun defaultOverviewCards() = OverviewCard.entries.map {
    OverviewCardConfig(it, size = if (it == OverviewCard.All || it == OverviewCard.Pending ||
        it == OverviewCard.Completed) OverviewCardSize.Compact else OverviewCardSize.Expanded)
}
