package com.ionut.quizapp.data

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val is_guest: Boolean = false
)