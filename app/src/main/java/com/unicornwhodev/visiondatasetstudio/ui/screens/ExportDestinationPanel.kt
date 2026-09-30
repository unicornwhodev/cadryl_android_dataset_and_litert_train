package com.unicornwhodev.visiondatasetstudio.ui.screens

import androidx.compose.foundation.layout.*
import com.unicornwhodev.visiondatasetstudio.ui.icons.CadrylIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.core.workflow.StudioWorkflow
import com.unicornwhodev.visiondatasetstudio.ui.MainViewModel
import com.unicornwhodev.visiondatasetstudio.ui.components.*

@Composable
fun ExportDestinationPanel(vm: MainViewModel) {
    val project by vm.projectFlow.collectAsState()
    val auth by vm.authStatus.collectAsState()
    val access by vm.destRepoStatus.collectAsState()
    val busy by vm.isBusy.collectAsState()
    val p = project ?: return
    var destination by rememberSaveable(p.id, p.hfDestRepo) { mutableStateOf(p.hfDestRepo) }
    var token by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(false) }
    val valid = destination.isBlank() || StudioWorkflow.normalizeRepo(destination, true) != null
    StudioSection(tr("Publier sur Hugging Face", "Publish to Hugging Face"),
        tr("Facultatif. L’archive locale reste disponible.", "Optional. Local archives remain available."), CadrylIcons.CloudUpload) {
        OutlinedTextField(destination, { destination = it }, enabled = !busy, singleLine = true,
            isError = !valid, label = { Text(tr("Dépôt de destination", "Destination repository")) },
            placeholder = { Text("organisation/dataset") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.saveExportDestination(destination) }, enabled = !busy && valid && destination != p.hfDestRepo) {
            Text(tr("Enregistrer la destination", "Save destination"))
        }
        OutlinedButton(onClick = { vm.checkDestinationRepo(destination) }, enabled = !busy && valid && destination.isNotBlank()) {
            Text(tr("Vérifier la lecture du dépôt", "Check repository read access"))
        }
        access?.let { Text(if (it.exists) tr("Dépôt lisible. Le droit d’écriture sera contrôlé par HF lors de la publication.",
            "Repository readable. HF checks write permission when publishing.") else it.message.orEmpty(), style = MaterialTheme.typography.bodySmall) }
        Text(if (auth?.isValid == true) tr("Connecté : ${auth?.username.orEmpty()}", "Signed in: ${auth?.username.orEmpty()}")
            else tr("Connectez-vous pour publier ou accéder à un dépôt privé.", "Sign in to publish or access a private repository."), style = MaterialTheme.typography.bodyMedium)
        StudioDisclosure(tr("Compte et jeton HF", "HF account & token"), CadrylIcons.Key, auth?.isValid != true) {
            OutlinedTextField(token, { token = it }, label = { Text(tr("Jeton HF", "HF token")) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), enabled = !busy, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.saveToken(token); token = "" }, enabled = !busy && token.isNotBlank()) { Text(tr("Connecter", "Connect")) }
            Text(tr("Pour publier, autorisez l’écriture sur le dépôt choisi dans les permissions du jeton et du compte.",
                "To publish, allow writes to the chosen repository in both token and account permissions."), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { create = true }, enabled = !busy && valid && destination.isNotBlank() && auth?.isValid == true) {
            Text(tr("Créer un dépôt privé…", "Create private repository…"))
        }
    }
    if (create) AlertDialog(onDismissRequest = { create = false }, title = { Text(tr("Créer ce dépôt privé ?", "Create this private repository?")) },
        text = { Text(StudioWorkflow.normalizeRepo(destination, true).orEmpty()) },
        confirmButton = { Button(onClick = { create = false; vm.createDestinationRepo(destination) }) { Text(tr("Créer", "Create")) } },
        dismissButton = { TextButton(onClick = { create = false }) { Text(tr("Annuler", "Cancel")) } })
}
