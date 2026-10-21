package com.markvoronin.immichswipe.domain.model

/** What the UI needs to show a saved account. Deliberately has no API key. */
data class AccountSummary(
    val userId: String,
    val baseUrl: String,
    val userName: String?,
    val userEmail: String,
    val avatarColor: String?,
    val lastActive: Long
)
