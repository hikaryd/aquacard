package net.aquadx.aquacard.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val idm: String, // 16 hex characters, starts with 02FE (for HCE-F compatibility)
    val color: String, // Hex color or styling preset e.g. "#FF2D55"
    val note: String? = null,
    val linkedAquaUsername: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
