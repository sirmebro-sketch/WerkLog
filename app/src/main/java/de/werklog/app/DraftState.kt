package de.werklog.app

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import java.io.Serializable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** UI state is serialized only into the vault-encrypted private checkpoint, never Android saved state. */
internal object DraftState {
    fun canSave(value: Any): Boolean = when (value) {
        is androidx.compose.runtime.snapshots.SnapshotMutableState<*> -> value.value?.let(::canSave) ?: true
        else -> value is Serializable || value is Parcelable || value is CharSequence
    }
    private fun pack(value: Any?): Bundle = Bundle().apply {
        when (value) {
            is MutableIntState -> { putInt("primitive", 1); putInt("value", value.intValue) }
            is MutableLongState -> { putInt("primitive", 2); putLong("value", value.longValue) }
            is MutableFloatState -> { putInt("primitive", 3); putFloat("value", value.floatValue) }
            is MutableDoubleState -> { putInt("primitive", 4); putDouble("value", value.doubleValue) }
            is androidx.compose.runtime.snapshots.SnapshotMutableState<*> -> {
                putInt("state", when (value.policy) { referentialEqualityPolicy<Any?>() -> 2; neverEqualPolicy<Any?>() -> 3; else -> 1 })
                putBundle("nested", pack(value.value))
            }
            null -> putString("value", null)
            is Parcelable -> putParcelable("value", value)
            is Serializable -> putSerializable("value", value)
            is CharSequence -> putCharSequence("value", value)
            else -> error("Entwurfstyp nicht unterstützt: ${value.javaClass.name}")
        }
    }
    @Suppress("DEPRECATION")
    private fun unpack(bundle: Bundle): Any? {
        bundle.classLoader = DraftState::class.java.classLoader
        when (bundle.getInt("primitive")) {
            1 -> return mutableIntStateOf(bundle.getInt("value"))
            2 -> return mutableLongStateOf(bundle.getLong("value"))
            3 -> return mutableFloatStateOf(bundle.getFloat("value"))
            4 -> return mutableDoubleStateOf(bundle.getDouble("value"))
        }
        val state = bundle.getInt("state")
        return if (state == 0) bundle.get("value") else mutableStateOf(unpack(requireNotNull(bundle.getBundle("nested"))),
            when (state) { 2 -> referentialEqualityPolicy(); 3 -> neverEqualPolicy(); else -> structuralEqualityPolicy() })
    }
    fun encode(state: Map<String, List<Any?>>, page: Int, tool: String?): ByteArray {
        val bundle = Bundle().apply { putInt("format", 1); putInt("page", page); putString("tool", tool) }
        val values = Bundle()
        state.forEach { (key, list) ->
            values.putParcelableArrayList(key, ArrayList(list.map(::pack)))
        }
        bundle.putBundle("values", values)
        val parcel = Parcel.obtain()
        return try { parcel.writeBundle(bundle); parcel.marshall() } finally { parcel.recycle() }
    }
    @Suppress("DEPRECATION")
    fun decode(bytes: ByteArray): Triple<Int, String?, Map<String, List<Any?>>> {
        val parcel = Parcel.obtain()
        return try {
            parcel.unmarshall(bytes, 0, bytes.size); parcel.setDataPosition(0)
            val bundle = requireNotNull(parcel.readBundle(DraftState::class.java.classLoader))
            require(bundle.getInt("format") == 1)
            val values = requireNotNull(bundle.getBundle("values"))
            values.classLoader = DraftState::class.java.classLoader
            val state = values.keySet().associateWith { key ->
                requireNotNull(values.getParcelableArrayList<Bundle>(key)).map {
                    unpack(it)
                }
            }
            Triple(bundle.getInt("page").coerceIn(0, 5), bundle.getString("tool"), state)
        } finally { parcel.recycle() }
    }
}

@Composable internal fun SecureWorkspaceState(model: WorkModel, content: @Composable () -> Unit) {
    val registry = remember { SaveableStateRegistry(model.restoreDraftState(), DraftState::canSave) }
    DisposableEffect(registry) {
        model.draftRegistry = registry
        onDispose { if (model.draftRegistry === registry) model.draftRegistry = null }
    }
    LaunchedEffect(registry) {
        while (isActive) { delay(500); model.checkpointDraft() }
    }
    CompositionLocalProvider(LocalSaveableStateRegistry provides registry, content = content)
}
