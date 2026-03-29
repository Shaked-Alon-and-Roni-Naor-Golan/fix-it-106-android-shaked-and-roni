package com.sr.fixit

import android.app.Application
import com.sr.fixit.room.DatabaseHolder
import com.sr.fixit.utils.AppContextProvider

class ApplicationStarter : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextProvider.init(this)
        DatabaseHolder.initDatabase(this)
    }
}