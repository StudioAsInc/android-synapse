package com.synapse.social.studioasinc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.synapse.social.studioasinc.attachments.Rv_attacmentListAdapter
import com.synapse.social.studioasinc.backend.SupabaseClient.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import java.util.UUID

class ChatGroupActivity : AppCompatActivity(), ChatAdapterListener {

    private var oldestMessageKey: String? = null
    private var isLoading = false
    private val CHAT_PAGE_SIZE = 80
    private var chatAdapter: ChatAdapter? = null
    private val messageKeys: MutableSet<String> = HashSet()
    private val ChatMessagesList: ArrayList<HashMap<String, Any>> = ArrayList()
    private val repliedMessagesCache: HashMap<String, HashMap<String, Any>> = HashMap()
    private val memberNamesMap: HashMap<String, String> = HashMap()

    private lateinit var back: ImageView
    private lateinit var topProfileLayoutProfileImage: ImageView
    private lateinit var topProfileLayoutUsername: TextView
    private lateinit var topProfileLayoutStatus: TextView
    private lateinit var ChatMessagesListRecycler: RecyclerView
    private lateinit var message_et: FadeEditText
    private lateinit var btn_sendMessage: MaterialButton
    private lateinit var galleryBtn: ImageView
    private lateinit var attachmentLayoutListHolder: RelativeLayout
    private lateinit var rv_attacmentList: RecyclerView
    private lateinit var close_attachments_btn: ImageView
    private lateinit var mMessageReplyLayout: LinearLayout
    private lateinit var mMessageReplyLayoutBodyRightUsername: TextView
    private lateinit var mMessageReplyLayoutBodyRightMessage: TextView
    private lateinit var mMessageReplyLayoutBodyCancel: ImageView
    private var ReplyMessageID: String? = "null"
    private var FirstUserName: String? = ""

