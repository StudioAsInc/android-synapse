package com.synapse.social.studioasinc

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.synapse.social.studioasinc.adapters.PostOptionsAdapter
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.PostActionItem
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class PostMoreBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var authService: SupabaseAuthenticationService
    private var post: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = SupabaseAuthenticationService()
        arguments?.getString("post_json")?.let {
            post = Json.decodeFromString(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_post_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val items = generateMenuItems()
        recyclerView.adapter = PostOptionsAdapter(items) {
            dismiss()
        }
    }

    private fun generateMenuItems(): List<PostActionItem> {
        val currentPost = post ?: return emptyList()
        val currentUserId = authService.getCurrentUserId()
        val isOwner = currentPost.authorUid == currentUserId
        val items = mutableListOf<PostActionItem>()

        if (isOwner) {
             items.add(PostActionItem("Edit", R.drawable.ic_edit_note_48px) { openEditPostActivity() })
             items.add(PostActionItem("Delete", R.drawable.ic_delete_48px, isDestructive = true) { deletePostDialog(currentPost) })
             items.add(PostActionItem("Archive", R.drawable.ic_download) { showToast("Archive feature coming soon") })
             items.add(PostActionItem("Turn off commenting", R.drawable.ic_visibility_off) { showToast("Feature coming soon") })
             items.add(PostActionItem("Pin to Profile", R.drawable.file_save_24px) { showToast("Feature coming soon") })
             items.add(PostActionItem("View Insights", R.drawable.data_usage_24px) { showPostStatistics(currentPost) })
             items.add(PostActionItem("Edit Alt Text", R.drawable.ic_text_fields_48px) { showToast("Feature coming soon") })
        } else {
             items.add(PostActionItem("Report", R.drawable.ic_report_48px, isDestructive = true) { reportPost(currentPost) })
             items.add(PostActionItem("Not Interested", R.drawable.ic_visibility_off) { hidePost(currentPost) })
             items.add(PostActionItem("Follow/Unfollow", R.drawable.ic_person) { showToast("Use profile button to follow") })
             items.add(PostActionItem("Block", R.drawable.mobile_block_24px, isDestructive = true) { blockUser(currentPost.authorUid) })
             items.add(PostActionItem("Mute", R.drawable.ic_notifications) { showToast("Mute feature coming soon") })
             items.add(PostActionItem("Turn on Notifications", R.drawable.ic_notifications) { showToast("Feature coming soon") })
             items.add(PostActionItem("Download Media", R.drawable.ic_download) { showToast("Download feature coming soon") })
             items.add(PostActionItem("View Alt Text", R.drawable.ic_text_fields_48px) { showToast("Feature coming soon") })
        }

        // Universal
        items.add(PostActionItem("Copy Link", R.drawable.ic_content_copy_48px) { copyPostTextToClipboard(currentPost) })
        items.add(PostActionItem("Save", R.drawable.file_save_24px) { showToast("Save feature coming soon") })
        items.add(PostActionItem("Share via...", R.drawable.ic_reply) { sharePost(currentPost) })

        return items
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun openEditPostActivity() {
        post?.let { p ->
            lifecycleScope.launch {
                try {
                     val result = SupabaseClient.client.from("posts").select {
                         filter { eq("id", p.id) }
                     }.decodeSingleOrNull<JsonObject>()

                     if (result != null) {
                        val editIntent = Intent(requireContext(), EditPostActivity::class.java).apply {
                            putExtra("postKey", p.id)
                            putExtra("post_id", p.id)
                            putExtra("postText", p.postText)
                            putExtra("postImage", p.postImage)
                            putExtra("postType", p.postType)

                            fun getBool(key: String): Boolean {
                                val primitive = result[key] as? JsonPrimitive
                                return primitive?.content?.toBoolean() ?: false
                            }
                            
                            fun getStringVal(key: String): String? {
                                return (result[key] as? JsonPrimitive)?.content
                            }

                            putExtra("hideViewsCount", getBool("post_hide_views_count"))
                            putExtra("hideLikesCount", getBool("post_hide_like_count"))
                            putExtra("hideCommentsCount", getBool("post_hide_comments_count"))
                            putExtra("hidePostFromEveryone", getStringVal("post_visibility") == "private")
                            putExtra("disableSaveToFavorites", getBool("post_disable_favorite"))
                            putExtra("disableComments", getBool("post_disable_comments"))
                        }

                        dismiss()
                        startActivity(editIntent)
                     } else {
                         showToast("Failed to load post data")
                     }
                } catch (e: Exception) {
                    showToast("Failed to load post data")
                }
            }
        }
    }

    private fun deletePostDialog(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_message_confirmation)
            .setMessage(R.string.delete_post_dialog_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    try {
                        SupabaseClient.client.from("posts").delete {
                            filter {
                                eq("id", post.id)
                            }
                        }
                        showToast(getString(R.string.post_deleted_toast))
                    } catch (e: Exception) {
                        showToast("Failed to delete post: ${e.message}")
                    }
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun copyPostTextToClipboard(post: Post) {
        val link = "https://synapse.app/post/${post.id}"
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Post Link", link)
        clipboard.setPrimaryClip(clip)
        showToast("Link copied")
    }

    private fun sharePost(post: Post) {
        val shareText = buildString {
             append("Check out this post on Synapse!\n\n")
             if (!post.postText.isNullOrEmpty()) {
                 append(post.postText)
                 append("\n\n")
             }
             append("https://synapse.app/post/${post.id}")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_post_subject))
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_post_chooser_title)))
    }

    private fun reportPost(post: Post) {
        val reasons = arrayOf("Spam", "Harassment", "Hate speech", "Violence", "Other")
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.report_title)
            .setItems(reasons) { _, which ->
                 lifecycleScope.launch {
                    try {
                        val currentUid = authService.getCurrentUserId()
                        if (currentUid != null) {
                             val reportData = buildJsonObject {
                                put("reporter_id", JsonPrimitive(currentUid))
                                put("post_id", JsonPrimitive(post.id))
                                put("reason", JsonPrimitive(reasons[which]))
                                put("status", JsonPrimitive("pending"))
                            }
                            SupabaseClient.client.from("post_reports").insert(reportData)
                            showToast(getString(R.string.report_submitted))
                        }
                    } catch (e: Exception) {
                        showToast("Failed to submit report")
                    }
                }
            }
            .show()
    }

    private fun hidePost(post: Post) {
         lifecycleScope.launch {
            try {
                val currentUid = authService.getCurrentUserId()
                if (currentUid != null) {
                    val hideData = buildJsonObject {
                        put("user_id", JsonPrimitive(currentUid))
                        put("post_id", JsonPrimitive(post.id))
                    }
                    SupabaseClient.client.from("hidden_posts").insert(hideData)
                    showToast("Post hidden")
                }
            } catch (e: Exception) {
                showToast("Failed to hide post")
            }
        }
    }

    private fun blockUser(userId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Block User")
            .setMessage("Are you sure you want to block this user?")
            .setPositiveButton("Block") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val currentUid = authService.getCurrentUserId()
                        if (currentUid != null) {
                            val blockData = buildJsonObject {
                                put("blocker_id", JsonPrimitive(currentUid))
                                put("blocked_id", JsonPrimitive(userId))
                            }
                            SupabaseClient.client.from("blocks").insert(blockData)
                            showToast("User blocked")
                        }
                    } catch (e: Exception) {
                        showToast("Failed to block user")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPostStatistics(post: Post) {
        val message = "Likes: ${post.likesCount}\nComments: ${post.commentsCount}\nViews: ${post.viewsCount}"
        AlertDialog.Builder(requireContext())
            .setTitle("Statistics")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    companion object {
        fun newInstance(post: Post): PostMoreBottomSheetDialog {
            return PostMoreBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString("post_json", Json.encodeToString(post))
                }
            }
        }
    }
}
