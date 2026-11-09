package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.ConnectionStatus
import com.markvoronin.immichswipe.domain.model.User

@Composable
fun HomeLogo(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val logoRes = if (isDark) R.drawable.immichswipe_logo_colors_dark else R.drawable.immichswipe_logo_colors_light

    Image(
        painter = painterResource(id = logoRes),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier
            .height(38.dp)
            .padding(vertical = 2.dp)
            // Logo PNG is 1254x242; the icon occupies the left ~250px, so crop it away.
            .aspectRatio((1254f - 250f) / 242f),
        contentScale = ContentScale.Crop,
        alignment = Alignment.CenterEnd,
    )
}

/** The account avatar with its connection status dot, shown at the end of every top bar except Settings. */
@Composable
fun ProfileButton(
    user: User?,
    baseUrl: String,
    connectionStatus: ConnectionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        UserAvatar(
            userId = user?.id,
            baseUrl = baseUrl,
            name = user?.name ?: user?.email,
            avatarColorName = user?.avatarColor,
            modifier = Modifier.size(32.dp),
            borderWidth = 1.dp,
            contentDescription = stringResource(R.string.settings_section_account),
            onClick = onClick
        )

        Surface(
            modifier = Modifier
                .size(8.dp)
                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
            color = connectionStatus.level.color,
            shape = CircleShape,
        ) {}
    }
}

@Composable
fun SearchBarField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchShape = CircleShape
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(searchShape)
            .height(52.dp),
        placeholder = { Text(stringResource(R.string.home_search_placeholder), fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_cancel), modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = searchShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
    )
}
