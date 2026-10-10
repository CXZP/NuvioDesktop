package com.nuvio.app.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nuvio.app.isDesktop

/*
 * Desktop: every text field looks like the search and addon URL fields (NuvioInputField): a glass
 * capsule (a rounded box when it holds several lines) with its label as the placeholder, instead of
 * Material's outlined box with a label cut into its border.
 */

/** The field shape: a capsule for one line, a rounded box for several. */
@Composable
fun nuvioFieldShape(singleLine: Boolean = true, fallback: Shape = MaterialTheme.shapes.extraSmall): Shape = when {
    !isDesktop -> fallback
    singleLine -> RoundedCornerShape(percent = 50)
    else -> RoundedCornerShape(22.dp)
}

/** The field colours on desktop; [fallback] elsewhere. */
@Composable
fun nuvioFieldColors(fallback: TextFieldColors = OutlinedTextFieldDefaults.colors()): TextFieldColors {
    if (!isDesktop) return fallback
    val tokens = MaterialTheme.nuvio
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.White.copy(alpha = 0.35f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
        disabledBorderColor = Color.White.copy(alpha = 0.06f),
        errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
        focusedContainerColor = Color(0xFF1C1C1E).copy(alpha = 0.82f),
        unfocusedContainerColor = Color(0xFF1C1C1E).copy(alpha = 0.72f),
        disabledContainerColor = Color(0xFF1C1C1E).copy(alpha = 0.5f),
        errorContainerColor = Color(0xFF1C1C1E).copy(alpha = 0.72f),
        focusedTextColor = tokens.colors.textPrimary,
        unfocusedTextColor = tokens.colors.textPrimary,
        focusedPlaceholderColor = tokens.colors.textMuted,
        unfocusedPlaceholderColor = tokens.colors.textMuted,
        cursorColor = tokens.colors.accent,
    )
}

/** Desktop shows the label as the placeholder; other platforms keep Material's floating label. */
fun nuvioFieldLabel(text: String): (@Composable () -> Unit)? = if (isDesktop) null else ({ Text(text) })

/** The placeholder: the label's text on desktop, else [placeholder] (if any). */
fun nuvioFieldPlaceholder(label: String, placeholder: String? = null): (@Composable () -> Unit)? = when {
    isDesktop -> ({ Text(placeholder ?: label) })
    placeholder != null -> ({ Text(placeholder) })
    else -> null
}
