package com.photosdbrowser.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LinkConfig(
    val id: String,
    val label: String,
    val url: String
)
