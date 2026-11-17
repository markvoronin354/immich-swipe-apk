package com.markvoronin.immichswipe.domain.model

// The last two fields stay nullable on purpose: Gson builds this class without running the Kotlin
// defaults, so a field the server leaves out arrives as null whatever default is declared here.
data class Album(
    val id: String,
    val albumName: String,
    val description: String? = null,
    val assetCount: Int,
    val albumThumbnailAssetId: String?,
    val ownerId: String? = null,
    val albumUsers: List<AlbumUser>? = null
) {
    val isVirtual: Boolean
        get() = id == VIRTUAL_ALL_ID ||
                id == VIRTUAL_ORPHANS_ID ||
                id == VIRTUAL_DUPLICATES_ID

    /**
     * Whether [userId] can add photos here: the owner, or a member with the editor role. A server that
     * doesn't report members (an older Immich) gives no answer, which counts as yes so nothing is hidden.
     */
    fun canAddAssets(userId: String): Boolean {
        if (ownerId == null || ownerId == userId) return true
        val members = albumUsers ?: return true
        return members.any { it.user?.id == userId && it.role.equals("editor", ignoreCase = true) }
    }

    companion object {
        const val VIRTUAL_ALL_ID = "virtual_all_assets"
        const val VIRTUAL_ORPHANS_ID = "virtual_orphans"
        const val VIRTUAL_DUPLICATES_ID = "virtual_duplicates"
    }
}

data class AlbumUser(
    val user: AlbumMember? = null,
    val role: String? = null
)

data class AlbumMember(
    val id: String? = null
)
