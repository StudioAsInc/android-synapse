package com.synapse.social.studioasinc.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "chats")
@TypeConverters(ParticipantsConverter::class)
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val participants: List<String>,
    val lastMessage: String?,
    val timestamp: Long,
    val isGroup: Boolean,
    val lastMessageSender: String?,
    val createdAt: Long,
    val isActive: Boolean
)

class ParticipantsConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromParticipantsList(participants: List<String>?): String? {
        return participants?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toParticipantsList(participantsString: String?): List<String>? {
        if (participantsString == null) return null
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(participantsString, type)
        } catch (e: Exception) {
            null
        }
    }
}
