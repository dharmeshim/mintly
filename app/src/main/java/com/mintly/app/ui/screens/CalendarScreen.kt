package com.mintly.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mintly.app.viewmodel.MintlyViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(viewModel: MintlyViewModel) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    
    val dates = (1 until firstDayOfWeek).map { "" } + (1..daysInMonth).map { it.toString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = ":: TEMPORAL_LOG",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = monthLabel.uppercase(),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 32.dp, top = 8.dp),
            fontSize = 42.sp
        )



        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
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
            items(dates) { date ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (date.isNotEmpty()) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            fontWeight = if (date == Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString() && 
                                calendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) 
                                FontWeight.Bold else FontWeight.Normal,
                            color = if (date == Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString() && 
                                calendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) 
                                MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )

                    }
                }
            }
        }
    }
}
