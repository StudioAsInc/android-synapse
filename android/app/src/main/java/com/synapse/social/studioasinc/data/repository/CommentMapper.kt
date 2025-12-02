package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.local.CommentEntity
import com.synapse.social.studioasinc.model.Comment

object CommentMapper {

    fun toEntity(comment: Comment): CommentEntity {
        return CommentEntity(
            id = comment.id,
            postId = comment.postId,
            authorUid = comment.authorUid,
            text = comment.text,
            timestamp = comment.timestamp,
            username = comment.username,
            avatarUrl = comment.avatarUrl
        )
    }

    fun toModel(entity: CommentEntity): Comment {
        return Comment(
            id = entity.id,
            postId = entity.postId,
            authorUid = entity.authorUid,
            text = entity.text,
            timestamp = entity.timestamp,
            username = entity.username,
            avatarUrl = entity.avatarUrl
        )
    }
}
