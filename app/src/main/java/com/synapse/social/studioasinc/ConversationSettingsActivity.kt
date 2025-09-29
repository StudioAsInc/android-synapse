package com.synapse.social.studioasinc

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.synapse.social.studioasinc.backend.SupabaseAuthService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.databinding.ActivityConversationSettingsBinding
import kotlin.math.abs

class ConversationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationSettingsBinding
    private val dbService: IDatabaseService = SupabaseDatabaseService()
    private val blocklistRef by lazy { dbService.getReference(REF_SKYLINE).child(REF_BLOCKLIST) }
    private val authService: IAuthenticationService = SupabaseAuthService()
    private lateinit var userSettings: SharedPreferences

    companion object {
        private const val REF_SKYLINE = "skyline"
        private const val REF_USERS = "users"
        private const val REF_BLOCKLIST = "blocklist"
        private const val KEY_UID = "uid"
        private const val KEY_BANNED = "banned"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_USERNAME = "username"

        private const val PREFS_NAME_PREFIX = "conversation_settings_"
        private const val PREF_READ_RECEIPT = "read_receipt"
        private const val PREF_DISAPPEARING_MESSAGES = "disappearing_messages"
        private const val PREF_AUTO_SAVE_MEDIA = "auto_save_media"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityConversationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = intent.getStringExtra(KEY_UID)
        if (userId == null) {
            finish()
            return
        }

        userSettings = getSharedPreferences("$PREFS_NAME_PREFIX$userId", Context.MODE_PRIVATE)

        initializeViews()
        initializeLogic()
    }

    private fun initializeViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = " "

        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (abs(verticalOffset) - appBarLayout.totalScrollRange == 0) {
                // Collapsed
                binding.toolbar.title = "Conversation settings"
            } else {
                // Expanded
                binding.toolbar.title = " "
            }
        })
    }

    private fun initializeLogic() {
        getUserReference()
        loadSettings()
        setupClickListeners()
    }

    private fun loadSettings() {
        binding.switchReadReceipt.isChecked = userSettings.getBoolean(PREF_READ_RECEIPT, true)
        binding.switchDisappearingMessages.isChecked = userSettings.getBoolean(PREF_DISAPPEARING_MESSAGES, false)
        binding.switchAutoSaveMedia.isChecked = userSettings.getBoolean(PREF_AUTO_SAVE_MEDIA, false)
    }

    private fun setupClickListeners() {
        binding.button1.setOnClickListener {
            // TODO: Implement action
        }

        binding.cardReadReceipt.setOnClickListener {
            binding.switchReadReceipt.performClick()
        }

        binding.switchReadReceipt.setOnCheckedChangeListener { _, isChecked ->
            userSettings.edit().putBoolean(PREF_READ_RECEIPT, isChecked).apply()
        }

        binding.disappearingMainSwitch.setOnClickListener {
            binding.switchDisappearingMessages.performClick()
        }

        binding.switchDisappearingMessages.setOnCheckedChangeListener { _, isChecked ->
            userSettings.edit().putBoolean(PREF_DISAPPEARING_MESSAGES, isChecked).apply()
        }

        binding.savePhotoVideoMainSwitch.setOnClickListener {
            binding.switchAutoSaveMedia.performClick()
        }

        binding.switchAutoSaveMedia.setOnCheckedChangeListener { _, isChecked ->
            userSettings.edit().putBoolean(PREF_AUTO_SAVE_MEDIA, isChecked).apply()
        }

        binding.blockMainOption.setOnClickListener {
            blockUser(intent.getStringExtra(KEY_UID))
        }
    }

    private fun getUserReference() {
        val userId = intent.getStringExtra(KEY_UID) ?: return
        val getUserReference = dbService.getReference(REF_SKYLINE).child(REF_USERS).child(userId)

        dbService.getData(getUserReference, object : IDataListener {
            @Suppress("UNCHECKED_CAST")
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userList = dataSnapshot.getValue(List::class.java) as? List<Map<String, Any>>
                    val userMap = userList?.firstOrNull()
                    if (userMap != null) {
                        val isBanned = userMap[KEY_BANNED] as? String == "true"
                        if (isBanned) {
                            binding.profilePictureIV.setImageResource(R.drawable.banned_avatar)
                        } else {
                            val avatarUrl = userMap[KEY_AVATAR] as? String
                            if (avatarUrl.isNullOrEmpty() || avatarUrl == "null") {
                                binding.profilePictureIV.setImageResource(R.drawable.avatar)
                            } else {
                                Glide.with(applicationContext).load(Uri.parse(avatarUrl)).into(binding.profilePictureIV)
                            }
                        }

                        val nickname = userMap[KEY_NICKNAME] as? String
                        val username = userMap[KEY_USERNAME] as? String

                        val user2nickname: String = if (nickname.isNullOrEmpty() || nickname == "null") {
                            if (username.isNullOrEmpty()) "" else "@$username"
                        } else {
                            nickname
                        }
                        binding.username.text = user2nickname
                    }
                }
            }

            override fun onCancelled(databaseError: IDatabaseError) {
                Log.e("ConversationSettings", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun blockUser(uid: String?) {
        uid?.let {
            val blockData = hashMapOf<String, Any>(it to true)
            authService.getCurrentUser()?.getUid()?.let { currentUserUid ->
                val ref = blocklistRef.child(currentUserUid)
                dbService.updateChildren(ref, blockData, object : ICompletionListener<Unit> {
                    override fun onComplete(result: Unit?, error: Exception?) {
                        if (error == null) {
                            Toast.makeText(this@ConversationSettingsActivity, "User blocked", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ConversationSettingsActivity, "Failed to block user", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}