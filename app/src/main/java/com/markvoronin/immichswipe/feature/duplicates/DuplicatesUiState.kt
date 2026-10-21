package com.markvoronin.immichswipe.feature.duplicates

import com.markvoronin.immichswipe.domain.model.Asset

data class DuplicatesUiState(
    val isLoading: Boolean = true,
    val clusters: List<DuplicateClusterUiModel> = emptyList(),
    val decisions: Map<String, DuplicateDecision> = emptyMap(), // assetId -> Decision
    val favorites: Map<String, Boolean> = emptyMap(), // assetId -> isFavorite
    val selectedAutoSelectMode: AutoSelectMode? = null,
    val sortOrder: DuplicatesSortOrder = DuplicatesSortOrder.CLUSTER_SIZE,
    val isSyncing: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val baseUrl: String = "",
    val error: String? = null,
    val hasCompletedDuplicatesTutorial: Boolean = true,
    val isAnalyzingAi: Boolean = false,
    val aiProgress: Pair<Int, Int>? = null
) {
    fun isFavorite(asset: Asset): Boolean {
        return favorites[asset.id] ?: asset.isFavorite
    }

}

data class DuplicateClusterUiModel(
    val clusterId: String,
    val assets: List<Asset>
)

enum class DuplicatesSortOrder {
    CLUSTER_SIZE,
    PHOTOS_FIRST,
    VIDEOS_FIRST
}

enum class AutoSelectMode {
    KEEP_LARGEST,
    KEEP_SMALLEST,
    KEEP_BEST_SHOT_AUTO,
    KEEP_BEST_SHOT_SINGLE,
    KEEP_BEST_SHOT_TOP_2,
    KEEP_BEST_SHOT_TOP_3
}

enum class DuplicateDecision {
    KEEP,
    DELETE,
    NONE
}
