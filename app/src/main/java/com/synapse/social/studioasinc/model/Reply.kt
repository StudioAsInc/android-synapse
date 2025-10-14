package com.synapse.social.studioasinc.model

data class Reply(
    var uid: String = "",
    var comment: String = "",
    var push_time: String = "",
    var key: String = "",
    var like: Long = 0,
    var replyCommentkey: String = ""
) {
    companion object {
        fun fromMap(map: Map<String, Any>): Reply {
            return Reply(
                uid = map["uid"] as? String ?: "",
                comment = map["comment"] as? String ?: "",
                push_time = map["push_time"] as? String ?: "",
                key = map["key"] as? String ?: "",
                like = (map["like"] as? Double)?.toLong() ?: 0L,
                replyCommentkey = map["replyCommentkey"] as? String ?: ""
            )
        }
    }
}
