package de.radicalfishgames.crosscode.overlay

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import de.radicalfishgames.crosscode.R
import java.util.WeakHashMap
import kotlin.math.hypot
import kotlin.math.min

/**
 * CrossCode-specific, non-invasive touch controller skin and editor.
 *
 * Input remains entirely inside CrossAndroid's existing CircleDirectionalPad,
 * CircleButton and SwipeControl classes. This class only changes drawing,
 * position, scale and edit-mode touch routing.
 *
 * Profiles are scoped to the game's combat, menu and direct layouts.
 * Stored relative to the parent view for S23 Ultra and other aspect ratios.
 */
object PremiumControls {
    private const val PREFS = "crosscode_premium_touch_v1"
    private val registered = WeakHashMap<ControlView, String>()
    private var editing = false
    private var selected: ControlView? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var pinchBase = 0f
    private var pinchScale = 1f
    private var controlContainer: FrameLayout? = null
    private var editButton: TextView? = null
    private var hideButton: TextView? = null
    private var editorButtons = ArrayList<View>()
    private var currentActivity: Activity? = null
    private var opacity = 0.78f

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun register(root: ViewGroup, mode: String) {
        traverse(root) { control ->
            val id = try {
                control.resources.getResourceEntryName(control.id)
            } catch (_: Exception) {
                "control" + control.id
            }
            val key = mode + "." + id
            registered[control] = key
            control.post { restore(control) }
        }
    }

