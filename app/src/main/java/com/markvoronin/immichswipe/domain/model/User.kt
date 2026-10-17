package com.markvoronin.immichswipe.domain.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val email: String,
    val name: String?,
    val profileImagePath: String? = null,
    val avatarColor: String? = null,
    @SerializedName("quotaUsageInBytes")
    val quotaUsageInBytes: Long? = null,
    @SerializedName("quotaSizeInBytes")
    val quotaSizeInBytes: Long? = null
)
