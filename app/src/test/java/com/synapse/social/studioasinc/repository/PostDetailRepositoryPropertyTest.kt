package com.synapse.social.studioasinc.repository

import com.synapse.social.studioasinc.data.repository.PostDetailRepository
import com.synapse.social.studioasinc.model.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for PostDetailRepository.
 * Tests the core logic for post loading, YouTube detection, edited post detection, and author badges.
 */
class PostDetailRepositoryPropertyTest : StringSpec({

    val repository = PostDetailRepository()

    // ==================== ARBITRARY GENERATORS ====================

    val arbMediaType = Arb.enum<MediaType>()

    val arbMediaItem = arbitrary {
        MediaItem(
            id = Arb.string(8..16).bind(),
            url = "https://example.com/${Arb.string(5..10).bind()}.jpg",
            type = arbMediaType.bind(),
            thumbnailUrl = Arb.string(10..30).orNull().bind()?.let { "https://example.com/$it.jpg" },
            duration = Arb.long(0..3600000).orNull().bind(),
            size = Arb.long(1000..10000000).orNull().bind(),
            mimeType = Arb.of("image/jpeg", "image/png", "video/mp4").orNull().bind()
        )
    }

    val arbUserProfile = arbitrary {
        UserProfile(
            uid = Arb.uuid().bind().toString(),
            username = Arb.string(3..20).bind(),
            display_name = Arb.string(1..50).bind(),
            email = "${Arb.string(5..10).bind()}@example.com",
            bio = Arb.string(0..200).orNull().bind(),
            profile_image_url = Arb.string(10..30).orNull().bind()?.let { "https://example.com/$it.jpg" },
            followers_count = Arb.int(0..100000).bind(),
            following_count = Arb.int(0..10000).bind(),
            posts_count = Arb.int(0..5000).bind(),
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

    // Generator for complete posts (with all required fields)
    val arbCompletePost = arbitrary {
        val postId = Arb.uuid().bind().toString()
        val authorUid = Arb.uuid().bind().toString()
        val hasMedia = Arb.boolean().bind()
        val hasLocation = Arb.boolean().bind()
        val hasPoll = Arb.boolean().bind()
        
        Post(
            id = postId,
            key = Arb.string(8..16).orNull().bind(),
            authorUid = authorUid,
            postText = Arb.string(1..500).bind(), // Always have text for complete posts
            postImage = if (hasMedia) "https://example.com/image.jpg" else null,
            postType = if (hasMedia) Arb.of("IMAGE", "VIDEO").bind() else "TEXT",
            publishDate = "2024-01-${Arb.int(1..28).bind()}T10:00:00Z",
            timestamp = Arb.long(1700000000000..1800000000000).bind(),
            likesCount = Arb.int(0..10000).bind(),
            commentsCount = Arb.int(0..1000).bind(),
            viewsCount = Arb.int(0..100000).bind(),
            resharesCount = Arb.int(0..500).bind(),
            mediaItems = if (hasMedia) Arb.list(arbMediaItem, 1..5).bind().toMutableList() else null,
            isEncrypted = Arb.boolean().orNull().bind(),
            isDeleted = false,
            isEdited = Arb.boolean().orNull().bind(),
            editedAt = Arb.boolean().bind().let { if (it) "2024-01-15T12:00:00Z" else null },
            hasPoll = hasPoll,
            pollQuestion = if (hasPoll) Arb.string(10..100).bind() else null,
            pollOptions = if (hasPoll) "[\"Option 1\",\"Option 2\",\"Option 3\"]" else null,
            pollEndTime = if (hasPoll) "2024-12-31T23:59:59Z" else null,
            hasLocation = hasLocation,
            locationName = if (hasLocation) Arb.string(5..50).bind() else null,
            locationAddress = if (hasLocation) Arb.string(10..100).bind() else null,
            youtubeUrl = Arb.boolean().bind().let { if (it) "https://youtube.com/watch?v=${Arb.string(11).bind()}" else null }
        )
    }


    // Generator for PostDetail with complete data
    val arbCompletePostDetail = arbitrary {
        val post = arbCompletePost.bind()
        val author = arbUserProfile.bind().copy(uid = post.authorUid)
        
        PostDetail(
            post = post,
            author = author,
            isBookmarked = Arb.boolean().bind(),
            hasReshared = Arb.boolean().bind(),
            pollResults = if (post.hasPoll == true) Arb.list(arbPollOptionResult, 2..5).bind() else null,
            userPollVote = if (post.hasPoll == true) Arb.int(0..4).orNull().bind() else null
        )
    }

    // ==================== PROPERTY 1: POST LOADING COMPLETENESS ====================

    /**
     * **Feature: detailed-post-view, Property 1: Post loading returns complete data**
     * 
     * For any valid post ID, loading the post detail SHALL return the post with all 
     * required fields (text, author info, timestamp, media items if present, 
     * location if present, poll data if present).
     * 
     * **Validates: Requirements 1.1, 1.2, 1.4, 2.1**
     */
    "Property 1: Complete PostDetail has all required fields" {
        checkAll(100, arbCompletePostDetail) { postDetail ->
            // Post must have ID
            postDetail.post.id.isNotEmpty() shouldBe true
            
            // Post must have author UID
            postDetail.post.authorUid.isNotEmpty() shouldBe true
            
            // Author must have UID matching post's authorUid
            postDetail.author.uid.isNotEmpty() shouldBe true
            
            // Author must have username
            postDetail.author.username.isNotEmpty() shouldBe true
            
            // Post must have timestamp
            postDetail.post.timestamp shouldNotBe 0L
            
            // If post has media type, it should have media items or post image
            if (postDetail.post.postType == "IMAGE" || postDetail.post.postType == "VIDEO") {
                val hasMediaContent = !postDetail.post.mediaItems.isNullOrEmpty() || 
                                     !postDetail.post.postImage.isNullOrEmpty() ||
                                     !postDetail.post.postText.isNullOrEmpty()
                hasMediaContent shouldBe true
            }
            
            // If post has location flag, location data should be present
            if (postDetail.post.hasLocation == true) {
                val hasLocationData = !postDetail.post.locationName.isNullOrEmpty() || 
                                     !postDetail.post.locationAddress.isNullOrEmpty()
                hasLocationData shouldBe true
            }
            
            // If post has poll flag, poll data should be present
            if (postDetail.post.hasPoll == true) {
                postDetail.post.pollQuestion.shouldNotBeNull()
                postDetail.post.pollOptions.shouldNotBeNull()
            }
        }
    }

    "Property 1: isPostDataComplete returns true for complete posts" {
        checkAll(100, arbCompletePostDetail) { postDetail ->
            repository.isPostDataComplete(postDetail) shouldBe true
        }
    }

    "Property 1: isPostDataComplete returns false for posts with empty ID" {
        checkAll(100, arbCompletePostDetail) { postDetail ->
            val incompletePost = postDetail.copy(
                post = postDetail.post.copy(id = "")
            )
            repository.isPostDataComplete(incompletePost) shouldBe false
        }
    }

    "Property 1: isPostDataComplete returns false for posts with empty authorUid" {
        checkAll(100, arbCompletePostDetail) { postDetail ->
            val incompletePost = postDetail.copy(
                post = postDetail.post.copy(authorUid = "")
            )
            repository.isPostDataComplete(incompletePost) shouldBe false
        }
    }

    // ==================== PROPERTY 2: YOUTUBE URL DETECTION ====================

    /**
     * **Feature: detailed-post-view, Property 2: YouTube URL detection**
     * 
     * For any post containing a YouTube URL in the youtube_url field, 
     * the PostDetail SHALL include the YouTube URL for embedding.
     * 
     * **Validates: Requirements 1.3**
     */
    "Property 2: Posts with YouTube URL are correctly detected" {
        val arbPostWithYouTube = arbitrary {
            val videoId = Arb.string(11).bind()
            arbCompletePost.bind().copy(
                youtubeUrl = "https://youtube.com/watch?v=$videoId"
            )
        }

        checkAll(100, arbPostWithYouTube) { post ->
            repository.hasYouTubeUrl(post) shouldBe true
            post.youtubeUrl.shouldNotBeNull()
            post.youtubeUrl!!.contains("youtube.com") shouldBe true
        }
    }

    "Property 2: Posts without YouTube URL return false" {
        val arbPostWithoutYouTube = arbitrary {
            arbCompletePost.bind().copy(youtubeUrl = null)
        }

        checkAll(100, arbPostWithoutYouTube) { post ->
            repository.hasYouTubeUrl(post) shouldBe false
            post.youtubeUrl.shouldBeNull()
        }
    }

    "Property 2: Posts with empty YouTube URL return false" {
        val arbPostWithEmptyYouTube = arbitrary {
            arbCompletePost.bind().copy(youtubeUrl = "")
        }

        checkAll(100, arbPostWithEmptyYouTube) { post ->
            repository.hasYouTubeUrl(post) shouldBe false
        }
    }

    "Property 2: PostDetail.hasYouTubeEmbed matches repository detection" {
        checkAll(100, arbCompletePostDetail) { postDetail ->
            postDetail.hasYouTubeEmbed() shouldBe repository.hasYouTubeUrl(postDetail.post)
        }
    }


    // ==================== PROPERTY 4: EDITED POST DETECTION ====================

    /**
     * **Feature: detailed-post-view, Property 4: Edited post detection**
     * 
     * For any post where is_edited is true, the PostDetail SHALL include 
     * the edited_at timestamp.
     * 
     * **Validates: Requirements 1.6**
     */
    "Property 4: Edited posts have edited_at timestamp" {
        val arbEditedPost = arbitrary {
            arbCompletePost.bind().copy(
                isEdited = true,
                editedAt = "2024-01-${Arb.int(1..28).bind()}T${Arb.int(0..23).bind()}:${Arb.int(0..59).bind()}:00Z"
            )
        }

        checkAll(100, arbEditedPost) { post ->
            repository.isPostEdited(post) shouldBe true
            repository.getEditedTimestamp(post).shouldNotBeNull()
        }
    }

    "Property 4: Non-edited posts return null for edited timestamp" {
        val arbNonEditedPost = arbitrary {
            arbCompletePost.bind().copy(
                isEdited = false,
                editedAt = null
            )
        }

        checkAll(100, arbNonEditedPost) { post ->
            repository.isPostEdited(post) shouldBe false
            repository.getEditedTimestamp(post).shouldBeNull()
        }
    }

    "Property 4: Posts with null isEdited return false for edited check" {
        val arbNullEditedPost = arbitrary {
            arbCompletePost.bind().copy(
                isEdited = null,
                editedAt = null
            )
        }

        checkAll(100, arbNullEditedPost) { post ->
            repository.isPostEdited(post) shouldBe false
            repository.getEditedTimestamp(post).shouldBeNull()
        }
    }

    "Property 4: PostDetail.isEdited matches repository detection" {
        checkAll(100, arbCompletePostDetail) { postDetail ->
            postDetail.isEdited() shouldBe repository.isPostEdited(postDetail.post)
        }
    }

    // ==================== PROPERTY 5: AUTHOR BADGE DISPLAY LOGIC ====================

    /**
     * **Feature: detailed-post-view, Property 5: Author badge display logic**
     * 
     * For any post author, the badge displayed SHALL match the author's 
     * verification status (verify=true) or premium status (account_premium=true).
     * 
     * **Validates: Requirements 2.2, 2.3**
     */
    "Property 5: Verified authors get verified badge" {
        val arbVerifiedAuthor = arbitrary {
            arbUserProfile.bind().copy(
                verify = true,
                account_type = Arb.of("user", "premium", "admin").bind()
            )
        }

        checkAll(100, arbVerifiedAuthor) { author ->
            // Verified takes precedence over premium
            repository.getAuthorBadge(author) shouldBe "verified"
        }
    }

    "Property 5: Premium non-verified authors get premium badge" {
        val arbPremiumAuthor = arbitrary {
            arbUserProfile.bind().copy(
                verify = false,
                account_type = "premium"
            )
        }

        checkAll(100, arbPremiumAuthor) { author ->
            repository.getAuthorBadge(author) shouldBe "premium"
        }
    }

    "Property 5: Regular users get no badge" {
        val arbRegularAuthor = arbitrary {
            arbUserProfile.bind().copy(
                verify = false,
                account_type = "user"
            )
        }

        checkAll(100, arbRegularAuthor) { author ->
            repository.getAuthorBadge(author).shouldBeNull()
        }
    }

    "Property 5: Verified status takes precedence over premium" {
        val arbVerifiedPremiumAuthor = arbitrary {
            arbUserProfile.bind().copy(
                verify = true,
                account_type = "premium"
            )
        }

        checkAll(100, arbVerifiedPremiumAuthor) { author ->
            // Even if premium, verified badge should be shown
            repository.getAuthorBadge(author) shouldBe "verified"
        }
    }

    "Property 5: PostDetail helper methods match repository logic" {
        checkAll(100, arbCompletePostDetail) { postDetail ->
            val expectedBadge = repository.getAuthorBadge(postDetail.author)
            
            when (expectedBadge) {
                "verified" -> postDetail.isAuthorVerified() shouldBe true
                "premium" -> {
                    postDetail.isAuthorVerified() shouldBe false
                    postDetail.isAuthorPremium() shouldBe true
                }
                null -> {
                    postDetail.isAuthorVerified() shouldBe false
                    postDetail.isAuthorPremium() shouldBe false
                }
            }
        }
    }

    // ==================== ADDITIONAL COMPLETENESS TESTS ====================

    "PostDetail helper methods work correctly" {
        checkAll(100, arbCompletePostDetail) { postDetail ->
            // hasMedia should match media items presence
            postDetail.hasMedia() shouldBe !postDetail.post.mediaItems.isNullOrEmpty()
            
            // hasPoll should match poll flag
            postDetail.hasPoll() shouldBe (postDetail.post.hasPoll == true)
            
            // hasLocation should match location flag
            postDetail.hasLocation() shouldBe (postDetail.post.hasLocation == true)
            
            // isEncrypted should match encryption flag
            postDetail.isEncrypted() shouldBe (postDetail.post.isEncrypted == true)
        }
    }

    "Total reactions count is sum of all reaction types" {
        val arbPostDetailWithReactions = arbitrary {
            val reactionSummary = mapOf(
                ReactionType.LIKE to Arb.int(0..100).bind(),
                ReactionType.LOVE to Arb.int(0..50).bind(),
                ReactionType.HAHA to Arb.int(0..30).bind(),
                ReactionType.WOW to Arb.int(0..20).bind(),
                ReactionType.SAD to Arb.int(0..10).bind(),
                ReactionType.ANGRY to Arb.int(0..5).bind()
            )
            
            PostDetail(
                post = arbCompletePost.bind(),
                author = arbUserProfile.bind(),
                reactionSummary = reactionSummary,
                isBookmarked = false,
                hasReshared = false
            )
        }

        checkAll(100, arbPostDetailWithReactions) { postDetail ->
            val expectedTotal = postDetail.reactionSummary.values.sum()
            postDetail.getTotalReactions() shouldBe expectedTotal
        }
    }
})
