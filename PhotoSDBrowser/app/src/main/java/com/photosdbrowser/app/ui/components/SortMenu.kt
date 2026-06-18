package com.photosdbrowser.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.photosdbrowser.app.data.model.SortOrder

/** Botón de la barra superior que despliega las opciones de ordenación. */
@Composable
fun SortMenu(current: SortOrder, onSelect: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Outlined.SwapVert,
            contentDescription = "Ordenar",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        SortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(order.label) },
                trailingIcon = {
                    if (order == current) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                    }
                },
                onClick = {
                    onSelect(order)
                    expanded = false
                }
            )
        }
    }
}
