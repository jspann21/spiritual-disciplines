package com.spiritualdisciplines

import android.app.Application
import com.spiritualdisciplines.data.AppDatabase
import com.spiritualdisciplines.data.AppPreferences
import com.spiritualdisciplines.data.AppRepository

class MainApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: AppRepository
    lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = AppRepository(database.appDao())
        appPreferences = AppPreferences(this)
    }
}

