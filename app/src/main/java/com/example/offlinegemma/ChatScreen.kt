package com.example.offlinegemma

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatApp(vm: ChatViewModel) {
    val state by vm.modelState.collectAsState()
    val messages by vm.messages.collectAsState()
    val generating by vm.generating.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Gemma") },
                actions = {
                    if (state is ModelState.Ready) {
                        TextButton(onClick = vm::newChat, enabled = !generating) { Text("New chat") }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state is ModelState.Ready) {
                ChatContent(messages, generating, vm::send)
            } else {
                SetupScreen(state, vm)
            }
        }
    }
}

@Composable
private fun SetupScreen(state: ModelState, vm: ChatViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(uri)
    }
    var token by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Set up the model", style = MaterialTheme.typography.headlineSmall)
        Text("Gemma 3 1B (.task file, about 550 MB). You only do this once.")

        if (state is ModelState.Working) {
            Text(state.label)
            val p = state.progress
            if (p == null) {
                CircularProgressIndicator()
            } else {
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                Text("${(p * 100).toInt()}%")
            }
        } else {
            if (state is ModelState.Failed) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = vm::loadModel) { Text("Retry loading") }
            }
            Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Pick model file from storage")
            }
            Text("or download inside the app (needs a free Hugging Face read token):")
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Hugging Face token (hf_...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { vm.downloadModel(token) }, modifier = Modifier.fillMaxWidth()) {
                Text("Download model")
            }
        }
    }
}

@Composable
private fun ChatContent(messages: List<ChatMessage>, generating: Boolean, onSend: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll down whenever a new message appears or the last one grows
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg -> Bubble(msg) }
        }
        Row(
            Modifier.fillMaxWidth().padding(8.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Message") },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            Button(
                onClick = { onSend(input); input = "" },
                enabled = input.isNotBlank() && !generating
            ) { Text("Send") }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(msg.text.ifEmpty { "..." }, modifier = Modifier.padding(12.dp))
        }
    }
}
