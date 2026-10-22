package com.gymtrack.presentation.workouts.execution

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing

/**
 * Single outlined field with integrated − / + controls inside the border.
 * Value stays editable via keyboard; steppers share the same string state.
 */
@Composable
fun AdjustableOutlinedNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    label: String,
    decreaseContentDescription: String,
    increaseContentDescription: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: (@Composable (() -> Unit))? = null,
    fieldTestTag: String? = null,
    decreaseTestTag: String? = null,
    increaseTestTag: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        textStyle = TextStyle(textAlign = TextAlign.Center),
        shape = RoundedCornerShape(GymShapeTokens.Medium),
        leadingIcon = {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(GymSpacing.TouchTarget)
                    .then(
                        if (decreaseTestTag != null) Modifier.testTag(decreaseTestTag) else Modifier,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = decreaseContentDescription,
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(GymSpacing.TouchTarget)
                    .then(
                        if (increaseTestTag != null) Modifier.testTag(increaseTestTag) else Modifier,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = increaseContentDescription,
                )
            }
        },
        modifier = modifier.then(
            if (fieldTestTag != null) Modifier.testTag(fieldTestTag) else Modifier,
        ),
    )
}
