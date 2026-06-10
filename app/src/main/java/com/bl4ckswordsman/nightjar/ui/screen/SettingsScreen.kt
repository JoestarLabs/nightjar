package com.bl4ckswordsman.nightjar.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import com.bl4ckswordsman.nightjar.BuildConfig
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.service.LockAccessibilityService

data class LanguageOption(val tag: String, val labelRes: Int)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("",   R.string.settings_language_system),
    LanguageOption("en", R.string.settings_language_en),
    LanguageOption("sv", R.string.settings_language_sv),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Read current locale from AppCompatDelegate
    var selectedLocaleTag by remember {
        mutableStateOf(
            AppCompatDelegate.getApplicationLocales()
                .toLanguageTags()
                .takeIf { it.isNotEmpty() } ?: ""
        )
    }

    val accessibilityEnabled = LockAccessibilityService.isEnabled()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            // ── Language ──────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_language))

            LANGUAGE_OPTIONS.forEach { option ->
                ListItem(
                    headlineContent = { Text(stringResource(option.labelRes)) },
                    leadingContent = {
                        if (option.tag.isEmpty()) {
                            Icon(Icons.Rounded.Language, contentDescription = null,
                                modifier = Modifier.size(24.dp))
                        } else {
                            RadioButton(
                                selected = selectedLocaleTag.startsWith(option.tag),
                                onClick = null,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedLocaleTag = option.tag
                            val locales = if (option.tag.isEmpty())
                                LocaleListCompat.getEmptyLocaleList()
                            else
                                LocaleListCompat.forLanguageTags(option.tag)
                            AppCompatDelegate.setApplicationLocales(locales)
                        }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Permissions ───────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_permissions))

            // Accessibility
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_perm_accessibility))
                },
                supportingContent = {
                    Text(
                        text = if (accessibilityEnabled)
                            stringResource(R.string.settings_perm_accessibility_enabled)
                        else
                            stringResource(R.string.settings_perm_accessibility_disabled),
                        color = if (accessibilityEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (accessibilityEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error,
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = if (accessibilityEnabled) Icons.Rounded.CheckCircle
                                      else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = if (accessibilityEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !accessibilityEnabled) {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
            )

            // Notifications
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_perm_notification))
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_perm_notification_desc))
                },
                leadingContent = {
                    Icon(Icons.Rounded.Notifications, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── About ─────────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_about))

            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME))
                },
                leadingContent = {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
