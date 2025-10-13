package com.synapse.social.studioasinc.model

import com.google.gson.annotations.SerializedName

data class ImgbbResponse(
    @SerializedName("data")
    val data: Data,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("status")
    val status: Int
)

data class Data(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("url_viewer")
    val urlViewer: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("display_url")
    val displayUrl: String,
    @SerializedName("width")
    val width: String,
    @SerializedName("height")
    val height: String,
    @SerializedName("size")
    val size: String,
    @SerializedName("time")
    val time: String,
    @SerializedName("expiration")
    val expiration: String,
    @SerializedName("image")
    val image: Image,
    @SerializedName("thumb")
    val thumb: Thumb,
    @SerializedName("delete_url")
    val deleteUrl: String
)

data class Image(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("mime")
    val mime: String,
    @SerializedName("extension")
    val extension: String,
    @SerializedName("url")
    val url: String
)

data class Thumb(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("mime")
    val mime: String,
    @SerializedName("extension")
    val extension: String,
    @SerializedName("url")
    val url: String
)
