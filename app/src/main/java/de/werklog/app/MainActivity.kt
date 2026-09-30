package de.werklog.app

import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val Mint = Color(0xFF64DECB)
internal val Amber = Color(0xFFFFCC80)
internal val Muted = Color(0xFFABC1C7)
private val WerkColors = darkColorScheme(primary = Mint, onPrimary = Color(0xFF00382F), secondary = Amber,
    background = Color(0xFF0C191E), surface = Color(0xFF14262D), surfaceVariant = Color(0xFF20363E), onSurface = Color(0xFFE8F2F3))

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private var handoffUntil = 0L
    private var transferScopes = 0
    private var backgroundLock: kotlinx.coroutines.Job? = null
    private var backgroundDeadline = 0L
    private val screenOff = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) { handoffUntil = 0; backgroundLock?.cancel(); model.lock() }
    }
    private fun beginHandoff() { if (model.data != null) handoffUntil = android.os.SystemClock.elapsedRealtime() + 120_000 }
    private var unlockVisit = 0
    private var autoBiometricAttempt: String? = null
    internal fun requestAutoBiometric() {
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) || model.data != null || !model.exists || model.busy || restoreBytes != null || model.error != null || !biometric.enabled()) return
        val attempt = "${model.session}:$unlockVisit"
        if (autoBiometricAttempt == attempt) return
        autoBiometricAttempt = attempt
        unlockBiometric()
    }
    override fun onResume() { super.onResume(); requestAutoBiometric() }
    private val biometric by lazy { BiometricLock(this) }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (!granted) model.error = "Benachrichtigungen sind deaktiviert. Termine bleiben im lokalen Kalender sichtbar." }
    fun enableReminders() { if (android.os.Build.VERSION.SDK_INT >= 33) notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    fun toggleBiometric() { if (biometric.enabled()) { biometric.disable(); model.error = "Biometrie deaktiviert." } else model.biometricKey()?.let { biometric.authenticate(it, { model.error = "Biometrie aktiviert. Für Sicherungen bleibt dein Passwort erforderlich." }, { model.error = it }) } }
    fun enableBiometric(done: () -> Unit) {
        val key = model.biometricKey() ?: run { done(); return }
        biometric.authenticate(key, { done() }, { model.error = it; done() })
    }
    fun biometricAvailable() = biometric.enabled()
    fun unlockBiometric() = biometric.authenticate(result = { it?.let(model::unlockKey) }, error = { model.error = it })
    private val model: WorkModel by viewModels()
    private var requestedAsset by mutableStateOf<String?>(null)
    private var sourceTarget by mutableStateOf<PhotoTarget?>(null)
    private val gallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = photoTarget; photoTarget = null
        if (uri != null && target != null) lifecycleScope.launch {
            var file: File? = null
            try {
                val copied = withContext(Dispatchers.IO) {
                    val folder = File(cacheDir, "camera").also { it.mkdirs() }
                    val output = File(folder, "import-${newId()}.jpg"); file = output
                    contentResolver.openInputStream(uri)?.use { input -> output.outputStream().use { out ->
                        val buffer = ByteArray(8192); var total = 0L
                        while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= 64L * 1024 * 1024); out.write(buffer, 0, n) }
                    } } ?: error("Kein Zugriff")
                    output
                }
                readyPhoto = copied to target
            } catch (_: Exception) { file?.delete(); model.error = "Bild nicht lesbar oder größer als 64 MB." }
        }
    }
    private var photoFile: File? = null
    private var photoTarget: PhotoTarget? = null
    private var readyPhoto by mutableStateOf<Pair<File, PhotoTarget>?>(null)
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success -> completeCameraCapture(success) }
    internal fun completeCameraCapture(success: Boolean) {
        val file = photoFile; val target = photoTarget; photoFile = null; photoTarget = null; handoffUntil = 0
        if (success && file != null && target != null) readyPhoto = file to target else file?.delete()
    }
    private var pendingBackup: File? = null
    private var importBytes by mutableStateOf<File?>(null)
    private val importAsset = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) readShare(uri) }
    private var restoreBytes by mutableStateOf<File?>(null)
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val file = pendingBackup; pendingBackup = null
        if (file != null) lifecycleScope.launch {
            try { if (uri != null) { withContext(Dispatchers.IO) { contentResolver.openOutputStream(uri, "wt")?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: error("Kein Zugriff") }; getSharedPreferences("backup", MODE_PRIVATE).edit().putLong("lastBackup", System.currentTimeMillis()).apply() } }
            catch (_: Exception) { model.error = "Sicherung konnte nicht geschrieben werden." }
            finally { file.delete() }
        }
    }
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch {
            val file = File(cacheDir, "restore-${newId()}.werklog")
            try { withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)?.use { i -> file.outputStream().use { i.copyBounded(it, MAX_ARCHIVE_BYTES) } } ?: error("Kein Zugriff") }; restoreBytes = file }
            catch (_: Exception) { file.delete(); model.error = "Sicherung nicht lesbar oder größer als 384 MiB." }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        importBytes = savedInstanceState?.getString("importFile")?.let { File(cacheDir, File(it).name).takeIf(File::isFile) }
        handoffUntil = savedInstanceState?.getLong("handoffUntil") ?: 0L
        backgroundDeadline = savedInstanceState?.getLong("backgroundDeadline") ?: 0L
        fun restoredFile(key: String): File? = savedInstanceState?.getString(key)?.let { name ->
            File(File(cacheDir, "camera"), File(name).name).takeIf { it.isFile && System.currentTimeMillis() - it.lastModified() < 15 * 60_000L }
        }
        val kind = savedInstanceState?.getString("photoKind")
        val targetId = savedInstanceState?.getString("photoId")
        if (kind != null && targetId != null) {
            val target = PhotoTarget(kind, targetId)
            val ready = restoredFile("readyPhoto")
            if (ready != null) readyPhoto = ready to target else {
                photoTarget = target; photoFile = restoredFile("photoFile")
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        androidx.core.content.ContextCompat.registerReceiver(this, screenOff, android.content.IntentFilter(Intent.ACTION_SCREEN_OFF), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        enableEdgeToEdge()
        receiveShare(intent)
        OrderAttachmentProvider.cleanup(this)
        File(cacheDir, "camera").listFiles()?.filter { it != photoFile && it != readyPhoto?.first }?.forEach { it.delete() }
        File(cacheDir, "shares").listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
        setContent { CompositionLocalProvider(LocalImageLoader provides { value -> model.image(value) }, LocalTransferScope provides { active -> transferScopes = (transferScopes + if (active) 1 else -1).coerceAtLeast(0) }) { MaterialTheme(colorScheme = WerkColors) {
            Surface(Modifier.fillMaxSize(), color = WerkColors.background) {
                key("werklog") {
                    if (model.data == null) LockScreen(model, restoreBytes, { restore.launch(arrayOf("*/*")) }, { restoreBytes = null })
                    else SecureWorkspaceState(model) { Workspace(model, onExport = {
                        lifecycleScope.launch {
                            val file = File(cacheDir, "backup-${newId()}.werklog")
                            try { model.backup(file); pendingBackup = file; beginHandoff(); export.launch("WerkLog-${java.time.LocalDate.now()}.werklog") }
                            catch (_: Exception) { file.delete(); model.error = "Sicherung konnte nicht erstellt werden." }
                        }
                    }, onShare = ::share, onShareFile = ::shareFile, onImport = { beginHandoff(); importAsset.launch(arrayOf("*/*")) }, onPhoto = ::requestPhoto, onOrder = ::sendOrder, requestedAsset = requestedAsset, assetOpened = { requestedAsset = null })
                    if (model.data != null && readyPhoto != null) {
                        val pending = readyPhoto!!
                        PhotoReview(pending.first, pending.second, model.data!!, model.busy, { pending.first.delete(); readyPhoto = null }, { requestedAsset = it; readyPhoto = null }) { next ->
                            try { model.update(next) } catch (_: Exception) { model.error = "Bild konnte nicht gespeichert werden. Größenbegrenzung prüfen." }
                        }
                    }
                    if (model.data != null && importBytes != null) ImportAssetDialog(importBytes!!, model.data!!, model.busy, { importBytes?.delete(); importBytes = null }) { incoming, target, replace -> model.importShare(incoming, target, replace) }
                }
                }
                sourceTarget?.let { target -> AlertDialog(onDismissRequest = { sourceTarget = null }, title = { Text("Bild hinzufügen") },
                    text = { Text("Das Bild wird verkleinert und verschlüsselt gespeichert. Bei Galerieauswahl bleibt das Original in deiner Galerie unverändert.") },
                    confirmButton = { TextButton(onClick = { sourceTarget = null; takePhoto(target) }) { Text("Kamera") } },
                    dismissButton = { TextButton(onClick = { sourceTarget = null; photoTarget = target; beginHandoff(); gallery.launch("image/*") }) { Text("Bild auswählen") } }) }
                model.error?.let { message -> AlertDialog(onDismissRequest = { model.error = null }, title = { Text("Hinweis") },
                    text = { Text(message) }, confirmButton = { TextButton(onClick = { model.error = null }) { Text("Verstanden") } }) }
            }
        } } }
    }
    override fun onStop() {
        super.onStop(); model.checkpointDraft(); sourceTarget = null
        if (isChangingConfigurations) return
        val now = android.os.SystemClock.elapsedRealtime()
        val deviceLocked = getSystemService(android.app.KeyguardManager::class.java).isDeviceLocked
        if (model.data != null && !deviceLocked && (transferScopes > 0 || handoffUntil > now)) {
            backgroundDeadline = if (transferScopes > 0) now + 120_000 else handoffUntil
            backgroundLock?.cancel()
            backgroundLock = lifecycleScope.launch { kotlinx.coroutines.delay((backgroundDeadline - now).coerceAtLeast(0)); handoffUntil = 0; model.lock() }
        } else { backgroundDeadline = 0; handoffUntil = 0; model.lock() }
    }
    override fun onStart() {
        super.onStart(); unlockVisit++
        if (backgroundDeadline > 0 && android.os.SystemClock.elapsedRealtime() >= backgroundDeadline) model.lock()
        backgroundLock?.cancel(); backgroundDeadline = 0
    }
    override fun onSaveInstanceState(outState: Bundle) {
        model.checkpointDraft()
        importBytes?.let { outState.putString("importFile", it.name) }
        outState.putLong("handoffUntil", handoffUntil)
        outState.putLong("backgroundDeadline", backgroundDeadline)
        val target = readyPhoto?.second ?: photoTarget
        target?.let { outState.putString("photoKind", it.kind); outState.putString("photoId", it.id) }
        photoFile?.let { outState.putString("photoFile", it.name) }
        readyPhoto?.first?.let { outState.putString("readyPhoto", it.name) }
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        unregisterReceiver(screenOff)
        if (isFinishing) { photoFile?.delete(); readyPhoto?.first?.delete() }
        if (!isChangingConfigurations) model.lock()
        super.onDestroy()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); receiveShare(intent)
    }
    private fun receiveShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.data?.let(::readShare)
    }
    private fun readShare(uri: Uri) {
        if (uri.scheme != "content") { model.error = "Bitte eine lokale Freigabedatei über den Dateidialog öffnen."; return }
        lifecycleScope.launch {
            try {
                val file = File(cacheDir, "import-${newId()}.werkshare")
                withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyBounded(it, MAX_ARCHIVE_BYTES) } } ?: error("Kein Zugriff") }
                importBytes = file
            } catch (_: Exception) { model.error = "Anlagenfreigabe nicht lesbar oder größer als 384 MiB." }
        }
    }
    private fun requestPhoto(target: PhotoTarget) { if (target.kind in listOf("meter", "asset")) takePhoto(target) else sourceTarget = target }
    private fun takePhoto(target: PhotoTarget) {
        try {
            val folder = File(cacheDir, "camera").also { it.mkdirs() }
            val file = File(folder, "capture-${newId()}.jpg")
            photoFile = file; photoTarget = target
            beginHandoff()
            camera.launch(FileProvider.getUriForFile(this, "$packageName.files", file))
        } catch (_: Exception) { photoFile?.delete(); photoFile = null; photoTarget = null; model.error = "Keine Kamera verfügbar. Einträge können weiterhin manuell erfasst werden." }
    }
    private fun sendOrder(order: PartsOrder) {
        lifecycleScope.launch { val uris = ArrayList<Uri>(); try {
            order.items.forEachIndexed { index, item -> if (item.image.isNotEmpty()) uris.add(withContext(Dispatchers.IO) { OrderAttachmentProvider.register(applicationContext, "$packageName.order-files", "Position-${index + 1}.jpg", model.image(item.image)) }) }
            val intent = if (uris.isEmpty()) Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")) else Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/jpeg").putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            intent.putExtra(Intent.EXTRA_SUBJECT, "Bestellanfrage · ${order.title}").putExtra(Intent.EXTRA_TEXT, orderText(order))
            if (order.recipient.isNotBlank()) intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(order.recipient))
            if (uris.isNotEmpty()) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val clip = ClipData.newRawUri("Bestellbilder", uris.first()); uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }; intent.clipData = clip
            }
            startActivity(Intent.createChooser(intent, "Bestellanfrage: E-Mail-App auswählen"))
        } catch (_: Exception) { OrderAttachmentProvider.discard(uris); model.error = "Kein passender E-Mail-Entwurf möglich. Bitte eine E-Mail-App installieren oder erneut versuchen." } }
    }
    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val intent = Intent(Intent.ACTION_SEND).setType("application/vnd.werklog.asset")
                .putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_SUBJECT, "WerkLog · verschlüsselte Anlagenfreigabe")
                .putExtra(Intent.EXTRA_TEXT, "Verschlüsselte WerkLog-Freigabe. Den Code übermittle ich separat.")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.clipData = ClipData.newRawUri("WerkLog-Anlagenfreigabe", uri)
            beginHandoff()
            startActivity(Intent.createChooser(intent, "Verschlüsselte Datei teilen"))
        } catch (_: Exception) { model.error = "Datei konnte nicht geteilt werden. Freigabe bitte erneut erstellen." }
    }
    private fun share(subject: String, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                .putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(intent, "E-Mail-Entwurf öffnen"))
        } catch (_: Exception) { model.error = "Keine passende E-Mail-App verfügbar." }
    }
}
private fun java.io.InputStream.readBytesLimited(): ByteArray {
    val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
    while (true) { val n = read(buffer); if (n < 0) break; require(out.size() + n <= Vault.MAX_BYTES); out.write(buffer, 0, n) }
    return out.toByteArray()
}

