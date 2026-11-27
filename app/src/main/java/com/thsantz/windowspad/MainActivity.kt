package com.thsantz.windowspad

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PadApp(val name: String, val pkg: String, val icon: Drawable)

class MainActivity : ComponentActivity() {
    private val apps = mutableStateListOf<PadApp>()
    private val recentPackages = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadApps()
        val prefs = getSharedPreferences("windowspad", MODE_PRIVATE)
        recentPackages.clear()
        recentPackages.addAll(prefs.getString("recent_packages", "").orEmpty().split("|").filter { it.isNotBlank() }.take(12))
        setContent {
            var dex by remember { mutableStateOf(prefs.getBoolean("dex_mode", false)) }
            var drawer by remember { mutableStateOf(false) }
            var assistant by remember { mutableStateOf(false) }
            val dark = isSystemInDarkTheme()
            val bg = if (dark) Color(0xFF10151F) else Color(0xFFF3F5F9)
            val ink = if (dark) Color(0xFFF2F5FC) else Color(0xFF17243A)
            val muted = if (dark) Color(0xFF9BAAC0) else Color(0xFF687991)
            val card = if (dark) Color(0xFF1B2535) else Color.White
            val accent = Color(0xFF3979E8)
            BackHandler(drawer || assistant) { drawer = false; assistant = false }
            MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = accent) else lightColorScheme(primary = accent)) {
                Surface(Modifier.fillMaxSize(), color = bg) {
                    if (dex) {
                        DexDesktop(apps, recentPackages, ink, muted, accent,
                            onOpen = ::openApp,
                            onAssistant = { assistant = true },
                            onTablet = { dex = false; prefs.edit().putBoolean("dex_mode", false).apply() },
                            onSettings = { openSettings(Settings.ACTION_SETTINGS) })
                    } else {
                        TabletHome(apps, recentPackages, card, ink, muted, accent,
                            onOpen = ::openApp, onDrawer = { drawer = true },
                            onAssistant = { assistant = true },
                            onDex = { dex = true; prefs.edit().putBoolean("dex_mode", true).apply() },
                            onSettings = { openSettings(Settings.ACTION_SETTINGS) })
                    }
                    if (drawer) AppDrawer(apps, ink, muted, card, { drawer = false }, { drawer = false; openApp(it) })
                    if (assistant) AssistantSheet(ink, muted, card, accent,
                        onClose = { assistant = false },
                        onCommand = { command -> assistant = false; executeCommand(command) { toDex ->
                            dex = toDex; prefs.edit().putBoolean("dex_mode", toDex).apply()
                        } },
                        onVoice = ::launchVoice)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {
        apps.clear()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL).forEach {
            val p = it.activityInfo.packageName
            if (p != packageName) apps.add(PadApp(it.loadLabel(packageManager).toString(), p, it.loadIcon(packageManager)))
        }
        apps.sortBy { it.name.lowercase(Locale.getDefault()) }
    }

    private fun openApp(app: PadApp) {
        recentPackages.remove(app.pkg)
        recentPackages.add(0, app.pkg)
        while (recentPackages.size > 12) recentPackages.removeAt(recentPackages.lastIndex)
        getSharedPreferences("windowspad", MODE_PRIVATE).edit()
            .putString("recent_packages", recentPackages.joinToString("|")).apply()
        packageManager.getLaunchIntentForPackage(app.pkg)?.let { startActivity(it) }
    }

    private fun openSettings(action: String) {
        try { startActivity(Intent(action)) } catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun launchVoice() {
        try {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga o que deseja fazer")
            }
            @Suppress("DEPRECATION")
            startActivityForResult(i, 41)
        } catch (_: Exception) {}
    }

    @Deprecated("Android speech activity compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) executeCommand(spoken) {}
        }
    }

    private fun executeCommand(raw: String, onDex: (Boolean) -> Unit) {
        val q = raw.trim().lowercase(Locale("pt", "BR"))
        when {
            q.contains("dex") || q.contains("desktop") -> onDex(true)
            q.contains("tablet") || q.contains("início") -> onDex(false)
            q.contains("configura") -> openSettings(Settings.ACTION_SETTINGS)
            q.contains("wi-fi") || q.contains("wifi") -> openSettings(Settings.ACTION_WIFI_SETTINGS)
            q.contains("bluetooth") -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            else -> apps.firstOrNull { it.name.contains(q, true) }?.let(::openApp)
        }
    }
}

@Composable
private fun AppIcon(app: PadApp, size: Int, modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier.size(size.dp), factory = { ctx ->
        ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setImageDrawable(app.icon) }
    }, update = { it.setImageDrawable(app.icon) })
}

