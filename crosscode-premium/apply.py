#!/usr/bin/env python3
"""Apply an original premium-control overlay to an upstream CrossDroid checkout.

This repository intentionally carries patches only; upstream CrossDroid /
CrossAndroid source is fetched at build time. It does not publish game files.
The underlying CrossCode input listeners are unchanged.
"""
from pathlib import Path
import shutil
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: python3 crosscode-premium/apply.py <CrossDroid-source-directory>")

root = Path(sys.argv[1])
pkg = root / "app/src/main/java/de/radicalfishgames/crosscode"
control = pkg / "overlay/ControlView.kt"
feature = pkg / "features/VirtualControllerFeature.kt"
assert control.is_file() and feature.is_file(), "Expected CrossDroid source missing"

def patch(path, needle, replacement):
    contents = path.read_text()
    if needle not in contents:
        raise RuntimeError("Upstream changed; refusing unsafe patch: " + str(path) + ": " + repr(needle))
    if contents.count(needle) != 1:
        raise RuntimeError("Patch anchor not unique in " + str(path))
    path.write_text(contents.replace(needle, replacement, 1))

shutil.copyfile(Path(__file__).parent / "PremiumControls.kt",
                pkg / "overlay/PremiumControls.kt")

# Keep the original input semantics and only intercept touches when editing.
patch(control,
      "        val pointerIndex = event.actionIndex",
      "        if (PremiumControls.editTouch(this, event)) return true\n\n        val pointerIndex = event.actionIndex")
# The existing imageBitmap property is still used, with a compact glass-style skin.
start = control.read_text()
anchor_start = "    override fun onDraw(canvas: Canvas) {"
anchor_end = "\n    override fun onSizeChanged("
a = start.find(anchor_start)
b = start.find(anchor_end, a)
if a < 0 or b < 0: raise RuntimeError("ControlView draw anchors missing")
start = start[:a] + """    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        PremiumControls.draw(this, canvas, imageBitmap)
    }
""" + start[b:]
control.write_text(start)

# Layouts stay CrossCode's own Combat/Menu/Direct implementations.
patch(feature,
      "            controller.bindControls(gamepadState, view, this, gameWrapper.getFeature(GameEventManager::class))",
      """            controller.bindControls(gamepadState, view, this, gameWrapper.getFeature(GameEventManager::class))
            de.radicalfishgames.crosscode.overlay.PremiumControls.register(view, controller.javaClass.simpleName)""")
patch(feature,
      "        isControllerReady = true",
      """        isControllerReady = true
        de.radicalfishgames.crosscode.overlay.PremiumControls.install(hostActivity)""")

# Fix minimum supported S23 Ultra build SDK only if requested by Gradle.
# Install alongside the user's existing CrossAndroid APK, never silently overwrite it.
build = root / "app/build.gradle"
patch(build,
      'applicationId "de.radicalfishgames.crosscode"',
      'applicationId "de.radicalfishgames.crosscode.premium"')

print("CrossCode premium controls applied:")
print("  - Existing combat/menu bindings unchanged")
print("  - Premium glass UI and separate COMBAT/MENU saved layouts")
print("  - Per-button drag, pinch/size, reset, opacity, hide and restore")
print("  - User game data not included")
