package com.unicornwhodev.visiondatasetstudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.core.storage.BoundedUtf8

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegalPanel() {
    var document by rememberSaveable { mutableStateOf<String?>(null) }
    Text(tr("Confidentialité et licences", "Privacy and licences"),style=MaterialTheme.typography.titleMedium)
    Text("Unicorn Who Dev · France\nunicornwhodev@gmail.com",style=MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("privacy.txt" to tr("Confidentialité", "Privacy"), "notices.txt" to tr("Licences", "Licences")).forEach { (file,title) ->
            OutlinedButton(onClick={document=file},modifier=Modifier.testTag("legal_${file.substringBefore('.')}") ) { Text(title) }
        }
    }
    document?.let { file -> LegalDocumentDialog(file) { document=null } }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LegalDocumentDialog(file: String,onDismiss: () -> Unit) {
    val context=LocalContext.current
    val documentDensity=LocalDensity.current
    val localizedFile=if(java.util.Locale.getDefault().language=="en" && file!="notices.txt") file.removeSuffix(".txt")+"-en.txt" else file
    // Small, immutable APK assets: prepare once before the dialog's separate
    // window composition, so reopening never depends on a window coroutine.
    val document=remember(localizedFile,context) {
        runCatching {
            context.assets.open("legal/$localizedFile").use { input ->
                BoundedUtf8.reader(input,2L*1024*1024).use { it.readText().chunked(4000) }
            }
        }
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalDensity provides documentDensity) {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),color=MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(tr("Informations légales", "Legal information"),style=MaterialTheme.typography.titleMedium)
                    TextButton(onClick=onDismiss) { Text(tr("Fermer", "Close")) }
                }
                if(document.isFailure) Text(tr("Document indisponible.", "Document unavailable."))
                else LazyColumn(Modifier.weight(1f).testTag("legal_document"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    items(document.getOrThrow()) { chunk -> SelectionContainer { Text(chunk,style=MaterialTheme.typography.bodySmall) } }
                }
            }
        }
        }
    }
}
