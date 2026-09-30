package de.werklog.app

import androidx.compose.runtime.staticCompositionLocalOf

/** Editors close only after a durable write. Failed writes leave the form intact. */
class DataSaver(private val write: (Data, () -> Unit) -> Unit) {
    operator fun invoke(data: Data, onSaved: () -> Unit = {}) = write(data, onSaved)
}
val LocalSaving = staticCompositionLocalOf { false }
