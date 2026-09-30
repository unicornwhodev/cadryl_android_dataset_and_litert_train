#!/usr/bin/env python3
"""Fetch the pinned, licensed design assets used by Cadryl's native UI.

Only the used Phosphor Duotone vectors are vendored. The paths are upstream
artwork; this script does not draw icons or introduce an icon SDK at runtime.
"""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
ICONS_COMMIT = 'd83bea0a9ed83c38c91cb56e927381e1b2943db8'
FONTS_COMMIT = '6cdf01867df0813c2390f90dff7dc66c87f14cf7'
PHOSPHOR_COMMIT = '2b75f3ad12b420c9504ef05df8d2564a28f8500e'
ICON_MAP = dict(
    AccountTree='TreeStructure', Add='Plus', Adjust='CircleDashed', Archive='Archive',
    ArrowDropDown='CaretDown', ArrowForward='ArrowRight', Assignment='ClipboardText',
    AutoAwesome='Sparkle', AutoFixOff='Eraser', Block='Prohibit', BookmarkAdd='BookmarkSimple',
    BrokenImage='ImageBroken', Brush='PaintBrush', Cancel='XCircle', ChangeHistory='Polygon',
    Check='Check', CheckCircle='CheckCircle', CheckCircleOutline='CheckCircle',
    Checklist='ListChecks', ChevronLeft='CaretLeft', ChevronRight='CaretRight', Circle='Circle',
    Close='X', CloudDone='CloudCheck', CloudDownload='CloudArrowDown', CloudUpload='CloudArrowUp',
    Code='Code', ContentCopy='Copy', ContentPaste='Clipboard', CopyAll='Files', CropFree='FrameCorners',
    CropSquare='BoundingBox', DataObject='BracketsCurly', DeleteOutline='Trash', Description='FileText',
    Download='DownloadSimple', Edit='PencilSimple', EditNote='NotePencil', ErrorOutline='WarningCircle',
    ExpandLess='CaretUp', ExpandMore='CaretDown', FactCheck='Exam', FilterAlt='Funnel', FilterAltOff='FunnelX',
    Folder='FolderSimple', FolderOpen='FolderOpen', FolderZip='FileZip', FormatColorFill='PaintBucket',
    Gesture='Lasso', GridView='GridFour', Groups='UsersThree', HelpOutline='Question',
    History='ClockCounterClockwise', Image='Image', ImageSearch='FileMagnifyingGlass', Info='Info',
    InsertDriveFile='File', Insights='ChartLine', Inventory2='Package', IosShare='Export', Key='Key',
    KeyboardArrowDown='CaretDown', KeyboardArrowLeft='CaretLeft', KeyboardArrowRight='CaretRight',
    KeyboardArrowUp='CaretUp', Label='Tag', Layers='Stack', Link='LinkSimple', Lock='LockKey', Memory='Cpu',
    ModelTraining='Brain', MoreVert='DotsThreeVertical', MyLocation='Crosshair', NearMe='Cursor',
    Palette='Palette', PanTool='Hand', PhonelinkSetup='DeviceMobile', PhotoLibrary='Images', PlayArrow='Play',
    RadioButtonChecked='RadioButton', RadioButtonUnchecked='Circle', Refresh='ArrowClockwise', Remove='Minus',
    Schedule='Clock', Science='Flask', Search='MagnifyingGlass', SkipNext='SkipForward', SmartToy='Robot',
    SpaceDashboard='SquaresFour', Speed='Gauge', Stop='Stop', Storage='Database', Sync='ArrowsClockwise',
    TouchApp='HandTap', Tune='FadersHorizontal', UploadFile='FileArrowUp', VerifiedUser='ShieldCheck',
    Visibility='Eye', VisibilityOff='EyeSlash', Widgets='Shapes', ArrowBack='ArrowLeft', Undo='ArrowUUpLeft',
    Redo='ArrowUUpRight',
)


def fetch(url):
    with urlopen(Request(url, headers={'User-Agent': 'Cadryl-design-vendor'}), timeout=60) as response:
        return response.read()


