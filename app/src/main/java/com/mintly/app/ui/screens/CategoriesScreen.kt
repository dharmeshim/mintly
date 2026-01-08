package com.mintly.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mintly.app.data.model.Category
import com.mintly.app.viewmodel.MintlyViewModel
import kotlinx.coroutines.delay

@Composable
fun CategoriesScreen(viewModel: MintlyViewModel) {
    val categories by viewModel.allCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "fabScale"
            )
            
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .scale(scale),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEW_LABEL",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
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
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Text(
                        text = ":: LABELS_DATABASE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    text = "CATEGORICAL_INDEX",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp, top = 8.dp),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            if (categories.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                itemsIndexed(
                    items = categories,
                    key = { _, category -> category.id }
                ) { index, category ->
                    AnimatedCategoryItem(
                        category = category,
                        index = index
                    )
                    if (index < categories.size - 1) {
                        Divider(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
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
fun AnimatedCategoryItem(category: Category, index: Int) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 50L) // Stagger animation
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally { it / 2 }
    ) {
        CategoryItem(category)
    }
}

@Composable
fun CategoryItem(category: Category) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "itemScale"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(14.dp),
            shape = CircleShape,
            color = Color(category.colorDot),
            shadowElevation = 4.dp
        ) {}
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name.uppercase(), 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (category.keywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.keywords.joinToString(" • "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NO_CATEGORIES_FOUND",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first category to start organizing expenses",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}


@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var keywordsText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF00FF88.toInt()) } // Default mint green
    
    val predefinedColors = listOf(
        0xFF00FF88.toInt(), // Mint Green
        0xFFFF6B6B.toInt(), // Red
        0xFF4ECDC4.toInt(), // Teal
        0xFFFFA07A.toInt(), // Light Salmon
        0xFF9B59B6.toInt(), // Purple
        0xFFF39C12.toInt(), // Orange
        0xFF3498DB.toInt(), // Blue
        0xFFE74C3C.toInt(), // Crimson
        0xFF1ABC9C.toInt(), // Turquoise
        0xFFE67E22.toInt(), // Carrot
        0xFF2ECC71.toInt(), // Emerald
        0xFFF1C40F.toInt(), // Yellow
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "NEW CATEGORY",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp
            ) 
        },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Food)", style = MaterialTheme.typography.bodyLarge) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = { Text("Keywords (comma separated)", style = MaterialTheme.typography.bodyLarge) },
                    placeholder = { Text("e.g. restaurant, grocery, snacks") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "COLOR",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(predefinedColors) { color ->
                        ColorOption(
                            color = color,
                            isSelected = color == selectedColor,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onConfirm(name, keywords, selectedColor)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("ADD", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
fun ColorOption(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "colorScale"
    )
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .background(
                color = Color(color),
                shape = CircleShape
            )
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.tertiary,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
