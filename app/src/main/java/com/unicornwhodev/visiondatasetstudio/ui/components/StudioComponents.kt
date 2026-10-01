package com.unicornwhodev.visiondatasetstudio.ui.components

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.unicornwhodev.visiondatasetstudio.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.unicornwhodev.visiondatasetstudio.ui.icons.CadrylIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import com.unicornwhodev.visiondatasetstudio.ui.OperationProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioTopBar(title: String, eyebrow: String? = null, onBack: (() -> Unit)? = null,
                 actions: @Composable RowScope.() -> Unit = {}) {
    Column {
        StudioBrandBar()
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = if(onBack==null) 20.dp else 4.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(CadrylIcons.ArrowBack, tr("Retour", "Back"), Modifier.size(22.dp)) }
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(title, modifier = Modifier.testTag("screen_title"), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (eyebrow != null) Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun StudioBrandBar() {
    val openTools = LocalStudioToolbox.current
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().heightIn(min=64.dp).padding(horizontal=20.dp,vertical=4.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_cadryl),stringResource(R.string.app_name),Modifier.size(44.dp),tint=Color.Unspecified)
            Spacer(Modifier.width(6.dp))
            Text("cadryl",style=MaterialTheme.typography.headlineLarge.copy(fontSize=30.sp,fontWeight=FontWeight.SemiBold),
                color=MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            if(openTools!=null) OutlinedButton(onClick=openTools,modifier=Modifier.testTag("studio_tools"),
                shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
                contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp),
                colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.onSurface)) {
                Icon(CadrylIcons.Tune,null,Modifier.size(22.dp));Spacer(Modifier.width(8.dp));Text(tr("Outils","Tools"))
            }
        }
        Box(Modifier.fillMaxWidth().height(4.dp).padding(horizontal=20.dp)) {
            HorizontalDivider(Modifier.align(Alignment.Center),color=MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.align(Alignment.CenterStart).offset(x=70.dp).size(width=12.dp,height=2.dp)
                .background(MaterialTheme.colorScheme.onSurface))
            Box(Modifier.align(Alignment.CenterEnd).offset(x=(-38).dp).size(width=38.dp,height=2.dp)
                .background(MaterialTheme.colorScheme.primary))
        }
    }
}

/** Primary commands have one continuous visual and touch surface. */
@Composable
fun StudioAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                 icon: ImageVector? = null, enabled: Boolean = true, primary: Boolean = false) {
    if (primary) Button(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=50.dp),
        shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(horizontal=18.dp,vertical=12.dp)) {
        if(icon!=null) { Icon(icon,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp)) }
        Text(label,style=MaterialTheme.typography.labelLarge)
    } else FilledTonalButton(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=50.dp),
        shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp)) {
        if(icon!=null) { Icon(icon,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp)) }
        Text(label,style=MaterialTheme.typography.labelLarge)
    }
}

/** One content tree: route changes never duplicate editors or their saved drafts. */
@Composable
fun StudioRouteMotion(route: String, content: @Composable () -> Unit) {
    val enter = remember { Animatable(1f) }
    val offset = with(LocalDensity.current) { 4.dp.toPx() }
    LaunchedEffect(route) { enter.snapTo(0f); enter.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = enter.value; translationY = offset * (1f - enter.value) }) { content() }
}

@Composable
fun StudioSection(title: String, subtitle: String? = null, icon: ImageVector? = null,
                  modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    var help by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,
        color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                if(icon!=null) Icon(icon,null,Modifier.size(24.dp),tint=MaterialTheme.colorScheme.onSurface)
                Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                if(subtitle!=null) IconButton(onClick={help=!help}) {
                    Icon(CadrylIcons.HelpOutline,tr("Aide : $title","Help: $title"),Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AnimatedVisibility(help && subtitle!=null) {
                Text(subtitle.orEmpty(),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
fun StudioDisclosure(title: String, icon: ImageVector = CadrylIcons.Tune, initiallyExpanded: Boolean = false,
                     keepContent: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "disclosure")
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = if (expanded) tr("Déplié", "Expanded") else tr("Replié", "Collapsed") }.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Icon(CadrylIcons.ExpandMore, null, Modifier.size(18.dp).graphicsLayer { rotationZ = rotation })
            }
            if(keepContent) {
                // Keep unsaved form state while the user folds the options.
                Column(if(expanded) Modifier.padding(start=14.dp,end=14.dp,bottom=14.dp)
                    else Modifier.height(0.dp).clipToBounds().clearAndSetSemantics {},
                    verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
            } else AnimatedVisibility(expanded, enter = expandVertically(tween(200)) + fadeIn(), exit = shrinkVertically(tween(160)) + fadeOut()) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            }
        }
    }
}

