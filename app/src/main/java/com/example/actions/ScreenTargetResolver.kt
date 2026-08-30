package com.example.actions

import android.graphics.Rect
import java.util.Locale

enum class TargetCategory {
    VIDEO,
    BUTTON,
    TEXT,
    LINK,
    ANY
}

enum class SpatialPosition {
    TOP,
    CENTER,
    BOTTOM,
    FIRST,
    SECOND,
    THIRD,
    LAST,
    UNSPECIFIED
}

sealed class TargetResolutionResult {
    data class SingleMatch(
        val element: UiElementNode,
        val matchType: String,
        val confidence: Float
    ) : TargetResolutionResult()

    data class AmbiguousMatches(
        val candidates: List<UiElementNode>,
        val questionToUser: String
    ) : TargetResolutionResult()

    data class NoMatchFound(
        val reason: String
    ) : TargetResolutionResult()
}

class ScreenTargetResolver {

    /**
     * Resolves a target UI element on the screen based on natural language query and spatial hints.
     */
    fun resolveTarget(
        query: String,
        elements: List<UiElementNode>,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): TargetResolutionResult {
        if (elements.isEmpty()) {
            return TargetResolutionResult.NoMatchFound("No visible elements detected on screen.")
        }

        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val category = detectCategory(cleanQuery)
        val spatial = detectSpatialPosition(cleanQuery)

        // 1. Filter elements by category
        val categoryFiltered = filterByCategory(elements, category)

        // 2. If spatial position is specified (e.g. "center video", "second video", "top button")
        if (spatial != SpatialPosition.UNSPECIFIED) {
            val spatialResult = resolveBySpatial(categoryFiltered.ifEmpty { elements }, spatial, screenHeight)
            if (spatialResult != null) {
                return spatialResult
            }
        }

        // 3. Match by text/title (Exact, Partial, Similar)
        val textKeywords = extractCoreKeywords(cleanQuery)
        val rankedMatches = mutableListOf<RankedMatch>()

        for (el in categoryFiltered.ifEmpty { elements }) {
            val label = (el.text + " " + el.contentDescription).trim().lowercase(Locale.ROOT)
            if (label.isBlank()) continue

            // Exact match
            if (label == cleanQuery || (textKeywords.isNotBlank() && label == textKeywords)) {
                rankedMatches.add(RankedMatch(el, 1.0f, "Exact Title Match"))
                continue
            }

            // Substring / partial match
            if (textKeywords.isNotBlank() && (label.contains(textKeywords) || textKeywords.contains(label))) {
                rankedMatches.add(RankedMatch(el, 0.85f, "Partial Title Match"))
                continue
            }

            // Keyword token overlap
            val tokens = textKeywords.split(" ").filter { it.length > 2 }
            if (tokens.isNotEmpty()) {
                val matchedCount = tokens.count { label.contains(it) }
                if (matchedCount > 0) {
                    val score = (matchedCount.toFloat() / tokens.size.toFloat()) * 0.75f
                    rankedMatches.add(RankedMatch(el, score, "Keyword Similarity"))
                }
            }
        }

        if (rankedMatches.isEmpty()) {
            return TargetResolutionResult.NoMatchFound("Could not find any element matching \"$query\".")
        }

        // Sort by confidence
        val sorted = rankedMatches.sortedByDescending { it.score }
        val topScore = sorted.first().score

        // Ambiguity check: if multiple items have very close high scores
        val highConfidenceMatches = sorted.filter { it.score >= topScore - 0.05f }
        if (highConfidenceMatches.size > 1 && topScore < 0.95f) {
            val candidateLabels = highConfidenceMatches.take(3).mapIndexed { idx, it ->
                val desc = (it.element.text.ifBlank { it.element.contentDescription }).take(40)
                "${idx + 1}. \"$desc\""
            }.joinToString("\n")

            return TargetResolutionResult.AmbiguousMatches(
                candidates = highConfidenceMatches.map { it.element },
                questionToUser = "I found multiple matching items:\n$candidateLabels\nWhich one would you like me to open?"
            )
        }

        val best = sorted.first()
        return TargetResolutionResult.SingleMatch(
            element = best.element,
            matchType = best.matchType,
            confidence = best.score
        )
    }

