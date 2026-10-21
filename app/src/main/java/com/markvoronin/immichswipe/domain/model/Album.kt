package com.markvoronin.immichswipe.domain.model

data class Album(
    val id: String,
    val albumName: String,
    val description: String? = null,
    val assetCount: Int,
    val albumThumbnailAssetId: String?
) {
    val isVirtual: Boolean
        get() = id == VIRTUAL_ALL_ID ||
                id == VIRTUAL_ORPHANS_ID ||
                id == VIRTUAL_DUPLICATES_ID ||
                albumName.equals("SKIP", ignoreCase = true)

    companion object {
        const val VIRTUAL_ALL_ID = "virtual_all_assets"
        const val VIRTUAL_ORPHANS_ID = "virtual_orphans"
        const val VIRTUAL_DUPLICATES_ID = "virtual_duplicates"
        /**
         * Single switch for the SKIP feature. While false: the SKIP album is hidden from the home
         * list, the swipe-down gesture and its indicator are off, and applying changes never reads,
         * creates or cleans up a SKIP album on the server.
         */
        const val IS_SKIP_FEATURE_ENABLED = false
    }
}