@Composable
private fun AppTile(app: PadApp, ink: Color, onClick: () -> Unit, desktop: Boolean = false) {
    Column(Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(app, if (desktop) 42 else 44)
        Spacer(Modifier.height(8.dp))
        Text(app.name, color = if (desktop) Color.White else ink, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp)
    }
}

@Composable
private fun TabletHome(
    apps: List<PadApp>, recentPackages: List<String>, card: Color, ink: Color, muted: Color, accent: Color,
    onOpen: (PadApp) -> Unit, onDrawer: () -> Unit, onAssistant: () -> Unit, onDex: () -> Unit, onSettings: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 760.dp
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = if (wide) 26.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth().height(if (wide) 76.dp else 68.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("WINDOWS PAD", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Bom ter você aqui.", color = ink, fontSize = if (wide) 24.sp else 21.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(onClick = onDex, shape = CircleShape, color = card) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DesktopWindows, null, tint = accent, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp)); Text("Modo DeX", color = ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Column(Modifier.weight(1f)) {
                        HomeHero(accent)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            FeatureCard("Windows Pad AI", "Pesquisa e comandos", "✦", accent, card, ink, muted, Modifier.weight(1f), onAssistant)
                            FeatureCard("Aplicativos", "Toda a sua biblioteca", "▦", accent, card, ink, muted, Modifier.weight(1f), onDrawer)
                        }
                        Spacer(Modifier.height(16.dp))
                        AppSection(apps, ink, accent, onOpen, onDrawer, Modifier.weight(1f))
                    }
                    Column(Modifier.width(210.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("CENTRAL", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        SideCard("Aparência", "Tema e personalização", Icons.Default.DarkMode, card, ink, muted, onSettings)
                        SideCard("Conectividade", "Wi-Fi e Bluetooth", Icons.Default.Wifi, card, ink, muted, onSettings)
                        SideCard("Configurações", "Preferências do tablet", Icons.Default.Settings, card, ink, muted, onSettings)
                        Spacer(Modifier.weight(1f))
                        Text("Windows Pad  •  0.3.0", color = muted, fontSize = 10.sp)
                    }
                }
                HomeDock(apps, recentPackages, card, accent, onOpen, onDrawer)
            } else {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    HomeHero(accent, compact = true)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard("Windows Pad AI", "Pesquisa e comandos", "✦", accent, card, ink, muted, Modifier.weight(1f), onAssistant)
                        FeatureCard("Aplicativos", "Toda a sua biblioteca", "▦", accent, card, ink, muted, Modifier.weight(1f), onDrawer)
                    }
                    Spacer(Modifier.height(18.dp))
                    AppSection(apps, ink, accent, onOpen, onDrawer, Modifier.fillMaxWidth(), compact = true)
                    Spacer(Modifier.height(18.dp))
                    Text("CENTRAL", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(10.dp))
                    SideCard("Aparência", "Tema e personalização", Icons.Default.DarkMode, card, ink, muted, onSettings)
                    Spacer(Modifier.height(9.dp))
                    SideCard("Conectividade", "Wi-Fi e Bluetooth", Icons.Default.Wifi, card, ink, muted, onSettings)
                    Spacer(Modifier.height(9.dp))
                    SideCard("Configurações", "Preferências do tablet", Icons.Default.Settings, card, ink, muted, onSettings)
                    Spacer(Modifier.height(12.dp))
                    Text("Windows Pad  •  0.3.0", color = muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(12.dp))
                }
                HomeDock(apps, recentPackages, card, accent, onOpen, onDrawer)
            }
        }
    }
}

@Composable
private fun HomeHero(accent: Color, compact: Boolean = false) {
    Box(Modifier.fillMaxWidth().height(if (compact) 172.dp else 190.dp).clip(RoundedCornerShape(28.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF163B78), Color(0xFF3478D5), Color(0xFF76A9F0)))).padding(if (compact) 18.dp else 24.dp)) {
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.78f)) {
            Text("UM ESPAÇO PARA TUDO", color = Color(0xFFC9DEFF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(8.dp))
            Text("Seu tablet.\nSua experiência.", color = Color.White, fontSize = if (compact) 23.sp else 27.sp, fontWeight = FontWeight.Bold, lineHeight = if (compact) 27.sp else 32.sp)
            Spacer(Modifier.height(7.dp))
            Text("Produtividade e entretenimento em harmonia.", color = Color.White.copy(.85f), fontSize = 11.sp, maxLines = 2)
        }
        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), Modifier.align(Alignment.TopEnd), color = Color.White, fontSize = if (compact) 25.sp else 30.sp, fontWeight = FontWeight.Light)
        Text(SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date()).replaceFirstChar { it.uppercase() }, Modifier.align(Alignment.BottomEnd), color = Color.White.copy(.8f), fontSize = 10.sp)
    }
}

