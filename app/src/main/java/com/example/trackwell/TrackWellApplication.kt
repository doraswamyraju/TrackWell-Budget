package com.example.trackwell

import android.app.Application
import com.example.trackwell.data.AppDatabase
import com.example.trackwell.data.TrackWellRepository

class TrackWellApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TrackWellRepository(database) }

    override fun onCreate() {
        super.onCreate()
    }
}
