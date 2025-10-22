package com.synapse.social.studioasinc

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.launch

class PostMoreBottomSheetDialog : DialogFragment() {

    private lateinit var rootView: View
    private lateinit var dialog: BottomSheetDialog
    
    private lateinit var body: LinearLayout
    private lateinit var sldr: LinearLayout
    private lateinit var copyPostText: LinearLayout
    private lateinit var share: LinearLayout
    private lateinit var editPost: LinearLayout
    private lateinit var report: LinearLayout
    private lateinit var deletePost: LinearLayout
    private lateinit var editPostIc: ImageView
    private lateinit var editPostTitle: TextView
    private lateinit var deletePostIc: ImageView
    private lateinit var deletePostTitle: TextView
    private lateinit var copyPostTextIc: ImageView
    private lateinit var copyPostTextTitle: TextView
    private lateinit var shareIc: ImageView
    private lateinit var shareTitle: TextView
    private lateinit var reportIc: ImageView
    private lateinit var reportTitle: TextView
    
    private lateinit var authService: SupabaseAuthenticationService
    private lateinit var databaseService: SupabaseDatabaseService
    
    private var postKey: String? = null
    private var postPublisherUID: String? = null
    private var postType: String? = null
    private var postImg: String? = null
    private var postText: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = BottomSheetDialog(requireContext(), R.style.PostCommentsBottomSheetDialogStyle)
        rootView = View.inflate(context, R.layout.post_settings_cbsd, null)
        dialog.setContentView(rootView)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        
        initializeViews()
        initializeServices()
        setupClickListeners()
        setupDialog()
        loadArguments()
        applyDialogStyles()
        