    private val attactmentmap: ArrayList<HashMap<String, Any>> = ArrayList()
    private var AudioMessageRecorder: android.media.MediaRecorder? = null
    private var audioFilePath: String? = ""
    private var isRecording = false
    private var recordMs: Long = 0
    private var timer: java.util.TimerTask? = null
    private val _timer = java.util.Timer()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        handleSelectedFiles(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_group)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1000
            )
        } else {
            initializeLogic()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            initializeLogic()
        }
    }

    private fun initializeLogic() {
        // ... UI initializations
        galleryBtn.setOnClickListener {
            imagePickerLauncher.launch("*/*")
        }
        val groupId = intent.getStringExtra("uid")
        if (groupId == null) {
            Toast.makeText(this, "Group ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        _getGroupReference(groupId)
        _getChatMessagesRef(groupId)
        _attachChatListener(groupId)
        // ... rest of initializations
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            val resolvedFilePaths = uris.mapNotNull { uri ->
                StorageUtil.getPathFromUri(applicationContext, uri)
            }

            if (resolvedFilePaths.isNotEmpty()) {
                attachmentLayoutListHolder.visibility = View.VISIBLE
                val startingPosition = attactmentmap.size
                resolvedFilePaths.forEach { filePath ->
                    val itemMap = HashMap<String, Any>()
                    itemMap["localPath"] = filePath
                    itemMap["uploadState"] = "pending"
                    attactmentmap.add(itemMap)
                }
                rv_attacmentList.adapter?.notifyItemRangeInserted(startingPosition, resolvedFilePaths.size)
                resolvedFilePaths.indices.forEach { i ->
                    _startUploadForItem(startingPosition + i)
                }
            } else {
                Toast.makeText(this, "No valid files selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun _getGroupReference(groupId: String) {
        lifecycleScope.launch {
            try {
                val group = supabase.postgrest.from("groups").select {
                    filter { eq("groupId", groupId) }
                }.decodeSingle<HashMap<String, Any>>()

                topProfileLayoutUsername.text = group["name"] as? String
                fetchMemberUsernames(group)
                Glide.with(applicationContext)
                    .load(Uri.parse(group["icon"] as? String))
                    .into(topProfileLayoutProfileImage)
                topProfileLayoutStatus.text = "Group"

            } catch (e: Exception) {
                Toast.makeText(this@ChatGroupActivity, "Failed to load group details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun _getChatMessagesRef(groupId: String) {
        isLoading = true
        lifecycleScope.launch {
            try {
                val messages = supabase.postgrest.from("group-chats").select {
                    filter { eq("groupId", groupId) }
                    order("push_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    limit(CHAT_PAGE_SIZE)
                }.decodeList<HashMap<String, Any>>()

                if (messages.isNotEmpty()) {
                    val initialMessages = ArrayList(messages)
                    messageKeys.addAll(initialMessages.mapNotNull { it["key"]?.toString() })
                    oldestMessageKey = initialMessages.first()["key"]?.toString()
                    ChatMessagesList.addAll(initialMessages)
                    chatAdapter?.notifyDataSetChanged()
                    ChatMessagesListRecycler.scrollToPosition(ChatMessagesList.size - 1)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatGroupActivity, "Failed to load messages: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    private fun _attachChatListener(groupId: String) {
        lifecycleScope.launch {
            try {
                supabase.realtime.channel("group-chats").postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "group-chats"
                    filter = "groupId=eq.$groupId"
                }.collect {
                    val newMessage = Json.decodeFromString<HashMap<String, Any>>(it.record.toString())
                    if (newMessage.containsKey("key")) {
                        val messageKey = newMessage["key"].toString()
                        if (!messageKeys.contains(messageKey)) {
                            messageKeys.add(messageKey)
                            val insertPosition = _findCorrectInsertPosition(newMessage)
                            ChatMessagesList.add(insertPosition, newMessage)
                            chatAdapter?.notifyItemInserted(insertPosition)
                            ChatMessagesListRecycler.scrollToPosition(ChatMessagesList.size - 1)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatGroupActivity", "Error in realtime listener", e)
            }
        }
    }

    private fun fetchMemberUsernames(groupData: HashMap<String, Any>) {
        val membersMap = groupData["members"] as? Map<String, Any> ?: return
        val memberUids = membersMap.keys.toList()
        val currentUserUid = supabase.auth.currentUserOrNull()?.id

        lifecycleScope.launch {
            try {
                val users = supabase.postgrest.from("users").select {
                    filter { isIn("uid", memberUids) }
                }.decodeList<HashMap<String, Any>>()

                users.forEach { user ->
                    val uid = user["uid"] as? String
                    val username = user["username"] as? String
                    if (uid != null && username != null) {
                        memberNamesMap[uid] = username
                        if (uid == currentUserUid) {
                            FirstUserName = username
                            chatAdapter?.setFirstUserName(username)
                        }
                    }
                }
                chatAdapter?.setUserNamesMap(memberNamesMap)
                chatAdapter?.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(this@ChatGroupActivity, "Failed to fetch member details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun _send_btn() {
        val messageText = message_et.text.toString().trim()
        val senderUid = supabase.auth.currentUserOrNull()?.id
        val groupId = intent.getStringExtra("uid")

        if (senderUid == null || groupId == null) {
            Toast.makeText(this, "Cannot send message", Toast.LENGTH_SHORT).show()
            return
        }

        if (messageText.isNotEmpty()) {
            val uniqueMessageKey = UUID.randomUUID().toString()
            val chatSendMap = HashMap<String, Any>()
            chatSendMap["uid"] = senderUid
            chatSendMap["groupId"] = groupId
            chatSendMap["message_text"] = messageText
            chatSendMap["message_state"] = "sended"
            chatSendMap["key"] = uniqueMessageKey
            chatSendMap["push_date"] = System.currentTimeMillis()

            lifecycleScope.launch {
                try {
                    supabase.postgrest.from("group-chats").insert(chatSendMap)
                    message_et.setText("")
                    _updateInbox(groupId, messageText, senderUid)
                } catch (e: Exception) {
                    Toast.makeText(this@ChatGroupActivity, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun _updateInbox(groupId: String, lastMessage: String, senderUid: String) {
        lifecycleScope.launch {
            try {
                val groupMembers = supabase.postgrest.from("groups").select("members") {
                    filter { eq("groupId", groupId) }
                }.decodeSingle<Map<String, Map<String, Any>>>()["members"]?.keys?.toList() ?: emptyList()

                val inboxUpdates = groupMembers.map { memberUid ->
                    mapOf(
                        "chatID" to groupId,
                        "uid" to groupId,
                        "last_message_uid" to senderUid,
                        "last_message_text" to lastMessage,
                        "last_message_state" to "sended",
                        "push_date" to System.currentTimeMillis().toString(),
                        "chat_type" to "group",
                        "inbox_owner" to memberUid
                    )
                }
                supabase.postgrest.from("inbox").upsert(inboxUpdates, onConflict = "inbox_owner, chatID")
            } catch (e: Exception) {
                Log.e("ChatGroupActivity", "Failed to update inbox", e)
            }
        }
    }

    private fun _sendVoiceMessage(audioUrl: String, duration: Long) {
        val senderUid = supabase.auth.currentUserOrNull()?.id
        val groupId = intent.getStringExtra("uid")
        if (senderUid == null || groupId == null) return

        val uniqueMessageKey = UUID.randomUUID().toString()
        val chatSendMap = HashMap<String, Any>()
        chatSendMap["uid"] = senderUid
        chatSendMap["groupId"] = groupId
        chatSendMap["type"] = "VOICE_MESSAGE"
        chatSendMap["audio_url"] = audioUrl
        chatSendMap["audio_duration"] = duration
        chatSendMap["message_state"] = "sended"
        if (ReplyMessageID != null && ReplyMessageID != "null") {
            chatSendMap["replied_message_id"] = ReplyMessageID!!
        }
        chatSendMap["key"] = uniqueMessageKey
        chatSendMap["push_date"] = System.currentTimeMillis()

        lifecycleScope.launch {
            try {
                supabase.postgrest.from("group-chats").insert(chatSendMap)
                _updateInbox(groupId, "Voice Message", senderUid)
                ReplyMessageID = "null"
                mMessageReplyLayout.visibility = View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@ChatGroupActivity, "Failed to send voice message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadAudioFile() {
        audioFilePath?.let { path ->
            if (path.isNotEmpty()) {
                val file = File(path)
                if (file.exists()) {
                    lifecycleScope.launch {
                        try {
                            val bucket = supabase.storage["group_chat_files"]
                            val uploadPath = "public/${intent.getStringExtra("uid")}/${file.name}"
                            bucket.upload(uploadPath, file.readBytes())
                            val publicUrl = bucket.publicUrl(uploadPath)
                            _sendVoiceMessage(publicUrl, recordMs)
                        } catch (e: Exception) {
                            Toast.makeText(applicationContext, "Failed to upload audio.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun _startUploadForItem(position: Int) {
        if (position < 0 || position >= attactmentmap.size) return

        val itemMap = attactmentmap[position]
        if (itemMap["uploadState"] != "pending") return

        val filePath = itemMap["localPath"] as? String
        if (filePath.isNullOrEmpty()) {
            itemMap["uploadState"] = "failed"
            rv_attacmentList.adapter?.notifyItemChanged(position)
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            itemMap["uploadState"] = "failed"
            rv_attacmentList.adapter?.notifyItemChanged(position)
            return
        }

        itemMap["uploadState"] = "uploading"
        itemMap["uploadProgress"] = 0.0
        rv_attacmentList.adapter?.notifyItemChanged(position)

        lifecycleScope.launch {
            try {
                val bucket = supabase.storage["group_chat_files"]
                val uploadPath = "public/${intent.getStringExtra("uid")}/${file.name}"
                bucket.upload(uploadPath, file.readBytes())
                val publicUrl = bucket.publicUrl(uploadPath)

                if (position < attactmentmap.size) {
                    val mapToUpdate = attactmentmap[position]
                    if (filePath == mapToUpdate["localPath"]) {
                        mapToUpdate["uploadState"] = "success"
                        mapToUpdate["cloudinaryUrl"] = publicUrl
                        mapToUpdate["publicId"] = publicUrl
                        rv_attacmentList.adapter?.notifyItemChanged(position)
                    }
                }

            } catch (e: Exception) {
                if (position < attactmentmap.size) {
                    val currentItem = attactmentmap[position]
                    if (filePath == currentItem["localPath"]) {
                        currentItem["uploadState"] = "failed"
                        rv_attacmentList.adapter?.notifyItemChanged(position)
                    }
                }
            }
        }
    }
    // ... (rest of the class)
}
