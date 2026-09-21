package cn.super12138.todo.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class TagsTest {
    @Test fun catalogIncludesPresetsAndUsedTagsWithoutBlanksOrExactDuplicates() {
        assertEquals(
            listOf("Home", "Personal", "Work"),
            tagCatalog(listOf("Work", "", "Home"), listOf("Work", "Personal", "  "))
        )
    }

    @Test fun catalogPreservesLegacyIdentitiesForExactWidgetFilters() {
        assertEquals(3, tagCatalog(listOf("Work"), listOf("work", " Work ")).size)
    }

    @Test fun typingReusesExistingSpellingAndIgnoresSurroundingWhitespace() {
        assertEquals("Work", resolveTag("  wOrK  ", listOf("Home", "Work")))
        assertEquals("New tag", resolveTag("  New tag  ", listOf("Work")))
        assertEquals("", resolveTag("  ", listOf("Work")))
    }

    @Test fun anExactSelectionPreservesExistingCaseVariants() {
        assertEquals("work", resolveTag("work", listOf("Work", "work")))
        assertEquals(" Work ", resolveTag(" Work ", listOf("Work", " Work ")))
    }

    @Test fun inputMatchesLegacyWhitespaceWithoutCreatingAnotherIdentity() {
        assertEquals(" Work ", resolveTag("work", listOf(" Work ")))
    }

    @Test fun matchingIgnoresCaseAndPrioritizesPrefixesBeforeSubstrings() {
        assertEquals(
            listOf("Work", "Homework"),
            matchingTags(" wo ", listOf("Home", "Homework", "Work"))
        )
        assertEquals(emptyList<String>(), matchingTags("new", listOf("Work")))
    }

    @Test fun anEmptyQueryShowsAllTags() {
        val tags = listOf("Home", "Work")
        assertEquals(tags, matchingTags("", tags))
    }
}
