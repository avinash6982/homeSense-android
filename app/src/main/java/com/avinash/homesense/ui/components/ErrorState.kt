package com.avinash.homesense.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.avinash.homesense.data.model.ClimateError

@Composable
fun ClimateErrorState(
    error: ClimateError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, title, message) = when (error) {
        ClimateError.NO_INTERNET -> Triple(
            Icons.Default.CloudOff,
            "No internet connection",
            "Check your network and try again.",
        )
        ClimateError.API_FAILURE -> Triple(
            Icons.Default.ErrorOutline,
            "Couldn't load data",
            "Something went wrong talking to the server.",
        )
        ClimateError.EMPTY -> Triple(
            Icons.Default.Inbox,
            "No readings yet",
            "The sensor hasn't reported any data yet.",
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (error != ClimateError.EMPTY) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun EmptyDayState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No data recorded for this day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