@Composable private fun LockScreen(model: WorkModel, backup: File?, onRestore: () -> Unit, cancelRestore: () -> Unit) {
    var password by remember { mutableStateOf("") }; var repeat by remember { mutableStateOf("") }
    var confirmed by remember(backup) { mutableStateOf(false) }
    val creating = !model.exists && backup == null
    val lockActivity = androidx.compose.ui.platform.LocalContext.current as MainActivity
    LaunchedEffect(model.session, backup, model.busy, model.error) { if (!creating) lockActivity.requestAutoBiometric() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("W /", color = Mint, fontSize = 58.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp)); Text("WerkLog", fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("Dein Technikalltag. Gut dokumentiert.", color = Muted)
        Spacer(Modifier.height(36.dp))
        Text(if (backup != null) "Sicherung wiederherstellen" else if (creating) "Deinen Tresor einrichten" else "Willkommen zurück", fontSize = 22.sp)
        Text(if (creating) "Ein lokales Passwort schützt deine Arbeitsdaten. Mindestens 10 Zeichen; keine Wiederherstellung möglich."
            else if (backup != null) "Gib das Passwort dieser Sicherung ein. Eine Wiederherstellung ersetzt alle aktuellen Daten."
            else "Deine Daten bleiben verschlüsselt auf diesem Gerät.", color = Muted, modifier = Modifier.padding(vertical = 12.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Passwort") }, visualTransformation = PasswordVisualTransformation(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), enabled = !model.busy)
        if (creating) OutlinedTextField(repeat, { repeat = it }, label = { Text("Passwort wiederholen") }, visualTransformation = PasswordVisualTransformation(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        if (backup != null && model.exists) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(confirmed, { confirmed = it }); Text("Aktuelle Daten durch diese Sicherung ersetzen", modifier = Modifier.weight(1f))
        }
        Button(onClick = { model.unlock(password.toCharArray(), backup); password = ""; repeat = ""; cancelRestore() },
            enabled = !model.busy && password.isNotEmpty() && (!creating || (password.length >= 10 && password == repeat)) && (backup == null || !model.exists || confirmed),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 52.dp)) {
            Text(if (model.busy) "Tresor wird geöffnet …" else if (creating) "Tresor erstellen" else "Entsperren")
        }
        val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
        if (!creating && backup == null && activity.biometricAvailable()) OutlinedButton(onClick = activity::unlockBiometric, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text("Mit Fingerabdruck / Biometrie öffnen") }
        TextButton(onClick = if (backup == null) onRestore else cancelRestore, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text(if (backup == null) "Verschlüsselte Sicherung laden" else "Wiederherstellung abbrechen") }
        Spacer(Modifier.height(24.dp)); Text("OFFLINE  ·  OHNE KONTO  ·  VERSCHLÜSSELT", color = Mint, fontSize = 11.sp)
    }
}

@Composable private fun Workspace(model: WorkModel, onExport: () -> Unit, onShare: (String, String) -> Unit, onShareFile: (File) -> Unit, onImport: () -> Unit, onPhoto: (PhotoTarget) -> Unit, onOrder: (PartsOrder) -> Unit, requestedAsset: String?, assetOpened: () -> Unit) {
    val d = model.data ?: return
    var tool by model.workspaceTool
    var page by model.workspacePage
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAssetId by rememberSaveable { mutableStateOf<String?>(null) }
    var preselectedAssetId by rememberSaveable { mutableStateOf<String?>(null) }
    var editAsset by rememberSaveable { mutableStateOf<Asset?>(null) }
    var editEntry by rememberSaveable { mutableStateOf<Entry?>(null) }
    var editReading by rememberSaveable { mutableStateOf<Reading?>(null) }
    var editRound by rememberSaveable { mutableStateOf<Round?>(null) }
    var removal by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var run by rememberSaveable { mutableStateOf<Round?>(null) }
    var mail by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var linkedRecord by rememberSaveable { mutableStateOf<String?>(null) }
    var nextGuideStep by remember { mutableStateOf<(() -> Unit)?>(null) }
    BackHandler(enabled = page == 4 && tool != null) { tool = null }
    BackHandler(enabled = page == 1 && selectedAssetId != null && dialog == null) { selectedAssetId = null }
    LaunchedEffect(requestedAsset) { requestedAsset?.let { selectedAssetId = it; page = 1; tool = null; assetOpened() } }
    val titles = listOf("Heute", "Anlagen", "Arbeitsprotokoll", "Rundgang", "Betrieb", "Einstellung")
    val context = androidx.compose.ui.platform.LocalContext.current
    val tourPrefs = context.getSharedPreferences("onboarding", 0)
    var tour by rememberSaveable { mutableIntStateOf(if (tourPrefs.getBoolean("done-v1", false)) -1 else 0) }
    BackHandler(enabled = page != 0 && dialog == null && selectedAssetId == null && tool == null) { page = if (page in 1..3) 4 else 0 }
    val activity = context as MainActivity
    var biometricPromptRunning by rememberSaveable { mutableStateOf(false) }
    val tourVisible = tour >= 0 && !model.offerBiometric && !biometricPromptRunning && model.error == null
    val tourSteps = listOf(
        Triple(0, "Heute", "Hier siehst du Termine und offene Arbeiten für deinen Tag."),
        Triple(4, "Betrieb", "Die Kacheln öffnen Anlagen, Zähler und deine weiteren Werkzeuge."),
        Triple(1, "Anlagen", "Öffne eine Anlage für Wissen, Zugänge und Verlauf."),
        Triple(5, "Einstellung", "Hier findest du Profil, Passwort, Fingerabdruck und Sicherungen."))
    fun finishTour() { tour = -1; page = 0; tourPrefs.edit().putBoolean("done-v1", true).apply() }
    LaunchedEffect(tour, tourVisible) { if (tourVisible) { page = tourSteps[tour].first; tool = null; selectedAssetId = null } }
    BackHandler(enabled = tourVisible) { finishTour() }
    CompositionLocalProvider(LocalRecordLink provides { kind, id ->
        dialog = null
        when (kind) {
            "Anlagen" -> { selectedAssetId = id; page = 1; tool = null }
            "Vorgang" -> { editEntry = d.entries.find { it.id == id }; dialog = "entry" }
            else -> { linkedRecord = id; tool = kind; page = 4 }
        }
    }) {
    Scaffold(containerColor = WerkColors.background, topBar = {
        Column {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("WERKLOG  /  LOKAL", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(if (page == 4 && tool != null) tool!! else titles[page], fontSize = 30.sp, fontWeight = FontWeight.Bold) }
                IconButton(onClick = { model.lock() }) { Icon(Icons.Outlined.Lock, "App sperren", tint = Mint) }
            }
            if (page == 4 && tool == "Anleitungen" && nextGuideStep != null) Button(
                onClick = { nextGuideStep?.invoke() }, enabled = !model.busy,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp)) { Text("+ Nächster Schritt") }
        }
    }, bottomBar = {
        Column {
        if (tourVisible) TourStrip(tour, tourSteps[tour].second, tourSteps[tour].third,
            back = { if (tour > 0) tour-- }, skip = { finishTour() },
            next = { if (tour == tourSteps.lastIndex) finishTour() else tour++ })
        NavigationBar(containerColor = WerkColors.surface) {
            NavigationBarItem(selected = page == 0, onClick = { page = 0; tool = null }, icon = { Icon(Icons.Outlined.Today, "Heute") }, label = { Text("Heute") })
            NavigationBarItem(selected = page in 1..4, onClick = { page = 4; tool = null; selectedAssetId = null }, icon = {
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Mint, modifier = Modifier.size(58.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(PowerPlantIcon, "Betrieb", tint = WerkColors.background, modifier = Modifier.size(32.dp)) }
                }
            }, label = { Text("Betrieb", fontWeight = FontWeight.Bold) })
            NavigationBarItem(selected = page == 5, onClick = { page = 5; tool = null }, icon = { Icon(Icons.Outlined.Settings, "Einstellung") }, label = { Text("Einstellung") })
        }
        }
    }) { padding ->
        val scroll = rememberScrollState()
        var lastScrollPage by rememberSaveable { mutableStateOf("$page:$tool:$selectedAssetId") }
        LaunchedEffect(page, tool, selectedAssetId) {
            val current = "$page:$tool:$selectedAssetId"
            if (current != lastScrollPage) { scroll.scrollTo(0); lastScrollPage = current }
        }
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(scroll).padding(horizontal = 20.dp)) {
            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (page) {
                0 -> {
                    Panel {
                        Text(if (d.profile.name.isBlank()) "Alles im Blick." else "Hallo, ${d.profile.name}.", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM", java.util.Locale.GERMAN)), color = Muted)
                        Spacer(Modifier.height(22.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Metric(d.entries.count { it.status != "Erledigt" }.toString(), "Offen")
                            Metric(d.entries.count { it.priority == "Dringend" && it.status != "Erledigt" }.toString(), "Dringend", Amber)
                            Metric(d.assets.size.toString(), "Anlagen")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { editEntry = null; preselectedAssetId = null; dialog = "entry" }, modifier = Modifier.weight(1f), enabled = d.assets.isNotEmpty() && !model.busy) { Text("+ Störung") }
                        OutlinedButton(onClick = { editReading = null; preselectedAssetId = null; dialog = "reading" }, modifier = Modifier.weight(1f), enabled = d.assets.isNotEmpty() && !model.busy) { Text("+ Messwert") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { page = 4; tool = "Zähler" }, modifier = Modifier.weight(1f)) { Text("Zähler ablesen") }
                        OutlinedButton(onClick = { page = 4; tool = "Bestellungen" }, modifier = Modifier.weight(1f)) { Text("Teil anfordern") }
                    }
                    OutlinedButton(onClick = { page = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Anlagenwissen nachschlagen") }
                    if (d.assets.isEmpty()) Empty("Dein Arbeitsplatz, deine Struktur", "Lege zuerst eine Anlage an. Danach kannst du Störungen und Messwerte direkt zuordnen.") { editAsset = null; dialog = "asset" }
                    val due = d.assets.filter { serviceState(it.nextService) in listOf("Überfällig", "Heute fällig", "In den nächsten 30 Tagen") }.sortedBy { parseServiceDate(it.nextService) }
                    if (due.isNotEmpty()) {
                        Section("Wartungen im Blick")
                        due.take(5).forEach { a -> Panel {
                            Text(a.name, fontWeight = FontWeight.Bold); Text("${serviceState(a.nextService)} · ${a.nextService}", color = Amber)
                            TextButton(onClick = { selectedAssetId = a.id; page = 1 }) { Text("Anlagenakte öffnen") }
                        } }
                    }
                    val upcoming = d.work.appointments.flatMap { occurrences(it, java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(7)) }.filter { it.status == "Geplant" && appointmentTime(it.start)?.toLocalDate()?.let { date -> date >= java.time.LocalDate.now() && date <= java.time.LocalDate.now().plusDays(7) } == true }.sortedBy { appointmentTime(it.start) }
                    if (upcoming.isNotEmpty()) { Section("Termine der nächsten 7 Tage"); upcoming.take(3).forEach { e -> Panel {
                        Text(e.title, fontWeight = FontWeight.Bold); Text("${e.start} · ${e.company}", color = Mint)
                        TextButton(onClick = { page = 4; tool = "Kalender" }) { Text("Kalender öffnen") }
                    } } }
                    Section("Für die nächste Übergabe")
                    val open = d.entries.filter { it.status != "Erledigt" }.sortedWith(compareByDescending<Entry> { priorities.indexOf(it.priority) }.thenByDescending { it.updated })
                    if (open.isEmpty()) Hint("Keine offenen Einträge. Neue Störungen erscheinen hier.")
                    open.take(5).forEach { e -> EntryCard(e, d, { editEntry = e; dialog = "entry" }) }
                    OutlinedButton(onClick = { mail = "WerkLog · Schichtübergabe" to handover(d) }, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text("Übergabe ansehen & per E-Mail teilen") }
                }
                1 -> {
                    val selected = d.assets.find { it.id == selectedAssetId }
                    if (selected != null) {
                        AssetDetails(selected, d, model.busy, { selectedAssetId = null }, { editAsset = selected; dialog = "asset" },
                            { next -> model.update(next) },
                            { editEntry = null; preselectedAssetId = selected.id; dialog = "entry" },
                            { editReading = null; preselectedAssetId = selected.id; dialog = "reading" },
                            { e -> editEntry = e; dialog = "entry" }, onShareFile, onPhoto)
                    } else {
                        var query by rememberSaveable { mutableStateOf("") }
                        Field(query, { query = it }, "Anlagen, Kennzeichen, Wissen und Anleitungen suchen")
                        OutlinedButton(onClick = { onPhoto(PhotoTarget("asset", "")) }, enabled = !model.busy) { Text("Anlagen-Code fotografieren") }
                        Button(onClick = { editAsset = null; dialog = "asset" }, enabled = !model.busy) { Text("+ Anlage anlegen") }
                        var tradeFilter by rememberSaveable { mutableStateOf("") }
                        var favorites by rememberSaveable { mutableStateOf(false) }
                        Picker("Gewerk filtern", listOf("" to "Alle Gewerke") + availableTrades(d).map { it to it }, tradeFilter) { tradeFilter = it }
                        Row { Checkbox(favorites, { favorites = it }); Text("Nur Favoriten", Modifier.padding(top = 12.dp)) }
                        val filtered = searchAssets(d, query).filter { (tradeFilter.isBlank() || it.trade == tradeFilter) && (!favorites || it.favorite) }
                        Text("${filtered.size} von ${d.assets.size} Anlagen", color = Muted)
                        if (filtered.isEmpty()) Hint("Keine Anlagen gefunden. Lege deine erste Anlage an oder ändere die Suche.")
                        filtered.forEach { a ->
                            OutlinedButton(onClick = { selectedAssetId = a.id; model.update(d.copy(assets = d.assets.map { if (it.id == a.id) it.copy(lastOpened = System.currentTimeMillis()) else it })) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AssetThumbnail(a.coverImage)
                                    Column(Modifier.weight(1f)) {
                                        Text("${if (a.favorite) "★ " else ""}${a.name}", fontWeight = FontWeight.Bold, maxLines = 2)
                                        Text(listOf(a.tag, a.trade, a.location).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 12.sp, maxLines = 2)
                                        val count = d.entries.count { it.assetId == a.id && it.status != "Erledigt" }
                                        if (count > 0 || a.nextService.isNotBlank()) Text("$count offen" + if (a.nextService.isNotBlank()) " · Wartung ${a.nextService}" else "", color = if (serviceState(a.nextService) == "Überfällig") Amber else Mint, fontSize = 12.sp)
                                    }
                                    Icon(Icons.Outlined.ChevronRight, "Anlagenakte öffnen")
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Hint("Was ist passiert, was wurde gemacht, was ist noch offen? Hier dokumentierst du Störungen und Arbeiten an deinen Anlagen.")
                    var query by rememberSaveable { mutableStateOf("") }; var filter by rememberSaveable { mutableStateOf("Alle") }; var kind by rememberSaveable { mutableStateOf("Störungen") }
                    Choices(listOf("Störungen", "Messwerte"), kind) { kind = it }
                    Field(query, { query = it }, "Arbeitsprotokoll durchsuchen")
                    if (kind == "Störungen") {
                        Choices(listOf("Alle") + statuses, filter) { filter = it }
                        Button(onClick = { editEntry = null; preselectedAssetId = null; dialog = "entry" }, enabled = d.assets.isNotEmpty() && !model.busy) { Text("+ Eintrag") }
                        val list = d.entries.filter { (filter == "Alle" || it.status == filter) && "${it.title} ${it.note} ${assetName(d, it.assetId)}".contains(query, true) }.sortedByDescending { it.updated }
                        if (list.isEmpty()) Hint("Keine passenden Einträge.")
                        list.forEach { e -> EntryCard(e, d, { editEntry = e; dialog = "entry" }) }
                    } else {
                        Button(onClick = { editReading = null; preselectedAssetId = null; dialog = "reading" }, enabled = d.assets.isNotEmpty() && !model.busy) { Text("+ Messwert erfassen") }
                        val list = d.readings.filter { "${it.label} ${assetName(d, it.assetId)}".contains(query, true) }.sortedByDescending { it.created }
                        if (list.isEmpty()) Hint("Noch keine passenden Messwerte. Es werden keine Grenzwerte oder automatischen Sicherheitsbewertungen angenommen.")
                        list.forEach { r -> Panel {
                            Text(assetName(d, r.assetId), color = Mint, fontSize = 12.sp); Text(r.label, fontWeight = FontWeight.Bold)
                            Text("${r.value} ${r.unit}", fontSize = 28.sp); Text(stamp(r.created), color = Muted, fontSize = 12.sp)
                            if (r.note.isNotBlank()) Text(r.note)
                            TextButton(onClick = { editReading = r; dialog = "reading" }, enabled = !model.busy) { Text("Messwert korrigieren") }
                            TextButton(onClick = { removal = "reading" to r.id }, enabled = !model.busy) { Text("Messwert löschen") }
                        } }
                    }
                }
                3 -> {
                    Hint("Eigene Checklisten für wiederkehrende Kontrollen. Ein übersprungener Punkt bleibt ausdrücklich als ungeprüft dokumentiert.")
                    Button(onClick = { editRound = null; dialog = "round" }, enabled = !model.busy) { Text("+ Checkliste erstellen") }
                    d.rounds.forEach { r -> Panel { Text(r.title, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("${r.checks.size} Prüfpunkte", color = Muted)
                        Button(onClick = { run = r }, enabled = !model.busy) { Text("Rundgang starten") }
                        Row { TextButton(onClick = { editRound = r; dialog = "round" }, enabled = !model.busy) { Text("Bearbeiten") }; TextButton(onClick = { removal = "round" to r.id }, enabled = !model.busy) { Text("Löschen") } } } }
                    Section("Abgeschlossene Rundgänge")
                    if (d.runs.isEmpty()) Hint("Noch kein Rundgang dokumentiert.")
                    d.runs.sortedByDescending { it.created }.forEach { r -> Panel { Text(r.title, fontWeight = FontWeight.Bold); Text(stamp(r.created), color = Muted)
                        r.results.forEach { Text(it, modifier = Modifier.padding(top = 6.dp)) }; if (r.note.isNotBlank()) Text(r.note)
                        TextButton(onClick = { removal = "run" to r.id }, enabled = !model.busy) { Text("Protokoll löschen") }
                    } }
                }
                4 -> {
                    if (tool != null) CompositionLocalProvider(LocalGuideStepAction provides { nextGuideStep = it }) { WorkTools(tool!!, d, model.busy, { tool = null }, { model.update(it) }, onPhoto, onOrder, linkedRecord) { linkedRecord = null } }
                    else OperationTiles { label -> when (label) {
                        "Anlagen" -> { page = 1; selectedAssetId = null }
                        "Arbeitsprotokoll" -> page = 2
                        "Rundgang" -> page = 3
                        else -> { linkedRecord = null; tool = label }
                    } }
                }
                5 -> {
                    val lastBackup = context.getSharedPreferences("backup", 0).getLong("lastBackup", 0)
                    if (System.currentTimeMillis() - lastBackup > 7L * 86400000) Hint("Sicherung fällig: Seit mindestens sieben Tagen keine vollständige Sicherung exportiert.")
                    ProfilePanel(d, model.busy, { model.update(it) }, onPhoto)
                    TradeSettings(d, model.busy) { model.update(it) }
                    Panel { Text("Dein Datentresor", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("Lokal verschlüsselt · Ohne Internetberechtigung", color = Mint)
                        Text("${imageValues(d).count { it.isNotEmpty() }} / $MAX_IMAGES Bilder · ${model.storageBytes() / (1024 * 1024)} MiB belegt")
                        Hint("Bilder separat verschlüsselt, bis 512 KiB pro Bild. Textdaten bis 32 MiB. Sicherungen enthalten alle Bilder.")
                        Text("Beim normalen Verlassen wird die App gesperrt. Bei Dateiauswahl und Kollegenaustausch sind App-Wechsel bis zu 2 Minuten möglich. Bildschirm aus oder manuelles Sperren sperrt sofort. Screenshots sind blockiert. Ein verlorenes Passwort lässt sich nicht zurücksetzen.", modifier = Modifier.padding(top = 12.dp)) }
                    val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
                    OutlinedButton(onClick = activity::toggleBiometric, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text("Biometrie aktivieren / deaktivieren") }
                    Button(onClick = onExport, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text("Verschlüsselte Sicherung speichern") }
                    Hint("Wähle einen lokalen Ordner, wenn die Sicherung auf dem Gerät bleiben soll. Der Android-Dateidialog kann auch Cloud-Anbieter anzeigen. Wiederherstellen ist am Sperrbildschirm möglich.")
                    OutlinedButton(onClick = onImport, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text("Anlagenfreigabe eines Kollegen importieren") }
                    Hint("Öffnet eine .werkshare-Datei. Datei auswählen und den separat erhaltenen Code eingeben. Kurze Wechsel zur Code-Nachricht sind bis zu zwei Minuten möglich.")
                    OutlinedButton(onClick = { mail = "WerkLog · Schichtübergabe" to handover(d) }, modifier = Modifier.fillMaxWidth()) { Text("Schichtübergabe vorbereiten") }
                    Section("Für deinen Arbeitsalltag")
                    Hint("WerkLog dokumentiert Beobachtungen und Tätigkeiten. Freigaben, Betriebsanweisungen und eure offiziellen Meldewege bleiben maßgeblich. Keine Anlagensteuerung oder Verbindung zur GLT.")
                    OutlinedButton(onClick = { dialog = "password" }, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text("App-Passwort ändern") }
                    OutlinedButton(onClick = { tour = 0; page = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Kurze App-Führung starten") }
                    Text("WERKLOG 0.5.0 · KOTLIN / ANDROID", color = Muted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (model.offerBiometric) AlertDialog(onDismissRequest = model::dismissBiometricOffer,
        title = { Text("Mit Fingerabdruck entsperren?") },
        text = { Text("Auf diesem Gerät aktivieren? Dein Passwort bleibt für Sicherungen erforderlich. Du kannst das später in Einstellung ändern.") },
        confirmButton = { TextButton(onClick = {
            model.dismissBiometricOffer(); biometricPromptRunning = true
            activity.enableBiometric { biometricPromptRunning = false }
        }, enabled = !model.busy) { Text("Jetzt aktivieren") } },
        dismissButton = { TextButton(onClick = model::dismissBiometricOffer) { Text("Später") } })
    if (dialog == "password") PasswordChangeDialog(model.busy, { dialog = null }) { old, next -> model.changePassword(old, next); dialog = null }
    if (dialog == "asset") AssetEditor(editAsset, d.assets, availableTrades(d), { dialog = null }) { a -> model.update(d.copy(assets = d.assets.filterNot { it.id == a.id } + a)); dialog = null }
    if (dialog == "entry") EntryEditor(editEntry, d, { model.update(it) }, d.assets, d.work.templates, preselectedAssetId, { dialog = null }, { editEntry?.let { removal = "entry" to it.id }; dialog = null }, { e ->
        val order = orderFromEntry(e)
        model.update(d.copy(entries = d.entries.filterNot { it.id == e.id } + e, work = d.work.copy(orders = d.work.orders + order))); linkedRecord = order.id; dialog = null; page = 4; tool = "Bestellungen"
    }, { e ->
        model.update(d.copy(entries = d.entries.filterNot { it.id == e.id } + e, work = d.work.copy(appointments = d.work.appointments + Appointment(title = e.title, start = formatAppointment(java.time.LocalDate.now().plusDays(1).atTime(8, 0)), assetId = e.assetId, note = e.note)))); dialog = null; page = 4; tool = "Kalender"
    }) { e -> model.update(d.copy(entries = d.entries.filterNot { it.id == e.id } + e)); dialog = null }
    if (dialog == "reading") ReadingEditor(editReading, d.assets, preselectedAssetId, { dialog = null }) { r -> model.update(d.copy(readings = d.readings.filterNot { it.id == r.id } + r)); dialog = null }
    if (dialog == "round") RoundEditor(editRound, { dialog = null }) { r -> model.update(d.copy(rounds = d.rounds.filterNot { it.id == r.id } + r)); dialog = null }
    removal?.let { (kind, id) -> ConfirmRemoval("Eintrag löschen?", "Dieser lokale Eintrag wird entfernt. Bereits erstellte Sicherungen und abgeschlossene Rundgänge bleiben unverändert.", model.busy, { removal = null }) {
        model.update(when (kind) {
            "entry" -> removeEntry(d, id)
            "reading" -> d.copy(readings = d.readings.filterNot { it.id == id })
            "round" -> d.copy(rounds = d.rounds.filterNot { it.id == id })
            else -> d.copy(runs = d.runs.filterNot { it.id == id })
        })
    } }
    run?.let { r -> RoundRunner(r, { run = null }) { result -> model.update(d.copy(runs = d.runs + result)); run = null } }
    mail?.let { m -> AlertDialog(onDismissRequest = { mail = null }, title = { Text("E-Mail-Vorschau") }, text = {
        Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState())) {
            Text("Der folgende Text wird unverschlüsselt an deine E-Mail-App übergeben. Empfänger und Versand bestimmst du dort.", color = Amber)
            Text(m.second, modifier = Modifier.padding(top = 16.dp))
        }
    }, confirmButton = { TextButton(onClick = { mail = null; onShare(m.first, m.second) }) { Text("E-Mail-App öffnen") } }, dismissButton = { TextButton(onClick = { mail = null }) { Text("Abbrechen") } }) }
    }
}

@Composable private fun Metric(value: String, label: String, color: Color = Mint) { Column { Text(value, fontSize = 36.sp, color = color, fontWeight = FontWeight.Bold); Text(label, color = Muted, fontSize = 13.sp) } }
@Composable internal fun Panel(content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = WerkColors.surface)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content) } }
@Composable internal fun Section(title: String) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)) }
@Composable internal fun Hint(text: String) { Text(text, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp)) }
@Composable private fun Empty(title: String, body: String, click: () -> Unit) { Panel { Text(title, fontWeight = FontWeight.Bold); Hint(body); TextButton(onClick = click) { Text("Erste Anlage anlegen") } } }
@Composable internal fun EntryCard(e: Entry, d: Data, click: () -> Unit) { Panel {
    Text("${e.priority.uppercase()}  ·  ${e.status}", color = if (e.priority == "Dringend") Amber else Mint, fontSize = 11.sp)
    Text(e.title, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(assetName(d, e.assetId), color = Muted)
    if (e.note.isNotBlank()) Text(e.note, maxLines = 3)
    Text(stamp(e.updated), fontSize = 11.sp, color = Muted); TextButton(onClick = click) { Text("Öffnen & bearbeiten") }
} }
@Composable internal fun Field(value: String, change: (String) -> Unit, label: String, lines: Int = 1, numeric: Boolean = false) {
    OutlinedTextField(value, { if (it.length <= 10000) change(it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), minLines = lines, maxLines = if (lines == 1) 1 else 8,
        singleLine = lines == 1, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text))
}
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable internal fun Choices(options: List<String>, selected: String, change: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { FilterChip(selected == it, { change(it) }, label = { Text(it) }) } }
}
@Composable internal fun Picker(label: String, values: List<Pair<String, String>>, selected: String, change: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${values.find { it.first == selected }?.second ?: "Auswählen"}") }
        DropdownMenu(expanded, { expanded = false }) { values.forEach { v -> DropdownMenuItem(text = { Text(v.second) }, onClick = { change(v.first); expanded = false }) } }
    }
}
@Composable internal fun Form(title: String, valid: Boolean, close: () -> Unit, save: () -> Unit, confirmLabel: String = "Speichern", content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text(title) }, text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), content = content) },
        confirmButton = { TextButton(onClick = save, enabled = valid) { Text(confirmLabel) } }, dismissButton = { TextButton(onClick = close) { Text("Abbrechen") } })
}
@Composable private fun EntryEditor(existing: Entry?, data: Data, saveData: (Data) -> Unit, assets: List<Asset>, templates: List<EntryTemplate>, initialAssetId: String?, close: () -> Unit, delete: () -> Unit, order: (Entry) -> Unit, appointment: (Entry) -> Unit, save: (Entry) -> Unit) {
    var asset by rememberSaveable { mutableStateOf(existing?.assetId ?: initialAssetId ?: assets.firstOrNull()?.id.orEmpty()) }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }; var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var priority by rememberSaveable { mutableStateOf(existing?.priority ?: "Normal") }; var status by rememberSaveable { mutableStateOf(existing?.status ?: "Offen") }
    var minutes by rememberSaveable { mutableStateOf((existing?.minutes ?: 0).toString()) }
    var guideIds by rememberSaveable { mutableStateOf(existing?.guideIds ?: emptyList<String>()) }
    fun current() = Entry(existing?.id ?: newId(), asset, title.trim(), note.trim(), priority, status, existing?.created ?: System.currentTimeMillis(), System.currentTimeMillis(), minutes.toIntOrNull() ?: 0, guideIds)
    Form("Störung / Tätigkeit", title.isNotBlank() && asset.isNotEmpty() && (minutes.toIntOrNull()?.let { it >= 0 } == true), close, {
        save(current())
    }) {
        if (existing == null) Picker("Vorlage", (starterTemplates + templates).map { it.id to it.name }, "") { id -> (starterTemplates + templates).find { it.id == id }?.let { title = it.title; note = it.body } }
        Picker("Anlage", assets.map { it.id to it.name }, asset) { asset = it }; Field(title, { title = it }, "Kurzbeschreibung *")
        Field(note, { note = it }, "Beobachtung, Maßnahmen, nächste Schritte", 4); Text("Priorität"); Choices(priorities, priority) { priority = it }
        Text("Status"); Choices(statuses, status) { status = it }; Field(minutes, { minutes = it }, "Zeitaufwand in Minuten", numeric = true)
        LinkChoices("Passende Anleitungen", data.work.guides.map { it.id to it.title }, guideIds) { guideIds = it }
        data.work.guides.filter { it.id in guideIds || (it.assetId == asset && asset.isNotBlank()) }.forEach { g ->
            val open = LocalRecordLink.current
            TextButton(onClick = { save(current()); open("Anleitungen", g.id) }) { Text("Anleitung: ${g.title}") }
        }
        if (existing != null) {
            ContactLinks(data, "Vorgang", existing.id, false, saveData)
            data.work.orders.filter { it.entryId == existing.id }.forEach { o -> RecordLink("Bestellungen", o.id, "Bestellung: ${o.title}") }
            TextButton(onClick = { order(current()) }) { Text("Teileanforderung aus diesem Vorgang") }
            TextButton(onClick = { appointment(current()) }) { Text("Folgetermin anlegen (morgen 08:00, danach bearbeiten)") }
            TextButton(onClick = delete) { Text("Vorgang löschen", color = MaterialTheme.colorScheme.error) }
        }
        if (existing != null) TextButton(onClick = { save(existing.copy(id = newId(), title = "$title (Kopie)", note = note, status = "Offen", created = System.currentTimeMillis(), updated = System.currentTimeMillis(), minutes = 0)) }) { Text("Als neue Tätigkeit duplizieren") }
    }
}
@Composable private fun ReadingEditor(existing: Reading?, assets: List<Asset>, initialAssetId: String?, close: () -> Unit, save: (Reading) -> Unit) {
    var asset by rememberSaveable { mutableStateOf(existing?.assetId ?: initialAssetId ?: assets.firstOrNull()?.id.orEmpty()) }; var label by rememberSaveable { mutableStateOf(existing?.label ?: "") }
    var value by rememberSaveable { mutableStateOf(existing?.value?.toString() ?: "") }; var unit by rememberSaveable { mutableStateOf(existing?.unit ?: "bar") }; var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    Form("Messwert erfassen", asset.isNotEmpty() && label.isNotBlank() && number(value) != null && unit.isNotBlank(), close, {
        save(if (existing == null) Reading(assetId = asset, label = label.trim(), value = number(value)!!, unit = unit.trim(), note = note.trim()) else existing.copy(value = number(value)!!, note = note.trim()))
    }) { if (existing == null) { Picker("Anlage", assets.map { it.id to it.name }, asset) { asset = it }; Field(label, { label = it }, "Messpunkt *") } else Text("${existing.label} · ${existing.unit} · ursprünglicher Zeitpunkt bleibt erhalten")
        Field(value, { value = it }, "Messwert *", numeric = true); if (existing == null) Field(unit, { unit = it }, "Einheit *"); Field(note, { note = it }, "Beobachtung", 2) }
}
@Composable private fun RoundEditor(existing: Round?, close: () -> Unit, save: (Round) -> Unit) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }; var points by rememberSaveable { mutableStateOf(existing?.checks?.joinToString("\n") ?: "") }
    val checks = points.lines().map { it.trim() }.filter { it.isNotEmpty() }
    Form("Checkliste erstellen", title.isNotBlank() && checks.size in 1..100, close, { save(Round(id = existing?.id ?: newId(), title = title.trim(), checks = checks)) }) {
        Field(title, { title = it }, "Name *"); Field(points, { points = it }, "Ein Prüfpunkt pro Zeile *", 6)
        Hint("Nur eure freigegebenen Kontrollen übernehmen. Maximal 100 Punkte.")
    }
}
@Composable private fun RoundRunner(round: Round, close: () -> Unit, save: (RoundRun) -> Unit) {
    var results by rememberSaveable { mutableStateOf(List(round.checks.size) { "Ungeprüft" }) }; var note by rememberSaveable { mutableStateOf("") }
    Form(round.title, results.any { it != "Ungeprüft" } && (!results.contains("Auffällig") || note.isNotBlank()), close, {
        save(RoundRun(title = round.title, results = round.checks.mapIndexed { i, check -> "${results[i]} · $check" }, note = note.trim()))
    }) { round.checks.forEachIndexed { i, text -> Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Choices(listOf("Ungeprüft", "In Ordnung", "Auffällig"), results[i]) { v -> results = results.toMutableList().also { it[i] = v } }
    }; Field(note, { note = it }, if (results.contains("Auffällig")) "Auffälligkeit / Maßnahme *" else "Notiz", 3)
        Hint("Auffälligkeiten lösen keine automatische Störungsmeldung aus. Falls nötig, zusätzlich im Arbeitsprotokoll erfassen.")
    }
}
