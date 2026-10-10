from pathlib import Path
root=Path("source/android/app/src/main")
def edit(name,old,new):
 p=root/name
 s=p.read_text()
 if old not in s: raise SystemExit("Expected source not found: "+str(p)+" "+old[:70])
 p.write_text(s.replace(old,new))
edit("java/io/github/moronigranja/alabasterdawn/PortActivity.kt",'private var s23ScreenMode = "fit"','private var s23ScreenMode = "fill"')
edit("java/io/github/moronigranja/alabasterdawn/PortActivity.kt",'prefs.getString("s23_screen_mode", "fit") ?: "fit"','prefs.getString("s23_screen_mode", "fill") ?: "fill"')
edit("java/io/github/moronigranja/alabasterdawn/OnScreenPadModel.kt",'setPill(PILL_A, centre - u * PILL_PLAY_OFFSET, rowY, u * PILL_WIDE, u * PILL_HALF_HEIGHT)\n            setPill(PILL_B, centre + u * PILL_PLAY_OFFSET, rowY, u * PILL_WIDE, u * PILL_HALF_HEIGHT)','setPill(PILL_B, centre, rowY, u * 0.66f, u * 0.27f)')
edit("java/io/github/moronigranja/alabasterdawn/OnScreenPadView.kt",'drawRowPill(canvas, OnScreenPadModel.PILL_A, pillLabelA(drawn))\n                drawRowPill(canvas, OnScreenPadModel.PILL_MINUS, "-")\n                drawRowPill(canvas, OnScreenPadModel.PILL_PLUS, "+")\n                drawRowPill(canvas, OnScreenPadModel.PILL_B, if (model.editing) "DONE" else "EDIT")','if (model.editing) {\n                    drawRowPill(canvas, OnScreenPadModel.PILL_A, pillLabelA(drawn))\n                    drawRowPill(canvas, OnScreenPadModel.PILL_MINUS, "-")\n                    drawRowPill(canvas, OnScreenPadModel.PILL_PLUS, "+")\n                }\n                drawRowPill(canvas, OnScreenPadModel.PILL_B, if (model.editing) "DONE" else "EDIT")')
edit("assets/ada-shim.js",'canvas.style.setProperty("width", "100vw", "important");\n            canvas.style.setProperty("height", "100vh", "important");\n            canvas.style.setProperty("object-fit", mode === "fill" ? "cover" : "fill", "important");','canvas.style.setProperty("width", "100vw", "important");\n            canvas.style.setProperty("height", "100vh", "important");\n            canvas.style.setProperty("max-width", "none", "important");\n            canvas.style.setProperty("max-height", "none", "important");\n            canvas.style.setProperty("left", "0", "important");\n            canvas.style.setProperty("top", "0", "important");\n            canvas.style.setProperty("object-fit", mode === "fill" ? "cover" : "fill", "important");')
print("S23 UI repairs applied")
