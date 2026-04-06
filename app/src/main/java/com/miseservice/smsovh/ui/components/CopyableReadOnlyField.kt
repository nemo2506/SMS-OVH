package com.miseservice.smsovh.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CopyableReadOnlyField(
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                label = label,
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true,
                enabled = true,
                colors = smsOvhTextFieldColors()
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copier dans le presse-papiers"
            )
        }
    }
}
