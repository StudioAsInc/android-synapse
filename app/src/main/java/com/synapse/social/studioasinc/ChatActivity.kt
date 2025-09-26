package com.synapse.social.studioasinc

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.view.MotionEvent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.service.studioasinc.AI.Gemini
import com.synapse.social.studioasinc.chat.*
import com.synapse.social.studioasinc.util.ChatMessageManager
import com.synapse.social.studioasinc.util.UserProfileUpdater
import com.synapse.social.studioasinc.chat.ChatConstants
import com.synapse.social.studioasinc.chat.ChatFirebaseManager
import com.synapse.social.studioasinc.chat.AttachmentHandler
import com.synapse.social.studioasinc.chat.AudioRecordingManager
import com.synapse.social.studioasinc.chat.ChatUIManager

class ChatActivity : AppCompatActivity(), ChatFirebaseManager.ChatFirebaseListener,
    AttachmentHandler.AttachmentListener, AudioRecordingManager.AudioRecordingListener,
    ChatUIManager.ChatUIListener, ChatAdapterListener, ChatInteractionListener {

    // Core Components
    private lateinit var firebaseManager: ChatFirebaseManager
    private lateinit var attachmentHandler: AttachmentHandler
    private lateinit var audioManager: AudioRecordingManager
    private lateinit var uiManager: ChatUIManager
    private lateinit var messageSendingHandler: MessageSendingHandler
    private lateinit var messageInteractionHandler: MessageInteractionHandler
    private lateinit var aiFeatureHandler: AiFeatureHandler
    private lateinit var userProfileUpdater: UserProfileUpdater

    // Firebase
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDb: FirebaseDatabase = FirebaseDatabase.getInstance()
    private lateinit var chatMessagesRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var blocklistRef: DatabaseReference

    // Views
    private lateinit var ivBGimage: ImageView
    private lateinit var backBtn: ImageView
    private lateinit var topProfileLayout: LinearLayout
    private lateinit var icMore: ImageView
    private lateinit var chatMessagesListRecycler: RecyclerView
    private lateinit var noChatText: TextView
    private lateinit var messageEt: FadeEditText
    private lateinit var sendMessageBtn: MaterialButton
    private lateinit var galleryBtn: ImageView
    private lateinit var voiceMessageBtn: ImageView
    private lateinit var attachmentLayout: RelativeLayout
    private lateinit var rvAttachmentList: RecyclerView
    private lateinit var closeAttachmentsBtn: ImageView
    private lateinit var mMessageReplyLayout: LinearLayout
    private lateinit var mMessageReplyLayoutBodyCancel: ImageView
    private lateinit var blockedTxt: TextView
    private lateinit var messageInputOverallContainer: LinearLayout
    private lateinit var vbr: Vibrator

    // State
    private var chatAdapter: ChatAdapter? = null
    private val chatMessagesList = ArrayList<HashMap<String, Any>>()
    private val repliedMessagesCache = HashMap<String, HashMap<String, Any>>()
    private var isGroup: Boolean = false
    private var otherUserUid: String? = null
    private var firstUserName: String = "Me"
    private var secondUserName: String = "User"
    private var secondUserAvatar: String? = null
    private var replyMessageID: String? = null
    private val locallyDeletedMessages = mutableSetOf<String>()

    companion object {
        private const val REQ_CD_IMAGE_PICKER = 101
        private const val PERMISSIONS_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isGroup = intent.getBooleanExtra("isGroup", false)
        setContentView(if (isGroup) R.layout.activity_chat_group else R.layout.activity_chat)

        otherUserUid = intent.getStringExtra(ChatConstants.UID_KEY)
        if (otherUserUid == null) {
            finish()
            return
        }

        initializeViews()
        vbr = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (checkPermissions()) {
            initializeLogic()
        }
    }

    private fun initializeViews() {
        ivBGimage = findViewById(R.id.ivBGimage)
        backBtn = findViewById(R.id.back)
        topProfileLayout = findViewById(R.id.topProfileLayout)
        icMore = findViewById(R.id.ic_more)
        chatMessagesListRecycler = findViewById(R.id.ChatMessagesListRecycler)
        noChatText = findViewById(R.id.noChatText)
        messageEt = findViewById(R.id.message_et)
        sendMessageBtn = findViewById(R.id.btn_sendMessage)
        galleryBtn = findViewById(R.id.galleryBtn)
        voiceMessageBtn = findViewById(R.id.btn_voice_message)
        attachmentLayout = findViewById(R.id.attachmentLayoutListHolder)
        rvAttachmentList = findViewById(R.id.rv_attacmentList)
        closeAttachmentsBtn = findViewById(R.id.close_attachments_btn)
        mMessageReplyLayout = findViewById(R.id.mMessageReplyLayout)
        mMessageReplyLayoutBodyCancel = findViewById(R.id.mMessageReplyLayoutBodyCancel)
        blockedTxt = findViewById(R.id.blocked_txt)
        messageInputOverallContainer = findViewById(R.id.message_input_overall_container)

        findViewById<ImageView>(R.id.ic_video_call).visibility = View.GONE
        findViewById<ImageView>(R.id.ic_audio_call).visibility = View.GONE
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            initializeLogic()
        }
    }

    private fun initializeLogic() {
        setupRecyclerView()
        setupFirebaseReferences()
        setupManagers()
        setupClickListeners()
        loadChatBackground()
        fetchUserNames()

        if (isGroup) {
            getGroupReference()
        } else {
            firebaseManager.loadInitialMessages()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessagesList, repliedMessagesCache, this)
        chatMessagesListRecycler.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
            adapter = chatAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(50)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    if (dy < 0 && layoutManager.findFirstVisibleItemPosition() <= 2) {
                        firebaseManager.loadOldMessages()
                    }
                }
            })
        }
    }

    private fun setupFirebaseReferences() {
        val currentUserUid = auth.currentUser!!.uid
        val chatId = if (isGroup) otherUserUid!! else ChatMessageManager.getChatId(currentUserUid, otherUserUid!!)
        val chatRefPath = if (isGroup) "skyline/group-chats" else "chats"
        chatMessagesRef = firebaseDb.getReference(chatRefPath).child(chatId)
        userRef = firebaseDb.getReference(ChatConstants.SKYLINE_REF).child(ChatConstants.USERS_REF).child(otherUserUid!!)
        blocklistRef = firebaseDb.getReference(ChatConstants.SKYLINE_REF).child(ChatConstants.BLOCKLIST_REF)
    }

    private fun setupManagers() {
        firebaseManager = ChatFirebaseManager(chatMessagesRef, this)
        attachmentHandler = AttachmentHandler(this, attachmentLayout, rvAttachmentList, this)
        audioManager = AudioRecordingManager(this, this)
        uiManager = ChatUIManager(
            this,
            messageEt,
            findViewById(R.id.toolContainer),
            findViewById(R.id.message_input_outlined_round),
            chatMessagesListRecycler,
            mMessageReplyLayout,
            findViewById(R.id.mMessageReplyLayoutBodyRightUsername),
            findViewById(R.id.mMessageReplyLayoutBodyRightMessage),
            this
        )
        userProfileUpdater = UserProfileUpdater(
            this,
            findViewById(R.id.topProfileLayoutProfileImage),
            findViewById(R.id.topProfileLayoutUsername),
            findViewById(R.id.topProfileLayoutStatus),
            findViewById(R.id.topProfileLayoutGenderBadge),
            findViewById(R.id.topProfileLayoutVerifiedBadge)
        )

        val gemini = Gemini.Builder(this)
            .model("gemini-1.5-flash")
            .responseType("text")
            .maxTokens(2000)
            .responseTextView(messageEt)
            .build()

        aiFeatureHandler = AiFeatureHandler(
            this, gemini, messageEt, chatMessagesList, auth, secondUserName,
            findViewById(R.id.mMessageReplyLayoutBodyRightUsername),
            findViewById(R.id.mMessageReplyLayoutBodyRightMessage)
        )

        messageSendingHandler = MessageSendingHandler(
            this, auth, firebaseDb, chatMessagesList, attachmentHandler.attachments,
            chatAdapter!!, chatMessagesListRecycler, rvAttachmentList, attachmentLayout,
            mutableSetOf(), otherUserUid!!, firstUserName, isGroup
        )

        messageInteractionHandler = MessageInteractionHandler(
            this, this, auth, firebaseDb, chatMessagesList,
            chatMessagesListRecycler, vbr, aiFeatureHandler, firstUserName, secondUserName
        )
    }

    private fun setupClickListeners() {
        backBtn.setOnClickListener { onBackPressed() }
        sendMessageBtn.setOnClickListener {
            messageSendingHandler.sendButtonAction(messageEt, replyMessageID, mMessageReplyLayout)
            replyMessageID = null
            uiManager.hideReplyUI()
        }
        galleryBtn.setOnClickListener { StorageUtil.pickMultipleFiles(this, "*/*", REQ_CD_IMAGE_PICKER) }
        closeAttachmentsBtn.setOnClickListener { attachmentHandler.resetAttachmentState() }
        mMessageReplyLayoutBodyCancel.setOnClickListener {
            replyMessageID = null
            uiManager.hideReplyUI()
            vbr.vibrate(48)
        }
        voiceMessageBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> audioManager.startRecording()
                MotionEvent.ACTION_UP -> audioManager.stopRecording()
            }
            true
        }
        val profileClickListener = View.OnClickListener {
            val intent = Intent(this, ConversationSettingsActivity::class.java)
            intent.putExtra(ChatConstants.UID_KEY, otherUserUid)
            startActivity(intent)
        }
        topProfileLayout.setOnClickListener(profileClickListener)
        icMore.setOnClickListener(profileClickListener)
    }

    private fun loadChatBackground() {
        val themePrefs = getSharedPreferences("theme", MODE_PRIVATE)
        val backgroundUrl = themePrefs.getString("chat_background_url", null)
        if (!backgroundUrl.isNullOrEmpty()) {
            Glide.with(this).load(backgroundUrl).into(ivBGimage)
        }
    }

    private fun fetchUserNames() {
        firebaseDb.getReference(ChatConstants.SKYLINE_REF).child(ChatConstants.USERS_REF).child(auth.currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    firstUserName = snapshot.child("nickname").value?.toString() ?: snapshot.child("username").value?.toString() ?: "Me"
                    chatAdapter?.setFirstUserName(firstUserName)
                    messageSendingHandler.firstUserName = firstUserName
                    messageInteractionHandler.firstUserName = firstUserName
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun getGroupReference() {
        val groupRef = firebaseDb.getReference("groups").child(intent.getStringExtra("uid"))
        groupRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    findViewById<TextView>(R.id.topProfileLayoutUsername).text = dataSnapshot.child("name").getValue(String::class.java)
                    Glide.with(applicationContext).load(dataSnapshot.child("icon").getValue(String::class.java)).into(findViewById(R.id.topProfileLayoutProfileImage))
                    findViewById<ImageView>(R.id.topProfileLayoutGenderBadge).visibility = View.GONE
                    findViewById<ImageView>(R.id.topProfileLayoutVerifiedBadge).visibility = View.GONE
                    findViewById<TextView>(R.id.topProfileLayoutStatus).text = "Group"
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        firebaseManager.loadInitialMessages()
    }

    override fun onStart() {
        super.onStart()
        firebaseManager.attachMessagesListener()
        attachUserStatusListener()
        attachBlocklistListener()
        PresenceManager.setChattingWith(auth.currentUser!!.uid, otherUserUid!!)
    }

    override fun onStop() {
        super.onStop()
        firebaseManager.detachMessagesListener()
        detachUserStatusListener()
        detachBlocklistListener()
        PresenceManager.setActivity(auth.currentUser!!.uid, "Idle")
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }

    override fun onBackPressed() {
        if (intent.hasExtra(ChatConstants.ORIGIN_KEY)) {
            val originSimpleName = intent.getStringExtra(ChatConstants.ORIGIN_KEY)
            if (!originSimpleName.isNullOrBlank() && originSimpleName != "null") {
                try {
                    val fullClassName = "com.synapse.social.studioasinc.$originSimpleName"
                    val clazz = Class.forName(fullClassName)
                    val backIntent = Intent(this, clazz)
                    if (originSimpleName == "ProfileActivity") {
                        backIntent.putExtra(ChatConstants.UID_KEY, intent.getStringExtra(ChatConstants.UID_KEY))
                    }
                    startActivity(backIntent)
                } catch (e: ClassNotFoundException) {
                     // Fallback to default behavior
                }
            }
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CD_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
            attachmentHandler.handleAttachmentResult(data)
        }
    }

    // Listener Implementations
    override fun onInitialMessagesLoaded(messages: List<HashMap<String, Any>>) {
        chatMessagesList.clear()
        chatMessagesList.addAll(messages)
        chatAdapter?.notifyDataSetChanged()
        chatMessagesListRecycler.scrollToPosition(chatMessagesList.size - 1)
        noChatText.visibility = View.GONE
        chatMessagesListRecycler.visibility = View.VISIBLE
    }

    override fun onOldMessagesLoaded(messages: List<HashMap<String, Any>>) {
        val currentSize = chatMessagesList.size
        chatMessagesList.addAll(0, messages)
        chatAdapter?.notifyItemRangeInserted(0, messages.size)
        (chatMessagesListRecycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(messages.size, 0)
    }

    override fun onNewMessageAdded(message: HashMap<String, Any>) {
        chatMessagesList.add(message)
        chatAdapter?.notifyItemInserted(chatMessagesList.size - 1)
        chatMessagesListRecycler.scrollToPosition(chatMessagesList.size - 1)
    }

    override fun onMessageChanged(message: HashMap<String, Any>) {
        val index = chatMessagesList.indexOfFirst { it[ChatConstants.KEY_KEY] == message[ChatConstants.KEY_KEY] }
        if (index != -1) {
            chatMessagesList[index] = message
            chatAdapter?.notifyItemChanged(index)
        }
    }

    override fun onMessageRemoved(messageKey: String) {
        if (locallyDeletedMessages.contains(messageKey)) {
            locallyDeletedMessages.remove(messageKey)
            return
        }
        val index = chatMessagesList.indexOfFirst { it[ChatConstants.KEY_KEY] == messageKey }
        if (index != -1) {
            chatMessagesList.removeAt(index)
            chatAdapter?.notifyItemRemoved(index)
        }
    }

    override fun onRepliedMessageFetched(repliedToKey: String, message: HashMap<String, Any>) {
        repliedMessagesCache[repliedToKey] = message
        val index = chatMessagesList.indexOfFirst { it[ChatConstants.REPLIED_MESSAGE_ID_KEY] == repliedToKey }
        if (index != -1) chatAdapter?.notifyItemChanged(index)
    }

    override fun onNoMessagesFound() {
        noChatText.visibility = View.VISIBLE
        chatMessagesListRecycler.visibility = View.GONE
    }

    override fun onLoadMoreStarted() = uiManager.showLoadMoreIndicator()
    override fun onLoadMoreFinished() = uiManager.hideLoadMoreIndicator()

    override fun onAttachmentUploaded(url: String) { /* Handled by MessageSendingHandler */ }

    override fun onRecordingFinished(filePath: String, duration: Long) {
        messageSendingHandler.sendVoiceMessage(filePath, duration, replyMessageID, mMessageReplyLayout)
        replyMessageID = null
    }

    override fun onTypingStateChanged(isTyping: Boolean) {
        val typingRef = chatMessagesRef.child(ChatConstants.TYPING_MESSAGE_REF)
        if (isTyping) {
            val typingSnd = hashMapOf<String, Any>(
                ChatConstants.UID_KEY to auth.currentUser!!.uid,
                "typingMessageStatus" to "true"
            )
            typingRef.updateChildren(typingSnd)
        } else {
            typingRef.removeValue()
        }
    }

    override fun onMessageSwiped(position: Int) {
        val message = chatMessagesList[position]
        replyMessageID = message[ChatConstants.KEY_KEY]?.toString()
        val senderName = if (message[ChatConstants.UID_KEY] == auth.currentUser!!.uid) firstUserName else secondUserName
        uiManager.showReplyUI(message, senderName)
        vbr.vibrate(48)
    }

    override fun onDeleteMessageConfirmed(messageKey: String) {
        locallyDeletedMessages.add(messageKey)
        chatMessagesRef.child(messageKey).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                chatMessagesRef.limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            val myUid = auth.currentUser!!.uid
                            val otherUid = otherUserUid!!
                            val chatId = ChatMessageManager.getChatId(myUid, otherUid)
                            firebaseDb.getReference(ChatConstants.INBOX_REF).child(myUid).child(otherUid).removeValue()
                            firebaseDb.getReference(ChatConstants.INBOX_REF).child(otherUid).child(myUid).removeValue()
                            firebaseDb.getReference(ChatConstants.USER_CHATS_REF).child(myUid).child(chatId).removeValue()
                            firebaseDb.getReference(ChatConstants.USER_CHATS_REF).child(otherUid).child(chatId).removeValue()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }

        val index = chatMessagesList.indexOfFirst { it[ChatConstants.KEY_KEY] == messageKey }
        if (index != -1) {
            chatMessagesList.removeAt(index)
            chatAdapter?.notifyItemRemoved(index)
        }
    }

    override fun onShowLoadMore() {
        if (chatMessagesList.isNotEmpty() && chatMessagesList.first()["isLoadingMore"] != true) {
            val loadingMap = hashMapOf<String, Any>("isLoadingMore" to true)
            chatMessagesList.add(0, loadingMap)
            chatAdapter?.notifyItemInserted(0)
        }
    }

    override fun onHideLoadMore() {
        if (chatMessagesList.isNotEmpty() && chatMessagesList.first()["isLoadingMore"] == true) {
            chatMessagesList.removeAt(0)
            chatAdapter?.notifyItemRemoved(0)
        }
    }

    override fun showMessageOverviewPopup(view: View, position: Int, data: ArrayList<HashMap<String, Any>>) {
        messageInteractionHandler.showMessageOverviewPopup(view, position)
    }

    override fun openUrl(URL: String) { /* Not implemented in this refactor */ }
    override fun performHapticFeedback() = vbr.vibrate(24)
    override fun scrollToMessage(messageKey: String) {
        val pos = chatMessagesList.indexOfFirst { it[ChatConstants.KEY_KEY] == messageKey }
        if (pos != -1) chatMessagesListRecycler.smoothScrollToPosition(pos)
    }
    override fun onReplySelected(messageId: String) { /* Handled by swipe */ }
    override fun onDeleteMessage(messageData: HashMap<String, Any>) = uiManager.showDeleteMessageDialog(messageData)
    override fun getRecipientUid(): String = otherUserUid!!

    // Listeners
    private var userStatusListener: ValueEventListener? = null
    private fun attachUserStatusListener() {
        if (userStatusListener == null) {
            userStatusListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        userProfileUpdater.updateAll(snapshot)
                        secondUserName = userProfileUpdater.secondUserName
                        secondUserAvatar = userProfileUpdater.secondUserAvatar
                        chatAdapter?.setSecondUserName(secondUserName)
                        chatAdapter?.setSecondUserAvatar(secondUserAvatar)
                        messageInteractionHandler.secondUserName = secondUserName
                        aiFeatureHandler.secondUserName = secondUserName
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            userRef.addValueEventListener(userStatusListener!!)
        }
    }

    private fun detachUserStatusListener() {
        userStatusListener?.let { userRef.removeEventListener(it) }
        userStatusListener = null
    }

    private var blocklistChildEventListener: ChildEventListener? = null
    private fun attachBlocklistListener() {
        if (blocklistChildEventListener == null) {
            blocklistChildEventListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) = handleBlocklistUpdate(snapshot)
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = handleBlocklistUpdate(snapshot)
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            }
            blocklistRef.addChildEventListener(blocklistChildEventListener!!)
        }
    }

    private fun detachBlocklistListener() {
        blocklistChildEventListener?.let { blocklistRef.removeEventListener(it) }
        blocklistChildEventListener = null
    }

    private fun handleBlocklistUpdate(snapshot: DataSnapshot) {
        val myUid = auth.currentUser?.uid ?: return
        val otherUid = otherUserUid ?: return

        val childKey = snapshot.key
        val childValue = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {}) ?: return

        if (childKey == otherUid) { // I am blocked by the other user
            if (childValue.containsKey(myUid)) {
                messageInputOverallContainer.visibility = View.GONE
                blockedTxt.visibility = View.VISIBLE
            } else {
                messageInputOverallContainer.visibility = View.VISIBLE
                blockedTxt.visibility = View.GONE
            }
        }
        if (childKey == myUid) { // I have blocked the other user
            if (childValue.containsKey(otherUid)) {
                messageInputOverallContainer.visibility = View.GONE
            } else {
                messageInputOverallContainer.visibility = View.VISIBLE
            }
        }
    }
}