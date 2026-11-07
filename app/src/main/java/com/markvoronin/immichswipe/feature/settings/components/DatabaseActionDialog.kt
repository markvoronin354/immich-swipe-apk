package com.markvoronin.immichswipe.feature.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.settings.DatabaseAction
import com.markvoronin.immichswipe.feature.settings.DatabaseScope

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DatabaseActionDialog(
    action: DatabaseAction,
    scope: DatabaseScope,
    userName: String,
    onScopeChange: (DatabaseAction, DatabaseScope) -> Unit,
    onConfirm: (DatabaseAction, DatabaseScope) -> Unit,
    onDismiss: () -> Unit
) {
    val isDelete = action == DatabaseAction.DELETE

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            // Same shapes and colors as the matching settings rows: gem for delete, 6-sided cookie otherwise.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
                        if (isDelete) MaterialShapes.Gem.toShape() else MaterialShapes.Cookie6Sided.toShape()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (action) {
                        DatabaseAction.DELETE -> Icons.Default.DeleteForever
                        DatabaseAction.EXPORT -> Icons.Default.FileUpload
                        DatabaseAction.IMPORT -> Icons.Default.FileDownload
                    },
                    contentDescription = null,
                    tint = if (isDelete) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        },
        title = {
            val titleRes = when (action) {
                DatabaseAction.DELETE -> R.string.settings_db_confirm_delete_title
                DatabaseAction.EXPORT -> R.string.settings_db_confirm_export_title
                DatabaseAction.IMPORT -> R.string.settings_db_confirm_import_title
            }
            Text(
                text = stringResource(titleRes),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                val msgRes = when (action) {
                    DatabaseAction.DELETE -> R.string.settings_db_confirm_delete_msg
                    DatabaseAction.IMPORT -> R.string.settings_db_confirm_import_msg
                    DatabaseAction.EXPORT -> null
                }
                msgRes?.let {
                    Text(stringResource(it))
                    Spacer(Modifier.height(16.dp))
                }

                if (action != DatabaseAction.IMPORT) {
                    Text(
                        text = stringResource(R.string.settings_db_scope_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.selectableGroup()) {
                        ScopeOption(
                            text = stringResource(R.string.settings_db_scope_user, userName),
                            selected = scope == DatabaseScope.USER,
                            onClick = { onScopeChange(action, DatabaseScope.USER) }
                        )
                        ScopeOption(
                            text = stringResource(R.string.settings_db_scope_all),
                            selected = scope == DatabaseScope.ALL,
                            onClick = { onScopeChange(action, DatabaseScope.ALL) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(action, scope) },
                colors = if (isDelete) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                }
            ) {
                Text(
                    stringResource(
                        when (action) {
                            DatabaseAction.DELETE -> R.string.common_delete
                            DatabaseAction.EXPORT -> R.string.common_export
                            DatabaseAction.IMPORT -> R.string.common_import
                        }
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

/** Rounded row like [com.markvoronin.immichswipe.feature.swipe.components.SortPopupItem]: the selected one gets a tonal container, and the ripple is clipped to the shape. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScopeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "scopeOptionContainer"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
