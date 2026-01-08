package com.mintly.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mintly.app.data.model.Category
import com.mintly.app.viewmodel.MintlyViewModel

@Composable
fun AddExpenseScreen(viewModel: MintlyViewModel) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(viewModel.lastCategoryId?.let { id -> 
        viewModel.allCategories.value.find { it.id == id } 
    }) }
    
    val categories by viewModel.allCategories.collectAsState()
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(description) {
        if (description.isNotEmpty()) {
            val suggested = categories.find { category ->
                category.keywords.any { keyword -> 
                    description.contains(keyword, ignoreCase = true) 
                }
            }
            if (suggested != null) {
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
            Column {
                Text(
                    text = ":: SYS_STATUS: READY",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "NET_EXPENDITURE_CURRENT_PERIOD",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "$${"%.2f".format(monthlyTotal)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }



            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large Numeric Input for Amount
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
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Indicator
                Row(
                    background = androidx.compose.ui.graphics.Color.Transparent,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { /* Future: Category selection */ }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(8.dp)
                            .androidx.compose.foundation.background(
                                color = selectedCategory?.let { androidx.compose.ui.graphics.Color(it.colorDot) } ?: androidx.compose.ui.graphics.Color.Gray,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedCategory?.name ?: "Assign Category",
                        style = MaterialTheme.typography.labelLarge,
                        color = selectedCategory?.let { MaterialTheme.colorScheme.primary } ?: MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Description Field
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("What for?", style = MaterialTheme.typography.bodyLarge) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button with Modern Styling
                val isValid = amount.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0.0
                
                Button(
                    onClick = {
                        if (isValid) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            viewModel.addExpense(amount.toDouble(), description, selectedCategory?.id)
                            
                            val oldAmount = amount
                            val oldDesc = description
                            
                            amount = ""
                            description = ""
                            selectedCategory = null

                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Entry Logged",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoLastExpense()
                                    amount = oldAmount
                                    description = oldDesc
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .androidx.compose.ui.draw.alpha(if (isValid) 1f else 0.4f),
                    enabled = isValid,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
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
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[EXECUTE]",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f)
                        )
                    }
                }



            }
        }
    }
}



