package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "value")
    val value: String
) {
    companion object {
        const val KEY_SHOP_NAME = "shop_name"
        const val KEY_OWNER_NAME = "owner_name"
        const val KEY_ADDRESS = "shop_address"
        const val KEY_MOBILE = "shop_mobile"
        const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    }
}
