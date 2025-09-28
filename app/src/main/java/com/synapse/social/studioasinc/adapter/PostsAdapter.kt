package com.synapse.social.studioasinc.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.PostCommentsBottomSheetDialog
import com.synapse.social.studioasinc.PostMoreBottomSheetDialog
import com.synapse.social.studioasinc.ProfileActivity
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.toPost
import com.synapse.social.studioasinc.styling.MarkdownRenderer
import com.synapse.social.studioasinc.util.CountUtils
import com.synapse.social.studioasinc.util.MentionUtils
import com.synapse.social.studioasinc.util.NotificationUtils
import com.synapse.social.studioasinc.util.TimeUtils
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener

class PostsAdapter(
    private val context: Context,
    private val posts: ArrayList<HashMap<String, Any>>,
    private val userInfoCache: HashMap<String, Any>,
    private val authService: IAuthenticationService,
    private val dbService: IDatabaseService,
    private val onMediaClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val currentUserUid = authService.getCurrentUser()?.getUid() ?: ""
    private val mediaPagerAdapters = mutableMapOf<Int, MediaPagerAdapter>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.synapse_post_cv, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val postMap = posts[position]
        val post = postMap.toPost()

        holder.bind(post, postMap)
    }

    override fun getItemCount(): Int = posts.size

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        // Clean up media adapters
        mediaPagerAdapters[holder.bindingAdapterPosition]?.releaseAllPlayers()
        mediaPagerAdapters.remove(holder.bindingAdapterPosition)
    }

    fun releaseAllPlayers() {
        mediaPagerAdapters.values.forEach { it.releaseAllPlayers() }
        mediaPagerAdapters.clear()
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views
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

        // Media views
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val mediaContainer: View = itemView.findViewById(R.id.mediaContainer)
        private val mediaViewPager: ViewPager2 = itemView.findViewById(R.id.mediaViewPager)
        private val pageIndicatorContainer: LinearLayout = itemView.findViewById(R.id.pageIndicatorContainer)
        private val mediaCountBadge: TextView = itemView.findViewById(R.id.mediaCountBadge)

        // Action buttons
        private val likeButton: LinearLayout = itemView.findViewById(R.id.likeButton)
        private val commentsButton: LinearLayout = itemView.findViewById(R.id.commentsButton)
        private val favoritePostButton: ImageView = itemView.findViewById(R.id.favoritePostButton)
        private val likeButtonIc: ImageView = itemView.findViewById(R.id.likeButtonIc)
        private val likeButtonCount: TextView = itemView.findViewById(R.id.likeButtonCount)
        private val commentsButtonCount: TextView = itemView.findViewById(R.id.commentsButtonCount)

        fun bind(post: Post, originalMap: HashMap<String, Any>) {
            setupPostContent(post)
            setupUserInfo(post.uid)
            setupMediaContent(post)
            setupActionButtons(post, originalMap)
            TimeUtils.setTime(post.publishDate.toDoubleOrNull() ?: 0.0, postPublishDate, context)
            updatePostVisibility(post)
        }

        private fun setupPostContent(post: Post) {
            val text: String? = post.postText
            if (!text.isNullOrEmpty()) {
                handleMentions(postMessageTextMiddle, text)
                postMessageTextMiddle.visibility = View.VISIBLE
            } else {
                postMessageTextMiddle.visibility = View.GONE
            }
        }

        private fun handleMentions(textView: TextView, text: String) {
            MentionUtils.handleMentions(context, textView, text)
        }

        private fun setupUserInfo(uid: String) {
            val cacheKey = "uid-$uid"
            if (userInfoCache.containsKey(cacheKey)) {
                displayUserInfoFromCache(uid)
            } else {
                loadUserInfo(uid)
            }
        }

        private fun setupMediaContent(post: Post) {
            post.convertLegacyImage()
            when {
                post.mediaItems.isNotEmpty() -> {
                    postImage.visibility = View.GONE
                    mediaContainer.visibility = View.VISIBLE
                    setupMediaViewPager(post)
                }
                !post.postImage.isNullOrEmpty() -> {
                    mediaContainer.visibility = View.GONE
                    postImage.visibility = View.VISIBLE
                    Glide.with(context).load(post.postImage).into(postImage)
                    postImage.setOnClickListener { onMediaClick?.invoke(post.postImage!!) }
                }
                else -> {
                    postImage.visibility = View.GONE
                    mediaContainer.visibility = View.GONE
                }
            }
        }

        private fun setupMediaViewPager(post: Post) {
            val adapter = MediaPagerAdapter(context, post.mediaItems) { mediaItem, position ->
                onMediaClick?.invoke(mediaItem.url)
            }
            mediaPagerAdapters[bindingAdapterPosition] = adapter
            mediaViewPager.adapter = adapter

            if (post.mediaItems.size > 1) {
                setupPageIndicator(post.mediaItems.size)
                mediaCountBadge.visibility = View.VISIBLE
                mediaCountBadge.text = "1/${post.mediaItems.size}"
                mediaViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        updatePageIndicator(position)
                        mediaCountBadge.text = "${position + 1}/${post.mediaItems.size}"
                    }
                })
            } else {
                pageIndicatorContainer.visibility = View.GONE
                mediaCountBadge.visibility = View.GONE
            }
        }

        private fun setupPageIndicator(count: Int) {
            pageIndicatorContainer.removeAllViews()
            pageIndicatorContainer.visibility = View.VISIBLE
            for (i in 0 until count) {
                val dot = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        context.resources.getDimensionPixelSize(R.dimen.dot_size),
                        context.resources.getDimensionPixelSize(R.dimen.dot_size)
                    ).apply {
                        marginStart = context.resources.getDimensionPixelSize(R.dimen.dot_margin)
                        marginEnd = context.resources.getDimensionPixelSize(R.dimen.dot_margin)
                    }
                    setBackgroundResource(R.drawable.page_indicator_dot)
                    isSelected = i == 0
                }
                pageIndicatorContainer.addView(dot)
            }
        }

        private fun updatePageIndicator(position: Int) {
            for (i in 0 until pageIndicatorContainer.childCount) {
                pageIndicatorContainer.getChildAt(i).isSelected = i == position
            }
        }

        private fun setupActionButtons(post: Post, originalMap: HashMap<String, Any>) {
            likeButtonCount.visibility = if (post.postHideLikeCount == "true") View.GONE else View.VISIBLE
            commentsButtonCount.visibility = if (post.postHideCommentsCount == "true") View.GONE else View.VISIBLE
            commentsButton.visibility = if (post.postDisableComments == "true") View.GONE else View.VISIBLE

            loadLikeStatus(post.key)
            loadCounts(post.key)
            loadFavoriteStatus(post.key)

            likeButton.setOnClickListener { toggleLike(post) }
            commentsButton.setOnClickListener { showComments(post, originalMap) }
            favoritePostButton.setOnClickListener { toggleFavorite(post.key) }
            topMoreButton.setOnClickListener { showMoreOptions(post, originalMap) }
            userInfoProfileImage.setOnClickListener { openProfile(post.uid) }
        }

        private fun updatePostVisibility(post: Post) {
            when (post.postVisibility) {
                "private" -> {
                    if (post.uid == currentUserUid) {
                        postPrivateStateIcon.visibility = View.VISIBLE
                        body.visibility = View.VISIBLE
                    } else {
                        body.visibility = View.GONE
                    }
                }
                else -> {
                    body.visibility = View.VISIBLE
                    postPrivateStateIcon.visibility = View.GONE
                }
            }
        }

        private fun displayUserInfoFromCache(uid: String) {
            val avatarUrl = userInfoCache["avatar-$uid"] as? String
            val nickname = userInfoCache["nickname-$uid"] as? String
            val username = userInfoCache["username-$uid"] as? String
            val gender = userInfoCache["gender-$uid"] as? String
            val accountType = userInfoCache["acc_type-$uid"] as? String
            val verified = userInfoCache["verify-$uid"] as? String
            val banned = userInfoCache["banned-$uid"] as? String

            if (banned == "true") {
                userInfoProfileImage.setImageResource(R.drawable.banned_avatar)
            } else if (!avatarUrl.isNullOrEmpty() && avatarUrl != "null") {
                Glide.with(context).load(avatarUrl).into(userInfoProfileImage)
            } else {
                userInfoProfileImage.setImageResource(R.drawable.avatar)
            }

            userInfoUsername.text = when {
                !nickname.isNullOrEmpty() && nickname != "null" -> nickname
                !username.isNullOrEmpty() -> "@$username"
                else -> "Unknown User"
            }

            userInfoGenderBadge.visibility = when (gender) {
                "male" -> {
                    userInfoGenderBadge.setImageResource(R.drawable.male_badge)
                    View.VISIBLE
                }
                "female" -> {
                    userInfoGenderBadge.setImageResource(R.drawable.female_badge)
                    View.VISIBLE
                }
                else -> View.GONE
            }

            userInfoUsernameVerifiedBadge.visibility = when (accountType) {
                "admin" -> {
                    userInfoUsernameVerifiedBadge.setImageResource(R.drawable.admin_badge)
                    View.VISIBLE
                }
                "moderator" -> {
                    userInfoUsernameVerifiedBadge.setImageResource(R.drawable.moderator_badge)
                    View.VISIBLE
                }
                "support" -> {
                    userInfoUsernameVerifiedBadge.setImageResource(R.drawable.support_badge)
                    View.VISIBLE
                }
                "user" -> if (verified == "true") View.VISIBLE else View.GONE
                else -> View.GONE
            }
        }

        private fun loadUserInfo(uid: String) {
            dbService.getData(dbService.getReference("skyline/users").child(uid), object : IDataListener {
                override fun onDataChange(snapshot: IDataSnapshot) {
                    if (snapshot.exists()) {
                        val userList = snapshot.getValue(List::class.java) as? List<Map<String, Any?>>
                        val userMap = userList?.firstOrNull()
                        if (userMap != null) {
                            userInfoCache["uid-$uid"] = uid
                            userInfoCache["banned-$uid"] = userMap["banned"]?.toString() ?: "false"
                            userInfoCache["nickname-$uid"] = userMap["nickname"]?.toString() ?: ""
                            userInfoCache["username-$uid"] = userMap["username"]?.toString() ?: ""
                            userInfoCache["avatar-$uid"] = userMap["avatar"]?.toString() ?: ""
                            userInfoCache["gender-$uid"] = userMap["gender"]?.toString() ?: "hidden"
                            userInfoCache["verify-$uid"] = userMap["verify"]?.toString() ?: "false"
                            userInfoCache["acc_type-$uid"] = userMap["account_type"]?.toString() ?: "user"
                            displayUserInfoFromCache(uid)
                        }
                    }
                }

                override fun onCancelled(error: IDatabaseError) {
                    userInfoProfileImage.setImageResource(R.drawable.avatar)
                    userInfoUsername.text = "Error User"
                }
            })
        }

        private fun loadLikeStatus(postKey: String) {
            val query = dbService.getReference("skyline/posts-likes").child(postKey).child(currentUserUid)
            dbService.getData(query, object : IDataListener {
                override fun onDataChange(snapshot: IDataSnapshot) {
                    likeButtonIc.setImageResource(if (snapshot.exists()) R.drawable.post_icons_1_2 else R.drawable.post_icons_1_1)
                }
                override fun onCancelled(error: IDatabaseError) {}
            })
        }

        private fun loadCounts(postKey: String) {
            dbService.getData(dbService.getReference("skyline/posts-likes").child(postKey), object : IDataListener {
                override fun onDataChange(snapshot: IDataSnapshot) {
                    val count = if (snapshot.exists()) (snapshot.getValue(List::class.java) as? List<*>)?.size ?: 0 else 0
                    CountUtils.setCount(likeButtonCount, count.toDouble())
                }
                override fun onCancelled(error: IDatabaseError) {}
            })
            dbService.getData(dbService.getReference("skyline/posts-comments").child(postKey), object : IDataListener {
                override fun onDataChange(snapshot: IDataSnapshot) {
                    val count = if (snapshot.exists()) (snapshot.getValue(List::class.java) as? List<*>)?.size ?: 0 else 0
                    CountUtils.setCount(commentsButtonCount, count.toDouble())
                }
                override fun onCancelled(error: IDatabaseError) {}
            })
        }

        private fun loadFavoriteStatus(postKey: String) {
            val query = dbService.getReference("skyline/favorite-posts").child(currentUserUid).child(postKey)
            dbService.getData(query, object : IDataListener {
                override fun onDataChange(snapshot: IDataSnapshot) {
                    favoritePostButton.setImageResource(if (snapshot.exists()) R.drawable.delete_favorite_post_ic else R.drawable.add_favorite_post_ic)
                }
                override fun onCancelled(error: IDatabaseError) {}
            })
        }

        private fun toggleLike(post: Post) {
            val likeRef = dbService.getReference("skyline/posts-likes").child(post.key).child(currentUserUid)
            val emptyListener = object : ICompletionListener<Unit> {
                override fun onComplete(result: Unit?, error: Exception?) { loadCounts(post.key) }
            }
            dbService.getData(likeRef, object : IDataListener {
                override fun onDataChange(snapshot: IDataSnapshot) {
                    if (snapshot.exists()) {
                        dbService.setValue(likeRef, null, emptyListener)
                        likeButtonIc.setImageResource(R.drawable.post_icons_1_1)
                    } else {
                        val likeData = mapOf("post_id" to post.key, "user_id" to currentUserUid)
                        dbService.setValue(likeRef, likeData, emptyListener)
                        likeButtonIc.setImageResource(R.drawable.post_icons_1_2)
                        NotificationUtils.sendPostLikeNotification(post.key, post.uid)
                    }
                }
                override fun onCancelled(error: IDatabaseError) {}
            })
        }

        private fun toggleFavorite(postKey: String) {
            val favoriteRef = dbService.getReference("skyline/favorite-posts").child(currentUserUid).child(postKey)
            val emptyListener = object : ICompletionListener<Unit> {
                override fun onComplete(result: Unit?, error: Exception?) {}
            }
            dbService.getData(favoriteRef, object : IDataListener {
                override fun onDataChange(snapshot: IDataSnapshot) {
                    if (snapshot.exists()) {
                        dbService.setValue(favoriteRef, null, emptyListener)
                        favoritePostButton.setImageResource(R.drawable.add_favorite_post_ic)
                    } else {
                        val favData = mapOf("post_id" to postKey, "user_id" to currentUserUid)
                        dbService.setValue(favoriteRef, favData, emptyListener)
                        favoritePostButton.setImageResource(R.drawable.delete_favorite_post_ic)
                    }
                }
                override fun onCancelled(error: IDatabaseError) {}
            })
        }

        private fun showComments(post: Post, originalMap: HashMap<String, Any>) {
            val bundle = Bundle().apply {
                putString("postKey", post.key)
                putString("postPublisherUID", post.uid)
                putString("postPublisherAvatar", userInfoCache["avatar-${post.uid}"] as? String ?: "")
            }
            val bottomSheet = PostCommentsBottomSheetDialog().apply { arguments = bundle }
            // Note: You'll need to pass FragmentManager from the Fragment/Activity
        }

        private fun showMoreOptions(post: Post, originalMap: HashMap<String, Any>) {
            val bundle = Bundle().apply {
                putString("postKey", post.key)
                putString("postPublisherUID", post.uid)
                putString("postType", post.postType)
                putString("postText", post.postText ?: "")
                if (!post.postImage.isNullOrEmpty()) {
                    putString("postImg", post.postImage)
                }
            }
            val bottomSheet = PostMoreBottomSheetDialog().apply { arguments = bundle }
            // Note: You'll need to pass FragmentManager from the Fragment/Activity
        }

        private fun openProfile(uid: String) {
            val intent = Intent(context, ProfileActivity::class.java).apply {
                putExtra("uid", uid)
            }
            context.startActivity(intent)
        }
    }
}