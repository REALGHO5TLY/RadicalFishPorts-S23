from pathlib import Path
root=Path("source/android/app/src/main")
def edit(name,old,new):
 p=root/name
 s=p.read_text()
 if old not in s: raise SystemExit("Expected source not found: "+str(p)+" "+old[:70])
 p.write_text(s.replace(old,new))
edit("java/io/github/moronigranja/alabasterdawn/PortActivity.kt",'private var s23ScreenMode = "fit"','private var s23ScreenMode = "stretch"')
edit("java/io/github/moronigranja/alabasterdawn/PortActivity.kt",'prefs.getString("s23_screen_mode", "fit") ?: "fit"','prefs.getString("s23_screen_mode", "stretch") ?: "fill"')
edit("java/io/github/moronigranja/alabasterdawn/OnScreenPadModel.kt",'setPill(PILL_A, centre - u * PILL_PLAY_OFFSET, rowY, u * PILL_WIDE, u * PILL_HALF_HEIGHT)\n            setPill(PILL_B, centre + u * PILL_PLAY_OFFSET, rowY, u * PILL_WIDE, u * PILL_HALF_HEIGHT)','setPill(PILL_A, centre - u * 0.48f, rowY, u * 0.42f, u * 0.24f)\n            setPill(PILL_B, centre + u * 0.48f, rowY, u * 0.42f, u * 0.24f)')
edit("java/io/github/moronigranja/alabasterdawn/OnScreenPadView.kt",'drawRowPill(canvas, OnScreenPadModel.PILL_A, pillLabelA(drawn))\n                drawRowPill(canvas, OnScreenPadModel.PILL_MINUS, "-")\n                drawRowPill(canvas, OnScreenPadModel.PILL_PLUS, "+")\n                drawRowPill(canvas, OnScreenPadModel.PILL_B, if (model.editing) "DONE" else "EDIT")','drawRowPill(canvas, OnScreenPadModel.PILL_A, pillLabelA(drawn))\n                if (model.editing) {\n                    drawRowPill(canvas, OnScreenPadModel.PILL_MINUS, "-")\n                    drawRowPill(canvas, OnScreenPadModel.PILL_PLUS, "+")\n                }\n                drawRowPill(canvas, OnScreenPadModel.PILL_B, if (model.editing) "DONE" else "EDIT")')
# Avoid cropping: use CSS stretching instead of cover; preserve existing canvas sizing rules.
p=root/"assets/ada-shim.js"
v=p.read_text()
v=v.replace('mode === "fill" ? "cover" : "fill"', '"fill"')
p.write_text(v)
# HIDE and EDIT have independent draggable positions saved in app preferences.
p=root/"java/io/github/moronigranja/alabasterdawn/OnScreenPadModel.kt"
v=p.read_text()
v=v.replace('    private val pills = Array(4) { Shape() }','''    private val pills = Array(4) { Shape() }
    private val pillOffsetX = FloatArray(4)
    private val pillOffsetY = FloatArray(4)
    fun setPillOffset(which: Int, x: Float, y: Float) {
        if (which != PILL_A && which != PILL_B) return
        pillOffsetX[which] = x
        pillOffsetY[which] = y
        placePills()
    }
    fun pillOffsetX(which: Int) = pillOffsetX[which]
    fun pillOffsetY(which: Int) = pillOffsetY[which]
    fun movePill(which: Int, dx: Float, dy: Float) {
        if (unit <= 0f || (which != PILL_A && which != PILL_B)) return
        pillOffsetX[which] += dx / unit
        pillOffsetY[which] += dy / unit
        placePills()
    }''')
v=v.replace('''            this.x = x
            this.y = y
            this.halfWidth = halfWidth''','''            this.x = (x + pillOffsetX[which] * unit).coerceIn(halfWidth, maxOf(halfWidth, viewWidth - halfWidth))
            this.y = (y + pillOffsetY[which] * unit).coerceIn(halfHeight, maxOf(halfHeight, viewHeight - halfHeight))
            this.halfWidth = halfWidth''')
p.write_text(v)
p=root/"java/io/github/moronigranja/alabasterdawn/OnScreenPadView.kt"
v=p.read_text()
# Save positions through SharedPreferences, independent from pad layout schema.
anchor='    var onToggle: ((Boolean) -> Unit)? = null'
v=v.replace(anchor,'''    private val pillPrefs by lazy { context.getSharedPreferences("s23_movable_menu", 0) }
    private val pillDrag = HashMap<Int, Int>()
    private val pillLast = HashMap<Int, Pair<Float, Float>>()
    private fun restorePillPositions() {
        for (which in intArrayOf(OnScreenPadModel.PILL_A, OnScreenPadModel.PILL_B)) {
            model.setPillOffset(which, pillPrefs.getFloat("x$which", 0f), pillPrefs.getFloat("y$which", 0f))
        }
    }
    private fun savePillPosition(which: Int) {
        pillPrefs.edit().putFloat("x$which", model.pillOffsetX(which))
            .putFloat("y$which", model.pillOffsetY(which)).apply()
    }
''' +anchor)
v=v.replace('''        model.resize(w.toFloat(), h.toFloat())''','''        model.resize(w.toFloat(), h.toFloat())
        restorePillPositions()''')
v=v.replace('''                if (pill != OnScreenPadModel.PILL_NONE) {
                    onPill(pill)
                } else if''','''                if (pill != OnScreenPadModel.PILL_NONE) {
                    if (model.editing && (pill == OnScreenPadModel.PILL_A || pill == OnScreenPadModel.PILL_B)) {
                        pillDrag[id] = pill
                        pillLast[id] = Pair(x, y)
                    } else onPill(pill)
                } else if''')
v=v.replace('''                        dragPointer[pid]?.let { model.dragTo(it, px, py); changed = true }''','''                        pillDrag[pid]?.let { which ->
                            val old = pillLast[pid] ?: Pair(px, py)
                            model.movePill(which, px - old.first, py - old.second)
                            pillLast[pid] = Pair(px, py)
                            changed = true
                        }
                        dragPointer[pid]?.let { model.dragTo(it, px, py); changed = true }''')
v=v.replace('''                    dragPointer.remove(id)
                    scalePointer.remove(id)''','''                    pillDrag.remove(id)?.let { savePillPosition(it) }
                    pillLast.remove(id)
                    dragPointer.remove(id)
                    scalePointer.remove(id)''')
v=v.replace('''                    dragPointer.clear()
                    scalePointer.clear()''','''                    pillDrag.clear()
                    pillLast.clear()
                    dragPointer.clear()
                    scalePointer.clear()''')
p.write_text(v)
print("S23 fullscreen and movable HIDE/EDIT patched")
