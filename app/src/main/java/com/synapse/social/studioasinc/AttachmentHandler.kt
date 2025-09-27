package com.synapse.social.studioasinc

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.attachments.Rv_attacmentListAdapter
import com.synapse.social.studioasinc.util.ChatMessageManager
import java.io.File

interface AttachmentUploadListener {
    fun onUploadSuccess(path: String)
    fun onResetAttachments()
}

class AttachmentHandler(
    private val activity: ChatActivity,
    private val attachmentLayoutListHolder: View,
    private val rv_attacmentList: RecyclerView,
    private var attactmentmap: ArrayList<HashMap<String, Any>>,
    private val close_attachments_btn: View,
    private val galleryBtn: View,
    private val auth: FirebaseAuth,
    private val listener: AttachmentUploadListener
) {

    fun setup() {
        galleryBtn.setOnClickListener {
            StorageUtil.pickMultipleFiles(activity, "*/*", activity.REQ_CD_IMAGE_PICKER)
        }

        close_attachments_btn.setOnClickListener {
            resetAttachmentState()
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
        if (auth.currentUser != null) {
            PresenceManager.setActivity(auth.currentUser!!.uid, "Sending an attachment")
        }

        if (position < 0 || position >= attactmentmap.size) {
            Log.e("AttachmentHandler", "Invalid position for upload: $position, size: ${attactmentmap.size}")
            return
        }

        if (!SketchwareUtil.isConnected(activity)) {
            try {
                val itemMap = attactmentmap[position]
                itemMap["uploadState"] = "failed"
                rv_attacmentList.adapter?.notifyItemChanged(position)
            } catch (e: Exception) {
                Log.e("AttachmentHandler", "Error updating upload state: " + e.message)
            }
            return
        }

        val itemMap = attactmentmap[position]
        if (itemMap["uploadState"] != "pending") {
            return
        }

        itemMap["uploadState"] = "uploading"
        itemMap["uploadProgress"] = 0.0
        rv_attacmentList.adapter?.notifyItemChanged(position)

        val filePath = itemMap["localPath"].toString()
        if (filePath.isEmpty()) {
            Log.e("AttachmentHandler", "Invalid file path for upload")
            itemMap["uploadState"] = "failed"
            rv_attacmentList.adapter?.notifyItemChanged(position)
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AttachmentHandler", "File does not exist: $filePath")
            itemMap["uploadState"] = "failed"
            rv_attacmentList.adapter?.notifyItemChanged(position)
            return
        }

        AsyncUploadService.uploadWithNotification(activity, filePath, file.name, object : AsyncUploadService.UploadProgressListener {
            override fun onProgress(filePath: String, percent: Int) {
                try {
                    if (position >= 0 && position < attactmentmap.size) {
                        val currentItem = attactmentmap[position]
                        if (filePath == currentItem["localPath"]) {
                            currentItem["uploadProgress"] = percent.toDouble()
                            rv_attacmentList.adapter?.notifyItemChanged(position)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AttachmentHandler", "Error updating upload progress: " + e.message)
                }
            }

            override fun onSuccess(filePath: String, url: String, publicId: String) {
                try {
                    if (position >= 0 && position < attactmentmap.size) {
                        val mapToUpdate = attactmentmap[position]
                        if (filePath == mapToUpdate["localPath"]) {
                            mapToUpdate["uploadState"] = "success"
                            mapToUpdate["cloudinaryUrl"] = url
                            mapToUpdate["publicId"] = publicId
                            rv_attacmentList.adapter?.notifyItemChanged(position)
                            listener.onUploadSuccess(url)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AttachmentHandler", "Error updating upload success: " + e.message)
                }
            }

            override fun onFailure(filePath: String, error: String) {
                try {
                    if (position >= 0 && position < attactmentmap.size) {
                        val currentItem = attactmentmap[position]
                        if (filePath == currentItem["localPath"]) {
                            currentItem["uploadState"] = "failed"
                            rv_attacmentList.adapter?.notifyItemChanged(position)
                        }
                    }
                    Log.e("AttachmentHandler", "Upload failed: $error")
                } catch (e: Exception) {
                    Log.e("AttachmentHandler", "Error updating upload failure: " + e.message)
                }
            }
        })
    }

    fun resetAttachmentState() {
        attachmentLayoutListHolder.visibility = View.GONE
        val oldSize = attactmentmap.size
        if (oldSize > 0) {
            attactmentmap.clear()
            rv_attacmentList.adapter?.notifyItemRangeRemoved(0, oldSize)
        }
        listener.onResetAttachments()
    }
}