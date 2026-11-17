package com.markvoronin.immichswipe.domain.model

/** The permissions an API key needs for every feature of the app, as listed in the README. */
object ApiKeyPermissions {
    val REQUIRED = listOf(
        "asset.read",
        "asset.statistics",
        "asset.download",
        "asset.update",
        "asset.edit.create",
        "asset.delete",
        "asset.view",
        "album.read",
        "album.update",
        "albumAsset.create",
        "duplicate.read",
        "user.read",
        "userProfileImage.read"
    )

    /** What [REQUIRED] lists and [granted] lacks. A key created with all permissions lacks nothing. */
    fun missing(granted: List<String>): List<String> =
        if ("all" in granted) emptyList() else REQUIRED.filter { it !in granted }
}
