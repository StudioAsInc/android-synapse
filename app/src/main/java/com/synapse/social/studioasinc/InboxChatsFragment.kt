package com.synapse.social.studioasinc

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.model.InboxChatItem
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class InboxChatsFragment : Fragment() {

    private lateinit var inboxListRecyclerView: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var fabNewGroup: FloatingActionButton
    private lateinit var viewModel: InboxChatsViewModel

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inbox_chats, container, false)
        initialize(view)
        initializeLogic()
        return view
    }

    private fun initialize(view: View) {
        inboxListRecyclerView = view.findViewById(R.id.inboxListRecyclerView)
        chipGroup = view.findViewById(R.id.linear9)
        fabNewGroup = view.findViewById(R.id.fab_new_group)
        auth = FirebaseAuth.getInstance()

        viewModel = ViewModelProvider(this).get(InboxChatsViewModel::class.java)

        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            val checkedChip = view.findViewById<Chip>(checkedId)
            val filter = checkedChip.text.toString().lowercase(Locale.getDefault())
            (inboxListRecyclerView.adapter as InboxListRecyclerViewAdapter).filter(filter)
        }
    }

    private fun initializeLogic() {
        inboxListRecyclerView.adapter = InboxListRecyclerViewAdapter(ArrayList())
        inboxListRecyclerView.layoutManager = LinearLayoutManager(context)

        viewModel.inboxChats.observe(viewLifecycleOwner, { chats ->
            (inboxListRecyclerView.adapter as InboxListRecyclerViewAdapter).updateData(chats)
        })

        viewModel.error.observe(viewLifecycleOwner, { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        })

        auth.currentUser?.uid?.let { viewModel.fetchInboxChats(it) }

        fabNewGroup.setOnClickListener {
            val intent = Intent(context, NewGroupActivity::class.java)
            startActivity(intent)
        }
    }

    inner class InboxListRecyclerViewAdapter(private var originalData: List<InboxChatItem>) :
        RecyclerView.Adapter<InboxListRecyclerViewAdapter.ViewHolder>() {

        private var filteredData: List<InboxChatItem> = originalData

        fun updateData(newData: List<InboxChatItem>) {
            originalData = newData
            filter(chipGroup.findViewById<Chip>(chipGroup.checkedChipId).text.toString().lowercase(Locale.getDefault()))
        }

        fun filter(filter: String) {
            filteredData = if (filter == "all") {
                originalData
            } else {
                val chatTypeToFilter = when (filter) {
                    "chats" -> "single"
                    "groups" -> "group"
                    else -> ""
                }
                originalData.filter { it.chatType == chatTypeToFilter }
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = requireActivity().layoutInflater
            val v = inflater.inflate(R.layout.inbox_msg_list_cv_synapse, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(filteredData[position])
        }

        override fun getItemCount(): Int {
            return filteredData.size
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            private val main = itemView.findViewById<LinearLayout>(R.id.main)
            private val profileCardImage = itemView.findViewById<ImageView>(R.id.profileCardImage)
            private val username = itemView.findViewById<TextView>(R.id.username)
            private val lastMessage = itemView.findViewById<TextView>(R.id.last_message)
            private val push = itemView.findViewById<TextView>(R.id.push)
            private val unreadMessagesCountBadge = itemView.findViewById<TextView>(R.id.unread_messages_count_badge)
            private val messageState = itemView.findViewById<ImageView>(R.id.message_state)
            private val genderBadge = itemView.findViewById<ImageView>(R.id.genderBadge)
            private val verifiedBadge = itemView.findViewById<ImageView>(R.id.verifiedBadge)
            private val userStatusCircleBG = itemView.findViewById<LinearLayout>(R.id.userStatusCircleBG)

            fun bind(item: InboxChatItem) {
                lastMessage.text = item.lastMessageText ?: getString(R.string.m_no_chats)
                setTime(item.pushDate, push)

                if (item.lastMessageUid == auth.currentUser!!.uid) {
                    messageState.setImageResource(
                        if (item.lastMessageState == "sended") R.drawable.icon_done_round
                        else R.drawable.icon_done_all_round
                    )
                    messageState.visibility = View.VISIBLE
                    unreadMessagesCountBadge.visibility = View.GONE
                } else {
                    messageState.visibility = View.GONE
                    if (item.unreadCount > 0) {
                        unreadMessagesCountBadge.text = item.unreadCount.toString()
                        unreadMessagesCountBadge.visibility = View.VISIBLE
                    } else {
                        unreadMessagesCountBadge.visibility = View.GONE
                    }
                }

                if (item.chatType == "group") {
                    username.text = item.groups?.name
                    Glide.with(itemView.context).load(Uri.parse(item.groups?.icon)).into(profileCardImage)
                    genderBadge.visibility = View.GONE
                    verifiedBadge.visibility = View.GONE
                    userStatusCircleBG.visibility = View.GONE
                    itemView.setOnClickListener {
                        val intent = Intent(itemView.context, ChatGroupActivity::class.java)
                        intent.putExtra("uid", item.uid)
                        itemView.context.startActivity(intent)
                    }
                } else {
                    username.text = item.users?.nickname ?: "@${item.users?.username}"
                    Glide.with(itemView.context).load(Uri.parse(item.users?.avatar)).into(profileCardImage)
                    userStatusCircleBG.visibility = if (item.users?.status == "online") View.VISIBLE else View.GONE

                    genderBadge.visibility = View.VISIBLE
                    when (item.users?.gender) {
                        "male" -> genderBadge.setImageResource(R.drawable.male_badge)
                        "female" -> genderBadge.setImageResource(R.drawable.female_badge)
                        else -> genderBadge.visibility = View.GONE
                    }

                    verifiedBadge.visibility = View.VISIBLE
                    when {
                        item.users?.accountType == "admin" -> verifiedBadge.setImageResource(R.drawable.admin_badge)
                        item.users?.accountType == "moderator" -> verifiedBadge.setImageResource(R.drawable.moderator_badge)
                        item.users?.accountType == "support" -> verifiedBadge.setImageResource(R.drawable.support_badge)
                        item.users?.isPremium == true -> verifiedBadge.setImageResource(R.drawable.premium_badge)
                        item.users?.isVerified == true -> verifiedBadge.setImageResource(R.drawable.verified_badge)
                        else -> verifiedBadge.visibility = View.GONE
                    }
                    itemView.setOnClickListener {
                        val intent = Intent(itemView.context, ChatActivity::class.java)
                        intent.putExtra("uid", item.uid)
                        itemView.context.startActivity(intent)
                    }
                }
            }
        }
    }

    private fun setTime(currentTime: Double, txt: TextView) {
        val c1 = Calendar.getInstance()
        val c2 = Calendar.getInstance()
        val timeDiff = c1.timeInMillis - currentTime.toLong()
        when {
            timeDiff < 60000 -> {
                val seconds = (timeDiff / 1000).toLong()
                txt.text = if (seconds < 2) "1 ${resources.getString(R.string.seconds_ago)}" else "$seconds ${resources.getString(R.string.seconds_ago)}"
            }
            timeDiff < 60 * 60000 -> {
                val minutes = (timeDiff / 60000).toLong()
                txt.text = if (minutes < 2) "1 ${resources.getString(R.string.minutes_ago)}" else "$minutes ${resources.getString(R.string.minutes_ago)}"
            }
            timeDiff < 24 * 60 * 60000 -> {
                val hours = (timeDiff / (60 * 60000)).toLong()
                txt.text = "$hours ${resources.getString(R.string.hours_ago)}"
            }
            timeDiff < 7 * 24 * 60 * 60000 -> {
                val days = (timeDiff / (24 * 60 * 60000)).toLong()
                txt.text = "$days ${resources.getString(R.string.days_ago)}"
            }
            else -> {
                c2.timeInMillis = currentTime.toLong()
                txt.text = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(c2.time)
            }
        }
    }
}
