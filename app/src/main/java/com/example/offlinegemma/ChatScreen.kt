package com.example.offlinegemma

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// =====================================================================
//  ROOT: decides which big screen to show
// =====================================================================
@Composable
fun ChatApp(vm: ChatViewModel) {
    val state by vm.modelState.collectAsState()
    GlassBackground {
        when (val s = state) {
            is ModelState.Ready -> MainShell(vm)
            is ModelState.Working -> SplashScreen(s)
            else -> SetupScreen(s, vm)
        }
    }
}

@Composable
private fun transparentFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = Accent
)

// =====================================================================
//  1. SPLASH (while the model loads / copies / downloads)
// =====================================================================
@Composable
private fun SplashScreen(s: ModelState.Working) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        GemLogo(96.dp)
        Spacer(Modifier.height(20.dp))
        Row {
            Text("Offline ", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("Gemma", style = TextStyle(brush = AccentBrush, fontSize = 34.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(6.dp))
        Text("Your Private AI Assistant", color = Color(0xFF8FA8FF), fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Run powerful AI locally - no internet needed",
            color = TextLo, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(56.dp))
        Text(s.label, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        val p = s.progress
        if (p == null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.7f).clip(CircleShape),
                color = Accent, trackColor = Color.White.copy(alpha = 0.1f)
            )
        } else {
            LinearProgressIndicator(
                progress = { p },
                modifier = Modifier.fillMaxWidth(0.7f).clip(CircleShape),
                color = Accent, trackColor = Color.White.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(6.dp))
            Text("${(p * 100).toInt()}%", color = TextLo, fontSize = 13.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text("Fast  ·  Private  ·  Offline", color = TextLo, fontSize = 12.sp)
    }
}

// =====================================================================
//  SETUP (no model yet, or an error)
// =====================================================================
@Composable
private fun SetupScreen(state: ModelState, vm: ChatViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(uri)
    }
    var token by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        GemLogo(72.dp)
        Text("Set up the model", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Gemma 3 1B (.task file, about 550 MB). You only do this once.",
            color = TextLo, textAlign = TextAlign.Center
        )
        if (state is ModelState.Failed) {
            Column(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(state.message, color = Bad)
                GlassButton("Retry loading", Modifier.fillMaxWidth()) { vm.loadModel() }
            }
        }
        GradientButton("Pick model file from storage", Modifier.fillMaxWidth()) {
            picker.launch(arrayOf("*/*"))
        }
        Text("or download inside the app (needs a free Hugging Face read token):", color = TextLo, fontSize = 13.sp)
        Box(Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp))) {
            TextField(
                value = token,
                onValueChange = { token = it },
                placeholder = { Text("hf_...", color = TextLo) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = transparentFieldColors()
            )
        }
        GlassButton("Download model", Modifier.fillMaxWidth()) { vm.downloadModel(token) }
    }
}

// =====================================================================
//  MAIN SHELL: side drawer + the 4 screens
// =====================================================================
@Composable
private fun MainShell(vm: ChatViewModel) {
    val screen by vm.screen.collectAsState()
    val settings by vm.settings.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { drawerState.close() } }
    val openMenu: () -> Unit = { scope.launch { drawerState.open() } }

    BackHandler(enabled = drawerState.isOpen || screen != Screen.HOME) {
        if (drawerState.isOpen) close() else vm.navigate(Screen.HOME)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xF20B1030)) {
                DrawerContent(vm, settings.userName, close)
            }
        }
    ) {
        when (screen) {
            Screen.HOME -> HomeScreen(vm, openMenu)
            Screen.CHAT -> ChatScreen(vm)
            Screen.MODELS -> ModelsScreen(vm)
            Screen.SETTINGS -> SettingsScreen(vm)
        }
    }
}

@Composable
private fun DrawerContent(vm: ChatViewModel, name: String, close: () -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(AccentBrush),
                contentAlignment = Alignment.Center
            ) { Text(name.take(1).uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text("Offline Gemma", color = TextLo, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        DrawerItem("✎", "New Chat") { vm.newChat(); vm.navigate(Screen.CHAT); close() }
        DrawerItem("🕘", "Chat History") { vm.navigate(Screen.HOME); close() }
        DrawerItem("◆", "Models") { vm.navigate(Screen.MODELS); close() }
        DrawerItem("⚙", "Settings") { vm.navigate(Screen.SETTINGS); close() }
        Spacer(Modifier.height(20.dp))
        OfflineBadge()
        Spacer(Modifier.height(10.dp))
        Text("Offline Gemma  v2.0", color = TextLo, fontSize = 12.sp)
    }
}

@Composable
private fun DrawerItem(glyph: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(glyph, fontSize = 18.sp, modifier = Modifier.width(32.dp))
        Text(label, fontSize = 16.sp)
    }
}

@Composable
private fun OfflineBadge() {
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp), 0.07f).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Good))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Offline Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Good)
            Text("Running locally on your device", color = TextLo, fontSize = 12.sp)
        }
    }
}