@Composable
private fun AppSection(
    apps: List<PadApp>, ink: Color, accent: Color, onOpen: (PadApp) -> Unit, onDrawer: () -> Unit,
    modifier: Modifier, compact: Boolean = false
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Seus aplicativos", color = ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("VER TODOS  ›", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onDrawer() })
        }
        Spacer(Modifier.height(8.dp))
        if (compact) {
            LazyVerticalGrid(columns = GridCells.Adaptive(76.dp), modifier = Modifier.fillMaxWidth().height(270.dp), userScrollEnabled = false) {
                items(apps.take(12), key = { it.pkg }) { AppTile(it, ink, onClick = { onOpen(it) }) }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(84.dp), modifier = Modifier.fillMaxSize()) {
                items(apps.take(14), key = { it.pkg }) { AppTile(it, ink, onClick = { onOpen(it) }) }
            }
        }
    }
}

@Composable
private fun HomeDock(apps: List<PadApp>, recentPackages: List<String>, card: Color, accent: Color, onOpen: (PadApp) -> Unit, onDrawer: () -> Unit) {
    val quickApps = (recentPackages.mapNotNull { pkg -> apps.firstOrNull { it.pkg == pkg } } + apps).distinctBy { it.pkg }.take(6)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(60.dp).clip(RoundedCornerShape(22.dp)).background(card).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        quickApps.forEach { app -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { AppIcon(app, 32, Modifier.clickable { onOpen(app) }) } }
        Icon(Icons.Default.Apps, "Todos os aplicativos", tint = accent, modifier = Modifier.size(27.dp).clickable { onDrawer() })
    }
}

@Composable
private fun FeatureCard(title: String, sub: String, symbol: String, accent: Color, card: Color, ink: Color, muted: Color, modifier: Modifier, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier.height(82.dp), color = card, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.Center) {
            Text("$symbol   $title", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp)); Text(sub, color = muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SideCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, card: Color, ink: Color, muted: Color, click: () -> Unit) {
    Surface(onClick = click, color = card, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF3979E8), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(title, color = ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text(sub, color = muted, fontSize = 10.sp) }
            Icon(Icons.Default.ChevronRight, null, tint = muted, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun DexDesktop(
    apps: List<PadApp>, recentPackages: List<String>, ink: Color, muted: Color, accent: Color,
    onOpen: (PadApp) -> Unit, onAssistant: () -> Unit, onTablet: () -> Unit, onSettings: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var startOpen by remember { mutableStateOf(false) }
    var panelOpen by remember { mutableStateOf(false) }
    val desktopApps = (recentPackages.mapNotNull { pkg -> apps.firstOrNull { it.pkg == pkg } } + apps).distinctBy { it.pkg }.take(12)
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().background(Brush.linearGradient(listOf(Color(0xFF08172F), Color(0xFF173C68), Color(0xFF245B8C))))) {
        Row(Modifier.fillMaxSize().padding(start = 22.dp, top = 20.dp, bottom = 82.dp)) {
            Column(Modifier.width(104.dp)) { desktopApps.forEach { AppTile(it, Color.White, { onOpen(it) }, true) } }
            Column(Modifier.fillMaxSize().padding(start = 20.dp)) {
                Text("WINDOWS PAD", color = Color.White.copy(.75f), fontSize = 12.sp, letterSpacing = 2.sp)
                Text("Área de trabalho", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Light)
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(18.dp).clip(CircleShape).background(Color.White.copy(.12f)).clickable { onAssistant() }.padding(horizontal = 14.dp, vertical = 9.dp)) {
            Text("✦  Windows Pad AI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        AnimatedVisibility(startOpen, Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 76.dp)) {
            Surface(Modifier.width(340.dp).heightIn(max = 540.dp), color = Color(0xF21A2639), shape = RoundedCornerShape(24.dp), shadowElevation = 18.dp) {
                Column(Modifier.padding(18.dp)) {
                    Text("Iniciar", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), placeholder = { Text("Pesquisar aplicativos") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.heightIn(max = 390.dp)) {
                        items(apps.filter { it.name.contains(search, true) }.take(28), key = { it.pkg }) { AppTile(it, Color.White, onClick = { onOpen(it); startOpen = false }) }
                    }
                }
            }
        }
        AnimatedVisibility(panelOpen, Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 76.dp)) {
            Surface(color = Color(0xF21A2639), shape = RoundedCornerShape(24.dp), shadowElevation = 18.dp) {
                Column(Modifier.width(290.dp).padding(18.dp)) {
                    Text("Central de controle", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    QuickControl("Wi-Fi", Icons.Default.Wifi, onSettings)
                    Spacer(Modifier.height(8.dp))
                    QuickControl("Bluetooth", Icons.Default.Bluetooth, onSettings)
                    Spacer(Modifier.height(8.dp))
                    QuickControl("Configurações", Icons.Default.Settings, onSettings)
                    Spacer(Modifier.height(8.dp))
                    QuickControl("Modo tablet", Icons.Default.Home, onTablet)
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            .height(58.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xE8172438)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Home, "Iniciar", tint = Color(0xFF83B6FF), modifier = Modifier.size(29.dp).clickable { startOpen = !startOpen; panelOpen = false })
            Spacer(Modifier.width(10.dp))
            Text("Pesquisar", color = Color.White.copy(.7f), fontSize = 12.sp, modifier = Modifier.weight(1f).clickable { startOpen = true; panelOpen = false })
            desktopApps.take(5).forEach { app -> AppIcon(app, 32, Modifier.padding(horizontal = 7.dp).clickable { onOpen(app) }) }
            Icon(Icons.Default.Notifications, "Central de controle", tint = Color.White, modifier = Modifier.padding(horizontal = 8.dp).size(22.dp).clickable { panelOpen = !panelOpen; startOpen = false })
            Column(Modifier.clickable { panelOpen = !panelOpen }, horizontalAlignment = Alignment.End) {
                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), color = Color.White, fontSize = 12.sp)
                Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()), color = Color.White.copy(.7f), fontSize = 9.sp)
            }
            Icon(Icons.Default.PowerSettingsNew, "Modo tablet", tint = Color.White, modifier = Modifier.padding(start = 12.dp).size(20.dp).clickable { onTablet() })
        }
    }
}

