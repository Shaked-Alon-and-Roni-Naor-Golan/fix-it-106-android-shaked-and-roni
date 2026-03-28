package com.sr.fixit106.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sr.fixit106.R

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        bottomNav = findViewById(R.id.bottomNavigationView)

        bottomNav.setOnItemSelectedListener { item ->
            navigateToTabRoot(item.itemId)
        }

        bottomNav.setOnItemReselectedListener { item ->
            navigateToTabRoot(item.itemId)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            syncBottomBarSelection(destination)
        }
    }

    private fun navigateToTabRoot(itemId: Int): Boolean {
        if (isAlreadyAtTabRoot(itemId)) {
            return true
        }

        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(false)
            .setPopUpTo(
                navController.graph.findStartDestination().id,
                false,
                false
            )
            .build()

        return try {
            navController.navigate(itemId, null, options)
            true
        } catch (e: IllegalArgumentException) {
            NavigationUI.onNavDestinationSelected(bottomNav.menu.findItem(itemId), navController)
        }
    }

    private fun isAlreadyAtTabRoot(targetId: Int): Boolean {
        val current = navController.currentDestination ?: return false
        return when (targetId) {
            R.id.homeFragment -> current.id == R.id.homeFragment
            R.id.report_graph -> current.id == R.id.createPostFragment
            R.id.postsListFragment -> current.id == R.id.postsListFragment
            R.id.mapFragment -> current.id == R.id.mapFragment
            R.id.profile_graph -> current.id == R.id.profilePageFragment
            else -> isInDestinationChain(current, targetId)
        }
    }

    private fun syncBottomBarSelection(destination: NavDestination) {
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (isInDestinationChain(destination, item.itemId)) {
                item.isChecked = true
                return
            }
        }
    }

    private fun isInDestinationChain(destination: NavDestination, targetId: Int): Boolean {
        var current: NavDestination? = destination
        while (current != null) {
            if (current.id == targetId) return true
            current = current.parent as? NavGraph
        }
        return false
    }
}