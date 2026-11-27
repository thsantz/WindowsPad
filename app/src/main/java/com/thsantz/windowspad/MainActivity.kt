package com.thsantz.windowspad

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import android.content.pm.PackageManager
import java.util.Locale

class MainActivity : Activity() {
    data class AppEntry(val label: String, val packageName: String, val icon: Drawable?)
    private val apps = mutableListOf<AppEntry>()
    private val prefs by lazy { getSharedPreferences("windowspad", MODE_PRIVATE) }
    private var dexMode = false
    private var dark = false
    private val accent = Color.rgb(55, 126, 245)
    private val bg get() = if (dark) Color.rgb(12, 17, 28) else Color.rgb(244, 246, 251)
    private val surface get() = if (dark) Color.rgb(27, 35, 50) else Color.WHITE
    private val surface2 get() = if (dark) Color.rgb(37, 47, 65) else Color.rgb(235, 240, 248)
    private val fg get() = if (dark) Color.rgb(244, 247, 255) else Color.rgb(27, 36, 54)
    private val muted get() = if (dark) Color.rgb(164, 178, 201) else Color.rgb(111, 124, 146)
    private val dp get() = resources.displayMetrics.density
    private fun d(v: Int) = (v * dp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        dexMode = prefs.getBoolean("dex_mode", false)
        loadApps()
        render()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {
        val query = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        apps.clear()
        packageManager.queryIntentActivities(query, PackageManager.MATCH_ALL).forEach {
            val info = it.activityInfo
            if (info.packageName != packageName) apps.add(AppEntry(it.loadLabel(packageManager).toString(), info.packageName, it.loadIcon(packageManager)))
        }
        apps.sortBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun shape(color: Int, radius: Int = 22, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = d(radius).toFloat()
            if (stroke != 0) setStroke(d(1), stroke)
        }

    private fun gradient(colors: IntArray, radius: Int = 26) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply { cornerRadius = d(radius).toFloat() }

    private fun label(value: String, size: Float, bold: Boolean = false, color: Int = fg): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun params(w: Int, h: Int, weight: Float = 0f) =
        LinearLayout.LayoutParams(if (w < 0) w else d(w), if (h < 0) h else d(h), weight)

    private fun openApp(app: AppEntry) {
        val launch = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launch != null) startActivity(launch)
        else Toast.makeText(this, "Não foi possível abrir " + app.label, Toast.LENGTH_SHORT).show()
    }

    private fun render() {
        window.statusBarColor = if (dexMode) Color.rgb(10, 19, 34) else bg
        window.navigationBarColor = if (dexMode) Color.rgb(10, 19, 34) else bg
        window.decorView.systemUiVisibility = if (!dark && !dexMode) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
        if (dexMode) buildDex() else buildTablet()
    }

    private fun switchMode() {
        dexMode = !dexMode
        prefs.edit().putBoolean("dex_mode", dexMode).apply()
        render()
    }

    private fun modeButton(isDex: Boolean): TextView = label(if (isDex) "▦  Modo tablet" else "▣  Modo DeX", 12f, true, if (isDex) Color.WHITE else fg).apply {
        gravity = Gravity.CENTER
        background = shape(if (isDex) Color.argb(58, 255, 255, 255) else surface2, 20)
        setPadding(d(15), 0, d(15), 0)
        setOnClickListener { switchMode() }
    }

    private fun sectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label(text, 18f, true), params(0, 46, 1f))
            if (action != null) addView(label(action, 12f, true, accent).apply {
                gravity = Gravity.CENTER
                setOnClickListener { onAction?.invoke() }
            }, params(-2, 42))
        }

    private fun buildTablet() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(22), d(18), d(22), d(10))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("WINDOWS PAD", 11f, true, accent))
        brand.addView(label("Seu espaço. Seu ritmo.", 22f, true))
        header.addView(brand, params(0, 60, 1f))
        header.addView(modeButton(false), params(-2, 42))
        root.addView(header)

        val scroll = ScrollView(this).apply { clipToPadding = false }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = gradient(intArrayOf(Color.rgb(24, 58, 116), Color.rgb(48, 105, 190), Color.rgb(91, 145, 224)), 28)
            setPadding(d(22), d(18), d(18), d(18))
        }
        val heroInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heroInfo.addView(label("SEU TABLET, DO SEU JEITO", 10f, true, Color.rgb(211, 228, 255)))
        heroInfo.addView(label("Tudo conectado.\nTudo no seu ritmo.", 23f, true, Color.WHITE).apply { setLineSpacing(0f, 1.04f) })
        heroInfo.addView(label("Acesse seus apps e ideias em um só lugar.", 11f, false, Color.rgb(225, 237, 255)))
        hero.addView(heroInfo, params(0, -2, 1f))
        hero.addView(TextClock(this).apply {
            format24Hour = "HH:mm"; format12Hour = "h:mm"
            textSize = 39f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        }, params(100, 76))
        val hp = params(-1, 142); hp.topMargin = d(14); content.addView(hero, hp)

        val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        quick.addView(actionCard("✦", "Windows Pad AI", "Ajuda e comandos") { showAiPanel() }, params(0, 82, 1f))
        val spacer = View(this); quick.addView(spacer, params(10, 1))
        quick.addView(actionCard("⌕", "Pesquisar", "Apps e atalhos") { showAppDrawer() }, params(0, 82, 1f))
        val qp = params(-1, 82); qp.topMargin = d(12); content.addView(quick, qp)

        content.addView(sectionTitle("Seus aplicativos", "Ver todos  ›") { showAppDrawer() })
        val grid = GridLayout(this).apply { columnCount = if (resources.configuration.screenWidthDp >= 850) 7 else 5 }
        apps.take(15).forEach { grid.addView(appTile(it, false), gridParams(grid.columnCount, 86)) }
        content.addView(grid)

        content.addView(sectionTitle("Personalize seu espaço"))
        val settingsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        settingsRow.addView(actionCard("☼", "Aparência", "Tema automático") { showAppearanceInfo() }, params(0, 78, 1f))
        val gap = View(this); settingsRow.addView(gap, params(9, 1))
        settingsRow.addView(actionCard("▤", "Organização", "Todos os aplicativos") { showAppDrawer() }, params(0, 78, 1f))
        val srp = params(-1, 78); srp.bottomMargin = d(10); settingsRow.layoutParams = srp
        content.addView(settingsRow)
        scroll.addView(content)
        root.addView(scroll, params(-1, 0, 1f))

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            background = shape(surface, 25, if (dark) Color.rgb(48, 59, 78) else Color.rgb(228, 234, 244))
            setPadding(d(8), d(5), d(8), d(5))
        }
        apps.take(5).forEach { app ->
            dock.addView(ImageView(this).apply {
                setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(d(8), d(5), d(8), d(5)); contentDescription = app.label
                setOnClickListener { openApp(app) }
            }, params(48, 46))
        }
        dock.addView(label("☷", 24f, true, accent).apply {
            gravity = Gravity.CENTER; setOnClickListener { showAppDrawer() }
        }, params(44, 46))
        val dpDock = params(-1, 58); dpDock.topMargin = d(8); root.addView(dock, dpDock)
        setContentView(root)
    }

    private fun actionCard(symbol: String, title: String, subtitle: String, action: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            background = shape(surface, 20, if (dark) Color.rgb(48, 59, 78) else Color.rgb(232, 237, 246))
            setPadding(d(13), d(8), d(8), d(8))
            addView(label(symbol + "   " + title, 12f, true, accent))
            addView(label(subtitle, 11f, false, muted))
            setOnClickListener { action() }
        }

    private fun gridParams(columns: Int, height: Int = 90): GridLayout.LayoutParams =
        GridLayout.LayoutParams().apply {
            width = 0; this.height = d(height)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(d(2), d(2), d(2), d(2))
        }

    private fun appTile(app: AppEntry, compact: Boolean): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(d(3), d(6), d(3), d(4))
            isClickable = true; isFocusable = true
            background = shape(Color.TRANSPARENT, 16)
            setOnClickListener { openApp(app) }
            addView(ImageView(this@MainActivity).apply {
                setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
            }, params(if (compact) 36 else 43, if (compact) 36 else 43))
            addView(TextView(this@MainActivity).apply {
                text = app.label; textSize = if (compact) 10f else 10.5f
                setTextColor(fg); gravity = Gravity.CENTER
                maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
            }, params(-1, 32))
        }

    private fun buildDex() {
        val root = FrameLayout(this)
        val desktop = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradient(intArrayOf(Color.rgb(8, 20, 40), Color.rgb(18, 43, 78), Color.rgb(30, 66, 108)), 0)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(d(22), d(8), d(22), d(4))
        }
        val title = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        title.addView(label("WINDOWS PAD", 10f, true, Color.rgb(142, 184, 247)))
        title.addView(label("Desktop", 19f, true, Color.WHITE))
        top.addView(title, params(0, 48, 1f))
        top.addView(label("✦  AI", 12f, true, Color.WHITE).apply {
            gravity = Gravity.CENTER; background = shape(Color.argb(55, 255, 255, 255), 18)
            setPadding(d(14), 0, d(14), 0); setOnClickListener { showAiPanel() }
        }, params(-2, 36))
        val topGap = View(this); top.addView(topGap, params(8, 1))
        top.addView(modeButton(true), params(-2, 36))
        desktop.addView(top)

        val work = FrameLayout(this).apply { setPadding(d(22), d(14), d(22), d(12)) }
        val deskScroll = ScrollView(this)
        val deskGrid = GridLayout(this).apply { columnCount = if (resources.configuration.screenWidthDp >= 850) 7 else 5 }
        apps.take(20).forEach { app ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(d(4), d(5), d(4), d(5))
                background = shape(Color.TRANSPARENT, 14)
                setOnClickListener { openApp(app) }
                addView(ImageView(this@MainActivity).apply {
                    setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
                }, params(42, 42))
                addView(TextView(this@MainActivity).apply {
                    text = app.label; textSize = 10.5f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                    maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
                }, params(76, 32))
            }
            deskGrid.addView(tile, GridLayout.LayoutParams().apply {
                width = d(84); height = d(84); setMargins(d(3), d(3), d(3), d(3))
            })
        }
        deskScroll.addView(deskGrid)
        work.addView(deskScroll, FrameLayout.LayoutParams(-1, -1))
        desktop.addView(work, LinearLayout.LayoutParams(-1, 0, 1f))

        val taskbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.argb(242, 13, 23, 40)); cornerRadius = d(20).toFloat()
                setStroke(d(1), Color.argb(75, 220, 235, 255))
            }
            setPadding(d(7), d(5), d(8), d(5))
        }
        taskbar.addView(label("⊞", 26f, true, Color.rgb(125, 178, 255)).apply {
            gravity = Gravity.CENTER; setOnClickListener { showAppDrawer() }
        }, params(42, 42))
        taskbar.addView(label("⌕  Pesquisar", 12f, false, Color.rgb(211, 224, 244)).apply {
            setOnClickListener { showAppDrawer() }
        }, params(0, 42, 1f))
        apps.take(4).forEach { app ->
            taskbar.addView(ImageView(this).apply {
                setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(d(5), d(5), d(5), d(5)); contentDescription = app.label
                setOnClickListener { openApp(app) }
            }, params(38, 40))
        }
        taskbar.addView(label("◉", 17f, true, Color.rgb(195, 213, 239)).apply {
            gravity = Gravity.CENTER; setOnClickListener { showSystemPanel() }
        }, params(32, 40))
        taskbar.addView(TextClock(this).apply {
            format24Hour = "EEE, d MMM  HH:mm"; format12Hour = "EEE, d MMM  h:mm a"
            textSize = 10.5f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        }, params(112, 40))
        val barParams = FrameLayout.LayoutParams(-1, d(56), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            setMargins(d(14), 0, d(14), d(10))
        }
        root.addView(desktop)
        root.addView(taskbar, barParams)
        setContentView(root)
    }

    private fun showAppDrawer() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(d(18), d(14), d(18), d(12)); setBackgroundColor(bg)
        }
        outer.addView(label("Aplicativos", 22f, true), params(-1, 42))
        val search = EditText(this).apply {
            hint = "Pesquisar aplicativos"; textSize = 14f; setSingleLine(true)
            setTextColor(fg); setHintTextColor(muted); background = shape(surface2, 22)
            setPadding(d(15), d(4), d(15), d(4))
        }
        val sp = params(-1, 46); sp.topMargin = d(8); outer.addView(search, sp)
        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply { columnCount = if (resources.configuration.screenWidthDp >= 700) 5 else 4 }
        scroll.addView(grid); outer.addView(scroll, params(-1, 0, 1f))
        fun populate(filter: String) {
            grid.removeAllViews()
            apps.filter { it.label.contains(filter, true) }.forEach { app ->
                grid.addView(appTile(app, true), gridParams(grid.columnCount, 78))
            }
        }
        populate("")
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { populate(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val dialog = AlertDialog.Builder(this).setView(outer).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), (resources.displayMetrics.heightPixels * 0.84f).toInt())
    }

    private fun showAiPanel(prefill: String = "") {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(d(20), d(18), d(20), d(16)); setBackgroundColor(bg)
        }
        panel.addView(label("✦  WINDOWS PAD AI", 12f, true, accent), params(-1, 28))
        panel.addView(label("Como posso ajudar?", 23f, true), params(-1, 38))
        panel.addView(label("Encontre aplicativos, abra funções ou peça ajuda para usar seu tablet.", 12f, false, muted), params(-1, 42))
        val input = EditText(this).apply {
            hint = "Ex.: abrir YouTube, encontrar fotos..."; setSingleLine(true)
            textSize = 13f; setTextColor(fg); setHintTextColor(muted)
            background = shape(surface2, 18); setPadding(d(14), d(5), d(14), d(5))
            setText(prefill)
        }
        val ip = params(-1, 48); ip.topMargin = d(12); panel.addView(input, ip)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(label("🎙  Falar", 12f, true, accent).apply {
            gravity = Gravity.CENTER; background = shape(surface2, 16)
            setOnClickListener { startVoiceSearch() }
        }, params(0, 42, 1f))
        val gap = View(this); actions.addView(gap, params(8, 1))
        actions.addView(label("Pesquisar  →", 12f, true, Color.WHITE).apply {
            gravity = Gravity.CENTER; background = shape(accent, 16)
            setOnClickListener { handleAssistantQuery(input.text.toString()) }
        }, params(0, 42, 1f))
        val ap = params(-1, 42); ap.topMargin = d(9); panel.addView(actions, ap)
        panel.addView(label("Sugestões rápidas", 13f, true), params(-1, 38))
        val suggestions = listOf("Abrir configurações", "Pesquisar aplicativos", "Ativar modo DeX")
        suggestions.forEach { suggestion ->
            panel.addView(label("›   " + suggestion, 13f, false, fg).apply {
                background = shape(surface, 14); setPadding(d(12), 0, d(12), 0)
                setOnClickListener { handleAssistantQuery(suggestion) }
            }, params(-1, 42).also { it.bottomMargin = d(6) })
        }
        panel.addView(label("A busca inteligente funciona no próprio tablet. Para IA generativa online, será necessária uma conexão e configuração opcional.", 10f, false, muted), params(-1, 48))
        val dialog = AlertDialog.Builder(this).setView(panel).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), -2)
    }

    private fun handleAssistantQuery(raw: String) {
        val query = raw.trim().lowercase(Locale.getDefault())
        if (query.isEmpty()) {
            Toast.makeText(this, "Digite o que você procura.", Toast.LENGTH_SHORT).show()
            return
        }
        when {
            query.contains("dex") || query.contains("desktop") || query.contains("área de trabalho") -> {
                if (!dexMode) switchMode()
                else Toast.makeText(this, "O modo DeX já está ativo.", Toast.LENGTH_SHORT).show()
            }
            query.contains("tablet") || query.contains("one ui") -> {
                if (dexMode) switchMode()
                else Toast.makeText(this, "O modo tablet já está ativo.", Toast.LENGTH_SHORT).show()
            }
            query.contains("configura") -> startActivity(Intent(Settings.ACTION_SETTINGS))
            query.contains("wifi") || query.contains("wi-fi") -> startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            query.contains("bluetooth") -> startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            query.contains("notifica") -> showSystemPanel()
            query.contains("pesquisar") || query.contains("aplicativos") || query.contains("apps") -> showAppDrawer()
            else -> {
                val match = apps.firstOrNull { it.label.contains(query, true) || query.contains(it.label.lowercase(Locale.getDefault())) }
                if (match != null) openApp(match)
                else {
                    val words = query.split(" ").filter { it.length > 2 }
                    val ranked = apps.map { app -> app to words.count { app.label.contains(it, true) } }
                        .filter { it.second > 0 }.sortedByDescending { it.second }.firstOrNull()?.first
                    if (ranked != null) openApp(ranked)
                    else Toast.makeText(this, "Não encontrei esse app ou comando. Tente pesquisar pelo nome.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale o que deseja fazer")
            }
            startActivityForResult(intent, 410)
        } catch (_: Exception) {
            Toast.makeText(this, "O reconhecimento de voz não está disponível neste aparelho.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Android API, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 410 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) handleAssistantQuery(spoken)
        }
    }

    private fun showSystemPanel() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(d(18), d(16), d(18), d(14)); setBackgroundColor(bg)
        }
        panel.addView(label("Central do dispositivo", 20f, true), params(-1, 42))
        panel.addView(label("Acesse rapidamente os controles do Android.", 12f, false, muted), params(-1, 30))
        listOf(
            Triple("Wi-Fi", "Configurações de rede", Intent(Settings.ACTION_WIFI_SETTINGS)),
            Triple("Bluetooth", "Dispositivos conectados", Intent(Settings.ACTION_BLUETOOTH_SETTINGS)),
            Triple("Notificações", "Preferências de notificações", Intent(Settings.ACTION_NOTIFICATION_SETTINGS)),
            Triple("Sistema", "Configurações completas", Intent(Settings.ACTION_SETTINGS))
        ).forEach { item ->
            panel.addView(label("◉   " + item.first + "     ›", 14f, true).apply {
                background = shape(surface, 16); setPadding(d(12), 0, d(12), 0)
                setOnClickListener { startActivity(item.third) }
            }, params(-1, 48).also { it.bottomMargin = d(7) })
        }
        val dialog = AlertDialog.Builder(this).setView(panel).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), -2)
    }

    private fun showAppearanceInfo() {
        AlertDialog.Builder(this)
            .setTitle("Aparência do Windows Pad")
            .setMessage("O launcher acompanha automaticamente o tema claro ou escuro definido no Android. Para mudar, abra as configurações de Tela do sistema.")
            .setNegativeButton("Fechar", null)
            .setPositiveButton("Abrir tela") { _, _ -> startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
            .show()
    }
}
