package com.unicornwhodev.visiondatasetstudio.ui.screens

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import com.unicornwhodev.visiondatasetstudio.R

import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import com.unicornwhodev.visiondatasetstudio.ui.theme.StudioGraphite
import com.unicornwhodev.visiondatasetstudio.ui.components.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.unicornwhodev.visiondatasetstudio.ui.icons.CadrylIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unicornwhodev.visiondatasetstudio.core.geometry.ImageViewport
import com.unicornwhodev.visiondatasetstudio.core.geometry.ViewPoint
import com.unicornwhodev.visiondatasetstudio.core.workflow.*
import com.unicornwhodev.visiondatasetstudio.data.model.*
import com.unicornwhodev.visiondatasetstudio.data.json.StudioJson
import com.unicornwhodev.visiondatasetstudio.domain.inference.ModelAction
import com.unicornwhodev.visiondatasetstudio.domain.inference.ModelCapabilities
import com.unicornwhodev.visiondatasetstudio.domain.inference.ModelConfig
import com.unicornwhodev.visiondatasetstudio.domain.inference.MaskCodec
import com.unicornwhodev.visiondatasetstudio.domain.validation.HumanAnnotationReview
import com.unicornwhodev.visiondatasetstudio.ui.*
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot

/** Compact visible controls with a full 48 dp touch target. */
@Composable
private fun EditorCommand(icon: ImageVector, description: String, onClick: () -> Unit,
                          modifier: Modifier = Modifier, enabled: Boolean = true, selected: Boolean? = null, accent: Boolean = false, outlined: Boolean = false) {
    val background by animateColorAsState(when { !enabled -> Color.Transparent; accent -> MaterialTheme.colorScheme.primary; selected == true -> MaterialTheme.colorScheme.primaryContainer; else -> Color.Transparent }, label = "editor command")
    val tint = when { !enabled -> LocalContentColor.current.copy(alpha = .38f); accent -> MaterialTheme.colorScheme.onPrimary; selected == true -> MaterialTheme.colorScheme.primary; else -> LocalContentColor.current }
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(48.dp).semantics { if (selected != null) this.selected = selected }) {
        val container = if(outlined) Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            .background(LocalContentColor.current.copy(alpha=.035f))
            .border(1.dp,LocalContentColor.current.copy(alpha=if(enabled) .20f else .10f),RoundedCornerShape(8.dp))
            else Modifier.size(28.dp).clip(RoundedCornerShape(5.dp)).background(background)
        Box(container,contentAlignment=Alignment.Center) {
            Icon(icon, description, Modifier.size(20.dp), tint = tint)
        }
    }
}

/** Selection and creation are separate tools: moving an existing target never creates another. */
enum class EditorTool { SELECT, BOX, POINT, MASK, ERASE, POLYGON, LASSO, FILL, SAM_POINT, PAN_ZOOM }
private val EditorTool.icon: ImageVector get() = when(this) {
    EditorTool.SELECT -> CadrylIcons.NearMe; EditorTool.BOX -> CadrylIcons.CropSquare
    EditorTool.POINT, EditorTool.SAM_POINT -> CadrylIcons.MyLocation; EditorTool.MASK -> CadrylIcons.Brush
    EditorTool.ERASE -> CadrylIcons.AutoFixOff; EditorTool.POLYGON -> CadrylIcons.ChangeHistory
    EditorTool.LASSO -> CadrylIcons.Gesture; EditorTool.FILL -> CadrylIcons.FormatColorFill
    EditorTool.PAN_ZOOM -> CadrylIcons.PanTool
}
private val EditorTool.title: String get() = when(this) {
    EditorTool.SELECT -> tr("Sélection", "Select"); EditorTool.BOX -> tr("Boîte", "Box")
    EditorTool.POINT -> tr("Point", "Point"); EditorTool.MASK -> tr("Masque", "Mask")
    EditorTool.ERASE -> tr("Gomme", "Eraser"); EditorTool.POLYGON -> tr("Polygone", "Polygon")
    EditorTool.LASSO -> "Lasso"; EditorTool.FILL -> tr("Remplir", "Fill")
    EditorTool.SAM_POINT -> "SAM"; EditorTool.PAN_ZOOM -> tr("Déplacer", "Pan")
}
private val EditorTool.description: String get() = when(this) {
    EditorTool.SELECT -> tr("Sélectionner et déplacer", "Select and move"); EditorTool.BOX -> tr("Dessiner une boîte", "Draw a box")
    EditorTool.POINT -> tr("Placer un point", "Place a point"); EditorTool.MASK -> tr("Peindre le masque", "Paint mask")
    EditorTool.ERASE -> tr("Effacer le masque", "Erase mask"); EditorTool.POLYGON -> tr("Polygone · double-tap pour fermer", "Polygon · double-tap to close")
    EditorTool.LASSO -> "Lasso"; EditorTool.FILL -> tr("Remplir une zone", "Fill an area")
    EditorTool.SAM_POINT -> tr("Pointer pour SAM", "Point for SAM"); EditorTool.PAN_ZOOM -> tr("Déplacer et zoomer l’image", "Pan and zoom the image")
}

