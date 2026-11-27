package com.thsantz.windowspad

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import android.graphics.drawable.Drawable

class MainActivity : Activity() {
    data class AppEntry(val label: String, val packageName: String, val icon: Drawable?)
    private val apps = mutableListOf<AppEntry>()
    private val prefs by lazy { getSharedPreferences("windowspad", MODE_PRIVATE) }
    private var dexMode = false
    private var dark = false
    private val accent = Color.rgb(44, 111, 235)
    private val bg get() = if (dark) Color.rgb(13, 18, 29) else Color.rgb(242, 245, 251)
    private val surface get() = if (dark) Color.rgb(27, 34, 48) else Color.WHITE
    private val fg get() = if (dark) Color.rgb(244, 247, 255) else Color.rgb(27, 36, 54)
    private val muted get() = if (dark) Color.rgb(169, 181, 201) else Color.rgb(111, 124, 146)
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
        if (apps.isNotEmpty()) loadApps()
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        apps.clear()
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL).forEach {
            val info = it.activityInfo
            if (info.packageName != packageName) apps.add(AppEntry(it.loadLabel(packageManager).toString(), info.packageName, it.loadIcon(packageManager)))
        }
        apps.sortBy { it.label.lowercase() }
    }

    private fun shape(color: Int, radius: Int = 24, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = d(radius).toFloat()
            if (stroke != 0) setStroke(d(1), stroke)
        }

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
        window.statusBarColor = if (dexMode) Color.rgb(13, 20, 34) else bg
        window.navigationBarColor = if (dexMode) Color.rgb(13, 20, 34) else bg
        window.decorView.systemUiVisibility = if (!dark && !dexMode) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
        if (dexMode) buildDex() else buildTablet()
    }

    private fun switchMode() {
        dexMode = !dexMode
        prefs.edit().putBoolean("dex_mode", dexMode).apply()
        render()
    }

    private fun modeButton(): TextView = label(if (dexMode) "▦  Modo tablet" else "▣  Modo DeX", 13f, true, fg).apply {
        gravity = Gravity.CENTER
        background = shape(surface, 22, if (dark) Color.rgb(58, 70, 92) else Color.rgb(225, 231, 241))
        setPadding(d(16), 0, d(16), 0)
        setOnClickListener { switchMode() }
    }

    private fun buildTablet() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(22), d(20), d(22), d(10))
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("WINDOWS PAD", 12f, true, accent))
        brand.addView(label("Seu espaço. Seu ritmo.", 23f, true))
        header.addView(brand, params(0, 64, 1f))
        header.addView(modeButton(), params(-2, 44))
        root.addView(header)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(29, 71, 139), Color.rgb(65, 117, 205), Color.rgb(104, 151, 222))).apply { cornerRadius = d(28).toFloat() }
            setPadding(d(22), d(18), d(18), d(18))
        }
        val heroInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heroInfo.addView(label("BOM DIA", 11f, true, Color.rgb(211, 226, 250)))
        heroInfo.addView(label("Tudo pronto\npara hoje?", 26f, true, Color.WHITE).apply { setLineSpacing(0f, 1.04f) })
        heroInfo.addView(label("Seu tablet em um só lugar.", 12f, false, Color.rgb(225, 236, 255)))
        hero.addView(heroInfo, params(0, -2, 1f))
        val clock = TextClock(this).apply {
            format24Hour = "HH:mm"
            format12Hour = "h:mm"
            textSize = 43f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        }
        hero.addView(clock, params(112, 76))
        val heroParams = params(-1, 142); heroParams.topMargin = d(18)
        root.addView(hero, heroParams)

        val cards = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cards.addView(infoCard("▣", "AGENDA", "Organize seu dia"), params(0, 86, 1f))
        val gap = View(this); cards.addView(gap, params(10, 1))
        cards.addView(infoCard("◷", "LEMBRETES", "Não perca nada"), params(0, 86, 1f))
        val cardParams = params(-1, 86); cardParams.topMargin = d(12)
        root.addView(cards, cardParams)

        val appHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        appHeader.addView(label("Seus aplicativos", 19f, true), params(0, 48, 1f))
        appHeader.addView(label("Ver todos  ›", 13f, true, accent).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showAppDrawer() }
        }, params(-2, 44))
        val ah = params(-1, 48); ah.topMargin = d(8)
        root.addView(appHeader, ah)

        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply { columnCount = if (resources.configuration.screenWidthDp >= 850) 7 else 6 }
        val favorites = apps.take(18)
        favorites.forEach { grid.addView(appTile(it, false), gridParams(grid.columnCount)) }
        scroll.addView(grid)
        root.addView(scroll, params(-1, 0, 1f))

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = shape(surface, 26, if (dark) Color.rgb(50, 61, 81) else Color.rgb(229, 234, 243))
            setPadding(d(8), d(7), d(8), d(7))
        }
        val dockApps = apps.take(5)
        dockApps.forEach { app ->
            val icon = ImageView(this).apply {
                setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(d(8), d(6), d(8), d(6))
                setOnClickListener { openApp(app) }
            }
            dock.addView(icon, params(52, 48))
        }
        val all = label("☷", 25f, true, accent).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showAppDrawer() }
        }
        dock.addView(all, params(48, 48))
        val dockParams = params(-1, 62); dockParams.topMargin = d(10)
        root.addView(dock, dockParams)
        setContentView(root)
    }

    private fun infoCard(symbol: String, title: String, subtitle: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            background = shape(surface, 22)
            setPadding(d(14), d(8), d(12), d(8))
            addView(label("$symbol   $title", 11f, true, accent))
            addView(label(subtitle, 13f, true))
        }

    private fun gridParams(columns: Int): GridLayout.LayoutParams =
        GridLayout.LayoutParams().apply {
            width = 0
            height = d(92)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(d(2), d(3), d(2), d(3))
        }

    private fun appTile(app: AppEntry, compact: Boolean): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(d(3), d(7), d(3), d(5))
            isClickable = true
            isFocusable = true
            background = shape(Color.TRANSPARENT, 16)
            setOnClickListener { openApp(app) }
            val icon = ImageView(this@MainActivity).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            addView(icon, params(if (compact) 36 else 44, if (compact) 36 else 44))
            addView(TextView(this@MainActivity).apply {
                text = app.label; textSize = if (compact) 10f else 11f
                setTextColor(fg); gravity = Gravity.CENTER
                maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
            }, params(-1, 34))
        }

    private fun buildDex() {
        val root = FrameLayout(this)
        val desktop = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(10, 24, 48), Color.rgb(24, 56, 100), Color.rgb(35, 75, 119)))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(d(20), d(8), d(20), d(8))
        }
        top.addView(label("WINDOWS PAD  /  DESKTOP", 12f, true, Color.rgb(206, 222, 246)), params(0, 38, 1f))
        top.addView(modeButtonDex(), params(-2, 38))
        desktop.addView(top)

        val workArea = FrameLayout(this).apply { setPadding(d(20), d(18), d(20), d(12)) }
        val deskGrid = GridLayout(this).apply { columnCount = 4 }
        val shortcuts = apps.take(12)
        shortcuts.forEach { app ->
            val tile = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(d(5), d(7), d(5), d(7))
                setOnClickListener { openApp(app) }
                val icon = ImageView(this@MainActivity).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER }
                addView(icon, params(42, 42))
                addView(TextView(this@MainActivity).apply {
                    text = app.label; textSize = 11f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                    maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
                }, params(74, 34))
            }
            deskGrid.addView(tile, GridLayout.LayoutParams().apply {
                width = d(82); height = d(88); setMargins(d(4), d(3), d(4), d(3))
            })
        }
        workArea.addView(deskGrid, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        val hint = label("Área de trabalho\nAbra seus aplicativos pelos atalhos ou pelo menu Iniciar.", 15f, true, Color.rgb(218, 231, 249)).apply {
            gravity = Gravity.CENTER
            setLineSpacing(d(5).toFloat(), 1f)
        }
        workArea.addView(hint, FrameLayout.LayoutParams(-1, -1))
        desktop.addView(workArea, LinearLayout.LayoutParams(-1, 0, 1f))

        val taskbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.argb(238, 15, 25, 43)); cornerRadius = d(22).toFloat()
                setStroke(d(1), Color.argb(90, 220, 235, 255))
            }
            setPadding(d(10), d(5), d(12), d(5))
        }
        taskbar.addView(label("⊞", 27f, true, Color.rgb(117, 170, 255)).apply {
            gravity = Gravity.CENTER; setOnClickListener { showAppDrawer() }
        }, params(48, 44))
        taskbar.addView(label("Pesquisar", 13f, false, Color.rgb(210, 222, 241)).apply {
            setOnClickListener { showAppDrawer() }
        }, params(0, 44, 1f))
        val clock = TextClock(this).apply {
            format24Hour = "EEE, d MMM  ·  HH:mm"
            format12Hour = "EEE, d MMM  ·  h:mm a"
            textSize = 12f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        }
        taskbar.addView(clock, params(148, 44))
        val barParams = FrameLayout.LayoutParams(-1, d(58), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            setMargins(d(18), 0, d(18), d(12))
        }
        root.addView(desktop)
        root.addView(taskbar, barParams)
        setContentView(root)
    }

    private fun modeButtonDex(): TextView = label("▦  Modo tablet", 12f, true, Color.WHITE).apply {
        gravity = Gravity.CENTER
        background = shape(Color.argb(65, 255, 255, 255), 20)
        setPadding(d(14), 0, d(14), 0)
        setOnClickListener { switchMode() }
    }

    private fun showAppDrawer() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(18), d(14), d(18), d(12))
            setBackgroundColor(bg)
        }
        outer.addView(label("Aplicativos", 22f, true), params(-1, 44))
        val search = EditText(this).apply {
            hint = "Pesquisar aplicativos"; textSize = 15f; setSingleLine(true)
            setTextColor(fg); setHintTextColor(muted); background = shape(surface, 24)
            setPadding(d(16), d(4), d(16), d(4))
        }
        val sp = params(-1, 48); sp.topMargin = d(8); outer.addView(search, sp)
        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply { columnCount = 4 }
        scroll.addView(grid)
        outer.addView(scroll, params(-1, 0, 1f))
        fun populate(filter: String) {
            grid.removeAllViews()
            apps.filter { it.label.contains(filter, true) }.forEach { app ->
                grid.addView(appTile(app, true), gridParams(4))
            }
        }
        populate("")
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { populate(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        val dialog = AlertDialog.Builder(this).setView(outer).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), (resources.displayMetrics.heightPixels * 0.82f).toInt())
    }
}
