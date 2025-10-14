
package com.synapse.social.studioasinc

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.model.UserProfile
import com.synapse.social.studioasinc.util.ViewUtilsKt
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class CompleteProfileActivity : AppCompatActivity() {

    private val REQ_CD_SELECTAVATAR = 101

    private var userNameErr = false
    private var avatarUri = "null"
    private var thedpurl = "null"
    private var path = ""

    private lateinit var username_input: FadeEditText
    private lateinit var nickname_input: FadeEditText
    private lateinit var biography_input: FadeEditText
    private lateinit var profile_image: ImageView
    private lateinit var complete_button: com.google.android.material.button.MaterialButton
    private lateinit var vbr: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_profile)
        initialize()
        initializeLogic()
    }

    private fun initialize() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""
        toolbar.setNavigationOnClickListener { onBackPressed() }

        username_input = findViewById(R.id.username_input)
        nickname_input = findViewById(R.id.nickname_input)
        biography_input = findViewById(R.id.biography_input)
        profile_image = findViewById(R.id.profile_image)
        complete_button = findViewById(R.id.complete_button)
        vbr = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val profile_image_card = findViewById<CardView>(R.id.profile_image_card)
        profile_image_card.setOnLongClickListener {
            if (thedpurl != "null") {
                ImageUploader.deleteImage(thedpurl) { success, errorMessage ->
                    if (success) {
                        thedpurl = "null"
                        avatarUri = "null"
                        profile_image.setImageResource(R.drawable.avatar)
                    } else {
                        SketchwareUtil.showMessage(applicationContext, errorMessage)
                    }
                }
            }
            vbr.vibrate(48)
            true
        }

        profile_image_card.setOnClickListener {
            val selectAvatar = Intent(Intent.ACTION_GET_CONTENT)
            selectAvatar.type = "image/*"
            selectAvatar.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(selectAvatar, REQ_CD_SELECTAVATAR)
        }

        username_input.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val charSeq = s.toString()
                if (charSeq.trim { it <= ' ' }.isEmpty()) {
                    username_input.isActivated = true
                    (username_input as EditText).error = getString(R.string.enter_username)
                    userNameErr = true
                } else {
                    if (charSeq.matches(Regex("[a-z0-9_.]+"))) {
                        if (charSeq.contains("q") || charSeq.contains("w") || charSeq.contains("e") || charSeq.contains("r") || charSeq.contains("t") || charSeq.contains("y") || charSeq.contains("u") || charSeq.contains("i") || charSeq.contains("o") || charSeq.contains("p") || charSeq.contains("a") || charSeq.contains("s") || charSeq.contains("d") || charSeq.contains("f") || charSeq.contains("g") || charSeq.contains("h") || charSeq.contains("j") || charSeq.contains("k") || charSeq.contains("l") || charSeq.contains("z") || charSeq.contains("x") || charSeq.contains("c") || charSeq.contains("v") || charSeq.contains("b") || charSeq.contains("n") || charSeq.contains("m")) {
                            if (username_input.text.toString().length < 3) {
                                username_input.isActivated = true
                                (username_input as EditText).error = getString(R.string.username_err_3_characters)
                                userNameErr = true
                            } else {
                                if (username_input.text.toString().length > 25) {
                                    username_input.isActivated = true
                                    (username_input as EditText).error = getString(R.string.username_err_25_characters)
                                    userNameErr = true
                                } else {
                                    username_input.isActivated = false
                                    lifecycleScope.launch {
                                        try {
                                            val result = Supabase.client.postgrest["profiles"].select {
                                                filter("username", "eq", charSeq.trim())
                                            }
                                            if (result.data.isNotEmpty()) {
                                                username_input.isActivated = true
                                                (username_input as EditText).error = getString(R.string.username_err_already_taken)
                                                userNameErr = true
                                            } else {
                                                username_input.isActivated = false
                                                userNameErr = false
                                            }
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                }
                            }
                        } else {
                            username_input.isActivated = true
                            (username_input as EditText).error = getString(R.string.username_err_one_letter)
                            userNameErr = true
                        }
                    } else {
                        username_input.isActivated = true
                        (username_input as EditText).error = getString(R.string.username_err_invalid_characters)
                        userNameErr = true
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        nickname_input.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.length > 30) {
                    nickname_input.background = GradientDrawable().apply {
                        cornerRadius = 28f
                        setStroke(3, -0xbbbc)
                        setColor(-0x1)
                    }
                    (nickname_input as EditText).error = getString(R.string.nickname_err_30_characters)
                } else {
                    nickname_input.background = GradientDrawable().apply {
                        cornerRadius = 28f
                        setStroke(3, -0x111112)
                        setColor(-0x1)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        biography_input.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.length > 250) {
                    biography_input.background = GradientDrawable().apply {
                        cornerRadius = 28f
                        setStroke(3, -0xbbbc)
                        setColor(-0x1)
                    }
                    (biography_input as EditText).error = getString(R.string.biography_err_250_characters)
                } else {
                    biography_input.background = GradientDrawable().apply {
                        cornerRadius = 28f
                        setStroke(3, -0x111112)
                        setColor(-0x1)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        val skip_button = findViewById<com.google.android.material.button.MaterialButton>(R.id.skip_button)
        skip_button.setOnClickListener {
            SketchwareUtil.showMessage(applicationContext, "Not possible")
        }

        complete_button.setOnClickListener {
            if (userNameErr) {
                SketchwareUtil.showMessage(applicationContext, getString(R.string.username_err_invalid))
                vbr.vibrate(48)
            } else {
                complete_button.isEnabled = false
                complete_button.text = "Loading..."
                username_input.isEnabled = false

                lifecycleScope.launch {
                    val user = Supabase.client.auth.currentUserOrNull()
                    if (user != null) {
                        val userProfile = UserProfile(
                            id = user.id,
                            username = username_input.text.toString().trim(),
                            nickname = nickname_input.text.toString().trim(),
                            biography = biography_input.text.toString().trim(),
                                avatar_url = thedpurl,
                                email = user.email ?: ""
                        )
                        val pusher = UserDataPusher()
                            pusher.pushData(userProfile, lifecycleScope) { success, errorMessage ->
                            if (success) {
                                val intent = Intent(this@CompleteProfileActivity, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                username_input.isEnabled = true
                                complete_button.isEnabled = true
                                complete_button.text = getString(R.string.continue_button)
                                SketchwareUtil.showMessage(applicationContext, errorMessage)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initializeLogic() {
        findViewById<TextView>(R.id.email_verification_title).typeface = Typeface.DEFAULT_BOLD
        findViewById<TextView>(R.id.subtitle).typeface = Typeface.DEFAULT
        findViewById<TextView>(R.id.title).typeface = Typeface.DEFAULT_BOLD
        ViewUtilsKt.setStateColor(this, -0x1, -0x1)
        avatarUri = "null"
        thedpurl = "null"
        userNameErr = true
        ViewUtilsKt.setImageColor(findViewById(R.id.email_verification_error_ic), -0xbbbc)
        ViewUtilsKt.setImageColor(findViewById(R.id.email_verification_verified_ic), -0xbfa510)
        ViewUtilsKt.setGradientDrawable(findViewById(R.id.profile_image_card), Color.TRANSPARENT, 300f, 0, Color.TRANSPARENT)
        ViewUtilsKt.setGradientDrawable(findViewById(R.id.email_verification), -0x1, 28f, 3, -0x111112)
        ViewUtilsKt.setViewGraphics(findViewById(R.id.email_verification_send), -0xbbac6f, -0xbbac6f, 300, 0, Color.TRANSPARENT)
        if (intent.hasExtra("findedUsername")) {
            username_input.setText(intent.getStringExtra("findedUsername"))
        } else {
            username_input.setText("")
        }
        if (intent.hasExtra("googleLoginName") && intent.hasExtra("googleLoginEmail") && intent.hasExtra("googleLoginAvatarUri")) {
            Glide.with(applicationContext).load(Uri.parse(intent.getStringExtra("googleLoginAvatarUri"))).into(profile_image)
            nickname_input.setText(intent.getStringExtra("googleLoginName"))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CD_SELECTAVATAR && resultCode == Activity.RESULT_OK) {
            val filePath = ArrayList<String>()
            if (data != null) {
                if (data.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        val item = data.clipData!!.getItemAt(i)
                        filePath.add(FileUtil.convertUriToFilePath(applicationContext, item.uri))
                    }
                } else {
                    filePath.add(FileUtil.convertUriToFilePath(applicationContext, data.data))
                }
            }
            profile_image.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(filePath[0], 1024, 1024))
            path = filePath[0]
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

    override fun onBackPressed() {
        val newCustomDialog = AlertDialog.Builder(this).create()
        val newCustomDialogLI = layoutInflater
        val newCustomDialogCV = newCustomDialogLI.inflate(R.layout.dialog_synapse_bg_view, null)
        newCustomDialog.setView(newCustomDialogCV)
        newCustomDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialog_title = newCustomDialogCV.findViewById<TextView>(R.id.dialog_title)
        val dialog_message = newCustomDialogCV.findViewById<TextView>(R.id.dialog_message)
        val dialog_no_button = newCustomDialogCV.findViewById<TextView>(R.id.dialog_no_button)
        val dialog_yes_button = newCustomDialogCV.findViewById<TextView>(R.id.dialog_yes_button)
        dialog_yes_button.setTextColor(-0xbbbc)
        ViewUtilsKt.setViewGraphics(dialog_yes_button, -0x1, -0x322d, 28, 0, Color.TRANSPARENT)
        dialog_no_button.setTextColor(-0xde690d)
        ViewUtilsKt.setViewGraphics(dialog_no_button, -0x1, -0x442205, 28, 0, Color.TRANSPARENT)
        dialog_title.text = getString(R.string.info)
        dialog_message.text = getString(R.string.cancel_complete_profile_warn) + "\n\n" + getString(R.string.cancel_complete_profile_warn2)
        dialog_yes_button.text = getString(R.string.yes)
        dialog_no_button.text = getString(R.string.no)
        dialog_yes_button.setOnClickListener {
            lifecycleScope.launch {
                Supabase.client.auth.signOut()
                finish()
            }
        }
        dialog_no_button.setOnClickListener { newCustomDialog.dismiss() }
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
            val dialog_title = newCustomDialogCV.findViewById<TextView>(R.id.dialog_title)
            val dialog_message = newCustomDialogCV.findViewById<TextView>(R.id.dialog_message)
            val dialog_no_button = newCustomDialogCV.findViewById<TextView>(R.id.dialog_no_button)
            val dialog_yes_button = newCustomDialogCV.findViewById<TextView>(R.id.dialog_yes_button)
            dialog_yes_button.setTextColor(-0xbbbc)
            ViewUtilsKt.setViewGraphics(dialog_yes_button, -0x1, -0x322d, 28, 0, Color.TRANSPARENT)
            dialog_no_button.setTextColor(-0xde690d)
            ViewUtilsKt.setViewGraphics(dialog_no_button, -0x1, -0x442205, 28, 0, Color.TRANSPARENT)
            dialog_title.text = getString(R.string.info)
            dialog_message.text = getString(R.string.cancel_create_account_warn) + "\n\n" + getString(R.string.cancel_create_account_warn2)
            dialog_yes_button.text = getString(R.string.yes)
            dialog_no_button.text = getString(R.string.no)
            dialog_yes_button.setOnClickListener {
                item.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val user = Supabase.client.auth.currentUserOrNull()
                        if (user != null) {
                            Supabase.client.functions.invoke(
                                "delete-user",
                                mapOf("id" to user.id)
                            )
                        }
                    } catch (e: Exception) {
                        // Handle error
                    }
                    newCustomDialog.dismiss()
                }
            }
            dialog_no_button.setOnClickListener { newCustomDialog.dismiss() }
            newCustomDialog.setCancelable(true)
            newCustomDialog.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
