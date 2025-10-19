package com.synapse.social.studioasinc.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Attachment(
    val url: String = "",
    val publicId: String = "",
    val width: Double = 0.0,
    val height: Double = 0.0,
    val localPath: String = "",
    var uploadState: String = "pending",
    var uploadProgress: Double = 0.0
) : Parcelable
