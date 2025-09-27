package com.synapse.social.studioasinc

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.EmojiTextView
import com.synapse.social.studioasinc.util.ChatMessageManager // Import the migrated ChatMessageManager

import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.createSupabaseClient
import io.github.jan_tennert.supabase.postgrest.Postgrest
import io.github.jan_tennert.supabase.gotrue.Auth
import io.github.jan_tennert.supabase.realtime.Realtime
import io.github.jan_tennert.supabase.realtime.PostgresAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChatGroupActivity : AppCompatActivity() {

    private lateinit var supabase: SupabaseClient
    private lateinit var chatMessageManager: ChatMessageManager // Instance of the migrated ChatMessageManager

    private var chatListMap: ArrayList<HashMap<String, Any>> = ArrayList()
    private var groupId: String = ""
    private var groupName: String = ""
    private var groupProfile: String = ""
    private var currentUid: String = ""
    private var currentUserUsername: String = ""
    private var currentUserProfile: String = ""
    private var oneSignalUserId: String = ""

    private lateinit var linear1: LinearLayout
    private lateinit var toolbar: RelativeLayout
    private lateinit var back: ImageView
    private lateinit var groupNameTv: TextView
    private lateinit var profileImage: ImageView
    private lateinit var emojiButton: ImageView
    private lateinit var editTextMessage: EmojiEditText
    private lateinit var sendButton: ImageView
    private lateinit var chatListView: ListView
    private lateinit var progressbar1: ProgressBar
    private lateinit var options: ImageView

    private lateinit var emojiPopup: EmojiPopup
    private val cal = Calendar.getInstance()
    private var timerTask: TimerTask? = null
    private val i = Intent()
    private lateinit var requestNetwork: RequestNetwork
    private lateinit var requestNetworkListener: RequestNetwork.RequestListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_group)
        initialize()
        initializeSupabase()
        initializeLogic()
    }

    private fun initialize() {
        linear1 = findViewById(R.id.linear1)
        toolbar = findViewById(R.id.toolbar)
        back = findViewById(R.id.back)
        groupNameTv = findViewById(R.id.group_name_tv)
        profileImage = findViewById(R.id.profile_image)
        emojiButton = findViewById(R.id.emoji_button)
        editTextMessage = findViewById(R.id.edittext_message)
        sendButton = findViewById(R.id.send_button)
        chatListView = findViewById(R.id.chat_listview)
        progressbar1 = findViewById(R.id.progressbar1)
        options = findViewById(R.id.options)

        requestNetwork = RequestNetwork(this)

        back.setOnClickListener { finish() }
        sendButton.setOnClickListener { sendMessage() }
        emojiButton.setOnClickListener { toggleEmojiPopup() }
        options.setOnClickListener { showOptions() }

        editTextMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (editTextMessage.text.toString().length > 0) {
                    sendButton.setColorFilter(Color.parseColor("#FFC107"))
                } else {
                    sendButton.setColorFilter(Color.parseColor("#BDBDBD"))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        requestNetworkListener = object : RequestNetwork.RequestListener {
            override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String, Any>?) {}
            override fun onErrorResponse(tag: String?, message: String?) {}
        }
    }

    private fun initializeSupabase() {
        supabase = createSupabaseClient(
            "YOUR_SUPABASE_URL", // Replace with your Supabase URL
            "YOUR_SUPABASE_ANON_KEY" // Replace with your Supabase anon key
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
        chatMessageManager = ChatMessageManager(supabase) // Initialize ChatMessageManager

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = supabase.auth.currentUser()
                if (user != null) {
                    currentUid = user.id
                    currentUserUsername = user.userMetadata?.get("username") as? String ?: "Unknown"
                    currentUserProfile = user.userMetadata?.get("profile_url") as? String ?: ""
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChatGroupActivity, "User not authenticated.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatGroupActivity", "Error getting current user: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatGroupActivity, "Error fetching user data.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        groupId = intent.getStringExtra("group_id") ?: ""
        groupName = intent.getStringExtra("group_name") ?: "Group Chat"
        groupProfile = intent.getStringExtra("group_profile") ?: ""
        oneSignalUserId = intent.getStringExtra("onesignal_user_id") ?: ""

        groupNameTv.text = groupName
        Glide.with(applicationContext).load(groupProfile).into(profileImage)

        // Setup Supabase Realtime listener for group chat messages
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.realtime.from("chats:chat_id=eq.$groupId") {
                    on(PostgresAction.Insert::class) {
                        withContext(Dispatchers.Main) {
                            // Handle new message
                            val newMessage = it.decode(HashMap::class) as HashMap<String, Any>
                            chatListMap.add(newMessage)
                            (chatListView.adapter as GroupChatAdapter).notifyDataSetChanged()
                            chatListView.smoothScrollToPosition(chatListMap.size - 1)
                        }
                    }
                }.subscribe()
            } catch (e: Exception) {
                Log.e("ChatGroupActivity", "Error setting up Realtime listener: ${e.message}", e)
            }
        }

        // Fetch initial group chat messages from Supabase Postgrest
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabase.postgrest.from("chats")
                    .select("*")
                    .eq("chat_id", groupId)
                    .order("timestamp", true)
                    .limit(50)
                    .execute()

                val messages = response.decodeList<HashMap<String, Any>>()
                withContext(Dispatchers.Main) {
                    chatListMap.clear()
                    chatListMap.addAll(messages)
                    chatListView.adapter = GroupChatAdapter(chatListMap, currentUid)
                    chatListView.smoothScrollToPosition(chatListMap.size - 1)
                    progressbar1.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("ChatGroupActivity", "Error fetching initial messages: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatGroupActivity, "Failed to load messages.", Toast.LENGTH_LONG).show()
                    progressbar1.visibility = View.GONE
                }
            }
        }
    }

    private fun initializeLogic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val w = this.window
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            w.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
        }

        emojiPopup = EmojiPopup.Builder.fromRootView(linear1).build(editTextMessage)

        // TODO: Implement user presence updates using Supabase Realtime for group members
        // PresenceManager.goOnline(currentUid);
    }

    private fun sendMessage() {
        if (editTextMessage.text.toString().isNotEmpty()) {
            val messageText = editTextMessage.text.toString().trim()
            editTextMessage.setText("") // Clear the input field immediately

            val messageMap = HashMap<String, Any>()
            messageMap["sender_id"] = currentUid
            messageMap["sender_username"] = currentUserUsername
            messageMap["message_text"] = messageText
            messageMap["timestamp"] = Calendar.getInstance().timeInMillis
            messageMap["type"] = "text"

            chatMessageManager.sendMessageToDb(messageMap, currentUid, groupId, isGroup = true)
            chatMessageManager.updateInbox(messageText, groupId, isGroup = true)

            // UI update will be handled by Realtime listener, but a local temporary add can make it feel snappier
            val tempMsg = HashMap<String, Any>()
            tempMsg["sender_id"] = currentUid
            tempMsg["message_text"] = messageText
            tempMsg["timestamp"] = Calendar.getInstance().timeInMillis
            tempMsg["type"] = "text"
            chatListMap.add(tempMsg)
            (chatListView.adapter as GroupChatAdapter).notifyDataSetChanged()
            chatListView.smoothScrollToPosition(chatListMap.size - 1)

            // TODO: Optionally send push notification via Supabase Functions/Edge Functions
            // sendPushNotification(oneSignalUserId, groupName, messageText);
        }
    }

    private fun toggleEmojiPopup() {
        emojiPopup.toggle()
    }

    private fun showOptions() {
        // TODO: Implement group chat options (e.g., view members, leave group, add members)
        Toast.makeText(this, "Group chat options not implemented yet", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (emojiPopup.isShowing) {
            emojiPopup.dismiss()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        // TODO: Implement user presence update (go offline)
        // PresenceManager.goOffline(currentUid);
    }

    override fun onResume() {
        super.onResume()
        // TODO: Implement user presence update (go online)
        // PresenceManager.goOnline(currentUid);
    }

    private inner class GroupChatAdapter(private val data: ArrayList<HashMap<String, Any>>, private val currentUserId: String) : BaseAdapter() {
        override fun getCount(): Int = data.size
        override fun getItem(position: Int): HashMap<String, Any> = data[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@ChatGroupActivity).inflate(R.layout.chat_message_item, parent, false)

            val leftBubble: LinearLayout = view.findViewById(R.id.left_bubble)
            val rightBubble: LinearLayout = view.findViewById(R.id.right_bubble)
            val leftBubbleImage: LinearLayout = view.findViewById(R.id.left_bubble_image)
            val rightBubbleImage: LinearLayout = view.findViewById(R.id.right_bubble_image)
            val leftMessage: EmojiTextView = view.findViewById(R.id.left_message)
            val rightMessage: EmojiTextView = view.findViewById(R.id.right_message)
            val leftTime: TextView = view.findViewById(R.id.left_time)
            val rightTime: TextView = view.findViewById(R.id.right_time)
            val leftImage: ImageView = view.findViewById(R.id.left_image)
            val rightImage: ImageView = view.findViewById(R.id.right_image)

            val item = data[position]

            if (item["sender_id"] == currentUid) {
                // Right side (current user)
                leftBubble.visibility = View.GONE
                leftBubbleImage.visibility = View.GONE
                rightBubble.visibility = View.VISIBLE
                rightBubbleImage.visibility = View.GONE

                if (item["type"] == "image") {
                    rightBubbleImage.visibility = View.VISIBLE
                    rightBubble.visibility = View.GONE
                    Glide.with(applicationContext).load(Uri.parse(item["url"].toString())).into(rightImage)
                } else {
                    rightMessage.text = item["message_text"].toString()
                }
                rightTime.text = formatTimestamp(item["timestamp"].toString().toDouble())
            } else {
                // Left side (other group member)
                rightBubble.visibility = View.GONE
                rightBubbleImage.visibility = View.GONE
                leftBubble.visibility = View.VISIBLE
                leftBubbleImage.visibility = View.GONE

                if (item["type"] == "image") {
                    leftBubbleImage.visibility = View.VISIBLE
                    leftBubble.visibility = View.GONE
                    Glide.with(applicationContext).load(Uri.parse(item["url"].toString())).into(leftImage)
                } else {
                    leftMessage.text = item["message_text"].toString()
                }
                leftTime.text = formatTimestamp(item["timestamp"].toString().toDouble())
            }

            return view
        }

        private fun formatTimestamp(timestamp: Double): String {
            val c = Calendar.getInstance()
            c.timeInMillis = timestamp.toLong()
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(c.time)
        }
    }
}
