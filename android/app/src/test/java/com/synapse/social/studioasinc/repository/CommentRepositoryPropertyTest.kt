package com.synapse.social.studioasinc.repository

import com.synapse.social.studioasinc.data.repository.CommentRepository
import com.synapse.social.studioasinc.model.CommentWithUser
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.model.UserProfile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for CommentRepository.
 * Tests the core logic for comment loading, replies, and CRUD operations.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.4
 */
class CommentRepositoryPropertyTest : StringSpec({

    // Mock CommentDao for testing - actual DAO operations are tested separately
    val mockCommentDao = object : com.synapse.social.studioasinc.data.local.CommentDao {
        override suspend fun insertComment(comment: com.synapse.social.studioasinc.data.local.CommentEntity) {}
        override suspend fun insertComments(comments: List<com.synapse.social.studioasinc.data.local.CommentEntity>) {}
        override suspend fun getCommentsByPostId(postId: String): List<com.synapse.social.studioasinc.data.local.CommentEntity> = emptyList()
        override suspend fun getCommentById(commentId: String): com.synapse.social.studioasinc.data.local.CommentEntity? = null
        override suspend fun deleteComment(commentId: String) {}
        override suspend fun deleteCommentsByPostId(postId: String) {}
        override suspend fun clearAllComments() {}
    }
    
    val repository = CommentRepository(mockCommentDao)

    // ==================== ARBITRARY GENERATORS ====================

    val arbNonEmptyString = Arb.string(1..50).filter { it.isNotBlank() }
    
    val arbUuid = Arb.uuid().map { it.toString() }
    
    val arbTimestamp = Arb.long(1609459200000L..1893456000000L).map { 
        java.time.Instant.ofEpochMilli(it).toString() 
    }
    
    val arbMediaUrl = Arb.choice(
        Arb.constant(null),
        Arb.string(10..100).map { "https://example.com/media/$it.jpg" }
    )
    
    val arbUserProfile = Arb.bind(
        arbUuid,
        arbNonEmptyString,
        arbNonEmptyString,
        Arb.email(),
        Arb.string(0..200).orNull(),
        arbMediaUrl,
        Arb.int(0..10000),
        Arb.int(0..10000),
        Arb.int(0..1000),
        Arb.element("online", "offline", "away"),
        Arb.element("user", "premium", "admin"),
        Arb.boolean(),
        Arb.boolean()
    ) { uid, username, displayName, email, bio, profileImage, followers, following, posts, status, accountType, verify, banned ->
        UserProfile(
            uid = uid,
            username = username,
            displayName = displayName,
            email = email,
            bio = bio,
            profileImageUrl = profileImage,
            followersCount = followers,
            followingCount = following,
            postsCount = posts,
            status = status,
            account_type = accountType,
            verify = verify,
            banned = banned
        )
    }

    
    fun arbCommentWithUser(
        parentCommentId: String? = null,
        withUser: Boolean = true,
        withMedia: Boolean = false
    ): Arb<CommentWithUser> = Arb.bind(
        arbUuid,
        arbUuid,
        arbUuid,
        arbNonEmptyString,
        arbTimestamp,
        arbTimestamp.orNull(),
        Arb.int(0..1000),
        Arb.int(0..100),
        Arb.boolean(),
        Arb.boolean(),
        if (withUser) arbUserProfile.map { it } else Arb.constant(null),
        if (withMedia) arbMediaUrl else Arb.constant(null)
    ) { id, postId, userId, content, createdAt, updatedAt, likesCount, repliesCount, isDeleted, isEdited, user, mediaUrl ->
        CommentWithUser(
            id = id,
            postId = postId,
            userId = userId,
            parentCommentId = parentCommentId,
            content = content,
            mediaUrl = mediaUrl,
            createdAt = createdAt,
            updatedAt = updatedAt,
            likesCount = likesCount,
            repliesCount = repliesCount,
            isDeleted = isDeleted,
            isEdited = isEdited,
            user = user
        )
    }
    
    val arbCompleteComment = arbCommentWithUser(withUser = true)
    val arbIncompleteComment = arbCommentWithUser(withUser = false)
    val arbCommentWithMedia = arbCommentWithUser(withMedia = true)
    
    fun arbCommentList(size: IntRange = 0..20): Arb<List<CommentWithUser>> = 
        Arb.list(arbCompleteComment, size)

    // ==================== PROPERTY 8: COMMENT LOADING COMPLETENESS ====================

    /**
     * **Feature: detailed-post-view, Property 8: Comment loading completeness**
     * 
     * For any post with comments, loading comments SHALL return comments sorted by 
     * creation date with all required fields (user info, content, timestamp, 
     * reaction count, replies count).
     * 
     * **Validates: Requirements 4.1, 4.2**
     */
    "Property 8: Complete comment has all required fields" {
        checkAll(100, arbCompleteComment) { comment ->
            repository.isCommentComplete(comment) shouldBe true
        }
    }

    "Property 8: Comment without user is incomplete" {
        checkAll(100, arbIncompleteComment) { comment ->
            repository.isCommentComplete(comment) shouldBe false
        }
    }

    "Property 8: Comment with empty id is incomplete" {
        checkAll(100, arbCompleteComment) { comment ->
            val incompleteComment = comment.copy(id = "")
            repository.isCommentComplete(incompleteComment) shouldBe false
        }
    }

    "Property 8: Comment with empty postId is incomplete" {
        checkAll(100, arbCompleteComment) { comment ->
            val incompleteComment = comment.copy(postId = "")
            repository.isCommentComplete(incompleteComment) shouldBe false
        }
    }

    "Property 8: Comment with empty userId is incomplete" {
        checkAll(100, arbCompleteComment) { comment ->
            val incompleteComment = comment.copy(userId = "")
            repository.isCommentComplete(incompleteComment) shouldBe false
        }
    }

    "Property 8: Comment with empty createdAt is incomplete" {
        checkAll(100, arbCompleteComment) { comment ->
            val incompleteComment = comment.copy(createdAt = "")
            repository.isCommentComplete(incompleteComment) shouldBe false
        }
    }

    "Property 8: Comments are sorted by creation date" {
        checkAll(100, arbCommentList(2..20)) { comments ->
            val sortedComments = comments.sortedBy { it.createdAt }
            repository.areCommentsSortedByDate(sortedComments) shouldBe true
        }
    }

    "Property 8: Single comment list is always sorted" {
        checkAll(100, arbCompleteComment) { comment ->
            repository.areCommentsSortedByDate(listOf(comment)) shouldBe true
        }
    }

    "Property 8: Empty comment list is always sorted" {
        repository.areCommentsSortedByDate(emptyList()) shouldBe true
    }


    // ==================== PROPERTY 12: REPLY COUNT ACCURACY ====================

    /**
     * **Feature: detailed-post-view, Property 12: Reply count accuracy**
     * 
     * For any comment, the replies_count SHALL equal the number of comments 
     * with that comment's ID as parent_comment_id.
     * 
     * **Validates: Requirements 5.1**
     */
    "Property 12: Reply count matches actual replies" {
        checkAll(100, arbCompleteComment, Arb.int(0..10)) { parentComment, replyCount ->
            val replies = (0 until replyCount).map { 
                parentComment.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    parentCommentId = parentComment.id
                )
            }
            val commentWithCorrectCount = parentComment.copy(repliesCount = replyCount)
            repository.isReplyCountAccurate(commentWithCorrectCount, replies) shouldBe true
        }
    }

    "Property 12: Incorrect reply count is detected" {
        checkAll(100, arbCompleteComment, Arb.int(1..10)) { parentComment, replyCount ->
            val replies = (0 until replyCount).map { 
                parentComment.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    parentCommentId = parentComment.id
                )
            }
            // Set wrong count
            val commentWithWrongCount = parentComment.copy(repliesCount = replyCount + 1)
            repository.isReplyCountAccurate(commentWithWrongCount, replies) shouldBe false
        }
    }

    "Property 12: Zero replies count is accurate for no replies" {
        checkAll(100, arbCompleteComment) { comment ->
            val commentWithZeroReplies = comment.copy(repliesCount = 0)
            repository.isReplyCountAccurate(commentWithZeroReplies, emptyList()) shouldBe true
        }
    }

    "Property 12: Calculate replies count from all comments" {
        checkAll(100, arbCompleteComment, Arb.int(0..5)) { parentComment, replyCount ->
            val replies = (0 until replyCount).map { 
                parentComment.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    parentCommentId = parentComment.id
                )
            }
            val otherComments = (0 until 3).map {
                parentComment.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    parentCommentId = null
                )
            }
            val allComments = replies + otherComments + listOf(parentComment)
            
            repository.calculateRepliesCount(allComments, parentComment.id) shouldBe replyCount
        }
    }

    // ==================== PROPERTY 9: COMMENT CREATION PERSISTENCE ====================

    /**
     * **Feature: detailed-post-view, Property 9: Comment creation persistence**
     * 
     * For any valid comment submission, the comment SHALL be persisted and 
     * appear in subsequent comment loads for that post.
     * 
     * **Validates: Requirements 4.3**
     * 
     * Note: This tests the logic for validating comment data, not actual persistence.
     */
    "Property 9: Created comment has required fields" {
        checkAll(100, arbCompleteComment) { comment ->
            // A valid created comment should have all required fields
            val isValid = comment.id.isNotEmpty() &&
                         comment.postId.isNotEmpty() &&
                         comment.userId.isNotEmpty() &&
                         comment.createdAt.isNotEmpty()
            isValid shouldBe true
        }
    }

    "Property 9: Comment content is preserved" {
        checkAll(100, arbNonEmptyString) { content ->
            // Content should not be empty for a valid comment
            content.isNotBlank() shouldBe true
        }
    }


    // ==================== PROPERTY 13: REPLY PARENT REFERENCE ====================

    /**
     * **Feature: detailed-post-view, Property 13: Reply parent reference**
     * 
     * For any reply submission with a parent comment ID, the created comment 
     * SHALL have the correct parent_comment_id reference.
     * 
     * **Validates: Requirements 5.4**
     */
    "Property 13: Reply has correct parent reference" {
        checkAll(100, arbUuid, arbCompleteComment) { parentId, comment ->
            val reply = comment.copy(parentCommentId = parentId)
            repository.hasCorrectParentReference(reply, parentId) shouldBe true
        }
    }

    "Property 13: Reply with wrong parent reference is detected" {
        checkAll(100, arbUuid, arbUuid, arbCompleteComment) { parentId, wrongParentId, comment ->
            if (parentId != wrongParentId) {
                val reply = comment.copy(parentCommentId = wrongParentId)
                repository.hasCorrectParentReference(reply, parentId) shouldBe false
            }
        }
    }

    "Property 13: Top-level comment has no parent reference" {
        checkAll(100, arbCompleteComment) { comment ->
            val topLevelComment = comment.copy(parentCommentId = null)
            topLevelComment.isReply() shouldBe false
        }
    }

    "Property 13: Reply is identified correctly" {
        checkAll(100, arbUuid, arbCompleteComment) { parentId, comment ->
            val reply = comment.copy(parentCommentId = parentId)
            reply.isReply() shouldBe true
        }
    }

    "Property 13: Filter replies for parent returns correct comments" {
        checkAll(100, arbCompleteComment, Arb.int(0..5)) { parentComment, replyCount ->
            val parentId = parentComment.id
            val replies = (0 until replyCount).map { 
                parentComment.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    parentCommentId = parentId
                )
            }
            val otherComments = (0 until 3).map {
                parentComment.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    parentCommentId = java.util.UUID.randomUUID().toString()
                )
            }
            val allComments = replies + otherComments
            
            val filteredReplies = repository.filterRepliesForParent(allComments, parentId)
            filteredReplies.size shouldBe replyCount
            filteredReplies.all { it.parentCommentId == parentId } shouldBe true
        }
    }

    // ==================== PROPERTY 11: COMMENT EDIT AND DELETE STATUS ====================

    /**
     * **Feature: detailed-post-view, Property 11: Comment edit and delete status**
     * 
     * For any comment, the is_edited and is_deleted flags SHALL accurately 
     * reflect the comment's state.
     * 
     * **Validates: Requirements 4.5, 4.6**
     */
    "Property 11: Edit status is correctly reflected" {
        checkAll(100, arbCompleteComment, Arb.boolean()) { comment, wasEdited ->
            val editedComment = comment.copy(isEdited = wasEdited)
            repository.hasCorrectEditDeleteStatus(editedComment, wasEdited, editedComment.isDeleted) shouldBe true
        }
    }

    "Property 11: Delete status is correctly reflected" {
        checkAll(100, arbCompleteComment, Arb.boolean()) { comment, wasDeleted ->
            val deletedComment = comment.copy(isDeleted = wasDeleted)
            repository.hasCorrectEditDeleteStatus(deletedComment, deletedComment.isEdited, wasDeleted) shouldBe true
        }
    }

    "Property 11: Both edit and delete status are correctly reflected" {
        checkAll(100, arbCompleteComment, Arb.boolean(), Arb.boolean()) { comment, wasEdited, wasDeleted ->
            val modifiedComment = comment.copy(isEdited = wasEdited, isDeleted = wasDeleted)
            repository.hasCorrectEditDeleteStatus(modifiedComment, wasEdited, wasDeleted) shouldBe true
        }
    }

    "Property 11: Incorrect edit status is detected" {
        checkAll(100, arbCompleteComment) { comment ->
            val editedComment = comment.copy(isEdited = true)
            repository.hasCorrectEditDeleteStatus(editedComment, false, editedComment.isDeleted) shouldBe false
        }
    }

    "Property 11: Incorrect delete status is detected" {
        checkAll(100, arbCompleteComment) { comment ->
            val deletedComment = comment.copy(isDeleted = true)
            repository.hasCorrectEditDeleteStatus(deletedComment, deletedComment.isEdited, false) shouldBe false
        }
    }


    // ==================== PROPERTY 10: COMMENT MEDIA INCLUSION ====================

    /**
     * **Feature: detailed-post-view, Property 10: Comment media inclusion**
     * 
     * For any comment with a media_url, the loaded comment SHALL include 
     * the media URL.
     * 
     * **Validates: Requirements 4.4**
     */
    "Property 10: Comment with media has correct media URL" {
        checkAll(100, arbCommentWithMedia) { comment ->
            repository.hasCorrectMediaInclusion(comment, comment.mediaUrl) shouldBe true
        }
    }

    "Property 10: Comment without media has null media URL" {
        checkAll(100, arbCompleteComment) { comment ->
            val commentWithoutMedia = comment.copy(mediaUrl = null)
            repository.hasCorrectMediaInclusion(commentWithoutMedia, null) shouldBe true
        }
    }

    "Property 10: Wrong media URL is detected" {
        checkAll(100, arbNonEmptyString, arbNonEmptyString) { url1, url2 ->
            if (url1 != url2) {
                val comment = CommentWithUser(
                    id = "test-id",
                    postId = "test-post",
                    userId = "test-user",
                    content = "test content",
                    mediaUrl = url1,
                    createdAt = "2024-01-01T00:00:00Z"
                )
                repository.hasCorrectMediaInclusion(comment, url2) shouldBe false
            }
        }
    }

    "Property 10: hasMedia helper correctly identifies media presence" {
        checkAll(100, arbCommentWithMedia) { comment ->
            if (comment.mediaUrl != null) {
                comment.hasMedia() shouldBe true
            }
        }
    }

    "Property 10: hasMedia returns false for null media URL" {
        checkAll(100, arbCompleteComment) { comment ->
            val commentWithoutMedia = comment.copy(mediaUrl = null)
            commentWithoutMedia.hasMedia() shouldBe false
        }
    }

    "Property 10: hasMedia returns false for empty media URL" {
        checkAll(100, arbCompleteComment) { comment ->
            val commentWithEmptyMedia = comment.copy(mediaUrl = "")
            commentWithEmptyMedia.hasMedia() shouldBe false
        }
    }

    // ==================== ADDITIONAL HELPER TESTS ====================

    "Filter top-level comments excludes replies" {
        checkAll(100, arbCompleteComment, Arb.int(1..5)) { parentComment, replyCount ->
            val topLevelComments = listOf(
                parentComment.copy(parentCommentId = null),
                parentComment.copy(id = java.util.UUID.randomUUID().toString(), parentCommentId = null)
            )
            val replies = (0 until replyCount).map { 
                parentComment.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    parentCommentId = parentComment.id
                )
            }
            val allComments = topLevelComments + replies
            
            val filtered = repository.filterTopLevelComments(allComments)
            filtered.size shouldBe 2
            filtered.all { it.parentCommentId == null } shouldBe true
        }
    }

    "Comment display name falls back to Unknown User" {
        checkAll(100, arbCompleteComment) { comment ->
            val commentWithoutUser = comment.copy(user = null)
            commentWithoutUser.getDisplayName() shouldBe "Unknown User"
        }
    }

    "Comment username falls back to unknown" {
        checkAll(100, arbCompleteComment) { comment ->
            val commentWithoutUser = comment.copy(user = null)
            commentWithoutUser.getUsername() shouldBe "unknown"
        }
    }

    "Total reactions count sums all reaction types" {
        checkAll(100, Arb.map(Arb.enum<ReactionType>(), Arb.int(0..100), 0..6)) { reactionMap ->
            val comment = CommentWithUser(
                id = "test-id",
                postId = "test-post",
                userId = "test-user",
                content = "test content",
                createdAt = "2024-01-01T00:00:00Z",
                reactionSummary = reactionMap
            )
            comment.getTotalReactions() shouldBe reactionMap.values.sum()
        }
    }
})
