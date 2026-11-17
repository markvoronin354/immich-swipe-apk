package com.markvoronin.immichswipe.domain.model

/** The Immich release reported by `GET /api/server/version`. */
data class ServerVersion(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0
) : Comparable<ServerVersion> {

    override fun compareTo(other: ServerVersion): Int =
        compareValuesBy(this, other, ServerVersion::major, ServerVersion::minor, ServerVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /** First release with `PUT /api/assets/{id}/edits`, which saves a photo's rotation. */
        val ROTATION_EDITS = ServerVersion(2, 5, 0)
    }
}
