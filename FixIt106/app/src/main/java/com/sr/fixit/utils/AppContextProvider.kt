package com.sr.fixit.utils

import android.content.Context

object AppContextProvider {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}