package dev.milkei.audiofocusmixer.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import dev.milkei.audiofocusmixer.R

/**
 * Uses Android's per-app locale system; AppCompat implements the same behavior
 * down to Android 8. Switching locales can recreate the Activity.
 */
@Composable
internal fun LanguagePickerDialog(onDismiss: () -> Unit) {
    val current = AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column {
                val options = listOf(
                    "" to R.string.language_system,
                    "ko" to R.string.language_korean,
                    "en" to R.string.language_english,
                )
                options.forEach { (language, nameRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(
                                selected = current == language,
                                role = Role.RadioButton,
                                onClick = {
                                    onDismiss()
                                    if (current != language) {
                                        AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags(language)
                                        )
                                    }
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = current == language,
                            onClick = null,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(nameRes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
