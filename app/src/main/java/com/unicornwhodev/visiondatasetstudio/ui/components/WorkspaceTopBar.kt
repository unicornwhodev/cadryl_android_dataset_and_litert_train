package com.unicornwhodev.visiondatasetstudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.ui.MainViewModel
import com.unicornwhodev.visiondatasetstudio.ui.Screen

fun Screen.workspace(): Screen = when (this) {
    Screen.Setup, Screen.Controls, Screen.SourceSettings -> Screen.Home
    Screen.ModelSettings, Screen.ModelAdvanced, Screen.Training -> Screen.Models
    Screen.TransferSettings -> Screen.Publication
    else -> this
}

/** Related tools share a visible navigation row and stay in the same primary workspace. */
@Composable
fun WorkspaceTopBar(vm: MainViewModel, title: String, eyebrow: String? = null,
                    onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    val screen by vm.currentScreen.collectAsState()
    val busy by vm.isBusy.collectAsState()
    val tabs = when (screen.workspace()) {
        Screen.Home -> listOf(Screen.Home to tr("Atelier", "Studio"), Screen.Setup to tr("Configurer", "Setup"), Screen.Controls to tr("Projets", "Projects"))
        Screen.Models -> listOf(Screen.Models to tr("Bibliothèque", "Library"), Screen.ModelSettings to tr("Réglages", "Settings"),
            Screen.Training to tr("Apprentissage", "Training"))
        Screen.Publication -> listOf(Screen.Publication to tr("Exporter", "Export"), Screen.TransferSettings to tr("Destination et stockage", "Destination & storage"))
        else -> emptyList()
    }
    Column {
        StudioTopBar(title, eyebrow, onBack, actions)
        if (tabs.isNotEmpty()) StudioTabs(tabs.map { it.second }, tabs.indexOfFirst {
            it.first == screen || (screen == Screen.ModelAdvanced && it.first == Screen.ModelSettings) || (screen == Screen.SourceSettings && it.first == Screen.Setup)
        }.coerceAtLeast(0), { if (!busy) vm.navigateTo(tabs[it].first) }, Modifier.padding(horizontal = 12.dp))
    }
}
