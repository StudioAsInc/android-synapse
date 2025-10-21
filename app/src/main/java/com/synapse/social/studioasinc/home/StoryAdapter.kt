package com.synapse.social.studioasinc.home

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.R

class StoryAdapter(
    private val context: Context,
    private var stories: List<com.synapse.social.studioasinc.model.Story>
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private val userInfoCache = mutableMapOf<String, User>()
    private val authService = SupabaseAuthenticationService()
    private val dbService = SupabaseDatabaseService()

    companion object {
        private const val VIEW_TYPE_MY_STORY = 0
        private const val VIEW_TYPE_OTHER_STORY = 1
    }

    fun updateStories(newStories: List<com.synapse.social.studioasinc.models.Story>) {
        stories = newStories
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_MY_STORY else VIEW_TYPE_OTHER_STORY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.synapse_story_cv, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        holder.bind(story, getItemViewType(position))
    }

    override fun getItemCount(): Int = stories.size

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val storiesMyStory: LinearLayout = itemView.findViewById(R.id.storiesMyStory)
        private val storiesSecondStory: LinearLayout = itemView.findViewById(R.id.storiesSecondStory)
        private val storiesMyStoryProfileCard: CardView = itemView.findViewById(R.id.storiesMyStoryProfileCard)
        private val storiesMyStoryTitle: TextView = itemView.findViewById(R.id.storiesMyStoryTitle)
        private val storiesMyStoryProfileImage: ImageView = itemView.findViewById(R.id.storiesMyStoryProfileImage)
        private val storiesMyStoryRelativeAddBody: LinearLayout = itemView.findViewById(R.id.storiesMyStoryRelativeAddBody)
        private val storiesMyStoryRelativeAdd: ImageView = itemView.findViewById(R.id.storiesMyStoryRelativeAdd)
        private val storiesSecondStoryProfileCard: CardView = itemView.findViewById(R.id.storiesSecondStoryProfileCard)
        private val storiesSecondStoryTitle: TextView = itemView.findViewById(R.id.storiesSecondStoryTitle)
        private val storiesSecondStoryProfileImage: ImageView = itemView.findViewById(R.id.storiesSecondStoryProfileImage)

        fun bind(story: com.synapse.social.studioasinc.models.Story, viewType: Int) {
            setupLayout()

            if (viewType == VIEW_TYPE_MY_STORY) {
                displayMyStory()
            } else {
                displayOtherStory(story)
            }
        }

        private fun setupLayout() {
            val layoutParams = LinearLayout.LayoutParams(
                (context.resources.displayMetrics.density * 80).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    (context.resources.displayMetrics.density * 4).toInt(),
                    (context.resources.displayMetrics.density * 8).toInt(),
                    (context.resources.displayMetrics.density * 4).toInt(),
                    (context.resources.displayMetrics.density * 8).toInt()
                )
            }
            itemView.layoutParams = layoutParams

            storiesMyStoryRelativeAdd.setColorFilter(Color.WHITE)
            storiesMyStory.background = GradientDrawable().apply { cornerRadius = 18f; setColor(Color.WHITE) }
            storiesSecondStory.background = GradientDrawable().apply { cornerRadius = 18f; setColor(Color.WHITE) }
            storiesMyStoryRelativeAddBody.background = GradientDrawable().apply { cornerRadius = 0f; setColor(0x7B000000) }
            storiesMyStoryProfileCard.background = GradientDrawable().apply { cornerRadius = 300f; setColor(Color.TRANSPARENT) }
            storiesSecondStoryProfileCard.background = GradientDrawable().apply { cornerRadius = 300f; setColor(Color.TRANSPARENT) }
        }

        private fun displayMyStory() {
            storiesMyStoryTitle.text = context.getString(R.string.add_story)
            storiesMyStory.visibility = View.VISIBLE
            storiesSecondStory.visibility = View.GONE

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val currentUser = authService.getCurrentUser()
                    currentUser?.let { user ->
                        val userData = dbService.selectSingle<User>("users", "*")
                        userData?.avatar?.takeIf { it != "null" }?.let {
                            Glide.with(context).load(Uri.parse(it)).into(storiesMyStoryProfileImage)
                        } ?: storiesMyStoryProfileImage.setImageResource(R.drawable.avatar)
                    }
                } catch (e: Exception) {
                    storiesMyStoryProfileImage.setImageResource(R.drawable.avatar)
                }
            }
            storiesMyStory.setOnClickListener {
                Toast.makeText(context, "Add story clicked", Toast.LENGTH_SHORT).show()
            }
        }

        private fun displayOtherStory(story: com.synapse.social.studioasinc.models.Story) {
            storiesMyStory.visibility = View.GONE
            storiesSecondStory.visibility = View.VISIBLE

            val storyUid = story.uid
            if (userInfoCache.containsKey(storyUid)) {
                _displayUserInfoForStory(userInfoCache[storyUid]!!, storiesSecondStoryProfileImage, storiesSecondStoryTitle)
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val user = dbService.selectSingle<User>("users", "*")
                        if (user != null) {
                            userInfoCache[storyUid] = user
                            _displayUserInfoForStory(user, storiesSecondStoryProfileImage, storiesSecondStoryTitle)
                        } else {
                            storiesSecondStoryProfileImage.setImageResource(R.drawable.avatar)
                            storiesSecondStoryTitle.text = "Unknown User"
                        }
                    } catch (e: Exception) {
                        storiesSecondStoryProfileImage.setImageResource(R.drawable.avatar)
                        storiesSecondStoryTitle.text = "Error User"
                    }
                }
            }
            storiesSecondStory.setOnClickListener {
                Toast.makeText(context, "Story clicked", Toast.LENGTH_SHORT).show()
            }
        }

        private fun _displayUserInfoForStory(user: User, profileImage: ImageView, titleTextView: TextView) {
            user.avatar?.takeIf { it != "null" }?.let {
                Glide.with(context).load(Uri.parse(it)).into(profileImage)
            } ?: profileImage.setImageResource(R.drawable.avatar)

            titleTextView.text = when {
                user.nickname != null && user.nickname != "null" && user.nickname.isNotEmpty() -> user.nickname
                user.username != null && user.username != "null" && user.username.isNotEmpty() -> "@${user.username}"
                else -> "User Story"
            }
        }
    }
}
