package com.nexus.ai.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

/** Empat tujuan utama di bottom navigation. */
enum class NexusDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Chat("chat", "Chat", Icons.Outlined.Forum),
    Chess("chess", "Chess", Icons.Outlined.SmartToy),
    History("history", "History", Icons.Outlined.History),
    Settings("settings", "Settings", Icons.Outlined.Settings),
}