@Composable
private fun ToolShelfItem(icon: ImageVector, title: String, description: String, selected: Boolean,
                          enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick=onClick,enabled=enabled,color=if(selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor=if(selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape=if(selected) CutCornerShape(8.dp) else RoundedCornerShape(8.dp),
        modifier=modifier.semantics { this.selected=selected; contentDescription=description }) {
        Column(Modifier.heightIn(min=60.dp).padding(horizontal=3.dp,vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Icon(icon,null,Modifier.size(24.dp))
            Text(title,style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}
private enum class EditorTab(private val titleText: () -> String) {
    REGIONS({ tr("Régions", "Regions") }),
    CAPTION({ tr("Légendes", "Captions") }),
    TAGS({ "Tags" }),
    GROUNDING({ tr("Texte ↔ région", "Text ↔ region") }),
    VQA({ "VQA" }),
    COUNTING({ tr("Comptage", "Counting") }),
    QUALITY({ tr("Qualité", "Quality") });
    val title get() = titleText()
}
private fun enabledTabs(tasks: Set<StudioTask>): List<EditorTab> = buildList {
    if (tasks.any { it in setOf(StudioTask.POINTING, StudioTask.POINTING_MULTI, StudioTask.DETECTION, StudioTask.SEGMENTATION, StudioTask.GROUNDING) }) add(EditorTab.REGIONS)
    if (StudioTask.CAPTIONING in tasks) add(EditorTab.CAPTION)
    if (StudioTask.CLASSIFICATION in tasks) add(EditorTab.TAGS)
    if (StudioTask.GROUNDING in tasks) add(EditorTab.GROUNDING)
    if (StudioTask.VQA in tasks) add(EditorTab.VQA)
    if (StudioTask.COUNTING in tasks) add(EditorTab.COUNTING)
    add(EditorTab.QUALITY)
}
private fun newId() = UUID.randomUUID().toString()
private fun withoutTarget(a: SampleAnnotations, id: String) = a.copy(
    masks = a.masks.filterNot { it.id == id }, points = a.points.filterNot { it.id == id }, boxes = a.boxes.filterNot { it.id == id },
    groundings = a.groundings.map { it.copy(boxIds = it.boxIds - id, pointIds = it.pointIds - id) },
    vqaList = a.vqaList.map { it.copy(targetIds = it.targetIds - id) }, counts = a.counts.map { it.copy(linkedInstanceIds = it.linkedInstanceIds - id) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotationEditorScreen(sampleId: String, viewModel: MainViewModel) {
    val sample by viewModel.currentSample.collectAsState()
    val a by viewModel.currentAnnotations.collectAsState()
    val project by viewModel.projectFlow.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val workflow by viewModel.workflow.collectAsState()
    var showWorkflowInstructions by remember { mutableStateOf(false) }
    val samples by viewModel.batchSamples.collectAsState()
    val saving by viewModel.saveState.collectAsState()
    val canUndo by viewModel.undoAvailable.collectAsState()
    val canRedo by viewModel.redoAvailable.collectAsState()
    val issues by viewModel.editorIssues.collectAsState()
    val busy by viewModel.isBusy.collectAsState()
    val editing by viewModel.editorBusy.collectAsState()
    val tasks = remember(project?.activeTasksCsv) { StudioWorkflow.parseTasks(project?.activeTasksCsv ?: "DETECTION") }
    val tabs = remember(tasks) { enabledTabs(tasks) }
    var tab by rememberSaveable { mutableStateOf(tabs.first()) }
    LaunchedEffect(tabs) { if (tab !in tabs) tab = tabs.first() }
    var tool by rememberSaveable { mutableStateOf(EditorTool.SELECT) }
    val classes = remember(project?.classesCsv) { com.unicornwhodev.visiondatasetstudio.core.workflow.ProjectVocabulary.parse(project?.classesCsv.orEmpty()).ifEmpty { listOf("object") } }
    var label by rememberSaveable { mutableStateOf(classes.first()) }
    LaunchedEffect(classes) { if (label !in classes) label = classes.first() }
    var selected by remember(sampleId) { mutableStateOf<String?>(null) }
    var copiedBox by remember { mutableStateOf<BoxTarget?>(null) }
    var copiedPoint by remember { mutableStateOf<PointTarget?>(null) }
    var brushSize by rememberSaveable { mutableFloatStateOf(.025f) }
    var eraserSize by rememberSaveable { mutableFloatStateOf(.025f) }
    var samPoint by remember(sampleId) { mutableStateOf<ViewPoint?>(null) }
    val samModel = project?.modelConfigJson?.contains("efficientvit_sam") == true
    var zoom by remember(sampleId) { mutableFloatStateOf(1f) }
    var pan by remember(sampleId) { mutableStateOf(Offset.Zero) }
    var rejectDialog by remember { mutableStateOf(false) }
    var acceptDialog by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    var more by remember { mutableStateOf(false) }
    var labelMenu by remember { mutableStateOf(false) }
    var quickClassMenu by remember { mutableStateOf(false) }
    var toolDrawer by remember { mutableStateOf(false) }
    val position = samples.indexOfFirst { it.sampleId == sampleId }.let { if (it < 0) 1 else it + 1 }
    val wideCommands = LocalConfiguration.current.screenWidthDp >= 600
    val compactHeight = LocalConfiguration.current.screenHeightDp < 480
    val locked = busy || editing
    val update: (SampleAnnotations) -> Unit = viewModel::updateAnnotations
    val regionTab = tab == EditorTab.REGIONS || tab == EditorTab.GROUNDING
    val maskAllowed = StudioTask.SEGMENTATION in tasks
    val boxAllowed = StudioTask.DETECTION in tasks || StudioTask.GROUNDING in tasks
    val pointAllowed = StudioTask.POINTING in tasks || StudioTask.POINTING_MULTI in tasks || StudioTask.GROUNDING in tasks
    val aiProposalCount=HumanAnnotationReview.unreviewedModelCount(a)
    val importedDraftCount=HumanAnnotationReview.unreviewedDraftCount(a)
    val reviewCount=aiProposalCount+importedDraftCount
    val hasUnreviewed=reviewCount>0
    val hasModel = !project?.modelPath.isNullOrBlank() || project?.modelConfigJson?.contains("local_http") == true
    val modelAction=remember(project?.modelConfigJson,project?.activeTasksCsv) {
        project?.modelConfigJson?.let { json -> runCatching { StudioJson.moshi.adapter(ModelConfig::class.java).fromJson(json) }.getOrNull() }
            ?.let { ModelCapabilities.action(it,project?.activeTasksCsv.orEmpty()) } ?: ModelAction.NONE
    }
    val availableTools = buildList {
        if(boxAllowed || pointAllowed || maskAllowed) add(EditorTool.SELECT)
        if(boxAllowed) add(EditorTool.BOX)
        if(pointAllowed) add(EditorTool.POINT)
        if(maskAllowed) addAll(listOf(EditorTool.MASK, EditorTool.POLYGON, EditorTool.LASSO, EditorTool.ERASE, EditorTool.FILL))
        if(samModel) add(EditorTool.SAM_POINT)
        add(EditorTool.PAN_ZOOM)
    }

    val inspectorState = rememberSaveableStateHolder()
    var propertiesOpen by rememberSaveable { mutableStateOf(false) }
    val chooseTool: (EditorTool) -> Unit = { next ->
        tool=next; toolDrawer=false; propertiesOpen=false
        if(next!=EditorTool.PAN_ZOOM && EditorTab.REGIONS in tabs) tab=EditorTab.REGIONS
    }
    val openReview: () -> Unit = {
        tab=when {
            a.tags.any{!it.isHumanVerified} && EditorTab.TAGS in tabs -> EditorTab.TAGS
            a.captions.any{!it.isHumanVerified} && EditorTab.CAPTION in tabs -> EditorTab.CAPTION
            a.vqaList.any{!it.isHumanVerified} && EditorTab.VQA in tabs -> EditorTab.VQA
            a.counts.any{!it.isHumanVerified} && EditorTab.COUNTING in tabs -> EditorTab.COUNTING
            a.groundings.any{!it.isHumanVerified} && EditorTab.GROUNDING in tabs -> EditorTab.GROUNDING
            else -> tabs.first()
        }; propertiesOpen=true
    }
    val inspector: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier.background(MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Annotations", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                if (!workflow?.instructions.isNullOrBlank()) EditorCommand(CadrylIcons.Assignment,
                    tr("Consignes du workflow", "Workflow instructions"), { showWorkflowInstructions = true })
                EditorCommand(CadrylIcons.Close, tr("Fermer les propriétés", "Close properties"), onClick = { propertiesOpen = false })
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                tabs.forEach { t -> TextButton(onClick = { tab = t }, colors = ButtonDefaults.textButtonColors(contentColor = if (tab == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text(t.title, style = MaterialTheme.typography.labelMedium) } }
            }
            if (regionTab) Box(Modifier.padding(horizontal = 8.dp)) {
                TextButton(onClick = { labelMenu = true }) { Text(tr("Classe · $label", "Class · $label"), style = MaterialTheme.typography.labelMedium); Icon(CadrylIcons.ArrowDropDown, null, Modifier.size(18.dp)) }
                DropdownMenu(expanded = labelMenu, onDismissRequest = { labelMenu = false }) {
                    classes.forEach { cls -> DropdownMenuItem(text = { Text(cls) }, onClick = { label = cls; labelMenu = false }) }
                }
            }
            StudioAction(stringResource(R.string.editor_similar_images),viewModel::findSimilarImages,icon=CadrylIcons.ImageSearch,enabled=!locked,modifier=Modifier.padding(horizontal=12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(1f)) {
                inspectorState.SaveableStateProvider("$sampleId:${tab.name}") {
                    when(tab) {
                        EditorTab.REGIONS -> RegionInspector(a, classes, selected, { selected = it; tool = EditorTool.SELECT }, update,
                            onDuplicate={id->a.boxes.firstOrNull{it.id==id}?.let{b->val n=b.copy(id=newId(),instanceId=null,xmin=(b.xmin+.02f).coerceAtMost(.98f),xmax=(b.xmax+.02f).coerceAtMost(1f));update(a.copy(boxes=a.boxes+n));selected=n.id}
                                ?:a.points.firstOrNull{it.id==id}?.let{p->val n=p.copy(id=newId(),instanceId=null,x=(p.x+.02f).coerceAtMost(1f),y=(p.y+.02f).coerceAtMost(1f));update(a.copy(points=a.points+n));selected=n.id}},
                            onCopy={id->copiedBox=a.boxes.firstOrNull{it.id==id};copiedPoint=a.points.firstOrNull{it.id==id}},
                            onPaste={val b=copiedBox;val pnt=copiedPoint;if(b!=null){val n=b.copy(id=newId(),instanceId=null,xmin=(b.xmin+.02f).coerceAtMost(.98f),xmax=(b.xmax+.02f).coerceAtMost(1f));update(a.copy(boxes=a.boxes+n));selected=n.id}else if(pnt!=null){val n=pnt.copy(id=newId(),instanceId=null,x=(pnt.x+.02f).coerceAtMost(1f),y=(pnt.y+.02f).coerceAtMost(1f));update(a.copy(points=a.points+n));selected=n.id}},canPaste=copiedBox!=null||copiedPoint!=null,
                            onInfer = { if (!hasModel) viewModel.navigateTo(Screen.Models) else viewModel.runLiteRtOnCurrentSample(samPoint?.let { listOf(it.x,it.y) }.orEmpty()) }, modelAction = modelAction, locked = locked)
                        EditorTab.CAPTION -> CaptionEditorTab(a, prefs.captionLanguage, update)
                        EditorTab.TAGS -> TagsEditorTab(a, classes, update)
                        EditorTab.GROUNDING -> GroundingEditorTab(a, update)
                        EditorTab.VQA -> VqaEditorTab(a, update)
                        EditorTab.COUNTING -> CountingEditorTab(a, classes, update)
                        EditorTab.QUALITY -> QualityEditorTab(a, update)
                    }
                }
            }
        }
    }
    Scaffold(contentWindowInsets = WindowInsets(0), modifier = Modifier.imePadding(), topBar = {
        Column {
            StudioBrandBar()
            if(!compactHeight) Row(Modifier.fillMaxWidth().heightIn(min=44.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick={viewModel.navigateTo(Screen.Controls)},enabled=!locked,modifier=Modifier.weight(1f)) {
                    Text(project?.name ?: tr("Projet","Project"),Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,
                        style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurface)
                    Icon(CadrylIcons.ArrowDropDown,null,Modifier.size(18.dp))
                }
                EditorCommand(CadrylIcons.GridView,tr("Revenir au lot après enregistrement","Return to batch after saving"),viewModel::back,enabled=!locked)
                Text("$position / ${samples.size.coerceAtLeast(1)}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(horizontal=20.dp),color=MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
                EditorCommand(CadrylIcons.ChevronLeft,tr("Image précédente","Previous image"),{viewModel.moveSample(-1)},enabled=position>1 && !locked)
                Text(sample?.assetId ?: tr("Image","Image"),Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleSmall)
                if(saving==tr("Enregistré sur cet appareil","Saved on this device")) Icon(CadrylIcons.CheckCircle,tr("Enregistré","Saved"),Modifier.size(18.dp),tint=MaterialTheme.colorScheme.secondary)
                EditorCommand(CadrylIcons.ChevronRight,tr("Image suivante","Next image"),{viewModel.moveSample(1)},enabled=position<samples.size && !locked)
            }
        }
    }, bottomBar = {
        if(compactHeight) Surface(color=StudioGraphite,contentColor=Color(0xFFF3F1E9)) {
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                val approveCompact: @Composable () -> Unit = {
                Button(onClick=viewModel::validateCurrentAndNext,enabled=!locked && !saving.startsWith(tr("Échec","Failed")),
                    shape=CutCornerShape(8.dp),modifier=Modifier.testTag("validate_next_button")) { Text(tr("Valider","Approve"));Spacer(Modifier.width(8.dp));Icon(CadrylIcons.Check,null) }
                }
                if(prefs.leftHanded) approveCompact()
                TextButton(onClick={toolDrawer=true},enabled=!locked,colors=ButtonDefaults.textButtonColors(contentColor=LocalContentColor.current)) {
                    Icon(tool.icon,null,Modifier.size(22.dp));Spacer(Modifier.width(6.dp));Text(tool.title);Icon(CadrylIcons.ArrowDropDown,null,Modifier.size(18.dp))
                }
                OutlinedButton(onClick={propertiesOpen=!propertiesOpen},enabled=!locked,
                    modifier=Modifier.testTag("editor_annotations"),colors=ButtonDefaults.outlinedButtonColors(contentColor=LocalContentColor.current),
                    border=BorderStroke(1.dp,Color(0xFF53615A)),shape=RoundedCornerShape(8.dp)) {
                    Icon(CadrylIcons.Layers,tr("Annotations et propriétés","Annotations and properties"),Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp));Text(if(regionTab) label else tab.title)
                }
                EditorCommand(CadrylIcons.Undo,tr("Annuler la dernière modification","Undo last change"),viewModel::undo,enabled=canUndo && !locked,modifier=Modifier.testTag("undo_button"))
                EditorCommand(CadrylIcons.Redo,tr("Rétablir","Redo"),viewModel::redo,enabled=canRedo && !locked,modifier=Modifier.testTag("redo_button"))
                if(hasUnreviewed) TextButton(onClick=openReview,modifier=Modifier.testTag("annotation_proposals"),colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFFFFAB8C))) {
                    Text(tr("$reviewCount à relire","$reviewCount to review"))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick=viewModel::deferCurrent,enabled=!locked,modifier=Modifier.testTag("defer_button"),
                    colors=ButtonDefaults.textButtonColors(contentColor=LocalContentColor.current)) { Text(tr("Passer","Skip")) }
                Box {
                    EditorCommand(CadrylIcons.MoreVert,tr("Actions du cas","Sample actions"),{more=true})
                    DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_zoom_in)) }, onClick = { zoom = (zoom * 1.25f).coerceAtMost(12f); more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_zoom_out)) }, onClick = { zoom = (zoom / 1.25f).coerceAtLeast(1f); more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_fit_image)) }, onClick = { zoom = 1f; pan = Offset.Zero; more = false })
                        if(maskAllowed) DropdownMenuItem(text = { Text(stringResource(R.string.editor_new_mask)) }, enabled = !locked, onClick = { selected = null; tool = EditorTool.MASK; more = false; propertiesOpen = false })
                        if(samModel) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.editor_sam_point)) }, enabled = !locked, onClick = { tool = EditorTool.SAM_POINT; more = false; propertiesOpen = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.editor_segment_point)) }, enabled = samPoint != null && !locked,
                                onClick = { viewModel.runLiteRtOnCurrentSample(samPoint!!.let { listOf(it.x,it.y) }); more = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.editor_clear_sam)) }, enabled = samPoint != null && !locked, onClick = { samPoint = null; more = false })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_previous_image)) }, enabled = position > 1 && !locked, onClick = { viewModel.moveSample(-1); more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_next_image)) }, enabled = position < samples.size && !locked, onClick = { viewModel.moveSample(1); more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_defer_image)) }, enabled = !locked, onClick = { viewModel.deferCurrent(); more = false }, modifier = Modifier.testTag("defer_menu_button"))
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_reject_reason)) }, enabled = !locked, onClick = { rejectDialog = true; more = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_retry_save)) }, onClick = { viewModel.retrySave(); more = false })
                        DropdownMenuItem(text = { Text(tr("Marquer les éléments à relire comme relus", "Mark review items as reviewed")) }, enabled = hasUnreviewed && !locked, onClick = { acceptDialog = true; more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_customize_tools)) }, onClick = { viewModel.navigateTo(Screen.Preferences); more = false })
                    }
                }
                if(!prefs.leftHanded) approveCompact()
            }
        } else
        Column {
            if(availableTools.size>1) Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),
                shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,shadowElevation=3.dp) {
                Row(Modifier.padding(6.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    availableTools.filter { it in setOf(EditorTool.SELECT,EditorTool.BOX,EditorTool.POINT,EditorTool.MASK) }.forEach { t ->
                        ToolShelfItem(t.icon,t.title,t.description,tool==t,!locked,Modifier.weight(1f)) { chooseTool(t) }
                    }
                    ToolShelfItem(CadrylIcons.MoreVert,tr("Plus","More"),tr("Tous les outils d’annotation","All annotation tools"),
                        tool !in setOf(EditorTool.SELECT,EditorTool.BOX,EditorTool.POINT,EditorTool.MASK),!locked,Modifier.weight(1f)) { toolDrawer=true }
                }
            }
            Surface(color=StudioGraphite,contentColor=Color(0xFFF3F1E9),shape=RoundedCornerShape(topStart=20.dp,topEnd=20.dp)) {
                Column(Modifier.padding(horizontal=16.dp).padding(top=8.dp,bottom=12.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        if(regionTab) Box(Modifier.weight(1f)) {
                            TextButton(onClick={quickClassMenu=true},enabled=!locked,contentPadding=PaddingValues(0.dp),
                                colors=ButtonDefaults.textButtonColors(contentColor=LocalContentColor.current)) {
                                val ordinal=classes.indexOf(label)+1
                                Surface(Modifier.size(32.dp),shape=CutCornerShape(6.dp),
                                    color=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary) {
                                    Box(contentAlignment=Alignment.Center) {
                                        Text(if(ordinal>0) ordinal.toString().padStart(2,'0') else "—",style=MaterialTheme.typography.labelLarge)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f),horizontalAlignment=Alignment.Start) {
                                    Text(tr("Classe","Class"),style=MaterialTheme.typography.labelSmall,color=Color(0xFFBBC5BE))
                                    Text(label,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                                }
                                Icon(CadrylIcons.ArrowDropDown,null,Modifier.size(18.dp))
                            }
                            DropdownMenu(quickClassMenu,{quickClassMenu=false}) {
                                classes.forEach { cls -> DropdownMenuItem(text={Text(cls)},onClick={label=cls;quickClassMenu=false}) }
                            }
                        } else Text(tab.title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                        EditorCommand(CadrylIcons.Undo,tr("Annuler la dernière modification","Undo last change"),viewModel::undo,enabled=canUndo && !locked,modifier=Modifier.testTag("undo_button"),outlined=true)
                        EditorCommand(CadrylIcons.Redo,tr("Rétablir","Redo"),viewModel::redo,enabled=canRedo && !locked,modifier=Modifier.testTag("redo_button"),outlined=true)
                        OutlinedButton(onClick={propertiesOpen=!propertiesOpen},enabled=!locked,
                            modifier=Modifier.testTag("editor_annotations"),border=BorderStroke(1.dp,Color(0xFF53615A)),
                            colors=ButtonDefaults.outlinedButtonColors(contentColor=LocalContentColor.current),shape=RoundedCornerShape(8.dp),
                            contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)) {
                            Icon(CadrylIcons.Layers,tr("Annotations et propriétés","Annotations and properties"),Modifier.size(22.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if(wideCommands) "Annotations · ${a.boxes.size+a.points.size+a.masks.size}" else "${a.boxes.size+a.points.size+a.masks.size}")
                        }
                    }
                    HorizontalDivider(color=Color(0xFF46534D))
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Surface(shape=RoundedCornerShape(8.dp),color=Color.White.copy(alpha=.025f),
                            contentColor=LocalContentColor.current,border=BorderStroke(1.dp,Color(0xFF46534D))) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                EditorCommand(CadrylIcons.Remove,tr("Zoom arrière","Zoom out"),{zoom=(zoom/1.25f).coerceAtLeast(1f)})
                                VerticalDivider(Modifier.height(28.dp),color=Color(0xFF46534D))
                                TextButton(onClick={zoom=1f;pan=Offset.Zero},modifier=Modifier.testTag("editor_fit_image"),
                                    colors=ButtonDefaults.textButtonColors(contentColor=LocalContentColor.current)) {
                                    Text("${(zoom*100).toInt()} %",style=MaterialTheme.typography.labelLarge)
                                }
                                VerticalDivider(Modifier.height(28.dp),color=Color(0xFF46534D))
                                EditorCommand(CadrylIcons.Add,tr("Zoom avant","Zoom in"),{zoom=(zoom*1.25f).coerceAtMost(12f)})
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        val failed=saving.startsWith(tr("Échec","Failed"))
                        TextButton(onClick={if(failed)viewModel.retrySave()},enabled=failed,
                            colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFFFFB4AB),disabledContentColor=Color(0xFFBBC5BE)),
                            modifier=Modifier.weight(1f)) {
                            if(saving==tr("Enregistré sur cet appareil","Saved on this device")) Surface(Modifier.size(20.dp),
                                shape=CircleShape,color=Color(0xFFB9C9B7),contentColor=StudioGraphite) {
                                Box(contentAlignment=Alignment.Center) { Icon(CadrylIcons.Check,null,Modifier.size(15.dp)) }
                            } else Icon(if(failed) CadrylIcons.ErrorOutline else CadrylIcons.Sync,null,Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if(saving==tr("Enregistré sur cet appareil","Saved on this device")) tr("Enregistré","Saved") else saving,
                                maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
                        }
                    }
                    if(tool in setOf(EditorTool.MASK,EditorTool.ERASE)) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(if(tool==EditorTool.MASK)tr("Pinceau","Brush") else tr("Gomme","Eraser"),style=MaterialTheme.typography.labelSmall)
                        Slider(value=if(tool==EditorTool.MASK)brushSize else eraserSize,onValueChange={if(tool==EditorTool.MASK)brushSize=it else eraserSize=it},valueRange=.005f.. .1f,modifier=Modifier.weight(1f))
                        Text("${((if(tool==EditorTool.MASK)brushSize else eraserSize)*100).toInt()} %",style=MaterialTheme.typography.labelSmall)
                    }
                    if(hasModel || hasUnreviewed) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        TextButton(onClick=openReview,modifier=Modifier.weight(1f).testTag("annotation_proposals"),
                            colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFFFFAB8C))) {
                            Icon(CadrylIcons.AutoAwesome,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp))
                            Text(if(hasUnreviewed) tr("$reviewCount à relire","$reviewCount to review") else tr("Assistance IA","AI assistance"),style=MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick={if(modelAction==ModelAction.NONE)viewModel.navigateTo(Screen.Models) else viewModel.runLiteRtOnCurrentSample(samPoint?.let{listOf(it.x,it.y)}.orEmpty())},
                            enabled=!locked && (!samModel || samPoint!=null),colors=ButtonDefaults.outlinedButtonColors(contentColor=LocalContentColor.current),
                            border=BorderStroke(1.dp,Color(0xFF53615A)),shape=RoundedCornerShape(8.dp)) {
                            Icon(CadrylIcons.AutoAwesome,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp))
                            Text(if(modelAction==ModelAction.NONE)tr("Modèle","Model") else tr("Suggérer","Suggest"),style=MaterialTheme.typography.labelLarge)
                        }
                    }
                    HorizontalDivider(color=Color(0xFF46534D))
                    Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        val approve: @Composable () -> Unit = {
                            Button(onClick=viewModel::validateCurrentAndNext,enabled=!locked && !saving.startsWith(tr("Échec","Failed")),
                                modifier=Modifier.weight(1.2f).heightIn(min=48.dp).testTag("validate_next_button")
                                    .semantics {contentDescription=if(prefs.autoAdvance)tr("Valider et passer à l’image suivante","Approve and move to the next image") else tr("Valider l’image","Approve image")},
                                shape=CutCornerShape(8.dp)) {
                                Text(tr("Valider","Approve"));Spacer(Modifier.width(12.dp));Icon(CadrylIcons.ArrowForward,null,Modifier.size(24.dp))
                            }
                        }
                        if(prefs.leftHanded) approve()
                        TextButton(onClick=viewModel::deferCurrent,enabled=!locked,modifier=Modifier.weight(.65f).testTag("defer_button"),
                            colors=ButtonDefaults.textButtonColors(contentColor=LocalContentColor.current)) { Text(tr("Passer","Skip")) }
                        Box {
                            EditorCommand(CadrylIcons.MoreVert,tr("Actions du cas","Sample actions"),{more=true},outlined=true)
                            DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_zoom_in)) }, onClick = { zoom = (zoom * 1.25f).coerceAtMost(12f); more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_zoom_out)) }, onClick = { zoom = (zoom / 1.25f).coerceAtLeast(1f); more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_fit_image)) }, onClick = { zoom = 1f; pan = Offset.Zero; more = false })
                        if(maskAllowed) DropdownMenuItem(text = { Text(stringResource(R.string.editor_new_mask)) }, enabled = !locked, onClick = { selected = null; tool = EditorTool.MASK; more = false; propertiesOpen = false })
                        if(samModel) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.editor_sam_point)) }, enabled = !locked, onClick = { tool = EditorTool.SAM_POINT; more = false; propertiesOpen = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.editor_segment_point)) }, enabled = samPoint != null && !locked,
                                onClick = { viewModel.runLiteRtOnCurrentSample(samPoint!!.let { listOf(it.x,it.y) }); more = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.editor_clear_sam)) }, enabled = samPoint != null && !locked, onClick = { samPoint = null; more = false })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_previous_image)) }, enabled = position > 1 && !locked, onClick = { viewModel.moveSample(-1); more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_next_image)) }, enabled = position < samples.size && !locked, onClick = { viewModel.moveSample(1); more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_defer_image)) }, enabled = !locked, onClick = { viewModel.deferCurrent(); more = false }, modifier = Modifier.testTag("defer_menu_button"))
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_reject_reason)) }, enabled = !locked, onClick = { rejectDialog = true; more = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_retry_save)) }, onClick = { viewModel.retrySave(); more = false })
                        DropdownMenuItem(text = { Text(tr("Marquer les éléments à relire comme relus", "Mark review items as reviewed")) }, enabled = hasUnreviewed && !locked, onClick = { acceptDialog = true; more = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_customize_tools)) }, onClick = { viewModel.navigateTo(Screen.Preferences); more = false })
                    }
                        }
                        if(!prefs.leftHanded) approve()
                    }
                }
            }
        }
    }) { inset ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(inset)) {
            val wide = maxWidth >= 840.dp
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest).clipToBounds()) {
                        InteractiveAnnotationCanvas(sample, a, if (locked || (propertiesOpen && !regionTab)) EditorTool.PAN_ZOOM else tool,
                            label, selected, zoom, pan, { z, o -> zoom = z; pan = o }, { selected = it }, { next ->
                                if (StudioTask.POINTING in tasks && next.points.size > 1 && next.points.size > a.points.size) viewModel.reportError(tr("Mode Point unique : déplacez le point existant ou activez Points multiples.", "Single-point mode: move the existing point or enable Multiple points.")) else update(next)
                            }, brushFraction=if(tool==EditorTool.ERASE)eraserSize else brushSize,showLabels = prefs.showCanvasLabels, promptPoint = samPoint, onPromptSelected = { samPoint = it })
                    }
                    if(!propertiesOpen && !workflow?.instructions.isNullOrBlank()) TextButton(onClick={showWorkflowInstructions=true}) {
                        Icon(CadrylIcons.Assignment,tr("Consignes du workflow","Workflow instructions"),Modifier.size(18.dp));Spacer(Modifier.width(6.dp))
                        Text(tr("Consignes du workflow","Workflow instructions"),style=MaterialTheme.typography.labelSmall)
                    }


                }
                if (wide && propertiesOpen) {
                    VerticalDivider()
                    inspector(Modifier.width(300.dp).fillMaxHeight())
                }
            }
            if (!wide && propertiesOpen) ModalBottomSheet(onDismissRequest = { propertiesOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = null) {
                inspector(Modifier.fillMaxWidth().fillMaxHeight(.8f))
            }
        }
    }
    if(toolDrawer) ModalBottomSheet(onDismissRequest={toolDrawer=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(bottom=24.dp)) {
            Text(tr("Outils d’annotation","Annotation tools"),style=MaterialTheme.typography.titleLarge)
            availableTools.forEach { t ->
                ListItem(headlineContent={Text(t.title)},supportingContent={Text(t.description,style=MaterialTheme.typography.bodySmall)},
                    leadingContent={Icon(t.icon,null)},trailingContent={if(tool==t)Icon(CadrylIcons.Check,null)},
                    modifier=Modifier.clickable(enabled=!locked){chooseTool(t)})
            }
        }
    }
    if(showWorkflowInstructions) AlertDialog(onDismissRequest={showWorkflowInstructions=false},title={Text(tr("Consignes du workflow", "Workflow instructions"))},text={Text(workflow?.instructions.orEmpty())},confirmButton={TextButton(onClick={showWorkflowInstructions=false}){Text(stringResource(R.string.action_close))}})
    if (issues.isNotEmpty()) AlertDialog(onDismissRequest = viewModel::clearEditorIssues, title = { Text(stringResource(R.string.editor_review_before_validate)) }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) { issues.forEach { Text(it) } }
    }, confirmButton = { TextButton(onClick = viewModel::clearEditorIssues) { Text(stringResource(R.string.editor_return_corrections)) } })
    if (rejectDialog) AlertDialog(onDismissRequest = { rejectDialog = false }, title = { Text(stringResource(R.string.editor_reject_case)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.editor_reject_explanation))
            OutlinedTextField(reason, { reason = it }, label = { Text(stringResource(R.string.editor_rejection_reason)) }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { Button(onClick = { rejectDialog = false; viewModel.rejectCurrent(reason) }, enabled = reason.isNotBlank()) { Text(stringResource(R.string.editor_confirm_rejection)) } }, dismissButton = { TextButton(onClick = { rejectDialog = false }) { Text(stringResource(R.string.common_cancel)) } })
    if (acceptDialog) AlertDialog(onDismissRequest = { acceptDialog = false },
        title = { Text(tr("Marquer tous les éléments comme relus ?", "Mark all review items as reviewed?")) },
        text = { Text(tr("Les suggestions IA et brouillons importés restants deviennent des décisions humaines relues, mais l’image reste « à valider ». Utilisez ensuite Valider l’image pour finaliser le cas.",
            "Remaining AI suggestions and imported drafts become human-reviewed decisions, but the image still needs approval. Then use Approve image to finalize the sample.")) },
        confirmButton = { Button(onClick = { acceptDialog = false; viewModel.acceptCurrentProposals() }) { Text(tr("Marquer comme relus", "Mark as reviewed")) } },
        dismissButton = { TextButton(onClick = { acceptDialog = false }) { Text(stringResource(R.string.common_cancel)) } })
}

