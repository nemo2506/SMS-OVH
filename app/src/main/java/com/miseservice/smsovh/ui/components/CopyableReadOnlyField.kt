package com.miseservice.smsovh.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CopyableReadOnlyField(
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = label,
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            enabled = false
        )
    }
}

