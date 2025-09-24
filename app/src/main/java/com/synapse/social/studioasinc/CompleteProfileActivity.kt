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
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.util.ViewUtilsKt
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class CompleteProfileActivity : AppCompatActivity() {

    private val REQ_CD_SELECTAVATAR = 101

    private var userNameErr = true
    private var avatarUri: String? = null
    private var thedpurl: String? = null

    private lateinit var scroll: ScrollView
    private lateinit var body: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var titleTextView: TextView
    private lateinit var subtitle: TextView
    private lateinit var profile_image_card: CardView
    private lateinit var username_input: FadeEditText
    private lateinit var nickname_input: FadeEditText
    private lateinit var biography_input: FadeEditText
    private lateinit var email_verification: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var buttons: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var profile_image: ImageView
    private lateinit var email_verification_title: TextView
    private lateinit var email_verification_subtitle: TextView
    private lateinit var email_verification_send: TextView
    private lateinit var email_verification_error_ic: ImageView
    private lateinit var email_verification_verified_ic: ImageView
    private lateinit var email_verification_status: TextView
    private lateinit var email_verification_status_refresh: ImageView
    private lateinit var skip_button: com.google.android.material.button.MaterialButton
    private lateinit var complete_button: com.google.android.material.button.MaterialButton

    private lateinit var vbr: Vibrator

    private val SelectAvatar = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_profile)
        initialize(savedInstanceState)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
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

    private fun initialize(savedInstanceState: Bundle?) {
        scroll = findViewById(R.id.scroll)
        body = findViewById(R.id.body)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
        titleTextView = findViewById(R.id.title)
        subtitle = findViewById(R.id.subtitle)
        profile_image_card = findViewById(R.id.profile_image_card)
        username_input = findViewById(R.id.username_input)
        nickname_input = findViewById(R.id.nickname_input)
        biography_input = findViewById(R.id.biography_input)
        email_verification = findViewById(R.id.email_verification)
        buttons = findViewById(R.id.buttons)
        profile_image = findViewById(R.id.profile_image)
        email_verification_title = findViewById(R.id.email_verification_title)
        email_verification_subtitle = findViewById(R.id.email_verification_subtitle)
        email_verification_send = findViewById(R.id.email_verification_send)
        email_verification_error_ic = findViewById(R.id.email_verification_error_ic)
        email_verification_verified_ic = findViewById(R.id.email_verification_verified_ic)
        email_verification_status = findViewById(R.id.email_verification_status)
        email_verification_status_refresh = findViewById(R.id.email_verification_status_refresh)
        skip_button = findViewById(R.id.skip_button)
        complete_button = findViewById(R.id.complete_button)
        vbr = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        profile_image_card.setOnLongClickListener {
            avatarUri = null
            profile_image.setImageResource(R.drawable.avatar)
            vbr.vibrate(48)
            true
        }

        profile_image_card.setOnClickListener {
            startActivityForResult(SelectAvatar, REQ_CD_SELECTAVATAR)
        }

        username_input.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val charSeq = s.toString()
                if (charSeq.trim { it <= ' ' } == "") {
                    username_input.isActivated = true
                    (username_input as EditText).error = getString(R.string.enter_username)
                    userNameErr = true
                } else {
                    if (charSeq.matches("[a-z0-9_.]+".toRegex())) {
                        if (charSeq.length < 3) {
                            username_input.isActivated = true
                            (username_input as EditText).error =
                                getString(R.string.username_err_3_characters)
                            userNameErr = true
                        } else {
                            if (charSeq.length > 25) {
                                username_input.isActivated = true
                                (username_input as EditText).error =
                                    getString(R.string.username_err_25_characters)
                                userNameErr = true
                            } else {
                                username_input.isActivated = false
                                lifecycleScope.launch {
                                    try {
                                        val result = Supabase.client.postgrest["profiles"].select {
                                            filter {
                                                eq("username", charSeq.trim())
                                            }
                                        }
                                        if (result.data.isNotEmpty()) {
                                            username_input.isActivated = true
                                            (username_input as EditText).error =
                                                getString(R.string.username_err_already_taken)
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
                        (username_input as EditText).error =
                            getString(R.string.username_err_invalid_characters)
                        userNameErr = true
                    }
                }
            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {}
        })

        nickname_input.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val charSeq = s.toString()
                if (charSeq.length > 30) {
                    nickname_input.background =
                        GradientDrawable().apply {
                            cornerRadius = 28f
                            setStroke(3, -0xbbcca)
                            setColor(-0x1)
                        }
                    (nickname_input as EditText).error =
                        getString(R.string.nickname_err_30_characters)
                } else {
                    nickname_input.background =
                        GradientDrawable().apply {
                            cornerRadius = 28f
                            setStroke(3, -0x111112)
                            setColor(-0x1)
                        }
                }
            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {}
        })

        biography_input.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val charSeq = s.toString()
                if (charSeq.length > 250) {
                    biography_input.background =
                        GradientDrawable().apply {
                            cornerRadius = 28f
                            setStroke(3, -0xbbcca)
                            setColor(-0x1)
                        }
                    (biography_input as EditText).error =
                        getString(R.string.biography_err_250_characters)
                } else {
                    biography_input.background =
                        GradientDrawable().apply {
                            cornerRadius = 28f
                            setStroke(3, -0x111112)
                            setColor(-0x1)
                        }
                }
            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {}
        })

        skip_button.setOnClickListener {
            SketchwareUtil.showMessage(
                applicationContext,
                "Not possible"
            )
        }

        complete_button.setOnClickListener {
            if (userNameErr) {
                SketchwareUtil.showMessage(
                    applicationContext,
                    getString(R.string.username_err_invalid)
                )
                vbr.vibrate(48)
            } else {
                complete_button.isEnabled = false
                complete_button.text = "Loading..."
                username_input.isEnabled = false
                pushUserData(
                    username_input.text.toString().trim(),
                    nickname_input.text.toString().trim(),
                    biography_input.text.toString().trim(),
                    thedpurl,
                    intent.getStringExtra("googleLoginAvatarUri")
                ) { success, errorMessage ->
                    if (success) {
                        val intent = Intent(applicationContext, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        username_input.isEnabled = true
                        complete_button.isEnabled = true
                        try {
                            complete_button.setText(R.string.continue_button)
                        } catch (e: Exception) {
                            complete_button.text = "Continue"
                        }
                        if (errorMessage == "Permission denied") {
                            SketchwareUtil.showMessage(
                                applicationContext,
                                "Email is not verified"
                            )
                        } else {
                            SketchwareUtil.showMessage(applicationContext, errorMessage)
                        }
                    }
                }
            }
        }
    }

    private fun initializeLogic() {
        email_verification_title.typeface = Typeface.DEFAULT_BOLD
        subtitle.typeface = Typeface.DEFAULT
        titleTextView.typeface = Typeface.DEFAULT_BOLD
        ViewUtilsKt.setStateColor(this, -0x1, -0x1)
        avatarUri = "null"
        thedpurl = "null"
        userNameErr = true
        ViewUtilsKt.setImageColor(email_verification_error_ic, -0xbbcca)
        ViewUtilsKt.setImageColor(email_verification_verified_ic, -0xb25a0b)
        ViewUtilsKt.setGradientDrawable(profile_image_card, Color.TRANSPARENT, 300f, 0, Color.TRANSPARENT)
        ViewUtilsKt.setGradientDrawable(email_verification, -0x1, 28f, 3, -0x111112)
        ViewUtilsKt.setViewGraphics(email_verification_send, -0xbbac6f, -0xbbac6f, 300, 0, Color.TRANSPARENT)
        if (intent.hasExtra("findedUsername")) {
            username_input.setText(intent.getStringExtra("findedUsername"))
        } else {
            username_input.setText("")
        }
        if (intent.hasExtra("googleLoginName") && intent.hasExtra("googleLoginEmail") && intent.hasExtra("googleLoginAvatarUri")) {
            Glide.with(applicationContext)
                .load(Uri.parse(intent.getStringExtra("googleLoginAvatarUri")))
                .into(profile_image)
            nickname_input.setText(intent.getStringExtra("googleLoginName"))
        }
        font()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_CD_SELECTAVATAR -> if (resultCode == Activity.RESULT_OK) {
                val filePath = ArrayList<String>()
                if (data?.data != null) {
                    filePath.add(
                        FileUtil.convertUriToFilePath(
                            applicationContext,
                            data.data
                        )
                    )
                } else if (data?.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        val item = data.clipData!!.getItemAt(i)
                        filePath.add(
                            FileUtil.convertUriToFilePath(
                                applicationContext,
                                item.uri
                            )
                        )
                    }
                }
                if (filePath.isNotEmpty()) {
                    profile_image.setImageBitmap(
                        FileUtil.decodeSampleBitmapFromPath(
                            filePath[0],
                            1024,
                            1024
                        )
                    )
                    val path = filePath[0]
                    uploadImage(path, { imageUrl ->
                        thedpurl = imageUrl
                    }, {
                        SketchwareUtil.showMessage(applicationContext, "Image upload failed")
                    })
                }
            }
        }
    }

    override fun onBackPressed() {
        val newCustomDialog = AlertDialog.Builder(this).create()
        val newCustomDialogLI = layoutInflater
        val newCustomDialogCV = newCustomDialogLI.inflate(R.layout.dialog_synapse_bg_view, null)
        newCustomDialog.setView(newCustomDialogCV)
        newCustomDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialog_title = newCustomDialogCV.findViewById<TextView>(R.id.dialog_title)
        val dialog_message = newCustomDialogCV.findViewById<TextView>(R.id.dialog_message)
        val dialog_no_button = newCustomDialogCV.findViewById<TextView>(R.id.dialog_no_button)
        val dialog_yes_button = newCustomDialogCV.findViewById<TextView>(R.id.dialog_yes_button)
        dialog_yes_button.setTextColor(-0xbbcca)
        ViewUtilsKt.setViewGraphics(dialog_yes_button, -0x1, -0x3323, 28, 0, Color.TRANSPARENT)
        dialog_no_button.setTextColor(-0xde690d)
        ViewUtilsKt.setViewGraphics(dialog_no_button, -0x1, -0x442105, 28, 0, Color.TRANSPARENT)
        dialog_title.text = getString(R.string.info)
        dialog_message.text =
            getString(R.string.cancel_complete_profile_warn) + "\n\n" + getString(R.string.cancel_complete_profile_warn2)
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
            val newCustomDialogCV =
                newCustomDialogLI.inflate(R.layout.dialog_synapse_bg_view, null)
            newCustomDialog.setView(newCustomDialogCV)
            newCustomDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val dialog_title = newCustomDialogCV.findViewById<TextView>(R.id.dialog_title)
            val dialog_message = newCustomDialogCV.findViewById<TextView>(R.id.dialog_message)
            val dialog_no_button =
                newCustomDialogCV.findViewById<TextView>(R.id.dialog_no_button)
            val dialog_yes_button =
                newCustomDialogCV.findViewById<TextView>(R.id.dialog_yes_button)
            dialog_yes_button.setTextColor(-0xbbcca)
            ViewUtilsKt.setViewGraphics(dialog_yes_button, -0x1, -0x3323, 28, 0, Color.TRANSPARENT)
            dialog_no_button.setTextColor(-0xde690d)
            ViewUtilsKt.setViewGraphics(dialog_no_button, -0x1, -0x442105, 28, 0, Color.TRANSPARENT)
            dialog_title.text = getString(R.string.info)
            dialog_message.text =
                getString(R.string.cancel_create_account_warn) + "\n\n" + getString(R.string.cancel_create_account_warn2)
            dialog_yes_button.text = getString(R.string.yes)
            dialog_no_button.text = getString(R.string.no)
            dialog_yes_button.setOnClickListener {
                item.isEnabled = false
                // TODO: Implement a serverless function to delete the user.
                // This requires admin privileges and should be handled by a secure serverless function.
                // The function should delete the user from the `auth.users` table and also from the `profiles` table.
                //
                // lifecycleScope.launch {
                //     try {
                //         Supabase.client.functions.invoke("delete-user")
                //         val intent = Intent(applicationContext, MainActivity::class.java)
                //         startActivity(intent)
                //         finish()
                //     } catch (e: Exception) {
                //         SketchwareUtil.showMessage(applicationContext, e.message)
                //         invalidateOptionsMenu()
                //     }
                // }
                newCustomDialog.dismiss()
            }
            dialog_no_button.setOnClickListener { newCustomDialog.dismiss() }
            newCustomDialog.setCancelable(true)
            newCustomDialog.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun font() {
        titleTextView.typeface = Typeface.DEFAULT_BOLD
    }

    private fun pushUserData(
        username: String,
        nickname: String,
        biography: String,
        avatarUrl: String?,
        googleLoginAvatarUri: String?,
        callback: (Boolean, String?) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val user = Supabase.client.auth.currentUserOrNull()
                if (user != null) {
                    val profileData = mapOf(
                        "id" to user.id,
                        "username" to username,
                        "nickname" to nickname,
                        "biography" to biography,
                        "avatar_url" to (avatarUrl ?: googleLoginAvatarUri),
                        "email" to user.email
                    )
                    try {
                        Supabase.client.postgrest["profiles"].insert(profileData)
                        callback(true, null)
                    } catch (e: Exception) {
                        callback(false, e.message)
                    }
                } else {
                    callback(false, "User not logged in")
                }
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }
    }

    private fun uploadImage(
        filePath: String,
        onUploadSuccess: (String) -> Unit,
        onUploadError: () -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val file = File(filePath)
                val fileName = "${UUID.randomUUID()}.${file.extension}"
                val url = Supabase.client.storage["avatars"].upload(fileName, file.readBytes())
                onUploadSuccess(url)
            } catch (e: Exception) {
                onUploadError()
            }
        }
    }
}