// =====================================================================
//  shared top bar
// =====================================================================
@Composable
private fun TopBar(
    title: String,
    subtitle: String?,
    leftGlyph: String,
    onLeft: () -> Unit,
    rightGlyph: String? = null,
    onRight: () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(leftGlyph, onLeft)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = TextLo)
        }
        if (rightGlyph != null) GlassIconButton(rightGlyph, onRight)
    }
}

private fun formatWhen(ms: Long): String = when {
    DateUtils.isToday(ms) -> "Today " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    DateUtils.isToday(ms + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))
}

// =====================================================================
//  2. HOME = chat history
// =====================================================================
@Composable
private fun HomeScreen(vm: ChatViewModel, openMenu: () -> Unit) {
    val convs by vm.conversations.collectAsState()
    var query by remember { mutableStateOf("") }
    val shown = remember(convs, query) {
        if (query.isBlank()) convs
        else convs.filter { c -> c.title.contains(query, true) || c.messages.any { it.text.contains(query, true) } }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Chat History", null, "☰", openMenu, "＋") { vm.newChat(); vm.navigate(Screen.CHAT) }
        Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().glass(RoundedCornerShape(16.dp))) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search chats...", color = TextLo) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = transparentFieldColors()
            )
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (shown.isEmpty()) {
                item {
                    Text(
                        if (convs.isEmpty()) "No chats yet. Tap New chat to start." else "No chats match your search.",
                        color = TextLo, modifier = Modifier.padding(top = 24.dp).fillMaxWidth(), textAlign = TextAlign.Center
                    )
                }
            }
            items(shown, key = { it.id }) { c ->
                ConversationRow(c, { vm.openChat(c.id); vm.navigate(Screen.CHAT) }, { vm.deleteChat(c.id) })
            }
        }
        Column(Modifier.padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OfflineBadge()
            GradientButton("New chat", Modifier.fillMaxWidth()) { vm.newChat(); vm.navigate(Screen.CHAT) }
        }
    }
}

@Composable
private fun ConversationRow(c: Conversation, onOpen: () -> Unit, onDelete: () -> Unit) {
    val preview = (c.messages.lastOrNull { !it.isUser }?.text ?: "").replace("\n", " ").take(70)
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp)).clickable(onClick = onOpen).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) { Text("💬") }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(c.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(preview, color = TextLo, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(formatWhen(c.updatedAt), fontSize = 11.sp, color = TextLo)
            Text("✕", color = TextLo, modifier = Modifier.clickable(onClick = onDelete).padding(6.dp))
        }
    }
}

// =====================================================================
//  3. CHAT
// =====================================================================
private val quickPrompts = listOf(
    "Explain Simply" to "Explain simply: ",
    "Write Essay" to "Write an essay about ",
    "Solve Math" to "Solve this math problem step by step: ",
    "Code Help" to "Help me with this code: ",
    "Study Plan" to "Make a study plan for ",
    "Summarize" to "Summarize this: ",
    "Creative Writing" to "Write a short creative story about "
)

@Composable
private fun ChatScreen(vm: ChatViewModel) {
    val messages by vm.messages.collectAsState()
    val generating by vm.generating.collectAsState()
    val title by vm.currentTitle.collectAsState()
    val clipboard = LocalClipboardManager.current
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        TopBar(title, "Gemma 3 1B  ·  offline", "←", { vm.navigate(Screen.HOME) }, "＋") { vm.newChat() }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                EmptyChat { input = it }
            } else {
                // reverseLayout: newest message sits at the bottom and the list follows the streaming text
                val reversed = remember(messages) { messages.asReversed() }
                val lastId = messages.last().id
                LazyColumn(
                    Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(reversed, key = { it.id }) { m ->
                        MessageBubble(
                            msg = m,
                            isStreaming = generating && m.id == lastId && !m.isUser,
                            onCopy = { clipboard.setText(AnnotatedString(m.text)) },
                            onSpeak = { vm.speak(m.text) }
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.weight(1f).glass(RoundedCornerShape(26.dp), 0.09f)) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Type a message...", color = TextLo) },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = transparentFieldColors()
                )
            }
            val canSend = generating || input.isNotBlank()
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(if (canSend) AccentBrush else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))))
                    .clickable(enabled = canSend) {
                        if (generating) vm.stop() else { vm.send(input); input = "" }
                    },
                contentAlignment = Alignment.Center
            ) { Text(if (generating) "■" else "➤", color = Color.White, fontSize = 18.sp) }
        }
    }
}

