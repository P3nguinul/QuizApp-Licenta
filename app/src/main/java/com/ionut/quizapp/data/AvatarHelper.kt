package com.ionut.quizapp.data

import androidx.annotation.DrawableRes
import com.ionut.quizapp.R

enum class UserAvatar(val id: Int, @DrawableRes val drawableRes: Int) {
    AVATAR_1(1, R.drawable.avatar_1),
    AVATAR_2(2, R.drawable.avatar_2),
    AVATAR_3(3, R.drawable.avatar_3),
    AVATAR_4(4, R.drawable.avatar_4),
    AVATAR_5(5, R.drawable.avatar_5),
    AVATAR_6(6, R.drawable.avatar_6),
    AVATAR_7(7, R.drawable.avatar_7),
    AVATAR_8(8, R.drawable.avatar_8),
    AVATAR_9(9, R.drawable.avatar_9),
    AVATAR_10(10, R.drawable.avatar_10),
    AVATAR_11(11, R.drawable.avatar_11),
    AVATAR_12(12, R.drawable.avatar_12),
    AVATAR_13(13, R.drawable.avatar_13),
    AVATAR_14(14, R.drawable.avatar_14),
    AVATAR_15(15, R.drawable.avatar_15),
    AVATAR_16(16, R.drawable.avatar_16);

    companion object {
        // Returnează avatarul pe baza ID-ului, fallback la primul dacă nu e găsit
        fun fromId(id: Int): UserAvatar {
            return entries.find { it.id == id } ?: AVATAR_1
        }
    }
}