    private fun detectCategory(query: String): TargetCategory {
        return when {
            query.contains("video") || query.contains("clip") || query.contains("reel") || query.contains("stream") -> TargetCategory.VIDEO
            query.contains("button") || query.contains("btn") || query.contains("switch") || query.contains("tab") -> TargetCategory.BUTTON
            query.contains("link") || query.contains("url") || query.contains("href") -> TargetCategory.LINK
            query.contains("text") || query.contains("heading") || query.contains("title") || query.contains("label") -> TargetCategory.TEXT
            else -> TargetCategory.ANY
        }
    }

    private fun detectSpatialPosition(query: String): SpatialPosition {
        return when {
            query.contains("center") || query.contains("middle") || query.contains("beech") -> SpatialPosition.CENTER
            query.contains("top") || query.contains("upper") || query.contains("up") || query.contains("pehla") || query.contains("first") -> SpatialPosition.FIRST
            query.contains("second") || query.contains("2nd") || query.contains("dusra") -> SpatialPosition.SECOND
            query.contains("third") || query.contains("3rd") || query.contains("teesra") -> SpatialPosition.THIRD
            query.contains("bottom") || query.contains("lower") || query.contains("last") || query.contains("aakhri") -> SpatialPosition.BOTTOM
            else -> SpatialPosition.UNSPECIFIED
        }
    }

    private fun filterByCategory(elements: List<UiElementNode>, category: TargetCategory): List<UiElementNode> {
        if (category == TargetCategory.ANY) return elements
        return elements.filter { el ->
            val cls = el.className.lowercase(Locale.ROOT)
            val desc = (el.text + " " + el.contentDescription).lowercase(Locale.ROOT)
            when (category) {
                TargetCategory.VIDEO -> desc.contains("video") || cls.contains("video") || cls.contains("player") || cls.contains("card") || (el.bounds.height() > 200 && el.isClickable)
                TargetCategory.BUTTON -> el.isClickable || cls.contains("button") || cls.contains("imageview")
                TargetCategory.LINK -> cls.contains("link") || cls.contains("url") || desc.contains("http") || desc.contains("www")
                TargetCategory.TEXT -> el.text.isNotBlank() && cls.contains("textview")
                TargetCategory.ANY -> true
            }
        }
    }

    private fun resolveBySpatial(
        elements: List<UiElementNode>,
        spatial: SpatialPosition,
        screenHeight: Int
    ): TargetResolutionResult? {
        val clickableOrCards = elements.filter { it.isClickable || it.bounds.height() > 100 }
        if (clickableOrCards.isEmpty()) return null

        // Sort vertically top to bottom
        val sortedByY = clickableOrCards.sortedBy { it.bounds.top }

        return when (spatial) {
            SpatialPosition.FIRST, SpatialPosition.TOP -> {
                TargetResolutionResult.SingleMatch(sortedByY.first(), "Spatial: Top/First Item", 0.95f)
            }
            SpatialPosition.SECOND -> {
                if (sortedByY.size >= 2) {
                    TargetResolutionResult.SingleMatch(sortedByY[1], "Spatial: Second Item", 0.95f)
                } else {
                    TargetResolutionResult.SingleMatch(sortedByY.first(), "Spatial: Only Item Found", 0.8f)
                }
            }
            SpatialPosition.THIRD -> {
                if (sortedByY.size >= 3) {
                    TargetResolutionResult.SingleMatch(sortedByY[2], "Spatial: Third Item", 0.95f)
                } else {
                    TargetResolutionResult.SingleMatch(sortedByY.last(), "Spatial: Last Item", 0.8f)
                }
            }
            SpatialPosition.BOTTOM, SpatialPosition.LAST -> {
                TargetResolutionResult.SingleMatch(sortedByY.last(), "Spatial: Bottom/Last Item", 0.95f)
            }
            SpatialPosition.CENTER -> {
                val screenCenterY = screenHeight / 2
                val centerItem = sortedByY.minByOrNull { Math.abs(it.bounds.centerY() - screenCenterY) }
                if (centerItem != null) {
                    TargetResolutionResult.SingleMatch(centerItem, "Spatial: Center Item", 0.95f)
                } else null
            }
            SpatialPosition.UNSPECIFIED -> null
        }
    }

    private fun extractCoreKeywords(query: String): String {
        val stopWords = setOf(
            "open", "click", "tap", "play", "karo", "please", "the", "that", "this",
            "and", "a", "an", "on", "in", "to", "show", "me", "find", "select"
        )
        return query.split(" ")
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it !in stopWords && it.isNotBlank() }
            .joinToString(" ")
    }

    private data class RankedMatch(
        val element: UiElementNode,
        val score: Float,
        val matchType: String
    )
}
