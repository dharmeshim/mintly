package com.mintly.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintly.app.data.MintlyRepository
import com.mintly.app.data.model.Category
import com.mintly.app.data.model.Expense
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MintlyViewModel(private val repository: MintlyRepository) : ViewModel() {

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val monthlyTotal: StateFlow<Double> = repository.allExpenses
        .map { expenses ->
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            expenses.filter { expense ->
                val expCal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
                expCal.get(Calendar.MONTH) == currentMonth && expCal.get(Calendar.YEAR) == currentYear
            }.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private var lastInsertedExpense: Expense? = null

    var lastCategoryId: String? = null
        private set

    fun addExpense(amount: Double, description: String, categoryId: String?) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                description = description,
                categoryId = categoryId
            )
            lastInsertedExpense = expense
            lastCategoryId = categoryId
            repository.insertExpense(expense)
        }
    }

    fun undoLastExpense() {
        val expense = lastInsertedExpense
        if (expense != null) {
            viewModelScope.launch {
                repository.deleteExpense(expense)
                lastInsertedExpense = null
            }
        }
    }


    fun addCategory(name: String, keywords: List<String>, colorDot: Int) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                keywords = keywords,
                colorDot = colorDot
            )
            repository.insertCategory(category)
        }
    }
}
