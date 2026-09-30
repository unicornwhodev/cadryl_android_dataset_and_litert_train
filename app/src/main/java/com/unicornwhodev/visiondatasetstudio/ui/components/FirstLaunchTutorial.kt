package com.unicornwhodev.visiondatasetstudio.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import com.unicornwhodev.visiondatasetstudio.ui.icons.CadrylIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.unicornwhodev.visiondatasetstudio.core.i18n.tr

/** Interactive examples only: no project, annotation, download or external write. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstLaunchTutorial(initiallyDisableNextLaunch: Boolean, saveError: Boolean,
                        onFinish: (Boolean) -> Unit, onPostpone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var source by rememberSaveable { mutableIntStateOf(-1) }
    var marked by rememberSaveable { mutableStateOf(false) }
    var assistance by rememberSaveable { mutableIntStateOf(-1) }
    var format by rememberSaveable { mutableIntStateOf(-1) }
    var verified by rememberSaveable { mutableStateOf(false) }
    var disableNextLaunch by rememberSaveable { mutableStateOf(initiallyDisableNextLaunch) }
    val ready = when(step) { 0 -> source>=0; 1 -> marked; 2 -> assistance>=0; else -> format>=0 && verified }
    StudioSection(tr("Découvrir Cadryl · ${step+1}/4", "Discover Cadryl · ${step+1}/4"),icon=CadrylIcons.AutoAwesome,
        modifier=Modifier.testTag("tutorial_card")) {
        Text(tr("Exemple interactif · vos projets restent inchangés.", "Interactive example · your projects stay unchanged."),
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(progress={ (step+1)/4f },modifier=Modifier.fillMaxWidth())
        Crossfade(targetState=step,label="tutorial_step") { current ->
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                when(current) {
                    0 -> {
                        Text(tr("1. Choisir ses images", "1. Choose your images"),style=MaterialTheme.typography.titleMedium)
                        Text(tr("Choisissez une source pour essayer.",
                            "Choose a source to try."))
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            FilterChip(source==0,{source=0},label={Text(tr("Dossier sur l’appareil", "Device folder"))},modifier=Modifier.testTag("tutorial_source_local"))
                            FilterChip(source==1,{source=1},label={Text(tr("HF, facultatif", "HF, optional"))},modifier=Modifier.testTag("tutorial_source_hf"))
                        }
                        if(source==1) Text(tr("Hugging Face nécessite une connexion. Aucune connexion n’est faite dans cet exemple.",
                            "Hugging Face needs a connection. This example makes no connection."),style=MaterialTheme.typography.bodySmall)
                    }
                    1 -> {
                        Text(tr("2. Annoter et relire", "2. Annotate and review"),style=MaterialTheme.typography.titleMedium)
                        Text(tr("Touchez l’image d’exemple pour placer une annotation.", "Tap the example image to place an annotation."))
                        Surface(Modifier.fillMaxWidth().height(100.dp).testTag("tutorial_mark_target")
                            .clickable(role=Role.Button){marked=true},shape=MaterialTheme.shapes.medium,
                            color=MaterialTheme.colorScheme.surfaceContainerHigh,
                            border=if(marked)BorderStroke(2.dp,MaterialTheme.colorScheme.primary) else null) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                                Icon(if(marked)CadrylIcons.CheckCircle else CadrylIcons.Image,
                                    tr("Image d’exemple", "Example image"),Modifier.size(30.dp),tint=MaterialTheme.colorScheme.primary)
                                Text(if(marked)tr("Annotation d’exemple ajoutée", "Example annotation added") else tr("Touchez pour essayer", "Tap to try"))
                            }
                        }
                        Text(tr("Vous décidez de valider ou de corriger. Une suggestion ne remplace pas vos annotations enregistrées.",
                            "You decide what to approve or correct. A suggestion does not replace your saved annotations."),style=MaterialTheme.typography.bodySmall)
                    }
                    2 -> {
                        Text(tr("3. Choisir son aide", "3. Choose your assistance"),style=MaterialTheme.typography.titleMedium)
                        Text(tr("Choisissez un mode d’exemple. L’IA reste facultative.", "Choose an example mode. AI is optional."))
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            FilterChip(assistance==0,{assistance=0},label={Text(tr("À la main", "Manual"))},modifier=Modifier.testTag("tutorial_mode_manual"))
                            FilterChip(assistance==1,{assistance=1},label={Text(tr("Avec un modèle", "With a model"))},modifier=Modifier.testTag("tutorial_mode_model"))
                        }
                        Text(tr("Importez un modèle dans Modèles. L’apprentissage travaille sur une copie ; l’original est conservé.",
                            "Import a model in Models. Training uses a copy and preserves the original."),style=MaterialTheme.typography.bodySmall)
                    }
                    else -> {
                        Text(tr("4. Exporter une copie vérifiée", "4. Export a verified copy"),style=MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            listOf("JSONL","COCO","YOLO").forEachIndexed { index, label ->
                                FilterChip(format==index,{format=index},label={Text(label)},modifier=Modifier.testTag("tutorial_export_${label.lowercase()}"))
                            }
                        }
                        TutorialCheck(tr("Exemple : la copie a été relue", "Example: the copy has been read back"),verified,
                            "tutorial_copy_verified") { verified=it }
                        Text(tr("L’export conserve le JSONL complet. Le nettoyage d’un lot exige une preuve de copie relue ; une publication ou suppression demande votre confirmation.",
                            "Export preserves the complete JSONL. Batch cleanup requires proof of a copy read back; publication or deletion needs your confirmation."),style=MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                        TutorialCheck(tr("Ne plus afficher au démarrage", "Do not show again on launch"),disableNextLaunch,
                            "tutorial_disable_next_launch") { disableNextLaunch=it }
                        Text(tr("Vous pourrez relancer ce guide dans les réglages.", "You can replay this guide in settings."),style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if(saveError) Text(tr("Le choix n’a pas pu être enregistré. Réessayez.", "Could not save your choice. Try again."),color=MaterialTheme.colorScheme.error)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(step>0) TextButton(onClick={step--},modifier=Modifier.testTag("tutorial_previous")) { Text(tr("Précédent", "Previous")) }
            Button(onClick={if(step<3)step++ else onFinish(disableNextLaunch)},enabled=ready,modifier=Modifier.testTag("tutorial_next")) {
                Text(if(step<3)tr("Suivant", "Next") else tr("Terminer", "Finish"))
            }
            TextButton(onClick=onPostpone,modifier=Modifier.testTag("tutorial_later")) { Text(tr("Plus tard", "Later")) }
        }
    }
}

@Composable
private fun TutorialCheck(label: String, checked: Boolean, tag: String, onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).toggleable(value=checked,role=Role.Checkbox,onValueChange=onChange),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Checkbox(checked,onCheckedChange=null)
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
    }
}
