package com.synapse.social.studioasinc

import android.Manifest
import android.app.Activity
import android.content.Context
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import androidx.activity.OnBackPressedCallback
import com.synapse.social.studioasinc.attachments.Rv_attacmentListAdapter
import com.synapse.social.studioasinc.backend.SupabaseAuthService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseReference
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IRealtimeListener
import java.util.ArrayList
import java.util.Calendar
import java.util.HashMap

class ChatGroupActivity : AppCompatActivity(), ChatAdapterListener {

    private var chatMessagesRef: IDatabaseReference? = null
    private var oldestMessageKey: String? = null
    private var isLoading = false
    private val CHAT_PAGE_SIZE = 80
    private var chatAdapter: ChatAdapter? = null
    private var _chat_child_listener: IRealtimeListener? = null
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
    private val REQ_CD_IMAGE_PICKER = 101
    private var AudioMessageRecorder: android.media.MediaRecorder? = null
    private var audioFilePath: String? = ""
    private var isRecording = false
    private var recordMs: Long = 0
    private var timer: java.util.TimerTask? = null
    private val _timer = java.util.Timer()


    private val dbService: IDatabaseService = SupabaseDatabaseService()
    private val authService: IAuthenticationService = SupabaseAuthService()

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
        back = findViewById(R.id.back)
        topProfileLayoutProfileImage = findViewById(R.id.topProfileLayoutProfileImage)
        topProfileLayoutUsername = findViewById(R.id.topProfileLayoutUsername)
        topProfileLayoutStatus = findViewById(R.id.topProfileLayoutStatus)
        ChatMessagesListRecycler = findViewById(R.id.ChatMessagesListRecycler)
        message_et = findViewById(R.id.message_et)
        btn_sendMessage = findViewById(R.id.btn_sendMessage)
        galleryBtn = findViewById(R.id.galleryBtn)
        val toolContainer = findViewById<LinearLayout>(R.id.toolContainer)
        val btn_voice_message = findViewById<ImageView>(R.id.btn_voice_message)
        attachmentLayoutListHolder = findViewById(R.id.attachmentLayoutListHolder)
        rv_attacmentList = findViewById(R.id.rv_attacmentList)
        close_attachments_btn = findViewById(R.id.close_attachments_btn)
        mMessageReplyLayout = findViewById(R.id.mMessageReplyLayout)
        mMessageReplyLayoutBodyRightUsername = findViewById(R.id.mMessageReplyLayoutBodyRightUsername)
        mMessageReplyLayoutBodyRightMessage = findViewById(R.id.mMessageReplyLayoutBodyRightMessage)
        mMessageReplyLayoutBodyCancel = findViewById(R.id.mMessageReplyLayoutBodyCancel)
        val message_input_outlined_round = findViewById<LinearLayout>(R.id.message_input_outlined_round)

        val chatRecyclerLayoutManager = LinearLayoutManager(this)
        chatRecyclerLayoutManager.stackFromEnd = true
        ChatMessagesListRecycler.layoutManager = chatRecyclerLayoutManager

        chatAdapter = ChatAdapter(ChatMessagesList, repliedMessagesCache, this)
        chatAdapter?.setHasStableIds(true)
        chatAdapter?.setGroupChat(true) // This is a group chat
        ChatMessagesListRecycler.adapter = chatAdapter

        val groupId = intent.getStringExtra("uid")
        if (groupId.isNullOrEmpty()) {
            finish()
            return
        }
        chatMessagesRef = dbService.getReference("group-chats").child(groupId)

        _getGroupReference()
        _getChatMessagesRef()
        _attachChatListener()

        back.setOnClickListener { finish() }

