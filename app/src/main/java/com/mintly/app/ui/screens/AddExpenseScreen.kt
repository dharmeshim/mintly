package com.mintly.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mintly.app.data.model.Category
import com.mintly.app.viewmodel.MintlyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddExpenseScreen(viewModel: MintlyViewModel) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(viewModel.lastCategoryId?.let { id -> 
        viewModel.allCategories.value.find { it.id == id } 
    }) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    
    val categories by viewModel.allCategories.collectAsState()
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Pulsing animation for total amount
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Auto-suggest category based on description
    LaunchedEffect(description) {
        if (description.isNotEmpty()) {
            delay(300) // Debounce
            val suggested = categories.find { category ->
                category.keywords.any { keyword -> 
                    description.contains(keyword, ignoreCase = true) 
                }
            }
            if (suggested != null && selectedCategory == null) {
                selectedCategory = suggested
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section with animated total
            Column {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Text(
                        text = ":: SYS_STATUS: READY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "NET_EXPENDITURE_CURRENT_PERIOD",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Text(
                    text = "$${\"%.2f\".format(monthlyTotal)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.scale(if (monthlyTotal > 0) pulseScale else 1f)
                )
            }

            // Amount Input Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large Numeric Input with animation
                AnimatedContent(
                    targetState = amount.isEmpty(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "amountAnimation"
                ) { isEmpty ->
                    TextField(
                        value = amount,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() || char == '.' }) {
                                amount = it 
                            }
                        },
                        placeholder = { 
                            Text(
                                "0", 
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ) 
                        },
                        textStyle = MaterialTheme.typography.displayLarge.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector with animation
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showCategoryPicker = true }
                            .border(
                                width = 1.dp,
                                color = if (selectedCategory != null) 
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f) 
                                else 
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = selectedCategory?.let { Color(it.colorDot) } ?: Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = selectedCategory?.name?.uppercase() ?: "SELECT CATEGORY",
                            style = MaterialTheme.typography.labelLarge,
                            color = selectedCategory?.let { MaterialTheme.colorScheme.primary } 
                                ?: MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedCategory != null) {
                            IconButton(
                                onClick = { selectedCategory = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Section
            Column(modifier = Modifier.fillMaxWidth()) {
                // Description Field with animation
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { 
                        Text(
                            "What for?", 
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        ) 
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button with animation
                val isValid = amount.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0.0
                val buttonScale by animateFloatAsState(
                    targetValue = if (isValid) 1f else 0.95f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "buttonScale"
                )
                
                Button(
                    onClick = {
                        if (isValid) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            viewModel.addExpense(amount.toDouble(), description, selectedCategory?.id)
                            
                            val oldAmount = amount
                            val oldDesc = description
                            val oldCategory = selectedCategory
                            
                            amount = ""
                            description = ""
                            // Keep category selected for next entry

                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "✓ Entry Logged: $${oldAmount}",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoLastExpense()
                                    amount = oldAmount
                                    description = oldDesc
                                    selectedCategory = oldCategory
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .scale(buttonScale)
                        .alpha(if (isValid) 1f else 0.4f),
                    enabled = isValid,
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        disabledContentColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TRACK IT",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[EXECUTE]",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }

    // Category Picker Dialog
    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = categories,
            onDismiss = { showCategoryPicker = false },
            onSelect = { category ->
                selectedCategory = category
                showCategoryPicker = false
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
        )
    }
}

@Composable
fun CategoryPickerDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSelect: (Category) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "SELECT CATEGORY",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (categories.isEmpty()) {
                    Text(
                        text = "No categories yet. Create one in the Labels tab.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(categories) { category ->
                            CategoryPickerItem(
                                category = category,
                                onClick = { onSelect(category) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CANCEL", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun CategoryPickerItem(
    category: Category,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "itemScale"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = Color(category.colorDot),
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
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
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

