package com.markvoronin.immichswipe.data.local.model

import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity

data class DatabaseExport(
    val swipeDecisions: List<SwipeDecisionEntity>? = emptyList(),
    val syncHistory: List<SyncHistoryEntity>? = emptyList(),
    val exportDate: Long = System.currentTimeMillis(),
    val scope: String? = null,
    val userId: String? = null
)
