package com.thsantz.windowspad

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.content.pm.PackageManager

class MainActivity : Activity() {
    private var dark = false
    private lateinit var appsGrid: GridLayout
    private lateinit var search: EditText
    private val apps = mutableListOf<AppEntry>()
    private val blue = Color.rgb(40, 105, 220)
    private val bg get() = if (dark) Color.rgb(24, 27, 34) else Color.rgb(239, 243, 250)
    private val panel get() = if (dark) Color.rgb(37, 42, 52) else Color.WHITE
    private val fg get() = if (dark) Color.WHITE else Color.rgb(30, 38, 52)
    private val muted get() = if (dark) Color.rgb(185, 193, 207) else Color.rgb(105, 116, 135)
    data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        loadApps()
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::appsGrid.isInitialized) {
            loadApps()
            renderApps(search.text?.toString().orEmpty())
        }
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

    private fun rounded(color: Int, radius: Float = 22f) =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun text(value: String, size: Float, bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(fg)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 12)
            setBackgroundColor(bg)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(text("Windows Pad", 25f, true), LinearLayout.LayoutParams(0, 58, 1f))
        val theme = TextView(this).apply {
            text = if (dark) "☀" else "☾"; textSize = 24f; gravity = Gravity.CENTER
            setTextColor(fg); background = rounded(panel, 50f)
            setOnClickListener { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
        }
        top.addView(theme, LinearLayout.LayoutParams(48, 48))
        root.addView(top)
        val subtitle = text("Seu tablet, com experiência de desktop.", 14f).apply { setTextColor(muted) }
        root.addView(subtitle)

        search = EditText(this).apply {
            hint = "Pesquisar aplicativos..."; textSize = 15f; singleLine = true
            setTextColor(fg); setHintTextColor(muted); background = rounded(panel, 28f)
            setPadding(20, 8, 20, 8)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderApps(s?.toString().orEmpty()) }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        val searchParams = LinearLayout.LayoutParams(-1, 54); searchParams.topMargin = 22
        root.addView(search, searchParams)
        val section = text("Aplicativos", 19f, true)
        val sectionParams = LinearLayout.LayoutParams(-1, 48); sectionParams.topMargin = 12
        root.addView(section, sectionParams)

        val scroll = ScrollView(this)
        appsGrid = GridLayout(this).apply { columnCount = if (resources.configuration.screenWidthDp >= 700) 6 else 4 }
        scroll.addView(appsGrid)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val taskbar = LinearLayout(this).apply {
            gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL
            background = rounded(panel, 28f); setPadding(16, 10, 16, 10)
        }
        val start = TextView(this).apply {
            text = "⊞"; textSize = 27f; setTextColor(blue); gravity = Gravity.CENTER
            setOnClickListener { search.setText("") }
        }
        taskbar.addView(start, LinearLayout.LayoutParams(54, 42))
        val label = text("Windows Pad", 14f, true).apply { gravity = Gravity.CENTER }
        taskbar.addView(label, LinearLayout.LayoutParams(0, 42, 1f))
        val clock = android.widget.TextClock(this).apply {
            format24Hour = "EEE, d MMM · HH:mm"
            format12Hour = "EEE, d MMM · h:mm a"
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            isSingleLine = true
        }
        taskbar.addView(clock, LinearLayout.LayoutParams(116, 42))
        val taskParams = LinearLayout.LayoutParams(-1, 62); taskParams.topMargin = 8
        root.addView(taskbar, taskParams)
        setContentView(root)
        renderApps("")
    }

    private fun renderApps(filter: String) {
        if (!::appsGrid.isInitialized) return
        appsGrid.removeAllViews()
        appsGrid.columnCount = if (resources.configuration.screenWidthDp >= 700) 6 else 4
        val shown = apps.filter { it.label.contains(filter, true) }
        shown.forEach { app ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(4, 12, 4, 12)
                isClickable = true; isFocusable = true
                setOnClickListener {
                    val launch = packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launch != null) startActivity(launch)
                    else Toast.makeText(this@MainActivity, "Não foi possível abrir " + app.label, Toast.LENGTH_SHORT).show()
                }
            }
            val icon = android.widget.ImageView(this).apply { setImageDrawable(app.icon); scaleType = android.widget.ImageView.ScaleType.FIT_CENTER }
            tile.addView(icon, LinearLayout.LayoutParams(48, 48))
            val appLabel = TextView(this).apply {
                text = app.label; textSize = 11f; setTextColor(fg); gravity = Gravity.CENTER
                maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
            }
            val lp = LinearLayout.LayoutParams(-1, 38); lp.topMargin = 5
            tile.addView(appLabel, lp)
            val item = GridLayout.LayoutParams().apply {
                width = 0; height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(2, 2, 2, 2)
            }
            appsGrid.addView(tile, item)
        }
        if (shown.isEmpty()) appsGrid.addView(text("Nenhum aplicativo encontrado", 14f).apply { setTextColor(muted) })
    }
}
