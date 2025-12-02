package com.synapse.social.studioasinc.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.PollOption
import com.synapse.social.studioasinc.model.ReactionType

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val key: String?,
    val authorUid: String,
    val postText: String?,
    var postImage: String?,
    var postType: String?,
    val postHideViewsCount: String?,
    val postHideLikeCount: String?,
    val postHideCommentsCount: String?,
    val postDisableComments: String?,
    val postVisibility: String?,
    val publishDate: String?,
    val timestamp: Long,
    val likesCount: Int,
    val commentsCount: Int,
    val viewsCount: Int,
    val resharesCount: Int,
    @TypeConverters(MediaItemConverter::class)
    var mediaItems: List<MediaItem>?,
    val isEncrypted: Boolean?,
    val nonce: String?,
    val encryptionKeyId: String?,
    val isDeleted: Boolean?,
    val isEdited: Boolean?,
    val editedAt: String?,
    val deletedAt: String?,
    val hasPoll: Boolean?,
    val pollQuestion: String?,
    @TypeConverters(PollOptionConverter::class)
    val pollOptions: List<PollOption>?,
    val pollEndTime: String?,
    val pollAllowMultiple: Boolean?,
    val hasLocation: Boolean?,
    val locationName: String?,
    val locationAddress: String?,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val locationPlaceId: String?,
    val youtubeUrl: String?,
    @TypeConverters(ReactionTypeConverter::class)
    var reactions: Map<ReactionType, Int>?,
    @TypeConverters(ReactionTypeConverter::class)
    var userReaction: ReactionType?,
    var username: String?,
    var avatarUrl: String?,
    var isVerified: Boolean
)

class MediaItemConverter {
    @TypeConverter
    fun fromMediaItemList(mediaItems: List<MediaItem>?): String? {
        if (mediaItems == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<MediaItem>>() {}.type
        return gson.toJson(mediaItems, type)
    }

    @TypeConverter
    fun toMediaItemList(mediaItemsString: String?): List<MediaItem>? {
        if (mediaItemsString == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<MediaItem>>() {}.type
        return gson.fromJson(mediaItemsString, type)
    }
}

class PollOptionConverter {
    @TypeConverter
    fun fromPollOptionList(pollOptions: List<PollOption>?): String? {
        if (pollOptions == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<PollOption>>() {}.type
        return gson.toJson(pollOptions, type)
    }

    @TypeConverter
    fun toPollOptionList(pollOptionsString: String?): List<PollOption>? {
        if (pollOptionsString == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<PollOption>>() {}.type
        return gson.fromJson(pollOptionsString, type)
    }
}

class ReactionTypeConverter {
    @TypeConverter
    fun fromReactionMap(reactions: Map<ReactionType, Int>?): String? {
        if (reactions == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<Map<ReactionType, Int>>() {}.type
        return gson.toJson(reactions, type)
    }

    @TypeConverter
    fun toReactionMap(reactionsString: String?): Map<ReactionType, Int>? {
        if (reactionsString == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<Map<ReactionType, Int>>() {}.type
        return gson.fromJson(reactionsString, type)
    }

    @TypeConverter
    fun fromReactionType(reactionType: ReactionType?): String? {
        return reactionType?.name
    }

    @TypeConverter
    fun toReactionType(reactionTypeString: String?): ReactionType? {
        return reactionTypeString?.let { ReactionType.valueOf(it) }
    }
}