@Composable
fun InteractiveAnnotationCanvas(sample: SampleEntity?, annotations: SampleAnnotations, activeTool: EditorTool, selectedClass: String,
    selectedTargetId: String?, scale: Float, offset: Offset, onTransformChanged: (Float, Offset) -> Unit,
    onTargetSelected: (String?) -> Unit, onAnnotationsUpdated: (SampleAnnotations) -> Unit, brushFraction:Float=.025f,showLabels: Boolean = true,
    promptPoint: ViewPoint? = null, onPromptSelected: (ViewPoint) -> Unit = {}) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val context = LocalContext.current
    val labelTypeface = remember(context) { ResourcesCompat.getFont(context, R.font.barlow_semibold) }
    val labelPaint = remember(density.density, density.fontScale, labelTypeface) {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = with(density) { 12.sp.toPx() }
            typeface = labelTypeface
            color = android.graphics.Color.WHITE
        }
    }
    val radius = with(density) { 24.dp.toPx() }
    val viewport = remember(canvasSize, sample?.imageWidth, sample?.imageHeight, scale, offset) {
        ImageViewport(canvasSize.width.toFloat(), canvasSize.height.toFloat(), (sample?.imageWidth ?: 0).toFloat(), (sample?.imageHeight ?: 0).toFloat(), scale, offset.x, offset.y)
    }
    val latest by rememberUpdatedState(annotations)
    val selectedId by rememberUpdatedState(selectedTargetId)
    val emit by rememberUpdatedState(onAnnotationsUpdated)
    val select by rememberUpdatedState(onTargetSelected)
    val selectPrompt by rememberUpdatedState(onPromptSelected)
    val latestScale by rememberUpdatedState(scale)
    val latestOffset by rememberUpdatedState(offset)
    val transform by rememberUpdatedState(onTransformChanged)
    var draft by remember(sample?.sampleId) { mutableStateOf<SampleAnnotations?>(null) }
    var dragStart by remember { mutableStateOf<ViewPoint?>(null) }
    var startAnnotation by remember { mutableStateOf<SampleAnnotations?>(null) }
    var dragTarget by remember { mutableStateOf<String?>(null) }
    var corner by remember { mutableIntStateOf(-1) }
    var creatingId by remember { mutableStateOf<String?>(null) }
    var paintingId by remember(sample?.sampleId) { mutableStateOf<String?>(null) }
    var polygonPoints by remember(sample?.sampleId) { mutableStateOf<List<Pair<Float,Float>>>(emptyList()) }
    var lassoPoints by remember(sample?.sampleId) { mutableStateOf<List<Pair<Float,Float>>>(emptyList()) }
    val transformState = rememberTransformableState { z, move, _ ->
        val newScale = (latestScale * z).coerceIn(1f, 12f)
        val maxPanX = canvasSize.width * newScale
        val maxPanY = canvasSize.height * newScale
        transform(newScale, Offset((latestOffset.x + move.x).coerceIn(-maxPanX, maxPanX), (latestOffset.y + move.y).coerceIn(-maxPanY, maxPanY)))
    }
    fun hitPoint(at: Offset): PointTarget? = latest.points.filter { it.canProvideCoordinates }.minByOrNull { p -> val s=viewport.toScreen(p.x,p.y); hypot(s.x-at.x,s.y-at.y) }?.takeIf { p -> val s=viewport.toScreen(p.x,p.y); hypot(s.x-at.x,s.y-at.y)<=radius }
    fun hitBox(n: ViewPoint): BoxTarget? = latest.boxes.filter { n.x in it.xmin..it.xmax && n.y in it.ymin..it.ymax }.minByOrNull { (it.xmax-it.xmin)*(it.ymax-it.ymin) }
    fun maskStroke(at: ViewPoint, previous: ViewPoint = at): SampleAnnotations {
        val origin=draft ?: latest
        val old=origin.masks.firstOrNull { it.id==(paintingId ?: selectedId) }
        if(old==null && activeTool==EditorTool.ERASE) return origin
        val iw=(sample?.imageWidth ?: 512).coerceAtLeast(1);val ih=(sample?.imageHeight ?: 512).coerceAtLeast(1)
        val (w,h)=MaskCodec.rasterSizeForImage(iw,ih)
        val mask=old ?: MaskTarget(newId(),selectedClass,w,h,listOf(w*h),isHumanVerified=true)
        val updated=com.unicornwhodev.visiondatasetstudio.domain.inference.MaskCodec.stroke(mask,previous.x,previous.y,at.x,at.y,brushFraction,activeTool==EditorTool.ERASE)
        paintingId=updated.id
        select(updated.id)
        return origin.copy(masks=origin.masks.filterNot { it.id==updated.id }+updated)
    }
    val drawn = draft ?: annotations
    val maskImages=remember(drawn.masks) { drawn.masks.map { m ->
        val pixels=com.unicornwhodev.visiondatasetstudio.domain.inference.MaskCodec.decode(m)
        val tint = if (m.isHumanVerified) 0x66D85836 else 0x66617C99
        m to android.graphics.Bitmap.createBitmap(IntArray(pixels.size) { if(pixels[it]) tint else 0 },m.width,m.height,android.graphics.Bitmap.Config.ARGB_8888)
    } }
    DisposableEffect(maskImages) { onDispose { maskImages.forEach { it.second.recycle() } } }
    Box(Modifier.fillMaxSize().clipToBounds().onSizeChanged { canvasSize = it }
        .semantics { contentDescription = tr("Image à annoter. ${annotations.boxes.size} boîtes et ${annotations.points.size} points. Les régions sont aussi accessibles dans le panneau de correction.", "Image to annotate. ${annotations.boxes.size} boxes and ${annotations.points.size} points. Regions are also accessible in the correction panel.") }
        .then(if(activeTool == EditorTool.PAN_ZOOM) Modifier.transformable(transformState) else Modifier)
        .pointerInput(sample?.sampleId, activeTool, selectedClass, viewport) {
            detectTapGestures(onDoubleTap = when(activeTool){EditorTool.PAN_ZOOM->{{_:Offset->transform(1f,Offset.Zero)}};EditorTool.POLYGON->{{_:Offset->if(polygonPoints.size>=3){val origin=latest;val old=origin.masks.firstOrNull{it.id==selectedId};val mask=old?:MaskCodec.empty(newId(),selectedClass,sample?.imageWidth?:512,sample?.imageHeight?:512);val next=MaskCodec.polygon(mask,polygonPoints);emit(origin.copy(masks=origin.masks.filterNot{it.id==next.id}+next));select(next.id);polygonPoints=emptyList()}}};else->null}, onTap = { at ->
                val n = viewport.toImage(at.x, at.y) ?: return@detectTapGestures
                when(activeTool) {
                    EditorTool.SAM_POINT -> selectPrompt(n)
                    EditorTool.POINT -> {
                        val near = hitPoint(at)
                        if (near != null) select(near.id) else {
                            val point = PointTarget(newId(), n.x, n.y, selectedClass, isHumanVerified = true)
                            emit(latest.copy(points = latest.points + point)); select(point.id)
                        }
                    }
                    EditorTool.SELECT, EditorTool.BOX -> select(hitPoint(at)?.id ?: hitBox(n)?.id ?: latest.masks.firstOrNull { m ->
                        val pixels=com.unicornwhodev.visiondatasetstudio.domain.inference.MaskCodec.decode(m)
                        pixels[(n.y*m.height).toInt().coerceIn(0,m.height-1)*m.width+(n.x*m.width).toInt().coerceIn(0,m.width-1)]
                    }?.id)
                    EditorTool.MASK, EditorTool.ERASE -> { paintingId=selectedId; emit(maskStroke(n)); paintingId=null }
                    EditorTool.POLYGON -> polygonPoints=polygonPoints+(n.x to n.y)
                    EditorTool.FILL -> {val origin=latest;val old=origin.masks.firstOrNull{it.id==selectedId};val mask=old?:MaskCodec.empty(newId(),selectedClass,sample?.imageWidth?:512,sample?.imageHeight?:512);val next=MaskCodec.fill(mask,n.x,n.y);emit(origin.copy(masks=origin.masks.filterNot{it.id==next.id}+next));select(next.id)}
                    EditorTool.LASSO -> Unit
                    EditorTool.PAN_ZOOM -> Unit
                }
            })
        }
        .then(if(activeTool in setOf(EditorTool.MASK,EditorTool.ERASE)) Modifier.pointerInput(sample?.sampleId,activeTool,selectedClass,viewport) {
            detectDragGestures(onDragStart={ at ->
                draft=null;paintingId=selectedId;dragStart=viewport.toImage(at.x,at.y);dragStart?.let { draft=maskStroke(it) }
            },onDrag={ change,_ ->
                val at=viewport.toImage(change.position.x,change.position.y,clamp=true)
                if(at!=null) { change.consume();draft=maskStroke(at,dragStart ?: at);dragStart=at }
            },onDragEnd={draft?.let(emit);draft=null;dragStart=null;paintingId=null},onDragCancel={draft=null;dragStart=null;paintingId=null})
        } else Modifier).then(if(activeTool==EditorTool.LASSO)Modifier.pointerInput(sample?.sampleId,selectedClass,viewport){detectDragGestures(onDragStart={at->lassoPoints=viewport.toImage(at.x,at.y)?.let{listOf(it.x to it.y)}.orEmpty()},onDrag={change,_->viewport.toImage(change.position.x,change.position.y,true)?.let{lassoPoints=lassoPoints+(it.x to it.y)};change.consume()},onDragEnd={if(lassoPoints.size>=3){val origin=latest;val old=origin.masks.firstOrNull{it.id==selectedId};val mask=old?:MaskCodec.empty(newId(),selectedClass,sample?.imageWidth?:512,sample?.imageHeight?:512);val next=MaskCodec.polygon(mask,lassoPoints);emit(origin.copy(masks=origin.masks.filterNot{it.id==next.id}+next));select(next.id)};lassoPoints=emptyList()},onDragCancel={lassoPoints=emptyList()})}else Modifier)
        .then(if (activeTool == EditorTool.BOX || activeTool == EditorTool.SELECT) Modifier.pointerInput(sample?.sampleId, activeTool, selectedClass, viewport) {
            detectDragGestures(onDragStart = { at ->
                dragStart = viewport.toImage(at.x, at.y)
                startAnnotation = latest; corner = -1; draft = null; dragTarget = null; creatingId = null
                if (dragStart != null) {
                    if (activeTool == EditorTool.BOX) creatingId = newId()
                    else {
                        val selectedBox = latest.boxes.firstOrNull { it.id == selectedId }
                        if (selectedBox != null) {
                            val corners = listOf(selectedBox.xmin to selectedBox.ymin, selectedBox.xmax to selectedBox.ymin, selectedBox.xmin to selectedBox.ymax, selectedBox.xmax to selectedBox.ymax)
                            corner = corners.indexOfFirst { (x,y) -> val s=viewport.toScreen(x,y); hypot(s.x-at.x,s.y-at.y)<=radius }
                        }
                        dragTarget = if(corner>=0) selectedBox?.id else hitPoint(at)?.id ?: hitBox(dragStart!!)?.id
                        select(dragTarget)
                    }
                }
            }, onDrag = { change, _ ->
                val start = dragStart
                val n = viewport.toImage(change.position.x, change.position.y, clamp = true)
                val origin = startAnnotation
                if (start != null && n != null && origin != null) {
                    change.consume()
                    if (creatingId != null) {
                        val box = BoxTarget(creatingId!!, minOf(start.x,n.x), minOf(start.y,n.y), maxOf(start.x,n.x), maxOf(start.y,n.y), selectedClass, isHumanVerified = true)
                        draft = origin.copy(boxes = origin.boxes + box)
                    } else {
                        val point = origin.points.firstOrNull { it.id == dragTarget }
                        val box = origin.boxes.firstOrNull { it.id == dragTarget }
                        if (point != null) draft = origin.copy(points = origin.points.map { if(it.id == point.id) it.copy(x = (it.x+n.x-start.x).coerceIn(0f,1f), y=(it.y+n.y-start.y).coerceIn(0f,1f), isHumanVerified=true) else it })
                        if (box != null) {
                            val dx=(n.x-start.x).coerceIn(-box.xmin,1f-box.xmax); val dy=(n.y-start.y).coerceIn(-box.ymin,1f-box.ymax)
                            val minW = 1f / viewport.imageWidth.coerceAtLeast(1f); val minH=1f / viewport.imageHeight.coerceAtLeast(1f)
                            val changed = when(corner) {
                                0 -> box.copy(xmin=n.x.coerceAtMost(box.xmax-minW).coerceAtLeast(0f), ymin=n.y.coerceAtMost(box.ymax-minH).coerceAtLeast(0f))
                                1 -> box.copy(xmax=n.x.coerceAtLeast(box.xmin+minW).coerceAtMost(1f), ymin=n.y.coerceAtMost(box.ymax-minH).coerceAtLeast(0f))
                                2 -> box.copy(xmin=n.x.coerceAtMost(box.xmax-minW).coerceAtLeast(0f), ymax=n.y.coerceAtLeast(box.ymin+minH).coerceAtMost(1f))
                                3 -> box.copy(xmax=n.x.coerceAtLeast(box.xmin+minW).coerceAtMost(1f), ymax=n.y.coerceAtLeast(box.ymin+minH).coerceAtMost(1f))
                                else -> box.copy(xmin=box.xmin+dx,xmax=box.xmax+dx,ymin=box.ymin+dy,ymax=box.ymax+dy)
                            }.copy(isHumanVerified=true)
                            draft = origin.copy(boxes=origin.boxes.map { if(it.id==box.id) changed else it })
                        }
                    }
                }
            }, onDragEnd = {
                val created = draft?.boxes?.firstOrNull { it.id == creatingId }
                val acceptable = creatingId == null || (created != null && (created.xmax-created.xmin)*viewport.width >= 4f && (created.ymax-created.ymin)*viewport.height >= 4f)
                if (acceptable) draft?.let { emit(it); if(creatingId != null) select(creatingId) }
                draft=null; dragStart=null; creatingId=null
            }, onDragCancel = { draft=null; dragStart=null; creatingId=null })
        } else Modifier)) {
        if (sample?.localImagePath != null) AsyncImage(model = File(sample.localImagePath), contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX=scale; scaleY=scale; translationX=offset.x; translationY=offset.y })
        Canvas(Modifier.fillMaxSize()) {
            val human = Color(0xFFD85836); val proposed = Color(0xFF617C99)
            fun label(text: String, anchor: Offset, verified: Boolean) {
                val paddingX = 8.dp.toPx(); val paddingY = 4.dp.toPx()
                val visible = TextUtils.ellipsize(text.take(40), labelPaint,
                    (size.width - 2 * paddingX).coerceAtLeast(1f), TextUtils.TruncateAt.END).toString()
                val metrics = labelPaint.fontMetrics
                val width = labelPaint.measureText(visible) + 2 * paddingX
                val height = metrics.descent - metrics.ascent + 2 * paddingY
                val left = (anchor.x + 5.dp.toPx()).coerceIn(0f, (size.width - width).coerceAtLeast(0f))
                val top = (anchor.y - height).coerceAtLeast(0f)
                drawRoundRect(if (verified) Color(0xFFB94222) else Color(0xFF415975),
                    Offset(left, top), Size(width, height), CornerRadius(4.dp.toPx()))
                drawContext.canvas.nativeCanvas.drawText(visible, left + paddingX,
                    top + paddingY - metrics.ascent, labelPaint)
            }
            if(viewport.isValid) {
                val first=viewport.toScreen(0f,0f);val last=viewport.toScreen(1f,1f)
                maskImages.forEach { (_,bitmap) -> drawContext.canvas.nativeCanvas.drawBitmap(bitmap,null,android.graphics.RectF(first.x,first.y,last.x,last.y),null) }
                promptPoint?.let { point ->
                    val screen=viewport.toScreen(point.x,point.y)
                    val at=Offset(screen.x,screen.y)
                    drawCircle(Color(0xFF617C99),10.dp.toPx(),at,style=Stroke(2.dp.toPx()))
                    drawCircle(Color(0xFF617C99),3.dp.toPx(),at)
                    if (showLabels) label("SAM", at + Offset(14.dp.toPx(), 0f), false)
                }
            }
            drawn.boxes.forEach { b ->
                val isSelected=b.id==selectedTargetId; val color=if(b.isHumanVerified) human else proposed
                val p1=viewport.toScreen(b.xmin,b.ymin); val p2=viewport.toScreen(b.xmax,b.ymax)
                if(viewport.isValid && p2.x>p1.x && p2.y>p1.y) {
                    if(isSelected) drawRect(color.copy(alpha=.04f),Offset(p1.x,p1.y),Size(p2.x-p1.x,p2.y-p1.y))
                    drawRect(color,Offset(p1.x,p1.y),Size(p2.x-p1.x,p2.y-p1.y),style=Stroke((if(isSelected) 2.5.dp else 2.dp).toPx(),pathEffect=if(!b.isHumanVerified) PathEffect.dashPathEffect(floatArrayOf(10f,8f)) else null))
                    if(isSelected) listOf(p1,ViewPoint(p2.x,p1.y),ViewPoint(p1.x,p2.y),p2).forEach {
                        val half = 3.5.dp.toPx()
                        drawRect(color, Offset(it.x - half, it.y - half), Size(half * 2, half * 2))
                    }
                    if(showLabels) label(b.label,Offset(p1.x,p1.y),b.isHumanVerified)
                }
            }
            drawn.points.filter { it.canProvideCoordinates }.forEach { p ->
                val screen=viewport.toScreen(p.x,p.y); val c=if(p.isHumanVerified) human else proposed
                if(viewport.isValid) {
                    val at=Offset(screen.x,screen.y)
                    drawCircle(Color.Black.copy(alpha=.7f),(if(p.id==selectedTargetId) 10.dp else 9.dp).toPx(),at); drawCircle(c,5.dp.toPx(),at)
                    drawLine(c,at-Offset(13.dp.toPx(),0f),at+Offset(13.dp.toPx(),0f),1.dp.toPx())
                    drawLine(c,at-Offset(0f,13.dp.toPx()),at+Offset(0f,13.dp.toPx()),1.dp.toPx())
                    if(showLabels) label(p.label,at+Offset(14.dp.toPx(),0f),p.isHumanVerified)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegionInspector(a: SampleAnnotations, classes: List<String>, selected: String?, onSelect: (String?) -> Unit,
    onUpdate: (SampleAnnotations) -> Unit,onDuplicate:(String)->Unit,onCopy:(String)->Unit,onPaste:()->Unit,canPaste:Boolean,onInfer: () -> Unit, modelAction: ModelAction, locked: Boolean) {
    val box=a.boxes.firstOrNull { it.id==selected }; val point=a.points.firstOrNull { it.id==selected }; val mask=a.masks.firstOrNull { it.id==selected }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if(mask!=null) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(tr("Masque · ${mask.width} × ${mask.height}", "Mask · ${mask.width} × ${mask.height}"),Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                IconButton(onClick={onUpdate(withoutTarget(a,mask.id));onSelect(null)}) { Icon(CadrylIcons.DeleteOutline,tr("Supprimer le masque", "Delete mask")) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) { classes.forEach { cls ->
                FilterChip(selected=mask.label==cls,onClick={onUpdate(a.copy(masks=a.masks.map { if(it.id==mask.id) it.copy(label=cls,isHumanVerified=true) else it }))},label={Text(cls)})
            } }
            Text(tr("Pinceau pour ajouter, gomme pour retirer. Annuler reste disponible.", "Brush to add, eraser to remove. Undo remains available."),style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                OutlinedButton(enabled=a.masks.count{it.label==mask.label&&it.width==mask.width&&it.height==mask.height}>=2,onClick={val chosen=a.masks.filter{it.label==mask.label&&it.width==mask.width&&it.height==mask.height};val merged=MaskCodec.merge(chosen,newId(),mask.label);onUpdate(a.copy(masks=a.masks-chosen.toSet()+merged));onSelect(merged.id)}){Text(stringResource(R.string.editor_merge))}
                OutlinedButton(onClick={val pieces=MaskCodec.split(mask){newId()};if(pieces.size>1){onUpdate(a.copy(masks=a.masks.filterNot{it.id==mask.id}+pieces));onSelect(pieces.first().id)}}){Text(stringResource(R.string.editor_split_islands))}
            }
            if(!mask.isHumanVerified) TextButton(onClick={onUpdate(a.copy(masks=a.masks.map { if(it.id==mask.id) it.copy(isHumanVerified=true) else it }))}) { Text(stringResource(R.string.editor_mask_reviewed)) }
            TextButton(onClick={onSelect(null)}) { Text(stringResource(R.string.editor_deselect)) }
        }
        if(box==null && point==null && mask==null) {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(tr("${a.masks.size+a.boxes.size+a.points.size} régions", "${a.masks.size+a.boxes.size+a.points.size} regions"), Modifier.weight(1f), style=MaterialTheme.typography.titleSmall)
                val actionLabel=if(a.unreviewedCount>0 && modelAction in setOf(ModelAction.PREANNOTATE,ModelAction.PREANNOTATE_PARTIALLY)) tr("Relancer l’IA", "Rerun AI") else when(modelAction){
                    ModelAction.PREANNOTATE->stringResource(com.unicornwhodev.visiondatasetstudio.R.string.editor_action_preannotate)
                    ModelAction.PREANNOTATE_PARTIALLY->stringResource(com.unicornwhodev.visiondatasetstudio.R.string.editor_action_preannotate_partial)
                    ModelAction.COMPUTE_REPRESENTATION->stringResource(com.unicornwhodev.visiondatasetstudio.R.string.editor_action_embedding)
                    ModelAction.INTERACTIVE_SEGMENTATION->stringResource(com.unicornwhodev.visiondatasetstudio.R.string.editor_action_interactive_segmentation)
                    ModelAction.INSPECT->stringResource(com.unicornwhodev.visiondatasetstudio.R.string.editor_action_inspect)
                    ModelAction.NONE->stringResource(com.unicornwhodev.visiondatasetstudio.R.string.editor_action_model)
                }
                StudioAction(actionLabel, onInfer, icon=CadrylIcons.Memory, enabled=!locked && modelAction!=ModelAction.INSPECT)
                IconButton(onClick=onPaste,enabled=canPaste&&!locked){Icon(CadrylIcons.ContentPaste,tr("Coller une boîte ou un point", "Paste a box or point"))}
            }
            if (a.masks.isEmpty() && a.boxes.isEmpty() && a.points.isEmpty()) Text(tr("Choisissez un outil de dessin.", "Choose a drawing tool."), style=MaterialTheme.typography.bodySmall)
        } else if(mask==null) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(if(box!=null) tr("Boîte sélectionnée", "Selected box") else tr("Point sélectionné", "Selected point"),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                IconButton(onClick={ onUpdate(withoutTarget(a, selected!!)); onSelect(null) }) { Icon(CadrylIcons.DeleteOutline,tr("Supprimer la région sélectionnée", "Delete selected region")) }
                IconButton(onClick={onDuplicate(selected!!)},enabled=!locked){Icon(CadrylIcons.CopyAll,tr("Dupliquer", "Duplicate"))}
                IconButton(onClick={onCopy(selected!!)},enabled=!locked){Icon(CadrylIcons.ContentCopy,tr("Copier", "Copy"))}
                IconButton(onClick={onSelect(null)}) { Icon(CadrylIcons.Close,tr("Désélectionner", "Deselect")) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                classes.forEach { label -> FilterChip(selected=(box?.label ?: point?.label)==label,onClick={
                    onUpdate(if(box!=null) a.copy(boxes=a.boxes.map { if(it.id==box.id) it.copy(label=label,isHumanVerified=true) else it }) else a.copy(points=a.points.map { if(it.id==point?.id) it.copy(label=label,isHumanVerified=true) else it }))
                },label={Text(label)}) }
            }
            Text(if(box!=null) tr("Glissez les coins pour redimensionner.", "Drag the corners to resize.") else tr("Glissez ou utilisez les flèches.", "Drag or use the arrows."),style=MaterialTheme.typography.bodySmall)
            if(point!=null) FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                fun state(value:PointLocalizationState) = onUpdate(a.copy(points=a.points.map{if(it.id==point.id)it.copy(
                    localizationState=value,isAbsent=value==PointLocalizationState.ABSENT,
                    isAbstained=value==PointLocalizationState.UNLOCALIZABLE||value==PointLocalizationState.UNCERTAIN,isHumanVerified=true)else it}))
                FilterChip(selected=point.effectiveLocalizationState==PointLocalizationState.LOCALIZED,onClick={state(PointLocalizationState.LOCALIZED)},label={Text(stringResource(R.string.point_localized))})
                FilterChip(selected=point.effectiveLocalizationState==PointLocalizationState.ABSENT,onClick={state(PointLocalizationState.ABSENT)},label={Text(stringResource(R.string.point_absent))})
                FilterChip(selected=point.effectiveLocalizationState==PointLocalizationState.UNLOCALIZABLE,onClick={state(PointLocalizationState.UNLOCALIZABLE)},label={Text(stringResource(R.string.point_unlocalizable))})
                FilterChip(selected=point.effectiveLocalizationState==PointLocalizationState.UNCERTAIN,onClick={state(PointLocalizationState.UNCERTAIN)},label={Text(stringResource(R.string.point_uncertain))})
            }
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
                listOf(CadrylIcons.KeyboardArrowLeft to (-.002f to 0f),CadrylIcons.KeyboardArrowUp to (0f to -.002f),CadrylIcons.KeyboardArrowDown to (0f to .002f),CadrylIcons.KeyboardArrowRight to (.002f to 0f)).forEachIndexed { i,(icon,d) ->
                    OutlinedIconButton(onClick={
                        if(point!=null) onUpdate(a.copy(points=a.points.map { if(it.id==point.id) it.copy(x=(it.x+d.first).coerceIn(0f,1f),y=(it.y+d.second).coerceIn(0f,1f),isHumanVerified=true) else it }))
                        if(box!=null) { val dx=d.first.coerceIn(-box.xmin,1f-box.xmax); val dy=d.second.coerceIn(-box.ymin,1f-box.ymax); onUpdate(a.copy(boxes=a.boxes.map { if(it.id==box.id) it.copy(xmin=it.xmin+dx,xmax=it.xmax+dx,ymin=it.ymin+dy,ymax=it.ymax+dy,isHumanVerified=true) else it })) }
                    },modifier=Modifier.size(48.dp)) { Icon(icon,listOf(tr("Déplacer à gauche", "Move left"),tr("Déplacer vers le haut", "Move up"),tr("Déplacer vers le bas", "Move down"),tr("Déplacer à droite", "Move right"))[i]) }
                }
                Text("0,2 %",style=MaterialTheme.typography.labelSmall)
            }
            if((box?.isHumanVerified ?: point?.isHumanVerified)==false) TextButton(onClick={
                onUpdate(if(box!=null) a.copy(boxes=a.boxes.map { if(it.id==box.id) it.copy(isHumanVerified=true) else it }) else a.copy(points=a.points.map { if(it.id==point?.id) it.copy(isHumanVerified=true) else it }))
            }) { Text(stringResource(R.string.editor_reviewed_proposal)) }
        }
        selected?.takeIf{box!=null||point!=null||mask!=null}?.let { targetId ->
            InstanceLinkEditor(a,targetId,onUpdate,locked)
        }
        a.masks.forEachIndexed { i,m -> RegionRow(tr("Masque ${i+1}", "Mask ${i+1}"),m.label,m.id==selected,m.isHumanVerified,m.sourceProvenance,CadrylIcons.Brush) { onSelect(m.id) } }
        a.boxes.forEachIndexed { i,b -> RegionRow(tr("Boîte ${i+1}", "Box ${i+1}"),b.label,b.id==selected,b.isHumanVerified,b.sourceProvenance,CadrylIcons.CropSquare) { onSelect(b.id) } }
        a.points.forEachIndexed { i,p -> RegionRow("Point ${i+1}",p.label,p.id==selected,p.isHumanVerified,p.sourceProvenance,CadrylIcons.MyLocation) { onSelect(p.id) } }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun InstanceLinkEditor(a:SampleAnnotations,targetId:String,onUpdate:(SampleAnnotations)->Unit,locked:Boolean) {
    val current=InstanceLinks.instanceId(a,targetId)
    val candidates=InstanceLinks.compatibleTargets(a,targetId)
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Text(if(current==null)stringResource(com.unicornwhodev.visiondatasetstudio.R.string.instance_unlinked) else stringResource(com.unicornwhodev.visiondatasetstudio.R.string.instance_linked,current.take(8)),style=MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            if(current==null) AssistChip(onClick={onUpdate(InstanceLinks.link(a,setOf(targetId),newId()))},enabled=!locked,label={Text(stringResource(com.unicornwhodev.visiondatasetstudio.R.string.instance_new))})
            else AssistChip(onClick={onUpdate(InstanceLinks.unlink(a,targetId))},enabled=!locked,label={Text(stringResource(com.unicornwhodev.visiondatasetstudio.R.string.instance_unlink))})
            candidates.forEach { candidateId ->
                val title=when {
                    a.boxes.any{it.id==candidateId}->stringResource(com.unicornwhodev.visiondatasetstudio.R.string.instance_link_box,candidateId.take(8))
                    a.masks.any{it.id==candidateId}->stringResource(com.unicornwhodev.visiondatasetstudio.R.string.instance_link_mask,candidateId.take(8))
                    else->stringResource(com.unicornwhodev.visiondatasetstudio.R.string.instance_link_point,candidateId.take(8))
                }
                AssistChip(onClick={
                    val instance=current ?: InstanceLinks.instanceId(a,candidateId) ?: newId()
                    onUpdate(InstanceLinks.link(a,setOf(targetId,candidateId),instance))
                },enabled=!locked,label={Text(title)})
            }
        }
    }
}

