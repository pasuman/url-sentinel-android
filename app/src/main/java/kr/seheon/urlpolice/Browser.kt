package kr.seheon.urlpolice

import android.graphics.drawable.Drawable

data class Browser(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null
)