@Composable
private fun QuickControl(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    Surface(onClick = click, color = Color(0xFF2A3D58), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF8DBBFF)); Spacer(Modifier.width(10.dp)); Text(label, color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AppDrawer(apps: List<PadApp>, ink: Color, muted: Color, card: Color, onDismiss: () -> Unit, onOpen: (PadApp) -> Unit) {
    var search by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.48f)), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth(.86f).fillMaxHeight(.88f), color = card, shape = RoundedCornerShape(28.dp), shadowElevation = 20.dp) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Aplicativos", color = ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("FECHAR", color = Color(0xFF3979E8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onDismiss() })
                }
                OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), singleLine = true, placeholder = { Text("Buscar um aplicativo") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                val filtered = apps.filter { it.name.contains(search, true) }
                Text("${filtered.size} aplicativos", color = muted, fontSize = 11.sp)
                LazyVerticalGrid(columns = GridCells.Adaptive(92.dp), modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                    items(filtered, key = { it.pkg }) { AppTile(it, ink, onClick = { onOpen(it) }) }
                }
            }
        }
    }
}

@Composable
private fun AssistantSheet(ink: Color, muted: Color, card: Color, accent: Color, onClose: () -> Unit, onCommand: (String) -> Unit, onVoice: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.48f)), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth(.76f), color = card, shape = RoundedCornerShape(28.dp), shadowElevation = 20.dp) {
            Column(Modifier.padding(26.dp)) {
                Text("✦  WINDOWS PAD AI", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(8.dp))
                Text("O que você precisa?", color = ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Encontre apps e execute comandos no tablet.", color = muted, fontSize = 13.sp)
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), placeholder = { Text("Ex.: abrir YouTube") }, singleLine = true)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onVoice, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(6.dp)); Text("Falar") }
                    Button(onClick = { onCommand(text) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(6.dp)); Text("Executar") }
                }
                Spacer(Modifier.height(16.dp))
                Text("ATALHOS", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                listOf("Abrir configurações", "Ativar modo DeX", "Voltar ao modo tablet", "Abrir Wi-Fi").forEach {
                    Row(Modifier.fillMaxWidth().clickable { onCommand(it) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it, color = ink, fontSize = 13.sp, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = muted)
                    }
                }
                Text("Comandos locais. IA generativa ainda não conectada.", color = muted, fontSize = 10.sp)
                Text("Fechar", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End).clickable { onClose() }.padding(top = 12.dp))
            }
        }
    }
}
