package com.gymtrack.presentation.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import com.gymtrack.R
import com.gymtrack.domain.exercise.CatalogDemoOption
import com.gymtrack.domain.exercise.ExerciseMediaConstants
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.MUSCLE_GROUPS

@Composable
fun CatalogDemoPickerDialog(
    options: List<CatalogDemoOption>,
    onConfirm: (CatalogDemoOption) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<CatalogDemoOption?>(null) }

    val filtered = remember(options, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            options
        } else {
            options.filter { it.displayName.contains(q, ignoreCase = true) }
        }
    }
    val grouped = remember(filtered) {
        val byMuscle = filtered.groupBy { it.muscleGroup }
        val orderedKeys = buildList {
            MUSCLE_GROUPS.forEach { if (it in byMuscle) add(it) }
            byMuscle.keys.filter { it !in MUSCLE_GROUPS }.sorted().forEach { add(it) }
        }
        orderedKeys.map { key -> key to byMuscle.getValue(key) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("catalog_demo_picker"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.select_demonstration),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    placeholder = { Text(stringResource(R.string.search_exercises)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_demo_search"),
                )
                Spacer(Modifier.height(8.dp))
                preview?.let { selected ->
                    val previewExercise = remember(selected) {
                        Exercise(
                            name = selected.displayName,
                            muscleGroup = selected.muscleGroup,
                            equipmentType = "",
                            mediaExternalSource = selected.mediaExternalSource,
                            mediaExternalId = selected.mediaExternalId,
                        )
                    }
                    ExerciseAnimation(
                        exercise = previewExercise,
                        height = 160.dp,
                        modifier = Modifier.testTag("catalog_demo_preview_animation"),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = selected.displayName,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onConfirm(selected) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("catalog_demo_confirm"),
                        ) {
                            Text(stringResource(R.string.confirm_demonstration))
                        }
                        OutlinedButton(
                            onClick = { preview = null },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    grouped.forEach { (muscle, items) ->
                        item(key = "header_$muscle") {
                            Text(
                                text = muscle.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(items, key = { it.mediaExternalId }) { option ->
                            CatalogDemoRow(
                                option = option,
                                selected = preview?.mediaExternalId == option.mediaExternalId,
                                onClick = { preview = option },
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("catalog_demo_dismiss"),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun CatalogDemoRow(
    option: CatalogDemoOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    var thumb by remember(option.mediaExternalId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(option.mediaExternalId) {
        val path = "${ExerciseMediaConstants.ASSETS_EXERCISES_ROOT}/" +
            "${option.mediaExternalId}/${ExerciseMediaConstants.FRAME_0_FILE}"
        thumb = ExerciseAssetImageLoader.loadBitmap(context, path)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("catalog_demo_option_${option.mediaExternalId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            if (thumb != null) {
                Image(
                    bitmap = thumb!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = option.displayName,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
