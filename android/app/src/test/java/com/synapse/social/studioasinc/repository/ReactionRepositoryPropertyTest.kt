package com.synapse.social.studioasinc.repository

import com.synapse.social.studioasinc.data.repository.ReactionRepository
import com.synapse.social.studioasinc.data.repository.ReactionToggleResult
import com.synapse.social.studioasinc.model.ReactionType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for ReactionRepository.
 * Tests the core logic for reaction toggle consistency and aggregation accuracy.
 * 
 * Requirements: 3.2, 3.3, 3.4, 3.5, 6.2, 6.3, 6.4
 */
class ReactionRepositoryPropertyTest : StringSpec({

    val repository = ReactionRepository()

    // ==================== ARBITRARY GENERATORS ====================

    val arbReactionType = Arb.enum<ReactionType>()

    val arbReactionTypeOrNull = arbReactionType.orNull()

    val arbReactionList = Arb.list(arbReactionType, 0..100)

    // ==================== PROPERTY 6: REACTION TOGGLE CONSISTENCY ====================

    /**
     * **Feature: detailed-post-view, Property 6: Reaction toggle consistency**
     * 
     * For any user and post, toggling a reaction SHALL either add the reaction 
     * (if none exists or different type) or remove it (if same type exists), 
     * and the reaction count SHALL reflect the change.
     * 
     * **Validates: Requirements 3.2, 3.3, 3.4**
     */
    "Property 6: Toggling with no existing reaction adds the reaction" {
        checkAll(100, arbReactionType) { newReaction ->
            val result = repository.determineToggleResult(
                existingReactionType = null,
                newReactionType = newReaction
            )
            result shouldBe ReactionToggleResult.ADDED
        }
    }

    "Property 6: Toggling same reaction type removes the reaction" {
        checkAll(100, arbReactionType) { reactionType ->
            val result = repository.determineToggleResult(
                existingReactionType = reactionType,
                newReactionType = reactionType
            )
            result shouldBe ReactionToggleResult.REMOVED
        }
    }

    "Property 6: Toggling different reaction type updates the reaction" {
        checkAll(100, arbReactionType, arbReactionType) { existing, newReaction ->
            if (existing != newReaction) {
                val result = repository.determineToggleResult(
                    existingReactionType = existing,
                    newReactionType = newReaction
                )
                result shouldBe ReactionToggleResult.UPDATED
            }
        }
    }

    "Property 6: Toggle result is deterministic for same inputs" {
        checkAll(100, arbReactionTypeOrNull, arbReactionType) { existing, newReaction ->
            val result1 = repository.determineToggleResult(existing, newReaction)
            val result2 = repository.determineToggleResult(existing, newReaction)
            result1 shouldBe result2
        }
    }

    "Property 6: Toggle result covers all possible outcomes" {
        checkAll(100, arbReactionTypeOrNull, arbReactionType) { existing, newReaction ->
            val result = repository.determineToggleResult(existing, newReaction)
            
            when {
                existing == null -> result shouldBe ReactionToggleResult.ADDED
                existing == newReaction -> result shouldBe ReactionToggleResult.REMOVED
                else -> result shouldBe ReactionToggleResult.UPDATED
            }
        }
    }

    // ==================== PROPERTY 7: REACTION AGGREGATION ACCURACY ====================

    /**
     * **Feature: detailed-post-view, Property 7: Reaction aggregation accuracy**
     * 
     * For any post with reactions, the reaction summary SHALL correctly count 
     * reactions grouped by type.
     * 
     * **Validates: Requirements 3.5**
     */
    "Property 7: Reaction summary total equals input list size" {
        checkAll(100, arbReactionList) { reactions ->
            val summary = repository.calculateReactionSummary(reactions)
            val totalCount = summary.values.sum()
            totalCount shouldBe reactions.size
        }
    }

    "Property 7: Each reaction type count matches actual occurrences" {
        checkAll(100, arbReactionList) { reactions ->
            val summary = repository.calculateReactionSummary(reactions)
            
            ReactionType.values().forEach { reactionType ->
                val expectedCount = reactions.count { it == reactionType }
                val actualCount = summary[reactionType] ?: 0
                actualCount shouldBe expectedCount
            }
        }
    }

    "Property 7: Empty reaction list produces empty summary" {
        val summary = repository.calculateReactionSummary(emptyList())
        summary.isEmpty() shouldBe true
    }

    "Property 7: Single reaction produces summary with count 1" {
        checkAll(100, arbReactionType) { reactionType ->
            val summary = repository.calculateReactionSummary(listOf(reactionType))
            summary[reactionType] shouldBe 1
            summary.values.sum() shouldBe 1
        }
    }

    "Property 7: isReactionSummaryAccurate validates correctly" {
        checkAll(100, arbReactionList) { reactions ->
            val summary = repository.calculateReactionSummary(reactions)
            repository.isReactionSummaryAccurate(summary, reactions.size) shouldBe true
        }
    }

    "Property 7: isReactionSummaryAccurate detects incorrect totals" {
        checkAll(100, arbReactionList) { reactions ->
            if (reactions.isNotEmpty()) {
                val summary = repository.calculateReactionSummary(reactions)
                // Wrong total should return false
                repository.isReactionSummaryAccurate(summary, reactions.size + 1) shouldBe false
                repository.isReactionSummaryAccurate(summary, reactions.size - 1) shouldBe false
            }
        }
    }

    // ==================== PROPERTY 14: COMMENT REACTION TOGGLE ====================

    /**
     * **Feature: detailed-post-view, Property 14: Comment reaction toggle**
     * 
     * For any user and comment, toggling a comment reaction SHALL either add 
     * or remove the reaction, updating the comment_reactions table accordingly.
     * 
     * **Validates: Requirements 6.2, 6.3, 6.4**
     * 
     * Note: The toggle logic for comment reactions uses the same determineToggleResult
     * method as post reactions, so these tests validate the shared logic.
     */
    "Property 14: Comment reaction toggle follows same rules as post reactions" {
        checkAll(100, arbReactionTypeOrNull, arbReactionType) { existing, newReaction ->
            // The toggle logic is identical for posts and comments
            val result = repository.determineToggleResult(existing, newReaction)
            
            when {
                existing == null -> result shouldBe ReactionToggleResult.ADDED
                existing == newReaction -> result shouldBe ReactionToggleResult.REMOVED
                else -> result shouldBe ReactionToggleResult.UPDATED
            }
        }
    }

    "Property 14: Comment reaction aggregation follows same rules as post reactions" {
        checkAll(100, arbReactionList) { reactions ->
            val summary = repository.calculateReactionSummary(reactions)
            
            // Total should match input size
            summary.values.sum() shouldBe reactions.size
            
            // Each type count should match actual occurrences
            ReactionType.values().forEach { reactionType ->
                val expectedCount = reactions.count { it == reactionType }
                val actualCount = summary[reactionType] ?: 0
                actualCount shouldBe expectedCount
            }
        }
    }
})