        btn_sendMessage.setOnClickListener {
            val messageText = message_et.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val messageSendingHandler = MessageSendingHandler(
                    context = this,
                    authService = authService,
                    dbService = dbService,
                    messageEt = message_et,
                    attachmentLayoutListHolder = attachmentLayoutListHolder,
                    attactmentmap = attactmentmap,
                    repliedMessageId = ReplyMessageID,
                    isGroup = true,
                    recipientUid = groupId
                )
                messageSendingHandler.sendMessage()
                ReplyMessageID = "null"
                mMessageReplyLayout.visibility = View.GONE
            }
        }

        galleryBtn.setOnClickListener {
            StorageUtil.pickMultipleFiles(this@ChatGroupActivity, "*/*", REQ_CD_IMAGE_PICKER)
        }

        close_attachments_btn.setOnClickListener {
            attachmentLayoutListHolder.visibility = View.GONE
            val oldSize = attactmentmap.size
            if (oldSize > 0) {
                attactmentmap.clear()
                rv_attacmentList.adapter?.notifyItemRangeRemoved(0, oldSize)
            }
        }

        mMessageReplyLayoutBodyCancel.setOnClickListener {
            ReplyMessageID = "null"
            mMessageReplyLayout.visibility = View.GONE
        }

        message_input_outlined_round.orientation = LinearLayout.HORIZONTAL
        toolContainer.visibility = View.VISIBLE
        btn_sendMessage.visibility = View.VISIBLE

        message_et.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim().isEmpty()) {
                    toolContainer.visibility = View.VISIBLE
                    message_input_outlined_round.orientation = LinearLayout.HORIZONTAL
                } else {
                    toolContainer.visibility = View.GONE
                    message_input_outlined_round.orientation = LinearLayout.VERTICAL
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        btn_voice_message.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        _AudioRecorderStart()
                        Toast.makeText(applicationContext, "Recording...", Toast.LENGTH_SHORT).show()
                    } else {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1000)
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    _AudioRecorderStop()
                    uploadAudioFile()
                    true
                }
                else -> false
            }
        }
    }

    private fun _getGroupReference() {
        val groupId = intent.getStringExtra("uid") ?: return
        val groupRef = dbService.getReference("groups").child(groupId)
        groupRef.addListenerForSingleValueEvent(object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                if (dataSnapshot.exists()) {
                    topProfileLayoutUsername.text = dataSnapshot.child("name").getValue(String::class.java)

                    // Fetch member usernames for displaying in chat bubbles
                    fetchMemberUsernames(dataSnapshot)
                    Glide.with(applicationContext)
                        .load(Uri.parse(dataSnapshot.child("icon").getValue(String::class.java)))
                        .into(topProfileLayoutProfileImage)
                    topProfileLayoutStatus.text = "Group"
                }
            }

            override fun onCancelled(databaseError: IDatabaseError) {}
        })
    }

    private fun _getChatMessagesRef() {
        isLoading = true
        chatMessagesRef?.limitToLast(CHAT_PAGE_SIZE)?.addListenerForSingleValueEvent(object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                if (dataSnapshot.exists()) {
                    val initialMessages = ArrayList<HashMap<String, Any>>()
                    for (_data in dataSnapshot.children) {
                        val messageData = _data.getValue(HashMap::class.java) as? HashMap<String, Any>
                        if (messageData != null && messageData.containsKey("key")) {
                            initialMessages.add(messageData)
                            messageKeys.add(messageData["key"].toString())
                        }
                    }
                    if (initialMessages.isNotEmpty()) {
                        initialMessages.sortWith { msg1, msg2 ->
                            val time1 = _getMessageTimestamp(msg1)
                            val time2 = _getMessageTimestamp(msg2)
                            time1.compareTo(time2)
                        }
                        oldestMessageKey = initialMessages[0]["key"].toString()
                        ChatMessagesList.addAll(initialMessages)
                        chatAdapter?.notifyDataSetChanged()
                        ChatMessagesListRecycler.scrollToPosition(ChatMessagesList.size - 1)
                    }
                }
                isLoading = false
            }

            override fun onCancelled(databaseError: IDatabaseError) {
                isLoading = false
            }
        })
    }

    private fun _attachChatListener() {
        if (chatMessagesRef == null) return
        _chat_child_listener = object : IRealtimeListener {
            override fun onChildAdded(dataSnapshot: IDataSnapshot, previousChildName: String?) {
                val newMessage = dataSnapshot.getValue(HashMap::class.java) as? HashMap<String, Any>
                if (newMessage != null && newMessage.containsKey("key")) {
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

            override fun onChildChanged(snapshot: IDataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: IDataSnapshot) {}
            override fun onChildMoved(snapshot: IDataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: IDatabaseError) {}
        }
        chatMessagesRef!!.addChildEventListener(_chat_child_listener!!)
    }
    
    private fun fetchMemberUsernames(groupSnapshot: IDataSnapshot) {
        val membersSnapshot = groupSnapshot.child("members")
        if (membersSnapshot.exists()) {
            val memberUids = mutableListOf<String>()
            for (memberSnapshot in membersSnapshot.children) {
                memberSnapshot.key?.let { memberUids.add(it) }
            }

            authService.getCurrentUser()?.getUid()?.let {
                if (!memberUids.contains(it)) {
                    memberUids.add(it)
                }
            }

            val totalMembers = memberUids.size
            var membersProcessed = 0

            for (memberUid in memberUids) {
                dbService.getReference("skyline/users").child(memberUid).child("username")
                    .addListenerForSingleValueEvent(object : IDataListener {
                        override fun onDataChange(userSnapshot: IDataSnapshot) {
                            val username = userSnapshot.getValue(String::class.java)
                            if (username != null) {
                                memberNamesMap[memberUid] = username
                                if (memberUid == authService.getCurrentUser()?.getUid()) {
                                    FirstUserName = username
                                    chatAdapter?.setFirstUserName(username)
                                }
                            }

                            membersProcessed++
                            if (membersProcessed == totalMembers) {
                                chatAdapter?.setUserNamesMap(memberNamesMap)
                                chatAdapter?.notifyDataSetChanged()
                            }
                        }

                        override fun onCancelled(error: IDatabaseError) {
                            membersProcessed++
                            if (membersProcessed == totalMembers) {
                                chatAdapter?.setUserNamesMap(memberNamesMap)
                                chatAdapter?.notifyDataSetChanged()
                            }
                        }
                    })
            }
        }
    }


    private fun _findCorrectInsertPosition(newMessage: HashMap<String, Any>): Int {
        if (ChatMessagesList.isEmpty()) {
            return 0
        }
        val newMessageTime = _getMessageTimestamp(newMessage)
        for (i in ChatMessagesList.indices) {
            val existingMessageTime = _getMessageTimestamp(ChatMessagesList[i])
            if (newMessageTime <= existingMessageTime) {
                return i
            }
        }
        return ChatMessagesList.size
    }

    private fun _getMessageTimestamp(message: HashMap<String, Any>): Long {
        return try {
            val pushDateObj = message["push_date"]
            if (pushDateObj is Long) {
                pushDateObj
            } else {
                (pushDateObj as Double).toLong()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    override fun scrollToMessage(messageId: String) {
        // Not implemented for group chat
    }

    override fun performHapticFeedback() {
        // Not implemented for group chat
    }

    override fun showMessageOverviewPopup(anchor: View, position: Int, data: ArrayList<HashMap<String, Any>>) {
        // Not implemented for group chat
    }

    override fun openUrl(url: String) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

    override fun getRecipientUid(): String? {
        return intent.getStringExtra("uid")
    }


    private fun _AudioRecorderStart() {
        val cc = Calendar.getInstance()
        recordMs = 0
        AudioMessageRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.media.MediaRecorder(this)
        } else {
            android.media.MediaRecorder()
        }

        val getCacheDir = externalCacheDir
        val getCacheDirName = "audio_records"
        val getCacheFolder = java.io.File(getCacheDir, getCacheDirName)
        getCacheFolder.mkdirs()
        val getRecordFile = java.io.File(getCacheFolder, cc.timeInMillis.toString() + ".mp3")
        audioFilePath = getRecordFile.absolutePath

        AudioMessageRecorder?.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
        AudioMessageRecorder?.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
        AudioMessageRecorder?.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
        AudioMessageRecorder?.setAudioEncodingBitRate(320000)
        AudioMessageRecorder?.setOutputFile(audioFilePath)

        try {
            AudioMessageRecorder?.prepare()
            AudioMessageRecorder?.start()
            isRecording = true
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }

        timer = object : java.util.TimerTask() {
            override fun run() {
                runOnUiThread {
                    recordMs += 500
                }
            }
        }
        _timer.scheduleAtFixedRate(timer, 0, 500)
    }

    private fun _AudioRecorderStop() {
        if (isRecording) {
            if (AudioMessageRecorder != null) {
                try {
                    AudioMessageRecorder?.stop()
                    AudioMessageRecorder?.release()
                } catch (e: RuntimeException) {
                    Log.e("ChatGroupActivity", "Error stopping media recorder: " + e.message)
                }
                AudioMessageRecorder = null
            }
            isRecording = false
            timer?.cancel()

            audioFilePath?.let { path ->
                if (path.isNotEmpty()) {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        val messageSendingHandler = MessageSendingHandler(
                            context = this,
                            authService = authService,
                            dbService = dbService,
                            messageEt = message_et,
                            attachmentLayoutListHolder = attachmentLayoutListHolder,
                            attactmentmap = attactmentmap,
                            repliedMessageId = ReplyMessageID,
                            isGroup = true,
                            recipientUid = intent.getStringExtra("uid")!!
                        )
                        messageSendingHandler.sendVoiceMessage(path, recordMs)
                    }
                }
            }
        }
    }

    private fun _startUploadForItem(position: Int) {
        if (position < 0 || position >= attactmentmap.size) {
            return
        }

        val itemMap = attactmentmap[position]
        if (itemMap["uploadState"] != "pending") {
            return
        }

        val filePath = itemMap["localPath"] as? String
        if (filePath.isNullOrEmpty()) {
            itemMap["uploadState"] = "failed"
            rv_attacmentList.adapter?.notifyItemChanged(position)
            return
        }

        val file = java.io.File(filePath)
        if (!file.exists()) {
            itemMap["uploadState"] = "failed"
            rv_attacmentList.adapter?.notifyItemChanged(position)
            return
        }

        itemMap["uploadState"] = "uploading"
        itemMap["uploadProgress"] = 0.0
        rv_attacmentList.adapter?.notifyItemChanged(position)

        AsyncUploadService.uploadWithNotification(this, filePath, file.name, object : AsyncUploadService.UploadProgressListener {
            override fun onProgress(filePath: String, percent: Int) {
                if (position < attactmentmap.size) {
                    val currentItem = attactmentmap[position]
                    if (filePath == currentItem["localPath"]) {
                        currentItem["uploadProgress"] = percent.toDouble()
                        rv_attacmentList.adapter?.notifyItemChanged(position)
                    }
                }
            }

            override fun onSuccess(filePath: String, url: String, publicId: String) {
                if (position < attactmentmap.size) {
                    val mapToUpdate = attactmentmap[position]
                    if (filePath == mapToUpdate["localPath"]) {
                        mapToUpdate["uploadState"] = "success"
                        mapToUpdate["cloudinaryUrl"] = url
                        mapToUpdate["publicId"] = publicId
                        rv_attacmentList.adapter?.notifyItemChanged(position)
                    }
                }
            }

            override fun onFailure(filePath: String, error: String) {
                if (position < attactmentmap.size) {
                    val currentItem = attactmentmap[position]
                    if (filePath == currentItem["localPath"]) {
                        currentItem["uploadState"] = "failed"
                        rv_attacmentList.adapter?.notifyItemChanged(position)
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CD_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val resolvedFilePaths = ArrayList<String>()
                try {
                    if (data.clipData != null) {
                        for (i in 0 until data.clipData!!.itemCount) {
                            val fileUri = data.clipData!!.getItemAt(i).uri
                            val path = StorageUtil.getPathFromUri(applicationContext, fileUri)
                            if (path != null && path.isNotEmpty()) {
                                resolvedFilePaths.add(path)
                            }
                        }
                    } else if (data.data != null) {
                        val fileUri = data.data
                        val path = StorageUtil.getPathFromUri(applicationContext, fileUri)
                        if (path != null && path.isNotEmpty()) {
                            resolvedFilePaths.add(path)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error processing selected files", Toast.LENGTH_SHORT).show()
                    return
                }

                if (resolvedFilePaths.isNotEmpty()) {
                    attachmentLayoutListHolder.visibility = View.VISIBLE
                    val startingPosition = attactmentmap.size
                    for (filePath in resolvedFilePaths) {
                        val itemMap = HashMap<String, Any>()
                        itemMap["localPath"] = filePath
                        itemMap["uploadState"] = "pending"
                        attactmentmap.add(itemMap)
                    }
                    rv_attacmentList.adapter?.notifyItemRangeInserted(startingPosition, resolvedFilePaths.size)
                    for (i in resolvedFilePaths.indices) {
                        _startUploadForItem(startingPosition + i)
                    }
                } else {
                    Toast.makeText(this, "No valid files selected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
