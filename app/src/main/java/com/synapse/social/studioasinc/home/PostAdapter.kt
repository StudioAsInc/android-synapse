package com.synapse.social.studioasinc.home

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.browser.customtabs.CustomTabsIntent
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.PostCommentsBottomSheetDialog
import com.synapse.social.studioasinc.PostMoreBottomSheetDialog
import com.synapse.social.studioasinc.ProfileActivity
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.styling.MarkdownRenderer
import com.synapse.social.studioasinc.util.NotificationUtils
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class PostAdapter(
    private val fragment: Fragment,
    private var posts: List<Post>
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val context = fragment.requireContext()
    private val userInfoCache = mutableMapOf<String, User>()
    private val postLikeCountCache = mutableMapOf<String, Long>()
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.synapse_post_cv, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int = posts.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val body: LinearLayout = itemView.findViewById(R.id.body)
        private val topMoreButton: ImageView = itemView.findViewById(R.id.topMoreButton)
        private val userInfoProfileCard: CardView = itemView.findViewById(R.id.userInfoProfileCard)
        private val userInfoProfileImage: ImageView = itemView.findViewById(R.id.userInfoProfileImage)
        private val userInfoUsername: TextView = itemView.findViewById(R.id.userInfoUsername)
        private val userInfoGenderBadge: ImageView = itemView.findViewById(R.id.userInfoGenderBadge)
        private val userInfoUsernameVerifiedBadge: ImageView = itemView.findViewById(R.id.userInfoUsernameVerifiedBadge)
        private val postPublishDate: TextView = itemView.findViewById(R.id.postPublishDate)
        private val postPrivateStateIcon: ImageView = itemView.findViewById(R.id.postPrivateStateIcon)
        private val postMessageTextMiddle: TextView = itemView.findViewById(R.id.postMessageTextMiddle)
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val likeButton: LinearLayout = itemView.findViewById(R.id.likeButton)
        private val commentsButton: LinearLayout = itemView.findViewById(R.id.commentsButton)
        private val favoritePostButton: ImageView = itemView.findViewById(R.id.favoritePostButton)
        private val likeButtonIc: ImageView = itemView.findViewById(R.id.likeButtonIc)
        private val likeButtonCount: TextView = itemView.findViewById(R.id.likeButtonCount)
        private val commentsButtonCount: TextView = itemView.findViewById(R.id.commentsButtonCount)

        fun bind(post: Post) {
            body.visibility = View.GONE
            userInfoProfileCard.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 300f
                setColor(Color.TRANSPARENT)
            }
            _imageColor(postPrivateStateIcon, 0xFF616161.toInt())
            _viewGraphics(topMoreButton, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 300.0, 0.0, Color.TRANSPARENT)

            post.post_text?.let {
                MarkdownRenderer.get(context).render(postMessageTextMiddle, it)
                postMessageTextMiddle.visibility = View.VISIBLE
            } ?: run {
                postMessageTextMiddle.visibility = View.GONE
            }

            post.post_image?.let {
                Glide.with(context).load(Uri.parse(it)).into(postImage)
                postImage.setOnClickListener { _openWebView(post.post_image) }
                postImage.visibility = View.VISIBLE
            } ?: run {
                postImage.visibility = View.GONE
            }

            likeButtonCount.visibility = if (post.post_hide_like_count == "true") View.GONE else View.VISIBLE
            commentsButtonCount.visibility = if (post.post_hide_comments_count == "true") View.GONE else View.VISIBLE
            commentsButton.visibility = if (post.post_disable_comments == "true") View.GONE else View.VISIBLE

            _setTime(post.publish_date.toDouble(), postPublishDate)

            fetchAndDisplayUserInfo(post)
            setupActionListeners(post)
        }

        private fun fetchAndDisplayUserInfo(post: Post) {
            val postUid = post.uid
            if (userInfoCache.containsKey(postUid)) {
                userInfoCache[postUid]?.let {
                    _updatePostViewVisibility(body, postPrivateStateIcon, post.uid, post.post_visibility)
                    _displayUserInfoFromCache(it, userInfoProfileImage, userInfoUsername, userInfoGenderBadge, userInfoUsernameVerifiedBadge)
                }
            } else {
                val mExecutorService = Executors.newSingleThreadExecutor()
                val mMainHandler = Handler(Looper.getMainLooper())
                mExecutorService.execute {
                    val userRef = firebaseDatabase.getReference("skyline/users").child(postUid)
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                val user = dataSnapshot.getValue(User::class.java)!!
                                userInfoCache[postUid] = user
                                mMainHandler.post {
                                    _updatePostViewVisibility(body, postPrivateStateIcon, post.uid, post.post_visibility)
                                    _displayUserInfoFromCache(user, userInfoProfileImage, userInfoUsername, userInfoGenderBadge, userInfoUsernameVerifiedBadge)
                                }
                            } else {
                                mMainHandler.post {
                                    userInfoProfileImage.setImageResource(R.drawable.avatar)
                                    userInfoUsername.text = "Unknown User"
                                    userInfoGenderBadge.visibility = View.GONE
                                    userInfoUsernameVerifiedBadge.visibility = View.GONE
                                    body.visibility = View.GONE
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            mMainHandler.post {
                                userInfoProfileImage.setImageResource(R.drawable.avatar)
                                userInfoUsername.text = "Error User"
                                userInfoGenderBadge.visibility = View.GONE
                                userInfoUsernameVerifiedBadge.visibility = View.GONE
                                body.visibility = View.GONE
                            }
                        }
                    })
                }
            }
        }

        private fun setupActionListeners(post: Post) {
            val postKey = post.key
            val currentUid = currentUser?.uid ?: ""

            val getLikeCheck = firebaseDatabase.getReference("skyline/posts-likes").child(postKey).child(currentUid)
            val getCommentsCount = firebaseDatabase.getReference("skyline/posts-comments").child(postKey)
            val getLikesCount = firebaseDatabase.getReference("skyline/posts-likes").child(postKey)
            val getFavoriteCheck = firebaseDatabase.getReference("skyline/favorite-posts").child(currentUid).child(postKey)

            getLikeCheck.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    likeButtonIc.setImageResource(if (dataSnapshot.exists()) R.drawable.post_icons_1_2 else R.drawable.post_icons_1_1)
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            })

            getCommentsCount.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    _setCount(commentsButtonCount, dataSnapshot.childrenCount.toDouble())
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            })

            getLikesCount.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val count = dataSnapshot.childrenCount
                    _setCount(likeButtonCount, count.toDouble())
                    postLikeCountCache[postKey] = count
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            })

            getFavoriteCheck.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    favoritePostButton.setImageResource(if (dataSnapshot.exists()) R.drawable.delete_favorite_post_ic else R.drawable.add_favorite_post_ic)
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            })

            likeButton.setOnClickListener {
                val likeRef = firebaseDatabase.getReference("skyline/posts-likes").child(postKey).child(currentUid)
                likeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        var currentLikes = postLikeCountCache[postKey] ?: 0L
                        if (dataSnapshot.exists()) {
                            likeRef.removeValue()
                            currentLikes--
                            likeButtonIc.setImageResource(R.drawable.post_icons_1_1)
                        } else {
                            likeRef.setValue(currentUid)
                            NotificationUtils.sendPostLikeNotification(postKey, post.uid)
                            currentLikes++
                            likeButtonIc.setImageResource(R.drawable.post_icons_1_2)
                        }
                        postLikeCountCache[postKey] = currentLikes
                        _setCount(likeButtonCount, currentLikes.toDouble())
                    }
                    override fun onCancelled(databaseError: DatabaseError) {}
                })
                vibrator.vibrate(24L)
            }

            commentsButton.setOnClickListener {
                val sendPostKey = Bundle().apply {
                    putString("postKey", postKey)
                    putString("postPublisherUID", post.uid)
                    userInfoCache[post.uid]?.let { putString("postPublisherAvatar", it.avatar) }
                }
                PostCommentsBottomSheetDialog().apply {
                    arguments = sendPostKey
                    show(fragment.childFragmentManager, tag)
                }
            }

            userInfoProfileImage.setOnClickListener {
                context.startActivity(Intent(context, ProfileActivity::class.java).putExtra("uid", post.uid))
            }

            favoritePostButton.setOnClickListener {
                val favoriteRef = firebaseDatabase.getReference("skyline/favorite-posts").child(currentUid).child(postKey)
                favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            favoriteRef.removeValue()
                        } else {
                            favoriteRef.setValue(postKey)
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {}
                })
                vibrator.vibrate(24L)
            }

            topMoreButton.setOnClickListener {
                val sendPostKey = Bundle().apply {
                    putString("postKey", postKey)
                    putString("postPublisherUID", post.uid)
                    putString("postType", post.post_type)
                    post.post_text?.let { putString("postText", it) } ?: putString("postText", "")
                    post.post_image?.let { if (it.isNotEmpty()) putString("postImg", it) }
                }
                PostMoreBottomSheetDialog().apply {
                    arguments = sendPostKey
                    show(fragment.childFragmentManager, tag)
                }
            }
        }
    }

    private fun _updatePostViewVisibility(body: LinearLayout, postPrivateStateIcon: ImageView, postUid: String, postVisibility: String) {
        if ("private" == postVisibility) {
            if (postUid == currentUser?.uid) {
                postPrivateStateIcon.visibility = View.VISIBLE
                body.visibility = View.VISIBLE
            } else {
                body.visibility = View.GONE
            }
        } else {
            body.visibility = View.VISIBLE
            postPrivateStateIcon.visibility = View.GONE
        }
    }

    private fun _displayUserInfoFromCache(user: User, userInfoProfileImage: ImageView, userInfoUsername: TextView, userInfoGenderBadge: ImageView, userInfoUsernameVerifiedBadge: ImageView) {
        if ("true" == user.banned) {
            userInfoProfileImage.setImageResource(R.drawable.banned_avatar)
        } else {
            if ("null" == user.avatar) {
                userInfoProfileImage.setImageResource(R.drawable.avatar)
            } else {
                Glide.with(context).load(Uri.parse(user.avatar)).into(userInfoProfileImage)
            }
        }

        userInfoUsername.text = if ("null" == user.nickname) "@" + user.username else user.nickname

        when (user.gender) {
            "hidden" -> userInfoGenderBadge.visibility = View.GONE
            "male" -> {
                userInfoGenderBadge.setImageResource(R.drawable.male_badge)
                userInfoGenderBadge.visibility = View.VISIBLE
            }
            "female" -> {
                userInfoGenderBadge.setImageResource(R.drawable.female_badge)
                userInfoGenderBadge.visibility = View.VISIBLE
            }
        }

        when (user.account_type) {
            "admin" -> {
                userInfoUsernameVerifiedBadge.setImageResource(R.drawable.admin_badge)
                userInfoUsernameVerifiedBadge.visibility = View.VISIBLE
            }
            "moderator" -> {
                userInfoUsernameVerifiedBadge.setImageResource(R.drawable.moderator_badge)
                userInfoUsernameVerifiedBadge.visibility = View.VISIBLE
            }
            "support" -> {
                userInfoUsernameVerifiedBadge.setImageResource(R.drawable.support_badge)
                userInfoUsernameVerifiedBadge.visibility = View.VISIBLE
            }
            "user" -> {
                if ("true" == user.verify) {
                    userInfoUsernameVerifiedBadge.visibility = View.VISIBLE
                } else {
                    userInfoUsernameVerifiedBadge.visibility = View.GONE
                }
            }
        }
    }

    private fun _imageColor(imageView: ImageView, color: Int) {
        imageView.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
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

    private fun _setTime(currentTime: Double, txt: TextView) {
        val c1 = Calendar.getInstance()
        val c2 = Calendar.getInstance()
        val timeDiff = c1.timeInMillis - currentTime
        val seconds = timeDiff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        when {
            seconds < 60 -> txt.text = if (seconds < 2) "1 ${context.getString(R.string.seconds_ago)}" else "$seconds ${context.getString(R.string.seconds_ago)}"
            minutes < 60 -> txt.text = if (minutes < 2) "1 ${context.getString(R.string.minutes_ago)}" else "$minutes ${context.getString(R.string.minutes_ago)}"
            hours < 24 -> txt.text = if (hours < 2) "$hours ${context.getString(R.string.hours_ago)}" else "$hours ${context.getString(R.string.hours_ago)}"
            days < 7 -> txt.text = if (days < 2) "$days ${context.getString(R.string.days_ago)}" else "$days ${context.getString(R.string.days_ago)}"
            else -> {
                c2.timeInMillis = currentTime.toLong()
                txt.text = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(c2.time)
            }
        }
    }

    private fun _setCount(txt: TextView, number: Double) {
        val decimalFormat = DecimalFormat("0.0")
        val (formattedNumber, numberFormat) = when {
            number < 1_000_000 -> Pair(number / 1_000, "K")
            number < 1_000_000_000 -> Pair(number / 1_000_000, "M")
            number < 1_000_000_000_000L -> Pair(number / 1_000_000_000, "B")
            else -> Pair(number / 1_000_000_000_000L, "T")
        }
        txt.text = if (number < 10_000) number.toLong().toString() else "${decimalFormat.format(formattedNumber)}$numberFormat"
    }

    private fun _openWebView(url: String) {
        CustomTabsIntent.Builder().apply {
            setToolbarColor(context.resources.getColor(R.color.md_theme_surface, null))
        }.build().launchUrl(context, Uri.parse(url))
    }
}
