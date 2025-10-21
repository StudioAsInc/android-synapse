package com.synapse.social.studioasinc.home

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.compatibility.FirebaseAuth
import com.synapse.social.studioasinc.compatibility.*
import com.synapse.social.studioasinc.CreateLineVideoActivity
import com.synapse.social.studioasinc.CreatePostActivity
import com.synapse.social.studioasinc.R
import java.util.*

class HeaderAdapter(
    private val context: Context,
    private val storyAdapter: StoryAdapter
) : RecyclerView.Adapter<HeaderAdapter.ViewHolder>() {

    private val udb = FirebaseDatabase.getInstance().getReference("skyline/users")
    private val vbr = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.feed_header, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = 1

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.profileRef?.removeEventListener(holder.profileListener!!)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storiesView: RecyclerView = itemView.findViewById(R.id.storiesView)
        val miniPostLayoutProfileCard: CardView = itemView.findViewById(R.id.miniPostLayoutProfileCard)
        private val miniPostLayoutTextPostInput: EditText = itemView.findViewById(R.id.miniPostLayoutTextPostInput)
        private val miniPostLayoutProfileImage: ImageView = itemView.findViewById(R.id.miniPostLayoutProfileImage)
        private val miniPostLayoutImagePost: ImageView = itemView.findViewById(R.id.miniPostLayoutImagePost)
        private val miniPostLayoutVideoPost: ImageView = itemView.findViewById(R.id.miniPostLayoutVideoPost)
        private val miniPostLayoutTextPost: ImageView = itemView.findViewById(R.id.miniPostLayoutTextPost)
        private val miniPostLayoutMoreButton: ImageView = itemView.findViewById(R.id.miniPostLayoutMoreButton)
        private val miniPostLayoutTextPostPublish: TextView = itemView.findViewById(R.id.miniPostLayoutTextPostPublish)

        var profileListener: ValueEventListener? = null
        var profileRef: DatabaseReference? = null

        fun bind() {
            storiesView.apply {
                adapter = storyAdapter
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }
            _viewGraphics(miniPostLayoutTextPostPublish, Color.TRANSPARENT, Color.TRANSPARENT, 300.0, 2.0, 0xFF616161.toInt())

            loadProfileImage()
            setupClickListeners()
        }

        private fun loadProfileImage() {
            profileRef = udb.child(FirebaseAuth.getInstance().currentUser!!.uid)
            profileListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val avatar = dataSnapshot.child("avatar").getValue(String::class.java)
                        if (avatar != null && avatar != "null") {
                            Glide.with(context).load(Uri.parse(avatar)).into(miniPostLayoutProfileImage)
                        } else {
                            miniPostLayoutProfileImage.setImageResource(R.drawable.avatar)
                        }
                    } else {
                        miniPostLayoutProfileImage.setImageResource(R.drawable.avatar)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(context, "Error fetching user profile: " + databaseError.message, Toast.LENGTH_SHORT).show()
                    miniPostLayoutProfileImage.setImageResource(R.drawable.avatar)
                }
            }
            profileRef?.addValueEventListener(profileListener!!)
        }

        private fun setupClickListeners() {
            miniPostLayoutTextPostPublish.visibility = View.GONE
            _imageColor(miniPostLayoutImagePost, 0xFF445E91.toInt())
            _imageColor(miniPostLayoutVideoPost, 0xFF445E91.toInt())
            _imageColor(miniPostLayoutTextPost, 0xFF445E91.toInt())
            _imageColor(miniPostLayoutMoreButton, 0xFF445E91.toInt())
            _viewGraphics(miniPostLayoutImagePost, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 300.0, 1.0, 0xFFEEEEEE.toInt())
            _viewGraphics(miniPostLayoutVideoPost, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 300.0, 1.0, 0xFFEEEEEE.toInt())
            _viewGraphics(miniPostLayoutTextPost, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 300.0, 1.0, 0xFFEEEEEE.toInt())
            _viewGraphics(miniPostLayoutMoreButton, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 300.0, 1.0, 0xFFEEEEEE.toInt())

            miniPostLayoutTextPostInput.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        miniPostLayoutTextPostPublish.visibility = View.GONE
                    } else {
                        _viewGraphics(miniPostLayoutTextPostPublish, context.resources.getColor(R.color.colorPrimary), 0xFFC5CAE9.toInt(), 300.0, 0.0, Color.TRANSPARENT)
                        miniPostLayoutTextPostPublish.visibility = View.VISIBLE
                    }
                }
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable) {}
            })

            miniPostLayoutImagePost.setOnClickListener {
                context.startActivity(Intent(context, CreatePostActivity::class.java))
            }

            miniPostLayoutVideoPost.setOnClickListener {
                context.startActivity(Intent(context, CreateLineVideoActivity::class.java))
            }

            miniPostLayoutTextPostPublish.setOnClickListener {
                if (miniPostLayoutTextPostInput.text.toString().trim().isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.please_enter_text), Toast.LENGTH_SHORT).show()
                } else {
                    if (miniPostLayoutTextPostInput.text.length <= 1500) {
                        val uniqueKey = udb.push().key ?: ""
                        val createPostMap = hashMapOf<String, Any>(
                            "key" to uniqueKey,
                            "uid" to FirebaseAuth.getInstance().currentUser!!.uid,
                            "post_text" to miniPostLayoutTextPostInput.text.toString().trim(),
                            "post_type" to "TEXT",
                            "post_hide_views_count" to "false",
                            "post_region" to "none",
                            "post_hide_like_count" to "false",
                            "post_hide_comments_count" to "false",
                            "post_visibility" to "public",
                            "post_disable_favorite" to "false",
                            "post_disable_comments" to "false",
                            "publish_date" to Calendar.getInstance().timeInMillis.toString()
                        )
                        FirebaseDatabase.getInstance().getReference("skyline/posts").child(uniqueKey).updateChildren(createPostMap) { error, _ ->
                            if (error == null) {
                                Toast.makeText(context, context.getString(R.string.post_publish_success), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        miniPostLayoutTextPostInput.setText("")
                    }
                }
                vbr.vibrate(48L)
            }
        }
    }

    private fun _viewGraphics(view: View, onFocus: Int, onRipple: Int, radius: Double, stroke: Double, strokeColor: Int) {
        val gradientDrawable = android.graphics.drawable.GradientDrawable()
        gradientDrawable.setColor(onFocus)
        gradientDrawable.cornerRadius = radius.toFloat()
        gradientDrawable.setStroke(stroke.toInt(), strokeColor)
        val rippleDrawable = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList(arrayOf(intArrayOf()), intArrayOf(onRipple)),
            gradientDrawable,
            null
        )
        view.background = rippleDrawable
    }

    private fun _imageColor(imageView: ImageView, color: Int) {
        imageView.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP)
    }
}