    private fun traverse(view: View, fn: (ControlView) -> Unit) {
        if (view is ControlView) fn(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) traverse(view.getChildAt(index), fn)
        }
    }

    private fun prefs(view: View) = view.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun restore(view: ControlView) {
        val key = registered[view] ?: return
        val parent = view.parent as? View ?: return
        if (parent.width <= 0 || parent.height <= 0) {
            view.post { if (view.isAttachedToWindow) restore(view) }
            return
        }
        val p = prefs(view)
        view.translationX = p.getFloat(key + ".x", 0f) * parent.width
        view.translationY = p.getFloat(key + ".y", 0f) * parent.height
        val size = p.getFloat(key + ".size", 1f).coerceIn(0.45f, 2.20f)
        view.scaleX = size
        view.scaleY = size
        view.alpha = opacity
        view.invalidate()
    }

    private fun save(view: ControlView) {
        val key = registered[view] ?: return
        val parent = view.parent as? View ?: return
        if (parent.width <= 0 || parent.height <= 0) return
        prefs(view).edit()
            .putFloat(key + ".x", view.translationX / parent.width)
            .putFloat(key + ".y", view.translationY / parent.height)
            .putFloat(key + ".size", view.scaleX)
            .apply()
    }

    fun install(activity: Activity) {
        if (currentActivity === activity) return
        currentActivity = activity
        opacity = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat("opacity", 0.78f).coerceIn(0.35f, 1f)
        val root = activity.findViewById<ConstraintLayout>(R.id.root_view)
        controlContainer = activity.findViewById(R.id.control_container)
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            elevation = dp(activity, 10).toFloat()
            setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4))
            background = GradientDrawable().apply {
                setColor(Color.argb(195, 12, 20, 33))
                cornerRadius = dp(activity, 15).toFloat()
                setStroke(dp(activity, 1), Color.rgb(38, 175, 215))
            }
        }
        fun button(label: String, action: () -> Unit): TextView {
            val view = TextView(activity).apply {
                text = label
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.CENTER
                setPadding(dp(activity, 10), dp(activity, 7), dp(activity, 10), dp(activity, 7))
                setOnClickListener { action() }
            }
            row.addView(view, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            return view
        }
        hideButton = button("HIDE") {
            if (editing) stopEditing()
            val container = controlContainer ?: return@button
            val nextVisible = container.visibility != View.VISIBLE
            container.visibility = if (nextVisible) View.VISIBLE else View.GONE
            hideButton?.text = if (nextVisible) "HIDE" else "SHOW"
        }
        editButton = button("EDIT") {
            val container = controlContainer
            if (container?.visibility != View.VISIBLE) {
                container?.visibility = View.VISIBLE
                hideButton?.text = "HIDE"
            }
            if (editing) stopEditing() else {
                editing = true
                selected = null
                editButton?.text = "DONE"
                editorButtons.forEach { it.visibility = View.VISIBLE }
                repaint()
            }
        }
        editorButtons.add(button("−") { resizeSelected(0.90f) })
        editorButtons.add(button("+") { resizeSelected(1.10f) })
        editorButtons.add(button("RESET") {
            val view = selected
            if (view != null) {
                view.translationX = 0f
                view.translationY = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                save(view)
                view.invalidate()
            }
        })
        editorButtons.add(button("FADE") {
            opacity = when {
                opacity > 0.88f -> 0.40f
                opacity > 0.65f -> 0.55f
                opacity > 0.45f -> 0.75f
                else -> 1f
            }
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putFloat("opacity", opacity).apply()
            registered.keys.toList().forEach { it.alpha = opacity }
        })
        editorButtons.forEach { it.visibility = View.GONE }
        val lp = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topToTop = androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
            startToStart = androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
            topMargin = dp(activity, 10)
            marginStart = dp(activity, 12)
        }
        root.addView(row, lp)
        registered.keys.toList().forEach { restore(it) }
    }

    private fun stopEditing() {
        editing = false
        selected?.let { save(it) }
        selected = null
        editButton?.text = "EDIT"
        editorButtons.forEach { it.visibility = View.GONE }
        repaint()
    }

    private fun resizeSelected(factor: Float) {
        val view = selected ?: return
        val next = (view.scaleX * factor).coerceIn(0.45f, 2.20f)
        view.scaleX = next
        view.scaleY = next
        save(view)
        view.invalidate()
    }

    private fun repaint() {
        registered.keys.toList().forEach { it.invalidate() }
    }

    /**
     * Return true only during edit mode. Normal touches are delegated completely
     * to the original engine input listeners and existing gamepad mappings.
     */
    fun editTouch(view: ControlView, event: MotionEvent): Boolean {
        if (!editing) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                selected = view
                downRawX = event.rawX
                downRawY = event.rawY
                pinchBase = 0f
                view.parent?.requestDisallowInterceptTouchEvent(true)
                repaint()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    pinchBase = hypot(
                        event.getX(0) - event.getX(1),
                        event.getY(0) - event.getY(1)
                    ).coerceAtLeast(1f)
                    pinchScale = view.scaleX
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2 && pinchBase > 0f) {
                    val distance = hypot(
                        event.getX(0) - event.getX(1),
                        event.getY(0) - event.getY(1)
                    )
                    val size = (pinchScale * distance / pinchBase).coerceIn(0.45f, 2.20f)
                    view.scaleX = size
                    view.scaleY = size
                } else {
                    val parent = view.parent as? View
                    val maxX = (parent?.width ?: view.width).toFloat()
                    val maxY = (parent?.height ?: view.height).toFloat()
                    view.translationX = (view.translationX + event.rawX - downRawX)
                        .coerceIn(-maxX, maxX)
                    view.translationY = (view.translationY + event.rawY - downRawY)
                        .coerceIn(-maxY, maxY)
                    downRawX = event.rawX
                    downRawY = event.rawY
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                pinchBase = 0f
                downRawX = event.rawX
                downRawY = event.rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                save(view)
                pinchBase = 0f
            }
        }
        view.invalidate()
        return true
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    /**
     * Compact charcoal-glass controls with CrossCode-specific labels,
     * cyan/purple rings, and a smaller version of the game's original icon.
     */
    fun draw(view: ControlView, canvas: Canvas, icon: Bitmap) {
        val w = view.width.toFloat()
        val h = view.height.toFloat()
        if (w < 1f || h < 1f) return
        val name = try { view.resources.getResourceEntryName(view.id) }
                   catch (_: Exception) { "" }
        val label = when (name) {
            "movement_pad" -> "MOVE"
            "aim_pad" -> "AIM / ATK"
            "dash_button" -> "DASH"
            "shield_button" -> "GUARD"
            "elemental_button" -> "ELEMENT"
            "quickmenu_button" -> "QUICK"
            "menu_button" -> "MENU"
            "overlay_toggle" -> "SWIPE MODE"
            "menu_directions" -> "SELECT"
            "tab_left" -> "L TAB"
            "tab_right" -> "R TAB"
            "help_button" -> "HELP"
            "rightcross_top" -> "Y"
            "rightcross_bottom" -> "A"
            "rightcross_left" -> "X"
            "rightcross_right" -> "B"
            else -> "ACTION"
        }
        val isAim = name == "aim_pad"
        val isMove = name == "movement_pad" || name == "menu_directions"
        val isMode = name == "overlay_toggle"
        val inset = min(w, h) * 0.06f
        val bounds = RectF(inset, inset, w - inset, h - inset)
        val corner = if (isMode) h * 0.38f else min(w, h) * 0.48f
        fill.style = Paint.Style.FILL
        fill.color = Color.argb(185, 13, 20, 38)
        canvas.drawRoundRect(bounds, corner, corner, fill)
        rim.strokeWidth = min(w, h) * if (isAim || isMove) 0.023f else 0.035f
        rim.color = when {
            editing && selected === view -> Color.rgb(245, 209, 90)
            isAim -> Color.rgb(255, 88, 142)
            else -> Color.rgb(68, 204, 244)
        }
        canvas.drawRoundRect(bounds, corner, corner, rim)
        // Existing icons are retained but shown at a controlled, subtler size.
        val diameter = min(w, h)
        val iconSize = diameter * if (isMove || isAim) 0.32f else 0.37f
        val iconBottom = h * if (isMode) 0.77f else 0.67f
        val iconTop = iconBottom - iconSize
        imagePaint.alpha = 125
        canvas.drawBitmap(icon, null, RectF(
            w / 2 - iconSize / 2, iconTop,
            w / 2 + iconSize / 2, iconBottom
        ), imagePaint)
        text.textSize = (diameter * 0.125f).coerceIn(dp(view.context, 9).toFloat(), dp(view.context, 15).toFloat())
        text.color = Color.rgb(232, 246, 255)
        canvas.drawText(label, w / 2, h * 0.84f, text)
        if (editing) {
            text.color = Color.rgb(245, 209, 90)
            text.textSize = dp(view.context, 9).toFloat()
            canvas.drawText("DRAG / PINCH", w / 2, h * 0.16f, text)
        }
    }
}