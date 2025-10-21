package com.synapse.social.studioasinc

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.synapse.social.studioasinc.compatibility.FirebaseAuth
import com.synapse.social.studioasinc.adapter.CommentsAdapter
import com.synapse.social.studioasinc.model.Comment
import com.synapse.social.studioasinc.model.Reply
import java.text.DecimalFormat

class PostCommentsBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var body: LinearLayout
    private lateinit var comments_list: RecyclerView
    private lateinit var no_comments_body: LinearLayout
    private lateinit var loading_body: LinearLayout
    private lateinit var title_count: TextView
    private lateinit var profile_image_x: ImageView
    private lateinit var comment_send_input: EditText
    private lateinit var cancel_reply_mode: ImageView
    private lateinit var comment_send_button: ImageView
    private lateinit var comment_send_layout: LinearLayout
    private lateinit var profile_image_bg_2_x2: CardView
    private lateinit var emoji1: TextView
    private lateinit var emoji2: TextView
    private lateinit var emoji3: TextView
    private lateinit var emoji4: TextView
    private lateinit var emoji5: TextView
    private lateinit var emoji6: TextView
    private lateinit var emoji7: TextView
    private lateinit var close: LinearLayout

    private var postKey: String? = null
    private var postPublisherUID: String? = null
    private var postPublisherAvatar: String? = null
    private var replyToCommentKey: String? = null
    private var replyToComment = false

    private val viewModel: PostCommentsViewModel by viewModels()
    private lateinit var commentsAdapter: CommentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.synapse_comments_cbsd, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupClickListeners()
        setupRecyclerView()
        observeViewModel()

        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val display = requireActivity().windowManager.defaultDisplay
                val screenHeight = display.height
                val desiredHeight = screenHeight * 3 / 4
                val params = it.layoutParams
                params.height = desiredHeight
                it.layoutParams = params
                BottomSheetBehavior.from(it).peekHeight = desiredHeight
                BottomSheetBehavior.from(it).isHideable = true
                BottomSheetBehavior.from(it).isDraggable = true
                BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        arguments?.let {
            postKey = it.getString("postKey")
            postPublisherUID = it.getString("postPublisherUID")
            postPublisherAvatar = it.getString("postPublisherAvatar")
            loading_body.visibility = View.VISIBLE
            viewModel.getCommentCount(postKey!!)
            viewModel.getComments(postKey!!)
        }

        FirebaseAuth.getInstance().getCurrentUser()?.uid?.let { viewModel.getUserData(it) }
    }

    private fun initializeViews(view: View) {
        body = view.findViewById(R.id.body)
        comments_list = view.findViewById(R.id.comments_list)
        no_comments_body = view.findViewById(R.id.no_comments_body)
        loading_body = view.findViewById(R.id.loading_body)
        title_count = view.findViewById(R.id.title_count)
        profile_image_x = view.findViewById(R.id.profile_image_x)
        comment_send_input = view.findViewById(R.id.comment_send_input)
        cancel_reply_mode = view.findViewById(R.id.cancel_reply_mode)
        comment_send_button = view.findViewById(R.id.comment_send_button)
        comment_send_layout = view.findViewById(R.id.comment_send_layout)
        profile_image_bg_2_x2 = view.findViewById(R.id.profile_image_bg_2_x2)
        emoji1 = view.findViewById(R.id.emoji1)
        emoji2 = view.findViewById(R.id.emoji2)
        emoji3 = view.findViewById(R.id.emoji3)
        emoji4 = view.findViewById(R.id.emoji4)
        emoji5 = view.findViewById(R.id.emoji5)
        emoji6 = view.findViewById(R.id.emoji6)
        emoji7 = view.findViewById(R.id.emoji7)
        close = view.findViewById(R.id.close)
    }

    private fun setupClickListeners() {
        close.setOnClickListener { dismiss() }

        emoji1.setOnClickListener { comment_send_input.append("😁") }
        emoji2.setOnClickListener { comment_send_input.append("🥰") }
        emoji3.setOnClickListener { comment_send_input.append("😂") }
        emoji4.setOnClickListener { comment_send_input.append("😳") }
        emoji5.setOnClickListener { comment_send_input.append("😏") }
        emoji6.setOnClickListener { comment_send_input.append("😅") }
        emoji7.setOnClickListener { comment_send_input.append("🥺") }

        cancel_reply_mode.setOnClickListener {
            replyToComment = false
            replyToCommentKey = null
            comment_send_input.hint = getString(R.string.comment)
            cancel_reply_mode.visibility = View.GONE
        }

        comment_send_button.setOnClickListener {
            val commentText = comment_send_input.text.toString().trim()
            if (commentText.isNotEmpty()) {
                viewModel.postComment(postKey!!, commentText, replyToComment, replyToCommentKey)
                comment_send_input.setText("")
                replyToComment = false
                replyToCommentKey = null
                cancel_reply_mode.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter(
            onCommentLiked = { comment -> viewModel.likeComment(postKey!!, comment.key) },
            onReplyClicked = { comment ->
                replyToComment = true
                replyToCommentKey = comment.key
                comment_send_input.hint = "Replying to ${comment.uid}"
                cancel_reply_mode.visibility = View.VISIBLE
            },
            onCommentLongClicked = { comment -> showEditDeleteDialog(comment) },
            onShowReplies = { commentKey -> viewModel.getReplies(postKey!!, commentKey) },
            onReplyLiked = { reply -> viewModel.likeReply(postKey!!, reply.replyCommentkey, reply.key) },
            onReplyLongClicked = { reply -> showEditDeleteDialog(reply) }
        )
        comments_list.adapter = commentsAdapter
        comments_list.layoutManager = LinearLayoutManager(context)
    }

    private fun observeViewModel() {
        viewModel.comments.observe(viewLifecycleOwner, Observer { comments ->
            loading_body.visibility = View.GONE
            if (comments.isEmpty()) {
                no_comments_body.visibility = View.VISIBLE
                comments_list.visibility = View.GONE
            } else {
                no_comments_body.visibility = View.GONE
                comments_list.visibility = View.VISIBLE
                commentsAdapter.submitList(comments)
            }
        })

        viewModel.replies.observe(viewLifecycleOwner, Observer { replies ->
            val repliesMap = replies.groupBy { it.replyCommentkey }
            commentsAdapter.setReplies(repliesMap)
        })

        viewModel.commentCount.observe(viewLifecycleOwner, Observer { count ->
            _setCommentCount(title_count, count.toDouble())
        })

        viewModel.userAvatar.observe(viewLifecycleOwner, Observer { avatarUrl ->
            if (avatarUrl != null && avatarUrl != "null") {
                Glide.with(requireContext()).load(Uri.parse(avatarUrl)).into(profile_image_x)
            } else {
                profile_image_x.setImageResource(R.drawable.avatar)
            }
        })

        viewModel.userData.observe(viewLifecycleOwner, Observer { userMap ->
            commentsAdapter.setUserData(userMap)
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        })
    }

    private fun showEditDeleteDialog(comment: Comment) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditCommentDialog(comment)
                    1 -> showDeleteCommentDialog(comment)
                }
            }
            .show()
    }

    private fun showEditCommentDialog(comment: Comment) {
        val editText = EditText(requireContext()).apply { setText(comment.comment) }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Comment")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString()
                if (newText.isNotEmpty()) {
                    viewModel.editComment(postKey!!, comment.key, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteCommentDialog(comment: Comment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteComment(postKey!!, comment.key) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDeleteDialog(reply: Reply) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditReplyDialog(reply)
                    1 -> showDeleteReplyDialog(reply)
                }
            }
            .show()
    }

    private fun showEditReplyDialog(reply: Reply) {
        val editText = EditText(requireContext()).apply { setText(reply.comment) }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Reply")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString()
                if (newText.isNotEmpty()) {
                    viewModel.editReply(postKey!!, reply.replyCommentkey, reply.key, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteReplyDialog(reply: Reply) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Reply")
            .setMessage("Are you sure you want to delete this reply?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteReply(postKey!!, reply.replyCommentkey, reply.key) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun _setCommentCount(_txt: TextView, _number: Double) {
        if (_number < 10000) {
            _txt.text = "(" + _number.toLong().toString() + ")"
        } else {
            val decimalFormat = DecimalFormat("0.0")
            val numberFormat: String
            val formattedNumber: Double
            if (_number < 1000000) {
                numberFormat = "K"
                formattedNumber = _number / 1000
            } else if (_number < 1000000000) {
                numberFormat = "M"
                formattedNumber = _number / 1000000
            } else {
                numberFormat = "B"
                formattedNumber = _number / 1000000000
            }
            _txt.text = "(" + decimalFormat.format(formattedNumber) + numberFormat + ")"
        }
    }
}
