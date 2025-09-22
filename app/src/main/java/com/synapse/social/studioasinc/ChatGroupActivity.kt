package com.synapse.social.studioasinc

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.synapse.social.studioasinc.attachments.Rv_attacmentListAdapter
import java.util.ArrayList
import java.util.Calendar
import java.util.HashMap

class ChatGroupActivity : AppCompatActivity() {

    private var chatMessagesRef: DatabaseReference? = null
    private var oldestMessageKey: String? = null
    private var isLoading = false
    private val CHAT_PAGE_SIZE = 80
    private var chatAdapter: ChatAdapter? = null
    private var _chat_child_listener: ChildEventListener? = null
    private val messageKeys: MutableSet<String> = HashSet()
    private val ChatMessagesList: ArrayList<HashMap<String, Any>> = ArrayList()
    private val repliedMessagesCache: HashMap<String, HashMap<String, Any>> = HashMap()

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

    private val _firebase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        attachmentLayoutListHolder = findViewById(R.id.attachmentLayoutListHolder)
        rv_attacmentList = findViewById(R.id.rv_attacmentList)
        close_attachments_btn = findViewById(R.id.close_attachments_btn)
        mMessageReplyLayout = findViewById(R.id.mMessageReplyLayout)
        mMessageReplyLayoutBodyRightUsername = findViewById(R.id.mMessageReplyLayoutBodyRightUsername)
        mMessageReplyLayoutBodyRightMessage = findViewById(R.id.mMessageReplyLayoutBodyRightMessage)
        mMessageReplyLayoutBodyCancel = findViewById(R.id.mMessageReplyLayoutBodyCancel)

        val chatRecyclerLayoutManager = LinearLayoutManager(this)
        chatRecyclerLayoutManager.stackFromEnd = true
        ChatMessagesListRecycler.layoutManager = chatRecyclerLayoutManager

        chatAdapter = ChatAdapter(ChatMessagesList, repliedMessagesCache)
        chatAdapter?.setHasStableIds(true)
        ChatMessagesListRecycler.adapter = chatAdapter

        val groupId = intent.getStringExtra("uid")
        chatMessagesRef = _firebase.getReference("group-chats").child(groupId!!)

        _getGroupReference()
        _getChatMessagesRef()
        _attachChatListener()

        back.setOnClickListener { onBackPressed() }

        btn_sendMessage.setOnClickListener { _send_btn() }

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
    }

    private fun _getGroupReference() {
        val groupId = intent.getStringExtra("uid")
        val groupRef = _firebase.getReference("groups").child(groupId!!)
        groupRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    topProfileLayoutUsername.text = dataSnapshot.child("name").getValue(String::class.java)
                    Glide.with(applicationContext)
                        .load(Uri.parse(dataSnapshot.child("icon").getValue(String::class.java)))
                        .into(topProfileLayoutProfileImage)
                    topProfileLayoutStatus.text = "Group"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun _getChatMessagesRef() {
        isLoading = true
        val getChatsMessages = chatMessagesRef!!.limitToLast(CHAT_PAGE_SIZE)
        getChatsMessages.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val initialMessages = ArrayList<HashMap<String, Any>>()
                    for (_data in dataSnapshot.children) {
                        val messageData =
                            _data.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
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

            override fun onCancelled(databaseError: DatabaseError) {
                isLoading = false
            }
        })
    }

    private fun _attachChatListener() {
        if (chatMessagesRef == null) return
        _chat_child_listener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val newMessage =
                    dataSnapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
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

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        chatMessagesRef!!.addChildEventListener(_chat_child_listener!!)
    }

    private fun _send_btn() {
        val messageText = message_et.text.toString().trim()
        val senderUid = auth.currentUser!!.uid
        val groupId = intent.getStringExtra("uid")

        if (messageText.isNotEmpty()) {
            val uniqueMessageKey = chatMessagesRef!!.push().key
            val chatSendMap = HashMap<String, Any>()
            chatSendMap["uid"] = senderUid
            chatSendMap["message_text"] = messageText
            chatSendMap["message_state"] = "sended"
            chatSendMap["key"] = uniqueMessageKey!!
            chatSendMap["push_date"] = ServerValue.TIMESTAMP

            chatMessagesRef!!.child(uniqueMessageKey).setValue(chatSendMap)
            message_et.setText("")
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
}
