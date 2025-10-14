// To-do: Migrate Firebase to Supabase
// 1. Replace `FirebaseAuth` with Supabase authentication.
//    - Remove the `FirebaseAuth` import.
//    - Update the constructor to accept a Supabase user or session.
// 2. Refactor chat draft management.
//    - The logic for saving and clearing chat drafts in `close_attachments_btn.setOnClickListener` uses SharedPreferences with a key derived from the Firebase user ID.
//    - This should be updated to use the Supabase user ID.
// 3. Replace `PresenceManager` with a Supabase Realtime implementation.
//    - The `PresenceManager.setActivity` call likely uses Firebase Realtime Database.
//    - This needs to be replaced with a Supabase Realtime equivalent for tracking user presence.
// 4. Investigate `StorageUtil`.
//    - The `StorageUtil.pickMultipleFiles` method is used for file selection.
//    - It's important to ensure that the subsequent upload process is migrated to Supabase Storage.

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
            val chatId = ChatMessageManager.getChatId(
                auth.currentUser!!.uid,
                activity.intent.getStringExtra("uid")
            )
            drafts.edit().remove(chatId + "_attachments").apply()
            if (auth.currentUser != null) {
                PresenceManager.setActivity(auth.currentUser!!.uid, "Idle")
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