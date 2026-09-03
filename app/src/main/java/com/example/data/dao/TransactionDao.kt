package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY entry_date DESC, entry_time DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY entry_date DESC, entry_time DESC, id DESC")
    suspend fun getAllTransactionsDirect(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionByIdDirect(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE customer_id = :customerId ORDER BY entry_date DESC, entry_time DESC, id DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE customer_id = :customerId ORDER BY entry_date ASC, entry_time ASC, id ASC")
    suspend fun getTransactionsForCustomerAscending(customerId: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE customer_id = :customerId AND entry_date >= :startDate AND entry_date <= :endDate ORDER BY entry_date ASC, entry_time ASC, id ASC")
    fun getTransactionsForCustomerInDateRange(customerId: Long, startDate: String, endDate: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE entry_date = :date ORDER BY entry_time DESC, id DESC")
    fun getTransactionsForDate(date: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE substr(entry_date, 1, 7) = :yearMonthPrefix ORDER BY entry_date ASC, entry_time ASC, id ASC")
    fun getTransactionsInMonth(yearMonthPrefix: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE substr(entry_date, 1, 7) = :yearMonthPrefix ORDER BY entry_date ASC, entry_time ASC, id ASC")
    suspend fun getTransactionsInMonthDirect(yearMonthPrefix: String): List<Transaction>

    @Query("SELECT DISTINCT customer_id FROM transactions WHERE entry_date = :date")
    fun getActiveCustomerIdsForDate(date: String): Flow<List<Long>>

    @Query("SELECT SUM(amount_paise) FROM transactions WHERE transaction_type = 'credit'")
    fun getTotalCreditPaise(): Flow<Long?>

    @Query("SELECT SUM(amount_paise) FROM transactions WHERE transaction_type = 'payment'")
    fun getTotalPaymentPaise(): Flow<Long?>

    @Query("SELECT SUM(amount_paise) FROM transactions WHERE customer_id = :customerId AND transaction_type = 'credit'")
    fun getCustomerCreditPaise(customerId: Long): Flow<Long?>

    @Query("SELECT SUM(amount_paise) FROM transactions WHERE customer_id = :customerId AND transaction_type = 'payment'")
    fun getCustomerPaymentPaise(customerId: Long): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction): Int

    @Delete
    suspend fun deleteTransaction(transaction: Transaction): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long): Int

    @Query("DELETE FROM transactions WHERE customer_id = :customerId")
    suspend fun deleteTransactionsForCustomer(customerId: Long): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions(): Int

    @Query("DELETE FROM sqlite_sequence WHERE name = 'transactions'")
    suspend fun resetTransactionSequence()
}
