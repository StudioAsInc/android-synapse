package com.synapse.social.studioasinc.backend

interface IDatabaseReference {
    fun child(s: String): IDatabaseReference
    fun push(): IDatabaseReference
    fun getKey(): String?
}