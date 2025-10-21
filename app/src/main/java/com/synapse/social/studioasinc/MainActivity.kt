package com.synapse.social.studioasinc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.synapse.social.studioasinc.compatibility.FirebaseAuth
import com.synapse.social.studioasinc.databinding.ActivityMainBinding
import com.synapse.social.studioasinc.databinding.DialogErrorBinding
import com.synapse.social.studioasinc.databinding.DialogUpdateBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFullscreen()
        createNotificationChannels()
        setupListeners()
        setupObservers()
        viewModel.checkForUpdates()
    }

    private fun setupFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val messagesChannel = NotificationChannel(
                "messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat message notifications"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }

            val generalChannel = NotificationChannel(
                "general",
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                enableLights(false)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    private fun setupListeners() {
        binding.appLogo.setOnLongClickListener {
            if (FirebaseAuth.getInstance().currentUser?.email == BuildConfig.DEVELOPER_EMAIL) {
                finish()
            }
            true
        }
    }

    private fun setupObservers() {
        viewModel.updateState.observe(this) { state ->
            when (state) {
                is UpdateState.UpdateAvailable -> showUpdateDialog(state.title, state.versionName, state.changelog, state.updateLink, state.isCancelable)
                is UpdateState.NoUpdate -> viewModel.checkUserAuthentication()
                is UpdateState.Error -> showErrorDialog(state.message)
            }
        }

        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Authenticated -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                is AuthState.Unauthenticated -> {
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                }
                is AuthState.Banned -> {
                    Toast.makeText(this, "You are banned & Signed Out.", Toast.LENGTH_LONG).show()
                    FirebaseAuth.getInstance().signOut()
                    finish()
                }
                is AuthState.NeedsProfileCompletion -> {
                    startActivity(Intent(this, CompleteProfileActivity::class.java))
                    finish()
                }
                is AuthState.Error -> showErrorDialog(state.message)
            }
        }
    }

    private fun showUpdateDialog(title: String, versionName: String, changelog: String, updateLink: String, isCancelable: Boolean) {
        val dialogBinding = DialogUpdateBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(isCancelable)
            .create()

        dialogBinding.updateTitle.text = title
        dialogBinding.updateVersion.text = "Version $versionName"
        dialogBinding.updateChangelog.text = changelog.replace("\\\\n", "\\n")

        dialogBinding.buttonUpdate.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateLink))
            startActivity(intent)
            dialog.dismiss()
            finish()
        }

        dialogBinding.buttonLater.setOnClickListener {
            dialog.dismiss()
            if (isCancelable) {
                viewModel.checkUserAuthentication()
            }
        }

        if (!isCancelable) {
            dialogBinding.buttonLater.visibility = View.GONE
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showErrorDialog(errorMessage: String) {
        val dialogBinding = DialogErrorBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.errorMessageTextview.text = errorMessage

        dialogBinding.okButton.setOnClickListener {
            dialog.dismiss()
            viewModel.checkUserAuthentication()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}
