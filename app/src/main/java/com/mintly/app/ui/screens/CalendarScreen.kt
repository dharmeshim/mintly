package com.mintly.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mintly.app.data.model.Expense
import com.mintly.app.viewmodel.MintlyViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(viewModel: MintlyViewModel) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf<Int?>(null) }
    val expenses by viewModel.allExpenses.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    val dates = (1 until firstDayOfWeek).map { "" } + (1..daysInMonth).map { it.toString() }
    
    // Get expenses for current month
    val monthExpenses = remember(expenses, calendar) {
        val startOfMonth = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        
        val endOfMonth = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, daysInMonth)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        
        expenses.filter { it.timestamp in startOfMonth..endOfMonth }
    }
    
    // Group expenses by day
    val expensesByDay = remember(monthExpenses) {
        monthExpenses.groupBy { expense ->
            Calendar.getInstance().apply { timeInMillis = expense.timestamp }.get(Calendar.DAY_OF_MONTH)
        }
    }
    
    // Get selected day expenses
    val selectedDayExpenses = selectedDate?.let { expensesByDay[it] } ?: emptyList()
    val selectedDayTotal = selectedDayExpenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Header
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically()
        ) {
            Text(
                text = ":: TEMPORAL_LOG",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        
        // Month Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                calendar = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                selectedDate = null
            }) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            
            AnimatedContent(
                targetState = monthLabel,
                transitionSpec = {
                    slideInHorizontally { if (targetState > initialState) it else -it } togetherWith
                            slideOutHorizontally { if (targetState > initialState) -it else it }
                },
                label = "monthTransition"
            ) { label ->
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            
            IconButton(onClick = {
                calendar = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                selectedDate = null
            }) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Calendar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Day headers
            val days = listOf("S", "M", "T", "W", "T", "F", "S")
            items(days) { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Date cells
            items(dates) { date ->
                if (date.isNotEmpty()) {
                    val dayNum = date.toInt()
                    val isToday = dayNum == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) &&
                            calendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) &&
                            calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
                    val isSelected = dayNum == selectedDate
                    val hasExpenses = expensesByDay.containsKey(dayNum)
                    val dayTotal = expensesByDay[dayNum]?.sumOf { it.amount } ?: 0.0
                    
                    DateCell(
                        date = date,
                        isToday = isToday,
                        isSelected = isSelected,
                        hasExpenses = hasExpenses,
                        total = dayTotal,
                        onClick = { selectedDate = if (isSelected) null else dayNum }
                    )
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
        
        // Selected Day Details
        AnimatedVisibility(
            visible = selectedDate != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAY_${selectedDate ?: 0}_EXPENSES",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$${\"%.2f\".format(selectedDayTotal)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(selectedDayExpenses) { expense ->
                        ExpenseItem(
                            expense = expense,
                            categoryName = categories.find { it.id == expense.categoryId }?.name
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateCell(
    date: String,
    isToday: Boolean,
    isSelected: Boolean,
    hasExpenses: Boolean,
    total: Double,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dateScale"
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    isToday -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isToday) 1.dp else 0.dp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.tertiary
                    isToday -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                fontSize = 14.sp
            )
            
            if (hasExpenses) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, categoryName: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.description.ifEmpty { "No description" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (categoryName != null) {
                Text(
                    text = categoryName.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 10.sp
                )
            }
        }
        Text(
            text = "$${\"%.2f\".format(expense.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}