        return dialog
    }

    private fun initializeViews() {
        body = rootView.findViewById(R.id.body)
        sldr = rootView.findViewById(R.id.sldr)
        copyPostText = rootView.findViewById(R.id.copyPostText)
        share = rootView.findViewById(R.id.share)
        editPost = rootView.findViewById(R.id.editPost)
        report = rootView.findViewById(R.id.report)
        deletePost = rootView.findViewById(R.id.deletePost)
        editPostIc = rootView.findViewById(R.id.editPostIc)
        editPostTitle = rootView.findViewById(R.id.editPostTitle)
        deletePostIc = rootView.findViewById(R.id.deletePostIc)
        deletePostTitle = rootView.findViewById(R.id.deletePostTitle)
        copyPostTextIc = rootView.findViewById(R.id.copyPostTextIc)
        copyPostTextTitle = rootView.findViewById(R.id.copyPostTextTitle)
        shareIc = rootView.findViewById(R.id.shareIc)
        shareTitle = rootView.findViewById(R.id.shareTitle)
        reportIc = rootView.findViewById(R.id.reportIc)
        reportTitle = rootView.findViewById(R.id.reportTitle)
    }

    private fun initializeServices() {
        authService = SupabaseAuthenticationService()
        databaseService = SupabaseDatabaseService()
    }

    private fun setupClickListeners() {
        copyPostText.setOnClickListener { copyPostTextToClipboard() }
        share.setOnClickListener { sharePost() }
        editPost.setOnClickListener { openEditPostActivity() }
        report.setOnClickListener { /* TODO: Implement report functionality */ }
        deletePost.setOnClickListener { deletePostDialog(postKey) }
    }

    private fun setupDialog() {
        val display = requireActivity().windowManager.defaultDisplay
        val screenHeight = display.height
        val desiredHeight = screenHeight * 2 / 4
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        body.layoutParams = params

        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                BottomSheetBehavior.from(it).apply {
                    isHideable = true
                    isDraggable = true
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    private fun loadArguments() {
        arguments?.let { args ->
            postKey = args.getString("postKey")
            postPublisherUID = args.getString("postPublisherUID")
            postType = args.getString("postType")
            postImg = args.getString("postImg")
            postText = args.getString("postText")
        }
    }

    private fun applyDialogStyles() {
        // Apply UI styling
        val d = requireActivity().resources.displayMetrics.density.toInt()
        val gradientDrawable = GradientDrawable().apply {
            setColor(0xFFFFFFFF.toInt())
            cornerRadii = floatArrayOf(
                d * 22f, d * 22f, d * 22f, d * 22f,
                d * 0f, d * 0f, d * 0f, d * 0f
            )
        }
        body.elevation = d * 1f
        body.background = gradientDrawable

        sldr.background = GradientDrawable().apply {
            cornerRadius = 300f
            setColor(0xFFEEEEEE.toInt())
        }

        // Apply view graphics to clickable items
        listOf(copyPostText, share, editPost, report, deletePost).forEach { view ->
            viewGraphics(view, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 0, 0, Color.TRANSPARENT)
        }

        // Set delete icon color
        deletePostIc.setColorFilter(0xFFF44336.toInt(), PorterDuff.Mode.SRC_ATOP)

        // Show/hide options based on post type and ownership
        if (postType == "TEXT") {
            copyPostText.visibility = View.VISIBLE
        } else {
            copyPostText.visibility = View.GONE
        }

        val currentUserId = authService.getCurrentUserId()
        val isOwner = postPublisherUID == currentUserId
        val isAdmin = authService.getCurrentUser()?.email == "mashikahamed0@gmail.com"

        if (isOwner || isAdmin) {
            share.visibility = View.VISIBLE
            editPost.visibility = View.VISIBLE
            report.visibility = View.GONE
            deletePost.visibility = View.VISIBLE
        } else {
            share.visibility = View.VISIBLE
            editPost.visibility = View.GONE
            report.visibility = View.VISIBLE
            deletePost.visibility = View.GONE
        }
    }

    private fun copyPostTextToClipboard() {
        if (!postText.isNullOrEmpty()) {
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Post text", postText)
            clipboard.setPrimaryClip(clip)
            SketchwareUtil.showMessage(requireActivity(), getString(R.string.text_copied_toast))
            dialog.dismiss()
        } else {
            SketchwareUtil.showMessage(requireActivity(), getString(R.string.no_text_to_copy))
        }
    }

    private fun sharePost() {
        val shareLink = "https://web-synapse.pages.dev/post.html?post=$postKey"
        val shareText = if (!postText.isNullOrEmpty()) {
            "$postText\n\n$shareLink"
        } else {
            shareLink
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_post_subject))
        }

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_post_chooser_title)))
        } catch (e: Exception) {
            SketchwareUtil.showMessage(requireActivity(), getString(R.string.share_error))
        }

        dialog.dismiss()
    }

    private fun openEditPostActivity() {
        postKey?.let { key ->
            lifecycleScope.launch {
                try {
                    val result = databaseService.selectById("posts", key)
                    
                    result.onSuccess { postData ->
                        if (postData != null) {
                            val editIntent = Intent(requireActivity(), EditPostActivity::class.java).apply {
                                putExtra("postKey", key)
                                putExtra("postText", postText)
                                putExtra("postImage", postImg)
                                putExtra("postType", postType)
                                
                                // Add post settings
                                putExtra("hideViewsCount", postData["post_hide_views_count"]?.toString()?.toBoolean() ?: false)
                                putExtra("hideLikesCount", postData["post_hide_like_count"]?.toString()?.toBoolean() ?: false)
                                putExtra("hideCommentsCount", postData["post_hide_comments_count"]?.toString()?.toBoolean() ?: false)
                                putExtra("hidePostFromEveryone", postData["post_visibility"]?.toString() == "private")
                                putExtra("disableSaveToFavorites", postData["post_disable_favorite"]?.toString()?.toBoolean() ?: false)
                                putExtra("disableComments", postData["post_disable_comments"]?.toString()?.toBoolean() ?: false)
                            }
                            
                            dialog.dismiss()
                            startActivity(editIntent)
                        }
                    }.onFailure {
                        SketchwareUtil.showMessage(requireActivity(), "Failed to load post data")
                    }
                } catch (e: Exception) {
                    SketchwareUtil.showMessage(requireActivity(), "Failed to load post data")
                }
            }
        }
    }

    private fun deletePostDialog(key: String?) {
        if (key == null) return

        val dialogBuilder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_synapse_bg_view, null)
        val alertDialog = dialogBuilder.create()
        alertDialog.setView(dialogView)
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialog_message)
        val dialogNoButton = dialogView.findViewById<TextView>(R.id.dialog_no_button)
        val dialogYesButton = dialogView.findViewById<TextView>(R.id.dialog_yes_button)

        dialogYesButton.setTextColor(0xFFF44336.toInt())
        viewGraphics(dialogYesButton, 0xFFFFFFFF.toInt(), 0xFFFFCDD2.toInt(), 28, 0, Color.TRANSPARENT)
        dialogNoButton.setTextColor(0xFF2196F3.toInt())
        viewGraphics(dialogNoButton, 0xFFFFFFFF.toInt(), 0xFFBBDEFB.toInt(), 28, 0, Color.TRANSPARENT)

        dialogTitle.text = getString(R.string.info)
        dialogMessage.text = getString(R.string.delete_post_dialog_message)
        dialogYesButton.text = getString(R.string.yes)
        dialogNoButton.text = getString(R.string.no)

        dialogYesButton.setOnClickListener {
            deletePostData(key)
            SketchwareUtil.showMessage(requireActivity(), getString(R.string.post_deleted_toast))
            alertDialog.dismiss()
            dialog.dismiss()
        }

        dialogNoButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.setCancelable(true)
        alertDialog.show()
    }

    private fun deletePostData(key: String) {
        lifecycleScope.launch {
            try {
                // Delete post and related data from Supabase
                databaseService.delete("posts", "id", key)
                databaseService.delete("post_comments", "post_id", key)
                databaseService.delete("post_likes", "post_id", key)
            } catch (e: Exception) {
                // Handle error silently or show error message
            }
        }
    }

    private fun viewGraphics(view: View, onFocus: Int, onRipple: Int, radius: Int, stroke: Int, strokeColor: Int) {
        val gradientDrawable = GradientDrawable().apply {
            setColor(onFocus)
            cornerRadius = radius.toFloat()
            setStroke(stroke, strokeColor)
        }
        val rippleDrawable = RippleDrawable(
            android.content.res.ColorStateList(arrayOf(intArrayOf()), intArrayOf(onRipple)),
            gradientDrawable,
            null
        )
        view.background = rippleDrawable
    }

    companion object {
        fun newInstance(
            postKey: String,
            postPublisherUID: String,
            postType: String,
            postImg: String?,
            postText: String?
        ): PostMoreBottomSheetDialog {
            return PostMoreBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString("postKey", postKey)
                    putString("postPublisherUID", postPublisherUID)
                    putString("postType", postType)
                    putString("postImg", postImg)
                    putString("postText", postText)
                }
            }
        }
    }
}