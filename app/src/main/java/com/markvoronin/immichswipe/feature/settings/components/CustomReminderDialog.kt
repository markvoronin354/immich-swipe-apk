package com.markvoronin.immichswipe.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R

@Composable
fun CustomReminderDialog(
    initialMinDays: Long,
    initialMaxDays: Long,
    onConfirm: (minDays: Long, maxDays: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var minDays by remember { mutableLongStateOf(initialMinDays.coerceAtLeast(1L)) }
    var maxDays by remember { mutableLongStateOf(initialMaxDays.coerceAtLeast(minDays)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.settings_reminder_custom_dialog_title))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DayCounterRow(
                    label = stringResource(R.string.settings_reminder_custom_min_label),
                    count = minDays,
                    onCountChange = { newMin ->
                        minDays = newMin.coerceAtLeast(1L)
                        if (maxDays < minDays) {
                            maxDays = minDays
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                DayCounterRow(
                    label = stringResource(R.string.settings_reminder_custom_max_label),
                    count = maxDays,
                    onCountChange = { newMax ->
                        maxDays = newMax.coerceAtLeast(minDays)
                    }
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = pluralStringResource(R.plurals.settings_reminder_custom_hint, maxDays.toInt(), minDays, maxDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(minDays, maxDays)
            }) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun DayCounterRow(
    label: String,
    count: Long,
    onCountChange: (Long) -> Unit
) {
    var textValue by remember(count) { mutableStateOf(count.toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    val newCount = (count - 1).coerceAtLeast(1L)
                    textValue = newCount.toString()
                    onCountChange(newCount)
                },
                enabled = count > 1
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease))
            }

            BasicTextField(
                value = textValue,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }.take(3)
                    textValue = filtered
                    val parsed = filtered.toLongOrNull()
                    if (parsed != null && parsed >= 1L) {
                        onCountChange(parsed)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .width(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                decorationBox = { innerTextField ->
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        innerTextField()
                    }
                }
            )

            IconButton(
                onClick = {
                    val newCount = count + 1
                    textValue = newCount.toString()
                    onCountChange(newCount)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase))
            }
        }
    }
}