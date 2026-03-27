package com.sr.fixit106

import android.app.Application
import com.sr.fixit106.room.DatabaseHolder
import com.sr.fixit106.utils.AppContextProvider

class ApplicationStarter : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextProvider.init(this)
        DatabaseHolder.initDatabase(this)
    }
}