@Composable
private fun EmptyChat(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        GemLogo(56.dp)
        Spacer(Modifier.height(14.dp))
        Text("How can I help you today?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text("Runs 100% on your phone", color = TextLo, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        Text("Quick prompts", color = TextLo, fontSize = 13.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        quickPrompts.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, prefill) ->
                    Box(
                        Modifier.weight(1f).glass(RoundedCornerShape(14.dp), 0.09f)
                            .clickable { onPick(prefill) }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, fontSize = 14.sp) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, isStreaming: Boolean, onCopy: () -> Unit, onSpeak: () -> Unit) {
    if (msg.isUser) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier.widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(AccentBrush)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) { Text(msg.text, color = Color.White) }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) { GemLogo(18.dp) }
            Column(Modifier.weight(1f, fill = false)) {
                Column(Modifier.glass(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)).padding(14.dp)) {
                    if (msg.text.isEmpty()) Text("Thinking...", color = TextLo) else MarkdownText(msg.text)
                }
                if (!isStreaming && msg.text.isNotEmpty()) {
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniAction("Copy", onCopy)
                        MiniAction("Read aloud", onSpeak)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAction(label: String, onClick: () -> Unit) {
    Text(
        label, fontSize = 12.sp, color = TextLo,
        modifier = Modifier.glass(RoundedCornerShape(10.dp), 0.07f).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

// =====================================================================
//  6. MODEL INFO
// =====================================================================
@Composable
private fun ModelsScreen(vm: ChatViewModel) {
    val settings by vm.settings.collectAsState()
    val sizeMb = remember { vm.modelSizeMb() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(uri)
    }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Models", null, "←", { vm.navigate(Screen.HOME) })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GemLogo(56.dp)
                Text("Gemma 3 1B IT", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Installed", color = Good, fontSize = 12.sp,
                    modifier = Modifier.glass(RoundedCornerShape(10.dp), 0.07f).padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Text(
                    "Google's lightweight Gemma model, running fully offline on your phone.",
                    color = TextLo, textAlign = TextAlign.Center, fontSize = 14.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoTile("Model size", "$sizeMb MB", Modifier.weight(1f))
                InfoTile("Context length", "${settings.maxTokens} tokens", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoTile("Quantization", "4-bit (int4)", Modifier.weight(1f))
                InfoTile("RAM usage", "~1.5 GB (approx)", Modifier.weight(1f))
            }
            Text(
                "Any Gemma .task file that fits your phone's RAM can be imported here. " +
                    "Importing replaces the current model.",
                color = TextLo, fontSize = 13.sp
            )
            GradientButton("Import a different .task file", Modifier.fillMaxWidth()) { picker.launch(arrayOf("*/*")) }
            GlassButton(
                if (confirmDelete) "Tap again to delete the model" else "Delete model",
                Modifier.fillMaxWidth(), color = Bad
            ) {
                if (confirmDelete) vm.deleteModel() else confirmDelete = true
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.glass(RoundedCornerShape(18.dp)).padding(14.dp)) {
        Text(label, color = TextLo, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// =====================================================================
//  5. SETTINGS
// =====================================================================
@Composable
private fun SettingsScreen(vm: ChatViewModel) {
    val settings by vm.settings.collectAsState()
    var draft by remember(settings) { mutableStateOf(settings) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Settings", null, "←", { vm.navigate(Screen.HOME) })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsCard("Your name") {
                Box(Modifier.fillMaxWidth().glass(RoundedCornerShape(14.dp), 0.06f)) {
                    TextField(
                        value = draft.userName,
                        onValueChange = { draft = draft.copy(userName = it); saved = false },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = transparentFieldColors()
                    )
                }
            }
            SettingsCard("Temperature   ${"%.1f".format(draft.temperature)}") {
                Text("Higher = more creative, lower = more focused. Applies from your next message.", color = TextLo, fontSize = 12.sp)
                Slider(
                    value = draft.temperature,
                    onValueChange = { draft = draft.copy(temperature = (it * 10).roundToInt() / 10f); saved = false },
                    valueRange = 0.1f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = Accent, activeTrackColor = Accent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
            SettingsCard("Max tokens   ${draft.maxTokens}") {
                Text("Maximum length of the whole conversation the model can hold. Changing it reloads the model.", color = TextLo, fontSize = 12.sp)
                Slider(
                    value = draft.maxTokens.toFloat(),
                    onValueChange = { draft = draft.copy(maxTokens = ((it / 256f).roundToInt() * 256).coerceIn(512, 2048)); saved = false },
                    valueRange = 512f..2048f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = Accent, activeTrackColor = Accent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
            SettingsCard("System prompt") {
                Text("Instructions the model follows in every new chat.", color = TextLo, fontSize = 12.sp)
                Box(Modifier.fillMaxWidth().glass(RoundedCornerShape(14.dp), 0.06f)) {
                    TextField(
                        value = draft.systemPrompt,
                        onValueChange = { draft = draft.copy(systemPrompt = it); saved = false },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        colors = transparentFieldColors()
                    )
                }
            }
            GradientButton("Save settings", Modifier.fillMaxWidth()) {
                val needsReload = draft.maxTokens != settings.maxTokens
                vm.saveSettings(draft)
                saved = true
                if (needsReload) vm.reloadModel()
            }
            if (saved) Text("Saved ✓", color = Good, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        content()
    }
}