def main():
    base = f'https://raw.githubusercontent.com/adamglin0/compose-phosphor-icon/{ICONS_COMMIT}/'
    fonts = f'https://raw.githubusercontent.com/google/fonts/{FONTS_COMMIT}/ofl/barlow/'
    java = ROOT / 'app/src/main/java'
    manifest = {'schema': 1, 'icon_commit': ICONS_COMMIT, 'font_commit': FONTS_COMMIT, 'files': {}}
    sources = []
    prefix = 'phosphor/src/commonMain/kotlin/com/adamglin/phosphoricons/duotone/'
    for icon in sorted(set(ICON_MAP.values())):
        name = icon[0] + icon[1:].lower() + '.kt'
        sources.append((base + prefix + name, java / 'com/adamglin/phosphoricons/duotone' / name))
    for weight in ('Regular', 'Medium', 'SemiBold', 'Bold'):
        resource = {'Regular': 'regular', 'Medium': 'medium', 'SemiBold': 'semibold', 'Bold': 'bold'}[weight]
        sources.append((fonts + f'Barlow-{weight}.ttf', ROOT / f'app/src/main/res/font/barlow_{resource}.ttf'))
    sources += [
        (base + 'LICENSE', ROOT / 'third_party/design/LICENSE.compose-phosphor-icon'),
        (fonts + 'OFL.txt', ROOT / 'third_party/design/OFL.barlow.txt'),
        (f'https://raw.githubusercontent.com/phosphor-icons/core/{PHOSPHOR_COMMIT}/LICENSE', ROOT / 'third_party/design/LICENSE.phosphor'),
    ]

    def install(source):
        url, path = source
        upstream = fetch(url)
        content = upstream
        if path.suffix == '.kt' and path.stem.lower() in {'arrowleft', 'arrowright', 'arrowuupleft', 'arrowuupright'}:
            # Match Android's RTL navigation behavior without changing artwork.
            content = upstream.replace(b'viewportHeight = 256.0f', b'viewportHeight = 256.0f, autoMirror = true')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
        return path.relative_to(ROOT).as_posix(), {
            'url': url, 'upstream_sha256': hashlib.sha256(upstream).hexdigest(),
            'sha256': hashlib.sha256(content).hexdigest(),
            **({'adaptation': 'autoMirror=true for RTL navigation'} if content != upstream else {}),
        }

    with ThreadPoolExecutor(max_workers=8) as pool:
        for path, receipt in pool.map(install, sources):
            manifest['files'][path] = receipt
    registry = java / 'com/adamglin/PhosphorIcons.kt'
    registry.parent.mkdir(parents=True, exist_ok=True)
    registry.write_text('''// Minimal registry for the used upstream MIT-licensed icons.
package com.adamglin
public object PhosphorIcons
''', encoding='utf-8')
    group = java / 'com/adamglin/phosphoricons/DuotoneGroup.kt'
    group.write_text('''// Minimal registry; unused upstream AllIcons aggregation is omitted.
package com.adamglin.phosphoricons
import com.adamglin.PhosphorIcons
public object DuotoneGroup
public val PhosphorIcons.Duotone: DuotoneGroup get() = DuotoneGroup
''', encoding='utf-8')
    adapter = java / 'com/unicornwhodev/visiondatasetstudio/ui/icons/CadrylIcons.kt'
    adapter.parent.mkdir(parents=True, exist_ok=True)
    lines = ['package com.unicornwhodev.visiondatasetstudio.ui.icons', '',
             'import androidx.compose.ui.graphics.vector.ImageVector', 'import com.adamglin.PhosphorIcons',
             'import com.adamglin.phosphoricons.Duotone', 'import com.adamglin.phosphoricons.duotone.*', '',
             '/** Semantic names keep one Phosphor Duotone family throughout the studio. */', 'object CadrylIcons {']
    lines += [f'    val {name}: ImageVector get() = PhosphorIcons.Duotone.{icon}' for name, icon in ICON_MAP.items()]
    adapter.write_text('\n'.join(lines + ['}', '']), encoding='utf-8')
    for generated in (registry, group):
        manifest['files'][generated.relative_to(ROOT).as_posix()] = {
            'source': 'Minimal registry for the pinned MIT-licensed Phosphor vectors',
            'sha256': hashlib.sha256(generated.read_bytes()).hexdigest(),
        }
    dest = ROOT / 'third_party/design/manifest.json'
    logo = ROOT / 'app/src/main/res/drawable-nodpi/ic_cadryl.png'
    if logo.is_file():
        manifest['files'][logo.relative_to(ROOT).as_posix()] = {
            'source': 'Owner-supplied and selected Cadryl lynx emblem; original bitmap',
            'sha256': hashlib.sha256(logo.read_bytes()).hexdigest(),
        }
    dest.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'icons': len(set(ICON_MAP.values())), 'files': len(manifest['files']), 'manifest': dest.relative_to(ROOT).as_posix()}))


if __name__ == '__main__':
    main()
