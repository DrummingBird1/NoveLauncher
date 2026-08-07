package com.ailauncher.app.domain.models

import androidx.annotation.StringRes
import com.ailauncher.app.R
import kotlinx.serialization.Serializable

/**
 * v9: [displayNameRes] replaces the previous hardcoded Hebrew `displayName: String`.
 * Resolve via `context.getString(category.displayNameRes)` or `stringResource(...)`.
 *
 * Note: @Serializable enums in kotlinx.serialization persist by enum constant name
 * (e.g. "SOCIAL"), not by the constructor params. So swapping the param type does
 * NOT break stored backups or settings.
 */
@Serializable
enum class AppCategory(
    @StringRes val displayNameRes: Int,
    val iconName: String,
    val playStoreCategories: Set<String>
) {
    SOCIAL(R.string.app_category_social, "people", setOf("SOCIAL", "COMMUNICATION", "DATING")),
    WORK(R.string.app_category_work, "work", setOf("BUSINESS", "PRODUCTIVITY", "FINANCE")),
    ENTERTAINMENT(R.string.app_category_entertainment, "movie", setOf("ENTERTAINMENT", "MUSIC_AND_AUDIO", "VIDEO_PLAYERS", "BOOKS_AND_REFERENCE", "NEWS_AND_MAGAZINES")),
    GAMES(R.string.app_category_games, "sports_esports", setOf("GAME", "GAME_ACTION", "GAME_ADVENTURE", "GAME_ARCADE",
        "GAME_BOARD", "GAME_CARD", "GAME_CASINO", "GAME_CASUAL", "GAME_EDUCATIONAL",
        "GAME_MUSIC", "GAME_PUZZLE", "GAME_RACING", "GAME_ROLE_PLAYING",
        "GAME_SIMULATION", "GAME_SPORTS", "GAME_STRATEGY", "GAME_TRIVIA", "GAME_WORD")),
    UTILITIES(R.string.app_category_utilities, "build", setOf("TOOLS", "WEATHER", "MAPS_AND_NAVIGATION", "TRAVEL_AND_LOCAL")),
    HEALTH(R.string.app_category_health, "favorite", setOf("HEALTH_AND_FITNESS", "MEDICAL", "FOOD_AND_DRINK")),
    EDUCATION(R.string.app_category_education, "school", setOf("EDUCATION", "LIBRARIES_AND_DEMO")),
    SHOPPING(R.string.app_category_shopping, "shopping_cart", setOf("SHOPPING", "LIFESTYLE")),
    PHOTOGRAPHY(R.string.app_category_photography, "camera_alt", setOf("PHOTOGRAPHY", "ART_AND_DESIGN")),
    SYSTEM(R.string.app_category_system, "settings", emptySet()),
    UNCATEGORIZED(R.string.app_category_uncategorized, "apps", emptySet());

    companion object {
        private val playCategoryMap: Map<String, AppCategory> by lazy {
            val map = mutableMapOf<String, AppCategory>()
            entries.forEach { cat ->
                cat.playStoreCategories.forEach { key -> map[key] = cat }
            }
            map
        }

        fun fromPlayCategory(playCategory: String?): AppCategory {
            if (playCategory == null) return UNCATEGORIZED
            return playCategoryMap[playCategory.uppercase()] ?: UNCATEGORIZED
        }
    }
}
