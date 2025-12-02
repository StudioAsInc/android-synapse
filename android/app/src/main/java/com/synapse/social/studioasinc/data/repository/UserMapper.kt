package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.local.UserEntity
import com.synapse.social.studioasinc.model.User

object UserMapper {

    fun toEntity(user: User): UserEntity {
        return UserEntity(
            uid = user.uid,
            username = user.username,
            email = user.email,
            avatarUrl = user.avatarUrl,
            isVerified = user.isVerified
        )
    }

    fun toModel(entity: UserEntity): User {
        return User(
            uid = entity.uid,
            username = entity.username,
            email = entity.email,
            avatarUrl = entity.avatarUrl,
            isVerified = entity.isVerified
        )
    }
}
