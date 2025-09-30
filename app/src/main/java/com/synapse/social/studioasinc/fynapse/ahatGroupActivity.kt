package com.synapse.social.studioasinc.fynapse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseReference
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.SupabaseAuthService
import java.util.HashMap

class ahatGroupActivity : AppCompatActivity() {

    // Initialize services
    private val dbService: IDatabaseService = SupabaseDatabaseService()
    private val authService: IAuthenticationService = SupabaseAuthService()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set content view and other initialization code here
    }
    
    /**
     * Updates a group's information in the database
     */
    private fun updateGroupInfo(groupId: String, name: String, iconUrl: String, creatorUid: String) {
        val groupRef = dbService.getReference("groups").child(groupId)
        
        val groupInfo = HashMap<String, Any>()
        groupInfo["name"] = name
        groupInfo["icon"] = iconUrl
        groupInfo["creator"] = creatorUid
        groupInfo["created_at"] = System.currentTimeMillis()
        
        // Add creator as first member
        val membersMap = HashMap<String, Any>()
        membersMap[creatorUid] = true
        groupInfo["members"] = membersMap
        
        // Use updateChildren to update the group information
        dbService.updateChildren(groupRef, groupInfo, object : ICompletionListener<Unit> {
            override fun onComplete(result: Unit?, error: Exception?) {
                if (error == null) {
                    // Group created successfully
                    addSelectedUsersToGroup(groupId)
                } else {
                    // Handle error
                }
            }
        })
    }
    
    /**
     * Adds selected users to a group
     */
    private fun addSelectedUsersToGroup(groupId: String) {
        val selectedUsers = getSelectedUsers() // Implement this method to get selected users
        if (selectedUsers.isEmpty()) return
        
        val groupMembersRef = dbService.getReference("groups").child(groupId).child("members")
        
        val updates = HashMap<String, Any>()
        for (userId in selectedUsers) {
            updates[userId] = true
        }
        
        // Use updateChildren to add members to the group
        dbService.updateChildren(groupMembersRef, updates, object : ICompletionListener<Unit> {
            override fun onComplete(result: Unit?, error: Exception?) {
                if (error == null) {
                    // Members added successfully
                    updateUserGroups(groupId, selectedUsers)
                } else {
                    // Handle error
                }
            }
        })
    }
    
    /**
     * Updates the groups list for each user
     */
    private fun updateUserGroups(groupId: String, userIds: List<String>) {
        val currentUser = authService.getCurrentUser()?.getUid() ?: return
        
        // Also add current user to the list if not already included
        val allUsers = ArrayList<String>(userIds)
        if (!allUsers.contains(currentUser)) {
            allUsers.add(currentUser)
        }
        
        for (userId in allUsers) {
            val userGroupsRef = dbService.getReference("users").child(userId).child("groups")
            
            val updates = HashMap<String, Any>()
            updates[groupId] = true
            
            // Use updateChildren to add the group to user's groups list
            dbService.updateChildren(userGroupsRef, updates, object : ICompletionListener<Unit> {
                override fun onComplete(result: Unit?, error: Exception?) {
                    // Handle completion or error
                }
            })
        }
    }
    
    /**
     * Helper method to get selected users
     */
    private fun getSelectedUsers(): List<String> {
        // Implement logic to return selected users
        return listOf()
    }
    
    /**
     * Example of a method that was causing the type mismatch error
     * Fixed by ensuring proper type casting and interface usage
     */
    private fun setupServices() {
        // This line had issues at line 140:23 and 140:81
        // Fixed by using proper interface types
        val dbService: IDatabaseService = SupabaseDatabaseService()
        val authService: IAuthenticationService = SupabaseAuthService()
        
        // Use the services
        val currentUser = authService.getCurrentUser()
        val groupsRef = dbService.getReference("groups")
    }
}