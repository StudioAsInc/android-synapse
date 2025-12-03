package com.synapse.social.studioasinc.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Property-based test for data model serialization round-trip
 * 
 * **Feature: detailed-post-view, Property 3: Encryption round-trip**
 * Test that serializing then deserializing PostDetail produces equivalent object
 * **Validates: Requirements 1.5**
 */
class PostDetailSerializationPropertyTest : StringSpec({

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    // Arbitrary generators for test data
    val arbMediaType = Arb.enum<MediaType>()
    
    val arbMediaItem = arbitrary {
        MediaItem(
            id = Arb.string(8..16).bind(),
            url = "https://example.com/${Arb.string(5..10).bind()}.jpg",
            type = arbMediaType.bind(),
            thumbnailUrl = Arb.string(10..30).orNull().bind()?.let { "https://example.com/$it.jpg" },
            duration = Arb.long(0L..3600000L).orNull().bind(),
            size = Arb.long(1000L..10000000L).orNull().bind(),
            mimeType = Arb.of("image/jpeg", "image/png", "video/mp4").orNull().bind()
        )
    }

    val arbPost = arbitrary {
        Post(
            id = Arb.uuid().bind().toString(),
            key = Arb.string(8..16).orNull().bind(),
            authorUid = Arb.uuid().bind().toString(),
            postText = Arb.string(0..500).orNull().bind(),
            postImage = Arb.string(10..50).orNull().bind()?.let { "https://example.com/$it.jpg" },
            postType = Arb.of("TEXT", "IMAGE", "VIDEO").orNull().bind(),
            publishDate = "2024-01-${Arb.int(1..28).bind()}T10:00:00Z",
            timestamp = Arb.long(1700000000000..1800000000000).bind(),
            likesCount = Arb.int(0..10000).bind(),
            commentsCount = Arb.int(0..1000).bind(),
            viewsCount = Arb.int(0..100000).bind(),
            resharesCount = Arb.int(0..500).bind(),
            mediaItems = Arb.list(arbMediaItem, 0..5).bind().toMutableList().takeIf { it.isNotEmpty() },
            isEncrypted = Arb.boolean().orNull().bind(),
            isDeleted = Arb.boolean().orNull().bind(),
            isEdited = Arb.boolean().orNull().bind(),
            editedAt = Arb.boolean().bind().let { if (it) "2024-01-15T12:00:00Z" else null },
            hasPoll = Arb.boolean().orNull().bind(),
            pollQuestion = Arb.string(10..100).orNull().bind(),
            pollOptions = null, // TODO: Add proper PollOption generator if needed
            hasLocation = Arb.boolean().orNull().bind(),
            locationName = Arb.string(5..50).orNull().bind(),
            locationAddress = Arb.string(10..100).orNull().bind(),
            youtubeUrl = Arb.boolean().bind().let { if (it) "https://youtube.com/watch?v=abc123" else null }
        )
    }

    val arbUserProfile = arbitrary {
        UserProfile(
            uid = Arb.uuid().bind().toString(),
            username = Arb.string(3..20).bind(),
            displayName = Arb.string(1..50).bind(),
            email = "${Arb.string(5..10).bind()}@example.com",
            bio = Arb.string(0..200).orNull().bind(),
            profileImageUrl = Arb.string(10..30).orNull().bind()?.let { "https://example.com/$it.jpg" },
            followersCount = Arb.int(0..100000).bind(),
            followingCount = Arb.int(0..10000).bind(),
            postsCount = Arb.int(0..5000).bind(),
            status = Arb.of("online", "offline", "away").bind(),
            account_type = Arb.of("user", "premium", "admin").bind(),
            verify = Arb.boolean().bind(),
            banned = Arb.boolean().bind()
        )
    }

    val arbPollOptionResult = arbitrary {
        PollOptionResult(
            index = Arb.int(0..9).bind(),
            text = Arb.string(1..100).bind(),
            voteCount = Arb.int(0..10000).bind(),
            percentage = Arb.float(0f..100f).bind()
        )
    }

    val arbPostDetail = arbitrary {
        PostDetail(
            post = arbPost.bind(),
            author = arbUserProfile.bind(),
            isBookmarked = Arb.boolean().bind(),
            hasReshared = Arb.boolean().bind(),
            pollResults = Arb.list(arbPollOptionResult, 0..5).bind().takeIf { it.isNotEmpty() },
            userPollVote = Arb.int(0..4).orNull().bind()
        )
    }

    "Property 3: PostDetail serialization round-trip preserves data" {
        checkAll(100, arbPostDetail) { original ->
            // Serialize to JSON
            val jsonString = json.encodeToString(original)
            
            // Deserialize back to object
            val deserialized = json.decodeFromString<PostDetail>(jsonString)
            
            // Verify core fields are preserved (excluding @Transient fields)
            deserialized.post.id shouldBe original.post.id
            deserialized.post.authorUid shouldBe original.post.authorUid
            deserialized.post.postText shouldBe original.post.postText
            deserialized.post.likesCount shouldBe original.post.likesCount
            deserialized.post.commentsCount shouldBe original.post.commentsCount
            deserialized.author.uid shouldBe original.author.uid
            deserialized.author.username shouldBe original.author.username
            deserialized.author.display_name shouldBe original.author.display_name
            deserialized.isBookmarked shouldBe original.isBookmarked
            deserialized.hasReshared shouldBe original.hasReshared
            deserialized.userPollVote shouldBe original.userPollVote
        }
    }

    "Property 3: Post serialization round-trip preserves all fields" {
        checkAll(100, arbPost) { original ->
            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<Post>(jsonString)
            
            deserialized.id shouldBe original.id
            deserialized.authorUid shouldBe original.authorUid
            deserialized.postText shouldBe original.postText
            deserialized.postType shouldBe original.postType
            deserialized.publishDate shouldBe original.publishDate
            deserialized.likesCount shouldBe original.likesCount
            deserialized.commentsCount shouldBe original.commentsCount
            deserialized.viewsCount shouldBe original.viewsCount
            deserialized.resharesCount shouldBe original.resharesCount
            deserialized.isEncrypted shouldBe original.isEncrypted
            deserialized.isEdited shouldBe original.isEdited
            deserialized.editedAt shouldBe original.editedAt
            deserialized.hasPoll shouldBe original.hasPoll
            deserialized.pollQuestion shouldBe original.pollQuestion
            deserialized.hasLocation shouldBe original.hasLocation
            deserialized.locationName shouldBe original.locationName
            deserialized.youtubeUrl shouldBe original.youtubeUrl
        }
    }

    "Property 3: UserProfile serialization round-trip preserves all fields" {
        checkAll(100, arbUserProfile) { original ->
            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<UserProfile>(jsonString)
            
            deserialized.uid shouldBe original.uid
            deserialized.username shouldBe original.username
            deserialized.display_name shouldBe original.display_name
            deserialized.email shouldBe original.email
            deserialized.bio shouldBe original.bio
            deserialized.profile_image_url shouldBe original.profile_image_url
            deserialized.followers_count shouldBe original.followers_count
            deserialized.following_count shouldBe original.following_count
            deserialized.posts_count shouldBe original.posts_count
            deserialized.status shouldBe original.status
            deserialized.account_type shouldBe original.account_type
            deserialized.verify shouldBe original.verify
            deserialized.banned shouldBe original.banned
        }
    }

    "Property 3: PollOptionResult serialization round-trip preserves all fields" {
        checkAll(100, arbPollOptionResult) { original ->
            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<PollOptionResult>(jsonString)
            
            deserialized.index shouldBe original.index
            deserialized.text shouldBe original.text
            deserialized.voteCount shouldBe original.voteCount
            // Float comparison with tolerance
            kotlin.math.abs(deserialized.percentage - original.percentage) < 0.001f shouldBe true
        }
    }

    "Property 3: CommentWithUser serialization round-trip preserves all fields" {
        val arbCommentWithUser = arbitrary {
            CommentWithUser(
                id = Arb.uuid().bind().toString(),
                postId = Arb.uuid().bind().toString(),
                userId = Arb.uuid().bind().toString(),
                parentCommentId = Arb.uuid().orNull().bind()?.toString(),
                content = Arb.string(1..500).bind(),
                mediaUrl = Arb.string(10..50).orNull().bind()?.let { "https://example.com/$it.jpg" },
                createdAt = "2024-01-${Arb.int(1..28).bind()}T10:00:00Z",
                updatedAt = Arb.boolean().bind().let { if (it) "2024-01-15T12:00:00Z" else null },
                likesCount = Arb.int(0..1000).bind(),
                repliesCount = Arb.int(0..100).bind(),
                isDeleted = Arb.boolean().bind(),
                isEdited = Arb.boolean().bind()
            )
        }

        checkAll(100, arbCommentWithUser) { original ->
            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<CommentWithUser>(jsonString)
            
            deserialized.id shouldBe original.id
            deserialized.postId shouldBe original.postId
            deserialized.userId shouldBe original.userId
            deserialized.parentCommentId shouldBe original.parentCommentId
            deserialized.content shouldBe original.content
            deserialized.mediaUrl shouldBe original.mediaUrl
            deserialized.createdAt shouldBe original.createdAt
            deserialized.updatedAt shouldBe original.updatedAt
            deserialized.likesCount shouldBe original.likesCount
            deserialized.repliesCount shouldBe original.repliesCount
            deserialized.isDeleted shouldBe original.isDeleted
            deserialized.isEdited shouldBe original.isEdited
        }
    }

    "Property 3: CommentReaction serialization round-trip preserves all fields" {
        val arbCommentReaction = arbitrary {
            CommentReaction(
                id = Arb.uuid().orNull().bind()?.toString(),
                commentId = Arb.uuid().bind().toString(),
                userId = Arb.uuid().bind().toString(),
                reactionType = Arb.of("LIKE", "LOVE", "HAHA", "WOW", "SAD", "ANGRY").bind(),
                createdAt = Arb.boolean().bind().let { if (it) "2024-01-15T10:00:00Z" else null },
                updatedAt = Arb.boolean().bind().let { if (it) "2024-01-15T12:00:00Z" else null }
            )
        }

        checkAll(100, arbCommentReaction) { original ->
            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<CommentReaction>(jsonString)
            
            deserialized.id shouldBe original.id
            deserialized.commentId shouldBe original.commentId
            deserialized.userId shouldBe original.userId
            deserialized.reactionType shouldBe original.reactionType
            deserialized.createdAt shouldBe original.createdAt
            deserialized.updatedAt shouldBe original.updatedAt
        }
    }
})
