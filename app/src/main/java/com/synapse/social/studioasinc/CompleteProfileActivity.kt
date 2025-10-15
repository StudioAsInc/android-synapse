package com.synapse.social.studioasinc

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.SupabaseClient
import com.synapse.social.studioasinc.backend.isUsernameTaken
import com.synapse.social.studioasinc.backend.createUserProfile
import com.synapse.social.studioasinc.backend.createUsernameMapping
import com.synapse.social.studioasinc.backend.signOut
import com.synapse.social.studioasinc.databinding.ActivityCompleteProfileBinding
import com.onesignal.OneSignal
import java.util.Calendar
import java.util.concurrent.Executors
import  com.synapse.social.studioasinc.util.ViewUtils.setImageColor
import  com.synapse.social.studioasinc.util.ViewUtils.setGradientDrawable
import  com.synapse.social.studioasinc.util.ViewUtils.setStateColor
import  com.synapse.social.studioasinc.util.ViewUtils.setViewGraphics

class CompleteProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteProfileBinding
    private var userNameErr = false
    private var avatarUri = "null"
    private var thedpurl = "null"
    private val executor = Executors.newSingleThreadExecutor()
    private val getJoinTime: Calendar = Calendar.getInstance()

    companion object {
        private const val REQ_CD_SELECTAVATAR = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialize()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000)
        } else {
            initializeLogic()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            initializeLogic()
        }
    }

    private fun initialize() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val vbr = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.profileImageCard.setOnLongClickListener {
            avatarUri = "null"
            binding.profileImage.setImageResource(R.drawable.avatar)
            vbr.vibrate(48)
            true
        }

        binding.profileImageCard.setOnClickListener {
            val selectAvatar = Intent(Intent.ACTION_GET_CONTENT)
            selectAvatar.type = "image/*"
            selectAvatar.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(selectAvatar, REQ_CD_SELECTAVATAR)
        }

        binding.usernameInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val charSeq = s.toString()
                if (charSeq.trim { it <= ' ' } == "") {
                    binding.usernameInput.isActivated = true
                    (binding.usernameInput as EditText).error = getString(R.string.enter_username)
                    userNameErr = true
                } else {
                    if (charSeq.matches(Regex("[a-z0-9_.]+"))) {
                        if (charSeq.contains("q") || charSeq.contains("w") || charSeq.contains("e") || charSeq.contains("r") || charSeq.contains("t") || charSeq.contains("y") || charSeq.contains("u") || charSeq.contains("i") || charSeq.contains("o") || charSeq.contains("p") || charSeq.contains("a") || charSeq.contains("s") || charSeq.contains("d") || charSeq.contains("f") || charSeq.contains("g") || charSeq.contains("h") || charSeq.contains("j") || charSeq.contains("k") || charSeq.contains("l") || charSeq.contains("z") || charSeq.contains("x") || charSeq.contains("c") || charSeq.contains("v") || charSeq.contains("b") || charSeq.contains("n") || charSeq.contains("m")) {
                            if (binding.usernameInput.text.toString().length < 3) {
                                binding.usernameInput.isActivated = true
                                (binding.usernameInput as EditText).error = getString(R.string.username_err_3_characters)
                                userNameErr = true
                            } else {
                                if (binding.usernameInput.text.toString().length > 25) {
                                    binding.usernameInput.isActivated = true
                                    (binding.usernameInput as EditText).error = getString(R.string.username_err_25_characters)
                                    userNameErr = true
                                } else {
                                    binding.usernameInput.isActivated = false
                                    isUsernameTaken(SupabaseClient.client.postgrest, charSeq.trim(), executor)
                                        .thenAccept { isTaken ->
                                            runOnUiThread {
                                                if (isTaken) {
                                                    binding.usernameInput.isActivated = true
                                                    (binding.usernameInput as EditText).error = getString(R.string.username_err_already_taken)
                                                    userNameErr = true
                                                } else {
                                                    binding.usernameInput.isActivated = false
                                                    userNameErr = false
                                                }
                                            }
                                        }
                                }
                            }
                        } else {
                            binding.usernameInput.isActivated = true
                            (binding.usernameInput as EditText).error = getString(R.string.username_err_one_letter)
                            userNameErr = true
                        }
                    } else {
                        binding.usernameInput.isActivated = true
                        (binding.usernameInput as EditText).error = getString(R.string.username_err_invalid_characters)
                        userNameErr = true
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        binding.nicknameInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.length > 30) {
                    binding.nicknameInput.background = GradientDrawable().apply {
                        cornerRadius = 28f
                        setStroke(3, -0xbbbc)
                        setColor(-0x1)
                    }
                    (binding.nicknameInput as EditText).error = getString(R.string.nickname_err_30_characters)
                } else {
                    binding.nicknameInput.background = GradientDrawable().apply {
                        cornerRadius = 28f
                        setStroke(3, -0x111112)
                        setColor(-0x1)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        binding.biographyInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.length > 250) {
                    binding.biographyInput.background = GradientDrawable().apply {
                        cornerRadius = 28f
                        setStroke(3, -0xbbbc)
                        setColor(-0x1)
                    }
                    (binding.biographyInput as EditText).error = getString(R.string.biography_err_250_characters)
                } else {
                    binding.biographyInput.background = GradientDrawable().apply {
                        cornerRadius = 28f
                        setStroke(3, -0x111112)
                        setColor(-0x1)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        binding.skipButton.setOnClickListener {
            SketchwareUtil.showMessage(applicationContext, "Not possible")
        }

        binding.completeButton.setOnClickListener {
            if (userNameErr) {
                SketchwareUtil.showMessage(applicationContext, getString(R.string.username_err_invalid))
                vbr.vibrate(48)
            } else {
                binding.completeButton.isEnabled = false
                binding.completeButton.text = "Loading..."
                binding.usernameInput.isEnabled = false
                val username = binding.usernameInput.text.toString().trim()
                val nickname = binding.nicknameInput.text.toString().trim()
                val biography = binding.biographyInput.text.toString().trim()
                val googleAvatar = intent.getStringExtra("googleLoginAvatarUri")
                val userData: HashMap<String, Any> = hashMapOf(
                    "uid" to (SupabaseClient.client.gotrue.currentUserOrNull()?.id ?: ""),
                    "email" to (SupabaseClient.client.gotrue.currentUserOrNull()?.email ?: ""),
                    "profile_cover_image" to "null",
                    "avatar" to (googleAvatar ?: thedpurl),
                    "avatar_history_type" to "local",
                    "username" to username,
                    "nickname" to if (nickname.isEmpty()) "null" else nickname,
                    "biography" to if (biography.isEmpty()) "null" else biography,
                    "account_premium" to "false",
                    "user_level_xp" to "500",
                    "verify" to "false",
                    "account_type" to "user",
                    "gender" to "hidden",
                    "banned" to "false",
                    "status" to "online",
                    "join_date" to getJoinTime.timeInMillis.toString()
                )
                createUserProfile(SupabaseClient.client.postgrest, userData, executor)
                    .thenCompose {
                        val usernameData: HashMap<String, Any> = hashMapOf(
                            "uid" to (SupabaseClient.client.gotrue.currentUserOrNull()?.id ?: ""),
                            "email" to (SupabaseClient.client.gotrue.currentUserOrNull()?.email ?: ""),
                            "username" to username
                        )
                        createUsernameMapping(SupabaseClient.client.postgrest, usernameData, executor)
                    }
                    .whenComplete { _, throwable ->
                        runOnUiThread {
                            if (throwable == null) {
                                val intent = Intent(applicationContext, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                binding.usernameInput.isEnabled = true
                                binding.completeButton.isEnabled = true
                                try {
                                    binding.completeButton.setText(R.string.continue_button)
                                } catch (e: Exception) {
                                    binding.completeButton.text = "Continue"
                                }
                                SketchwareUtil.showMessage(applicationContext, throwable.message)
                            }
                        }
                    }
            }
        }
    }

    private fun initializeLogic() {
        binding.emailVerificationTitle.typeface = Typeface.DEFAULT_BOLD
        binding.subtitle.typeface = Typeface.DEFAULT
        binding.title.typeface = Typeface.DEFAULT_BOLD
        setStateColor(this, -0x1, -0x1)
        avatarUri = "null"
        thedpurl = "null"
        userNameErr = true
        setImageColor(binding.emailVerificationErrorIc, -0xbbbc)
        setImageColor(binding.emailVerificationVerifiedIc, -0xbfa510)
        setGradientDrawable(binding.profileImageCard, Color.TRANSPARENT, 300f, 0, Color.TRANSPARENT)
        setGradientDrawable(binding.emailVerification, -0x1, 28f, 3, -0x111112)
        setViewGraphics(binding.emailVerificationSend, -0xbbac6f, -0xbbac6f, 300, 0, Color.TRANSPARENT)

        if (intent.hasExtra("findedUsername")) {
            binding.usernameInput.setText(intent.getStringExtra("findedUsername"))
        } else {
            binding.usernameInput.setText("")
        }

        if (intent.hasExtra("googleLoginName") && intent.hasExtra("googleLoginEmail") && intent.hasExtra("googleLoginAvatarUri")) {
            Glide.with(applicationContext).load(Uri.parse(intent.getStringExtra("googleLoginAvatarUri"))).into(binding.profileImage)
            binding.nicknameInput.setText(intent.getStringExtra("googleLoginName"))
        }

        font()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CD_SELECTAVATAR && resultCode == Activity.RESULT_OK) {
            val filePath = mutableListOf<String>()
            data?.clipData?.let {
                for (i in 0 until it.itemCount) {
                    filePath.add(FileUtil.convertUriToFilePath(applicationContext, it.getItemAt(i).uri))
                }
            } ?: data?.data?.let {
                filePath.add(FileUtil.convertUriToFilePath(applicationContext, it))
            }

            if (filePath.isNotEmpty()) {
                binding.profileImage.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(filePath[0], 1024, 1024))
                val path = filePath[0]
                ImageUploader.uploadImage(path, object : ImageUploader.UploadCallback {
                    override fun onUploadComplete(imageUrl: String) {
                        thedpurl = imageUrl
                    }
                    override fun onUploadError(errorMessage: String) {
                        SketchwareUtil.showMessage(applicationContext, "Something went wrong")
                    }
                })
            }
        }
    }

    override fun onBackPressed() {
        val newCustomDialog = AlertDialog.Builder(this).create()
        val newCustomDialogLI = layoutInflater
        val newCustomDialogCV = newCustomDialogLI.inflate(R.layout.dialog_synapse_bg_view, null)
        newCustomDialog.setView(newCustomDialogCV)
        newCustomDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogTitle = newCustomDialogCV.findViewById<TextView>(R.id.dialog_title)
        val dialogMessage = newCustomDialogCV.findViewById<TextView>(R.id.dialog_message)
        val dialogNoButton = newCustomDialogCV.findViewById<TextView>(R.id.dialog_no_button)
        val dialogYesButton = newCustomDialogCV.findViewById<TextView>(R.id.dialog_yes_button)
        dialogYesButton.setTextColor(-0xbbbc)
        setViewGraphics(dialogYesButton, -0x1, -0x322d, 28, 0, Color.TRANSPARENT)
        dialogNoButton.setTextColor(-0xde6a0d)
        setViewGraphics(dialogNoButton, -0x1, -0x442205, 28, 0, Color.TRANSPARENT)
        dialogTitle.text = getString(R.string.info)
        dialogMessage.text = getString(R.string.cancel_complete_profile_warn).plus("\n\n").plus(getString(R.string.cancel_complete_profile_warn2))
        dialogYesButton.text = getString(R.string.yes)
        dialogNoButton.text = getString(R.string.no)
        dialogYesButton.setOnClickListener {
            signOut(SupabaseClient.client.gotrue, executor).thenRun {
                finish()
            }
        }
        dialogNoButton.setOnClickListener { newCustomDialog.dismiss() }
        newCustomDialog.setCancelable(true)
        newCustomDialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.complete_profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.cancel) {
            val newCustomDialog = AlertDialog.Builder(this).create()
            val newCustomDialogLI = layoutInflater
            val newCustomDialogCV = newCustomDialogLI.inflate(R.layout.dialog_synapse_bg_view, null)
            newCustomDialog.setView(newCustomDialogCV)
            newCustomDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val dialogTitle = newCustomDialogCV.findViewById<TextView>(R.id.dialog_title)
            val dialogMessage = newCustomDialogCV.findViewById<TextView>(R.id.dialog_message)
            val dialogNoButton = newCustomDialogCV.findViewById<TextView>(R.id.dialog_no_button)
            val dialogYesButton = newCustomDialogCV.findViewById<TextView>(R.id.dialog_yes_button)
            dialogYesButton.setTextColor(-0xbbbc)
            setViewGraphics(dialogYesButton, -0x1, -0x322d, 28, 0, Color.TRANSPARENT)
            dialogNoButton.setTextColor(-0xde6a0d)
            setViewGraphics(dialogNoButton, -0x1, -0x442205, 28, 0, Color.TRANSPARENT)
            dialogTitle.text = getString(R.string.info)
            dialogMessage.text = getString(R.string.cancel_create_account_warn).plus("\n\n").plus(getString(R.string.cancel_create_account_warn2))
            dialogYesButton.text = getString(R.string.yes)
            dialogNoButton.text = getString(R.string.no)
            dialogYesButton.setOnClickListener {
                item.isEnabled = false
                signOut(SupabaseClient.client.gotrue, executor).thenRun {
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                newCustomDialog.dismiss()
            }
            dialogNoButton.setOnClickListener { newCustomDialog.dismiss() }
            newCustomDialog.setCancelable(true)
            newCustomDialog.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun font() {
        binding.title.typeface = Typeface.DEFAULT_BOLD
    }

    private fun addOneSignalPlayerIdToMap(userMap: HashMap<String, Any>) {
        if (OneSignal.User.pushSubscription.optedIn) {
            val playerId = OneSignal.User.pushSubscription.id
            if (!playerId.isNullOrEmpty()) {
                userMap["oneSignalPlayerId"] = playerId
            }
        }
    }
}
