# CrossCode Premium Touch — Galaxy S23 Ultra

## What this actually modifies

This is a **source-level CrossCode control overlay**, not a re-skin of the unrelated Alabaster Dawn game. The starting APK supplied for analysis was **CrossCode-Mobile-v1-4-2-2.apk** (SHA-256 \`bbf5df334e600cb9a795c2192d7dacfa3fb17061b01fbfbb6d336dd403a530f1\`), whose classes are from Namnodorel's CrossAndroid project.

The implementation builds on the publicly available CrossDroid source (a later CrossAndroid-derived host); **it does not modify that exact old APK in place**. It runs as a *separate app* with package name \`de.radicalfishgames.crosscode.premium\` to keep your installed original and its saves untouched. You must supply a legitimate copy of CrossCode game files. Do not uninstall the original or delete its app data.

Upstream CrossDroid code is not bundled here: its upstream CrossAndroid ancestry has no explicit reuse licence. This repository contains our own patch files only; the GitHub workflow fetches the upstream at a pinned revision for your personal build. CrossCode assets are not included.

## What the controller does

- Compact dark glass design (replaces oversized circular bitmaps), cyan movement and red aiming accents.
- CrossCode-specific MOVE, AIM/ATK, DASH, GUARD, ELEMENT, QUICK, MENU and SWIPE MODE labels.
- **Existing** CrossAndroid gamepad and gesture bindings, combat art timing and automatic menu transitions remain unchanged.
- Small top-left **HIDE / SHOW** and **EDIT / DONE** controls.
- EDIT allows individual finger dragging, two-finger pinch-resizing, **− / +** sizing, **RESET** for the selected control, **FADE** cycling between opacity choices.
- Persists positions and sizes by control and by screen type (CombatLayout, MenuLayout, DirectLayout); values are relative to screen dimensions to survive rotation/window changes.
- Hide/show is always accessible, and the premium controls do not interfere with the game's JS input bridge while editing is off.

## Build an APK on GitHub

1. Visit **Actions → Build CrossCode Premium Touch → Run workflow** on this repository, selecting branch \`crosscode-premium\`.
2. Open the finished run and download the artifact **CrossCode-Premium-S23-debug**.
3. Unzip the artifact and install the APK on your S23 Ultra. Because the app has a separate ID, it installs *beside* your old CrossAndroid app.
4. Supply your own CrossCode assets using the CrossDroid launcher. The new package has separate app storage, so an existing OBB or data folder from the original APK is **not** auto-imported. Back up your existing saves first.
5. Tap EDIT, drag the MOVE and AIM pads (or any button), pinch or press +/- to resize, then DONE. Your controls stay saved per CrossCode mode.

**Build status:** automated compilation is attempted in GitHub Actions. Passing a build is distinct from hardware/gameplay verification. The controls must be checked on the actual S23 Ultra before replacing a working setup.

## Source files

- \`PremiumControls.kt\` — look, editor, input interception only during editing, and per-mode SharedPreferences.
- \`apply.py\` — safely inserts the premium skin into CrossDroid's current source without altering the CrossCode combat/menu input classes.
- \`../.github/workflows/crosscode-premium.yml\` — reproducible Android build using a fixed upstream commit.

Original sources: [CrossAndroid by Namnodorel](https://gitlab.com/Namnodorel/crossandroid), [CrossDroid by dain98](https://github.com/dain98/CrossDroid), [Alabaster Dawn host](https://github.com/moronigranja/alabaster-android). None of these projects is endorsed by Radical Fish Games. CrossCode © Radical Fish Games.
