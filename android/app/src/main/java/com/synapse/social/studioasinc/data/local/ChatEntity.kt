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
    @TypeConverter
    fun fromParticipantsList(participants: List<String>?): String? {
        if (participants == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.toJson(participants, type)
    }

    @TypeConverter
    fun toParticipantsList(participantsString: String?): List<String>? {
        if (participantsString == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(participantsString, type)
    }
}
