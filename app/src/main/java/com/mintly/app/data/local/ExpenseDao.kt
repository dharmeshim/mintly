package com.mintly.app.data.local

import androidx.room.*
import com.mintly.app.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getExpensesInRange(start: Long, end: Long): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :start AND timestamp <= :end")
    fun getTotalInRange(start: Long, end: Long): Flow<Double?>
}
