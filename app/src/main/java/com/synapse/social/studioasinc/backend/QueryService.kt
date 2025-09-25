package com.synapse.social.studioasinc.backend

import com.google.firebase.database.Query
import com.synapse.social.studioasinc.backend.DatabaseService

class QueryService(private val dbService: DatabaseService) {

    fun fetch(query: Query, listener: DatabaseService.DataListener) {
        dbService.getData(query, listener)
    }

    fun fetchWithOrder(
        path: String,
        orderBy: String,
        equalTo: String?,
        listener: DatabaseService.DataListener
    ) {
        var query: Query = dbService.getReference(path).orderByChild(orderBy)
        if (equalTo != null) {
            query = query.equalTo(equalTo)
        }
        fetch(query, listener)
    }

    fun fetchWithLimit(
        path: String,
        orderBy: String,
        limit: Int,
        equalTo: String? = null,
        listener: DatabaseService.DataListener
    ) {
        var query: Query = dbService.getReference(path).orderByChild(orderBy)
        if (equalTo != null) {
            query = query.equalTo(equalTo)
        }
        query = query.limitToLast(limit)
        fetch(query, listener)
    }

    fun fetchUsersStartingWith(
        username: String,
        limit: Int,
        listener: DatabaseService.DataListener
    ) {
        val query = dbService.getReference("skyline/users")
            .orderByChild("username")
            .startAt(username)
            .endAt(username + "\uf8ff")
            .limitToFirst(limit)
        fetch(query, listener)
    }

    fun fetchAllUsers(
        limit: Int,
        listener: DatabaseService.DataListener
    ) {
        val query = dbService.getReference("skyline/users").limitToLast(limit)
        fetch(query, listener)
    }
}