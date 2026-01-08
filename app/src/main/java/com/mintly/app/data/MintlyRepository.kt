package com.mintly.app.data

import com.mintly.app.data.local.CategoryDao
import com.mintly.app.data.local.ExpenseDao
import com.mintly.app.data.model.Category
import com.mintly.app.data.model.Expense
import kotlinx.coroutines.flow.Flow

class MintlyRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
}
