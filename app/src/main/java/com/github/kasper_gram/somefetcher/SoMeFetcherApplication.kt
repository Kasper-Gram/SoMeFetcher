package com.github.kasper_gram.somefetcher

import android.app.Application
import com.github.kasper_gram.somefetcher.data.AppDatabase
import com.github.kasper_gram.somefetcher.repository.DigestRepository

class SoMeFetcherApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { DigestRepository(database.feedItemDao(), database.feedSourceDao()) }
}