private fun reviewStatusLabel(verified:Boolean,provenance:String)=when {
    !verified && HumanAnnotationReview.isModelAssistedSource(provenance) -> tr("Suggestion IA · à relire", "AI suggestion · needs review")
    !verified -> tr("Brouillon importé · à relire", "Imported draft · needs review")
    provenance.startsWith("human_correction") -> tr("Corrigé manuellement", "Manually corrected")
    provenance.startsWith("human_validated") -> tr("Validé manuellement", "Manually validated")
    else -> tr("Traité manuellement", "Manually handled")
}

@Composable
private fun RegionRow(title:String, label:String, selected:Boolean, verified:Boolean, provenance:String, icon:ImageVector, onClick:()->Unit) {
    val reviewLabel=reviewStatusLabel(verified,provenance)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(if(selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
        .clickable(role=androidx.compose.ui.semantics.Role.Tab,onClick=onClick).semantics { this.selected=selected }
        .heightIn(min=48.dp).padding(horizontal=8.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,Modifier.size(17.dp),tint=if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(label,style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text("$title · $reviewLabel",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(if(verified) CadrylIcons.Check else CadrylIcons.AutoAwesome,reviewLabel,Modifier.size(14.dp),tint=if(verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun EditorPanel(title: String, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium)
        if(hint!=null) StudioDetails(hint)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaptionEditorTab(a: SampleAnnotations, defaultLanguage: String = "fr", onUpdate: (SampleAnnotations) -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val item=a.captions.getOrNull(index) ?: a.captions.firstOrNull()
    val currentIndex=if(index in a.captions.indices) index else 0
    fun write(c: CaptionTarget) { onUpdate(a.copy(captions=if(item==null) a.captions+c else a.captions.map { if(it.id==item.id) c else it })) }
    EditorPanel(tr("Légende de l’image", "Image caption"), tr("Décrivez uniquement ce qui est visible. Plusieurs langues ou variantes peuvent coexister.", "Describe only what is visible. Multiple languages or variants can coexist.")) {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            a.captions.forEachIndexed { i,c -> FilterChip(selected=i==currentIndex,onClick={index=i},label={Text("${i+1} · ${c.language}")}) }
            AssistChip(onClick={index=a.captions.size;onUpdate(a.copy(captions=a.captions+CaptionTarget(newId(),"",defaultLanguage,isHumanVerified=true)))},label={Text(stringResource(R.string.editor_add_variant))})
        }
        OutlinedTextField(value=item?.text ?: "",onValueChange={write((item ?: CaptionTarget(newId(),"",defaultLanguage,isHumanVerified=true)).copy(text=it,isHumanVerified=true))},label={Text(stringResource(R.string.editor_description))},minLines=3,maxLines=8,modifier=Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("fr" to tr("Français", "French"),"en" to tr("Anglais", "English")).forEach { (lang,title) -> FilterChip(selected=(item?.language ?: defaultLanguage)==lang,onClick={write((item ?: CaptionTarget(newId(),"",defaultLanguage,isHumanVerified=true)).copy(language=lang))},label={Text(title)}) }
            FilterChip(selected=item?.isDetailed==true,onClick={write((item ?: CaptionTarget(newId(),"",defaultLanguage,isHumanVerified=true)).copy(isDetailed=item?.isDetailed!=true))},label={Text(stringResource(R.string.editor_detailed))})
        }
        if(item!=null) Text(reviewStatusLabel(item.isHumanVerified,item.sourceProvenance),
            color=if(item.isHumanVerified)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            style=MaterialTheme.typography.bodySmall)
        if(item!=null) TextButton(onClick={onUpdate(a.copy(captions=a.captions.filterNot { it.id==item.id }));index=0}) { Text(stringResource(R.string.editor_delete_variant)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsEditorTab(a: SampleAnnotations, classes: List<String>, onUpdate: (SampleAnnotations) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    EditorPanel(tr("Classes et tags", "Classes and tags"), tr("Touchez pour ajouter ou retirer une étiquette. Aucune classe n’est déduite automatiquement.", "Tap to add or remove a tag. Classes are never inferred automatically.")) {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            (classes+a.tags.map { it.label }).distinct().forEach { label ->
                val target=a.tags.firstOrNull { it.label==label }
                val selected=target!=null
                FilterChip(selected=selected,
                    leadingIcon=target?.let { value -> { Icon(if(value.isHumanVerified)CadrylIcons.Check else CadrylIcons.AutoAwesome,
                        reviewStatusLabel(value.isHumanVerified,value.sourceProvenance),Modifier.size(16.dp)) } },
                    onClick={onUpdate(a.copy(tags=if(selected) a.tags.filterNot { it.label==label } else a.tags+TagTarget(newId(),label,isHumanVerified=true)))},
                    label={Text(label)})
            }
        }
        OutlinedTextField(draft,{draft=it},label={Text(stringResource(R.string.editor_other_tag))},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedButton(enabled=draft.isNotBlank(),onClick={val label=draft.trim();if(a.tags.none { it.label==label }) onUpdate(a.copy(tags=a.tags+TagTarget(newId(),label,isHumanVerified=true)));draft=""}) { Text(stringResource(R.string.editor_add_tag)) }
        if(a.tags.any { !it.isHumanVerified }) Text(tr("Des tags proposés par le modèle restent à relire via le menu du cas.", "Model-proposed tags still need review in the sample menu."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
    }
}

@Composable
fun VqaEditorTab(a: SampleAnnotations, onUpdate: (SampleAnnotations) -> Unit) {
    EditorPanel(tr("Questions et réponses", "Questions and answers"), tr("L’image reste visible pendant la rédaction. Une abstention est différente d’une réponse vide.", "The image stays visible while writing. Abstaining differs from an empty answer.")) {
        a.vqaList.forEachIndexed { i,q ->
            fun write(v: VqaTarget) { onUpdate(a.copy(vqaList=a.vqaList.map { if(it.id==q.id) v else it })) }
            OutlinedCard {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) { Text("Question ${i+1}",Modifier.weight(1f));IconButton(onClick={onUpdate(a.copy(vqaList=a.vqaList.filterNot { it.id==q.id }))}){Icon(CadrylIcons.DeleteOutline,tr("Supprimer la question ${i+1}", "Delete question ${i+1}"))} }
                    Text(reviewStatusLabel(q.isHumanVerified,q.sourceProvenance),style=MaterialTheme.typography.labelSmall,
                        color=if(q.isHumanVerified)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(q.question,{write(q.copy(question=it,isHumanVerified=true))},label={Text(stringResource(R.string.editor_question))},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(q.answer,{write(q.copy(answer=it,isHumanVerified=true))},label={Text(stringResource(R.string.editor_answer))},enabled=!q.isAbstained,minLines=2,modifier=Modifier.fillMaxWidth())
                    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(q.isAbstained,{write(q.copy(isAbstained=it,isHumanVerified=true))});Text(tr("Indéterminable à partir de l’image", "Cannot be determined from the image"),style=MaterialTheme.typography.bodySmall) }
                }
            }
        }
        Button(onClick={onUpdate(a.copy(vqaList=a.vqaList+VqaTarget(newId(),"","",isHumanVerified=true)))}) { Text(stringResource(R.string.editor_add_question)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroundingEditorTab(a: SampleAnnotations, onUpdate: (SampleAnnotations) -> Unit) {
    EditorPanel(tr("Relier le texte à l’image", "Link text to the image"), tr("Dessinez d’abord les régions dans l’onglet Régions, puis associez-les à une expression.", "Draw regions in the Regions tab first, then link them to an expression.")) {
        a.groundings.forEach { g ->
            fun write(v: GroundingTarget) {onUpdate(a.copy(groundings=a.groundings.map {if(it.id==g.id) v else it}))}
            Text(reviewStatusLabel(g.isHumanVerified,g.sourceProvenance),style=MaterialTheme.typography.labelSmall,
                color=if(g.isHumanVerified)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            OutlinedTextField(g.phrase,{write(g.copy(phrase=it,isHumanVerified=true))},label={Text(stringResource(R.string.editor_grounding_phrase))},modifier=Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                a.boxes.forEachIndexed { i,b -> FilterChip(selected=b.id in g.boxIds,onClick={write(g.copy(boxIds=if(b.id in g.boxIds) g.boxIds-b.id else g.boxIds+b.id,isHumanVerified=true))},label={Text(tr("Boîte ${i+1} · ${b.label}", "Box ${i+1} · ${b.label}"))}) }
                a.points.filter{it.canProvideCoordinates}.forEachIndexed { i,p -> FilterChip(selected=p.id in g.pointIds,onClick={write(g.copy(pointIds=if(p.id in g.pointIds) g.pointIds-p.id else g.pointIds+p.id,isHumanVerified=true))},label={Text("Point ${i+1} · ${p.label}")}) }
            }
            TextButton(onClick={onUpdate(a.copy(groundings=a.groundings.filterNot{it.id==g.id}))}){Text(stringResource(R.string.editor_delete_expression))}
            HorizontalDivider()
        }
        Button(onClick={onUpdate(a.copy(groundings=a.groundings+GroundingTarget(newId(),"",isHumanVerified=true)))}){Text(stringResource(R.string.editor_add_expression))}
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountingEditorTab(a: SampleAnnotations, classes: List<String>, onUpdate: (SampleAnnotations) -> Unit) {
    EditorPanel(tr("Compter les instances", "Count instances"), tr("Zéro est une annotation valide. Indiquez si le comptage est exhaustif.", "Zero is a valid annotation. Specify whether the count is exhaustive.")) {
        a.counts.forEach { c ->
            fun write(v: CountingTarget) {onUpdate(a.copy(counts=a.counts.map{if(it.id==c.id)v else it}))}
            OutlinedCard {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(reviewStatusLabel(c.isHumanVerified,c.sourceProvenance),style=MaterialTheme.typography.labelSmall,
                        color=if(c.isHumanVerified)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) { classes.forEach { label -> FilterChip(selected=c.label==label,onClick={write(c.copy(label=label,isHumanVerified=true))},label={Text(label)}) } }
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        OutlinedIconButton(onClick={write(c.copy(count=(c.count-1).coerceAtLeast(0),isHumanVerified=true))}){Icon(CadrylIcons.Remove,tr("Diminuer le compte", "Decrease count"))}
                        Text("${c.count}",style=MaterialTheme.typography.headlineMedium)
                        OutlinedIconButton(onClick={write(c.copy(count=c.count+1,isHumanVerified=true))}){Icon(CadrylIcons.Add,tr("Augmenter le compte", "Increase count"))}
                        Spacer(Modifier.weight(1f));IconButton(onClick={onUpdate(a.copy(counts=a.counts.filterNot{it.id==c.id}))}){Icon(CadrylIcons.DeleteOutline,tr("Supprimer ce comptage", "Delete this count"))}
                    }
                    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(c.isExhaustive,{write(c.copy(isExhaustive=it,isHumanVerified=true))});Text(tr("Toutes les instances ont été comptées", "All instances have been counted"),style=MaterialTheme.typography.bodySmall)}
                    TextButton(onClick={val ids=a.boxes.filter{it.label==c.label}.map{it.id};write(c.copy(count=ids.size,linkedInstanceIds=ids,isHumanVerified=true))}){Text(stringResource(R.string.editor_count_boxes))}
                }
            }
        }
        Button(onClick={onUpdate(a.copy(counts=a.counts+CountingTarget(newId(),classes.firstOrNull()?:"object",0,isHumanVerified=true)))}){Text(stringResource(R.string.editor_add_count))}
    }
}

@Composable
fun QualityEditorTab(a: SampleAnnotations, onUpdate: (SampleAnnotations) -> Unit) {
    fun write(q: QualityAuditTarget){onUpdate(a.copy(quality=q))}
    EditorPanel(tr("Décision qualité", "Quality decision"), tr("Non annoté, absent, présent mais non localisable et incertain sont des états distincts.", "Unannotated, absent, present but unlocatable, and uncertain are distinct states.")) {
        listOf(
            Triple(tr("Cible absente, après vérification", "Target absent, after verification"),a.quality.verifiedNegativeQueries.isNotEmpty(),0),
            Triple(tr("Négatif difficile (hard negative)", "Hard negative"),a.quality.isHardNegative,1),
            Triple(tr("Cible présente, mais non localisable", "Target present but unlocatable"),a.quality.isUnlocalizablePresent,2),
            Triple(tr("Cas incertain / ambigu", "Uncertain / ambiguous sample"),a.quality.isUncertain,3)
        ).forEach { (label,checked,id) ->
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked,{value->write(when(id){
                    0->a.quality.copy(verifiedNegativeQueries=if(value) listOf(tr("cible du projet", "project target")) else emptyList())
                    1->a.quality.copy(isHardNegative=value)
                    2->a.quality.copy(isUnlocalizablePresent=value)
                    else->a.quality.copy(isUncertain=value)
                })});Text(label,style=MaterialTheme.typography.bodyMedium)
            }
        }
        if(a.quality.verifiedNegativeQueries.isNotEmpty()) OutlinedTextField(a.quality.verifiedNegativeQueries.joinToString(", "),{write(a.quality.copy(verifiedNegativeQueries=it.split(',').map(String::trim).filter(String::isNotBlank)))},label={Text(stringResource(R.string.editor_verified_absences))},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(a.quality.auditNotes,{write(a.quality.copy(auditNotes=it))},label={Text(stringResource(R.string.editor_audit_notes))},minLines=3,modifier=Modifier.fillMaxWidth())
        Text(tr("Un résultat vide du modèle ne justifie pas à lui seul un négatif. Différez le cas lorsque vous ne pouvez pas conclure.", "An empty model result alone does not justify a negative. Defer the sample if you cannot decide."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
