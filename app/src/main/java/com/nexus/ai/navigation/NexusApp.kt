package com.nexus.ai.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexus.ai.di.AppContainer
import com.nexus.ai.ui.chat.ChatScreen
import com.nexus.ai.ui.chess.ChessScreen
import com.nexus.ai.ui.settings.SettingsScreen
import com.nexus.ai.ui.theme.NexusAccent
import com.nexus.ai.ui.theme.NexusPrimary

@Composable
fun NexusApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NexusDestination.entries.forEach { dest ->
                    val selected = current?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NexusPrimary,
                            selectedTextColor = NexusAccent,
                            indicatorColor = Color(0x2EE53945),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NexusDestination.Chat.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) },
            exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(160)) },
        ) {
            composable(NexusDestination.Chat.route) { ChatScreen(container) }
            // Chess dan History belum diimplementasikan.
            composable(NexusDestination.Chess.route) { ChessScreen(container) }
            composable(NexusDestination.History.route) { Box(Modifier.fillMaxSize()) }
            composable(NexusDestination.Settings.route) { SettingsScreen(container) }
        }
    }
}
