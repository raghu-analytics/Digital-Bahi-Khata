package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["entry_date"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "customer_id")
    val customerId: Long,

    // "credit" for Udhar (उधार), "payment" for Jama (जमा)
    @ColumnInfo(name = "transaction_type")
    val transactionType: String,

    // Stored as Integer Paise (1 INR = 100 Paise) to eliminate floating point rounding issues
    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "entry_date")
    val entryDate: String, // "YYYY-MM-DD"

    @ColumnInfo(name = "entry_time")
    val entryTime: String, // "HH:MM:SS"

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
) {
    companion object {
        const val TYPE_CREDIT = "credit" // Udhar (उधार)
        const val TYPE_PAYMENT = "payment" // Jama (जमा)
    }

    val isCredit: Boolean
        get() = transactionType == TYPE_CREDIT

    val isPayment: Boolean
        get() = transactionType == TYPE_PAYMENT
}
