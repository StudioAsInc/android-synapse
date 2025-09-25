package com.synapse.social.studioasinc.backend

import com.google.firebase.database.Query
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

class QueryService(private val dbService: IDatabaseService) {

    fun fetch(query: Query, listener: IDataListener) {
        dbService.getData(query, listener)
    }

    fun fetchWithOrder(
        path: String,
        orderBy: String,
        equalTo: String?,
        listener: IDataListener
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
        listener: IDataListener
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
        listener: IDataListener
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
        listener: IDataListener
    ) {
        val query = dbService.getReference("skyline/users").limitToLast(limit)
        fetch(query, listener)
    }
}