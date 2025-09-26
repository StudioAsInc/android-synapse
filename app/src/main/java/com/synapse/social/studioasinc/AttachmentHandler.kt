package com.synapse.social.studioasinc

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.attachments.Rv_attacmentListAdapter
import com.synapse.social.studioasinc.util.ChatMessageManager

class AttachmentHandler(
    private val activity: ChatActivity,
    private val attachmentLayoutListHolder: View,
    private val rv_attacmentList: RecyclerView,
    private var attactmentmap: ArrayList<HashMap<String, Any>>,
    private val close_attachments_btn: View,
    private val galleryBtn: View,
    private val auth: FirebaseAuth
) {

    fun setup() {
        galleryBtn.setOnClickListener {
            StorageUtil.pickMultipleFiles(activity, "*/*", activity.REQ_CD_IMAGE_PICKER)
        }

        close_attachments_btn.setOnClickListener {
            attachmentLayoutListHolder.visibility = View.GONE
            val oldSize = attactmentmap.size
            if (oldSize > 0) {
                attactmentmap.clear()
                rv_attacmentList.adapter?.notifyItemRangeRemoved(0, oldSize)
            }

            val drafts: SharedPreferences = activity.getSharedPreferences("chat_drafts", Context.MODE_PRIVATE)
            auth.currentUser?.let { user ->
                val chatId = ChatMessageManager.getChatId(
                    user.uid,
                    activity.intent.getStringExtra("uid")
                )
                drafts.edit().remove(chatId + "_attachments").apply()
                PresenceManager.setActivity(user.uid, "Idle")
            }
        }

        val attachmentAdapter = Rv_attacmentListAdapter(activity, attactmentmap, attachmentLayoutListHolder)
        rv_attacmentList.adapter = attachmentAdapter
        rv_attacmentList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

        val attachmentSpacing = activity.resources.getDimension(R.dimen.spacing_small).toInt()
        rv_attacmentList.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position == 0) {
                    outRect.left = attachmentSpacing
                }
                outRect.right = attachmentSpacing
            }
        })
    }

    fun startUploadForItem(position: Int) {
        activity._startUploadForItem(position.toDouble())
    }

    public fun resetAttachmentState() {
        if (attachmentLayoutListHolder != null) {
            attachmentLayoutListHolder.visibility = View.GONE
        }
        if (rv_attacmentList.adapter != null) {
            val oldSize = attactmentmap.size
            if (oldSize > 0) {
                attactmentmap.clear()
                rv_attacmentList.adapter?.notifyItemRangeRemoved(0, oldSize)
            }
        }
    }
}