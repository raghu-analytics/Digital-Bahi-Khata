package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersDirect(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdDirect(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE customer_code = :code LIMIT 1")
    suspend fun getCustomerByCode(code: String): Customer?

    @Query("SELECT customer_code FROM customers ORDER BY id DESC LIMIT 1")
    suspend fun getLatestCustomerCode(): String?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR customer_code LIKE '%' || :query || '%' OR mobile_number LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT COUNT(*) FROM customers")
    fun getCustomerCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer): Int

    @Delete
    suspend fun deleteCustomer(customer: Customer): Int

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long): Int

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers(): Int

    @Query("DELETE FROM sqlite_sequence WHERE name = 'customers'")
    suspend fun resetCustomerSequence()
}
