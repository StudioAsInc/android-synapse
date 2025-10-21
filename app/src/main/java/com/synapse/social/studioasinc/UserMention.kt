package com.synapse.social.studioasinc

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.adapter.SearchUserAdapter
import com.synapse.social.studioasinc.models.User
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserMention(
    private val editText: EditText,
    private val sendButton: View? = null
) : TextWatcher, SearchUserAdapter.OnUserClickListener {

    private val context: Context = editText.context
    private val dbService = SupabaseDatabaseService()
    private lateinit var popupWindow: PopupWindow
    private lateinit var searchUserAdapter: SearchUserAdapter
    private val userList = mutableListOf<User>()

    init {
        setupPopupWindow()
    }

    private fun setupPopupWindow() {
        val recyclerView = RecyclerView(context)
        recyclerView.layoutManager = LinearLayoutManager(context)
        searchUserAdapter = SearchUserAdapter(context, userList, this)
        recyclerView.adapter = searchUserAdapter
        recyclerView.setBackgroundResource(R.drawable.rounded_background)

        val height = (200 * context.resources.displayMetrics.density).toInt()
        popupWindow = PopupWindow(
            recyclerView,
            RecyclerView.LayoutParams.MATCH_PARENT,
            height
        )
        popupWindow.isFocusable = false
        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(BitmapDrawable())
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val text = s.toString()
        val cursorPosition = editText.selectionStart

        sendButton?.visibility = if (text.trim().isNotEmpty()) View.VISIBLE else View.GONE

        if (cursorPosition > 0 && text[cursorPosition - 1] == '@') {
            searchUsers("")
            showPopup()
        } else {
            val words = text.substring(0, cursorPosition).split("\\s".toRegex())
            val lastWord = words.lastOrNull { it.isNotEmpty() } ?: ""

            if (lastWord.startsWith("@")) {
                val query = lastWord.substring(1)
                searchUsers(query)
                showPopup()
            } else {
                hidePopup()
            }
        }
    }

    override fun afterTextChanged(s: Editable?) {}

    private fun searchUsers(query: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                userList.clear()
                val users = dbService.select<User>("users", "*")
                // Filter users by username containing query
                val filteredUsers = users.filter { 
                    it.username?.contains(query, ignoreCase = true) == true 
                }.take(10)
                
                userList.addAll(filteredUsers)
                searchUserAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun showPopup() {
        if (!popupWindow.isShowing) {
            popupWindow.showAsDropDown(editText)
        }
    }

    private fun hidePopup() {
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }

    override fun onUserClick(user: User) {
        val username = user.username
        val text = editText.text.toString()
        val cursorPosition = editText.selectionStart

        val textBeforeCursor = text.substring(0, cursorPosition)
        val atIndex = textBeforeCursor.lastIndexOf('@')

        if (atIndex != -1) {
            val newText = text.substring(0, atIndex + 1) + username + " " + text.substring(cursorPosition)
            editText.setText(newText)
            editText.setSelection(atIndex + username.length + 2)
        }

        hidePopup()
    }
}
