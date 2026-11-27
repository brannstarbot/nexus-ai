package com.nexus.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.ai.di.AppContainer
import com.nexus.ai.ui.theme.NexusPrimary
import com.nexus.ai.ui.theme.ThemeMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(container: AppContainer) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionCard(title = "API Key") {
            Text(
                text = if (state.hasKey) "Tersimpan: ${state.maskedKey}" else "Belum ada key",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.keyInput,
                onValueChange = vm::onKeyInputChanged,
                label = { Text("Masukkan API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = vm::saveKey,
                    colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
                ) { Text("Simpan") }
                OutlinedButton(onClick = vm::clearKey, enabled = state.hasKey) { Text("Hapus") }
            }
        }

        SectionCard(title = "Model & Output") {
            OutlinedTextField(
                value = state.model,
                onValueChange = vm::onModelChanged,
                label = { Text("Nama model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
            Text(
                "Token output maksimum: ${state.maxOutputTokens}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = state.maxOutputTokens.toFloat(),
                onValueChange = { vm.onMaxTokensChanged(it.roundToInt()) },
                valueRange = 64f..8192f,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = NexusPrimary,
                    activeTrackColor = NexusPrimary,
                ),
            )
        }

        SectionCard(title = "Tema") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { vm.onThemeChanged(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "Sistem"
                                    ThemeMode.DARK -> "Gelap"
                                    ThemeMode.LIGHT -> "Terang"
                                }
                            )
                        },
                    )
                }
            }
        }

        SectionCard(title = "Tes Koneksi") {
            Button(
                onClick = vm::testConnection,
                enabled = !state.testing,
                colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
            ) { Text(if (state.testing) "Menguji..." else "Tes API") }
            state.testResult?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = NexusPrimary)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
