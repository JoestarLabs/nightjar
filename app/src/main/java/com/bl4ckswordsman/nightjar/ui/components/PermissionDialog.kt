package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme

/**
 * Two-step permission disclosure dialogs.
 *
 * Step 1: [NotificationPermissionDialog] — request POST_NOTIFICATIONS.
 * Step 2: [AccessibilityPermissionDialog] — explain and redirect to System Settings.
 *
 * Both dialogs follow Material 3 AlertDialog styling and include plain-language
 * disclosures as required by Google Play policy for sensitive permissions.
 */

@Composable
fun NotificationPermissionDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_notification_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_notification_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.dialog_btn_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_btn_not_now))
            }
        }
    )
}

@Composable
fun DeviceAdminPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_admin_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_admin_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.dialog_btn_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_btn_not_now))
            }
        }
    )
}

@Preview
@Composable
private fun NotificationDialogPreview() {
    NightjarTheme {
        NotificationPermissionDialog(onAllow = {}, onDismiss = {})
    }
}

@Preview
@Composable
private fun DeviceAdminDialogPreview() {
    NightjarTheme {
        DeviceAdminPermissionDialog(onOpenSettings = {}, onDismiss = {})
    }
}