@Composable
fun StudioTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement=Arrangement.spacedBy(2.dp)) {
        labels.forEachIndexed { index,label ->
            val active=index==selected
            Column(Modifier.width(IntrinsicSize.Min).clickable(role=Role.Tab) { onSelect(index) }.semantics { this.selected=active }) {
                Box(Modifier.heightIn(min=48.dp).padding(horizontal=12.dp,vertical=12.dp),contentAlignment=Alignment.Center) {
                    Text(label,style=MaterialTheme.typography.labelLarge,
                        color=if(active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(if(active) MaterialTheme.colorScheme.primary else Color.Transparent))
            }
        }
    }
}

@Composable
fun StatusPill(text: String, icon: ImageVector = CadrylIcons.Circle, attention: Boolean = false) {
    Surface(shape = RoundedCornerShape(50), color = if (attention) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(11.dp), tint = if(attention) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MetricTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AnimatedContent(value, label = "metric") { Text(it, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OperationBanner(progress: OperationProgress?, busy: Boolean, onDismiss: () -> Unit) {
    if (busy) {
        OperationProgressWindow(progress)
        return
    }
    if (progress == null) return
    var expanded by remember(progress.message) { mutableStateOf(false) }
    Surface(color = if (progress.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (progress.isError) CadrylIcons.ErrorOutline else if (busy) CadrylIcons.Sync else CadrylIcons.CheckCircleOutline, null, Modifier.size(20.dp))
                Text(progress.message, Modifier.weight(1f).clickable { expanded = !expanded }, maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                if (!busy) IconButton(onClick = onDismiss) { Icon(CadrylIcons.Close, tr("Fermer le message", "Dismiss message")) }
            }
            if (busy) {
                if (progress.total > 1) LinearProgressIndicator(progress = { (progress.current.toFloat() / progress.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun OperationProgressWindow(progress: OperationProgress?) {
    val total = progress?.total ?: 0
    val current = (progress?.current ?: 0).coerceIn(0, total.coerceAtLeast(0))
    val determinate = total > 1
    val fraction = if (determinate) current.toFloat() / total else 0f
    Dialog(onDismissRequest = {}, properties = DialogProperties(
        dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(24.dp).widthIn(max = 480.dp).fillMaxWidth()
            .testTag("operation_progress_window").semantics { liveRegion = LiveRegionMode.Polite },
            shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(Modifier.size(40.dp), strokeWidth = 4.dp)
                    Text(tr("Opération en cours", "Operation in progress"), style = MaterialTheme.typography.titleLarge)
                }
                Text(progress?.message?.takeIf { it.isNotBlank() } ?: tr("Préparation…", "Preparing…"),
                    style = MaterialTheme.typography.bodyLarge)
                if (determinate) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$current / $total", style = MaterialTheme.typography.titleMedium)
                        Text("${(fraction * 100).toInt()} %", style = MaterialTheme.typography.titleMedium)
                    }
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(8.dp))
                } else LinearProgressIndicator(Modifier.fillMaxWidth().height(8.dp))
            }
        }
    }
}

@Composable
fun EmptyWorkspace(title: String, message: String, icon: ImageVector, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, null, Modifier.padding(16.dp).size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
    }
}

val LocalStudioGuidance = staticCompositionLocalOf { true }

/** Optional inline guidance; confirmations and section help remain accessible. */
@Composable
fun StudioDetails(text: String, modifier: Modifier = Modifier,
                  style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall,
                  color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    if (!LocalStudioGuidance.current) return
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    Column(modifier.animateContentSize()) {
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Icon(if (expanded) CadrylIcons.ExpandLess else CadrylIcons.Info, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp)); Text(if (expanded) tr("Moins de détails", "Less detail") else tr("En savoir plus", "Learn more"))
        }
        AnimatedVisibility(expanded) { Text(text, style = style, color = color) }
    }
}
