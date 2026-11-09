package com.markvoronin.immichswipe.feature.settings.components

import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun LogsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = true) {
        onBack()
    }

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val logsCopiedMessage = stringResource(R.string.settings_logs_copied_toast)
    val logsClipLabel = stringResource(R.string.settings_logs_clip_label)

    var reloadKey by remember { mutableIntStateOf(0) }
    val logs = rememberLogs(viewModel::getLogs, reloadKey)
    val rawLogs = logs?.first

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        LogsContent(
            logs = logs,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.clearLogs()
                    reloadKey++
                },
                enabled = rawLogs != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.settings_logs_clear))
            }
            Button(
                onClick = {
                    if (rawLogs == null) return@Button
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(logsClipLabel, rawLogs)))
                    }
                    Toast.makeText(context, logsCopiedMessage, Toast.LENGTH_SHORT).show()
                },
                enabled = rawLogs != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.settings_logs_copy))
            }
        }
    }
}
