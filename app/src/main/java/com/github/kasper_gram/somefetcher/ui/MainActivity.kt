package com.github.kasper_gram.somefetcher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.github.kasper_gram.somefetcher.R
import com.github.kasper_gram.somefetcher.databinding.ActivityMainBinding
import com.github.kasper_gram.somefetcher.worker.DigestScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.digestFragment, R.id.settingsFragment)
        )
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        // Schedule digest on first launch if not already scheduled
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val times = DigestScheduler.loadTimes(prefs)
        DigestScheduler.scheduleAll(this, times)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}
