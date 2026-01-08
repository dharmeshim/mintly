package com.mintly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mintly.app.data.model.Category
import com.mintly.app.viewmodel.MintlyViewModel

@Composable
fun CategoriesScreen(viewModel: MintlyViewModel) {
    val categories by viewModel.allCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Button(
                onClick = { showAddDialog = true },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier.padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text(
                    text = "+ NEW_LABEL",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {

            item {
                Text(
                    text = ":: LABELS_DATABASE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "CATEGORICAL_INDEX",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 32.dp, top = 8.dp),
                    fontSize = 48.sp
                )
            }


            items(categories) { category ->
                CategoryItem(category)
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
            }
        }
    }


    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, keywords, color ->
                viewModel.addCategory(name, keywords, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CategoryItem(category: Category) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = Color(category.colorDot),
            shadowElevation = 4.dp
        ) {}
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(
                text = category.name.uppercase(), 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (category.keywords.isNotEmpty()) {
                Text(
                    text = category.keywords.joinToString(" • "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}


@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var keywordsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Food)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = { Text("Keywords (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onConfirm(name, keywords, 0xFF00E676.toInt()) // Default neon green for now
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
