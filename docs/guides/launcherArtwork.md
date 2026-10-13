# Android launcher artwork

The launcher uses the paper/checklist/magnifier icon with the title **مُختبِر**: damma above م, kasra beneath ب. The painted checkerboard was removed using the built-in imagegen tool; the [PNG master](../../artwork/app_icon.png) has real alpha transparency. The user renamed the master and removed the obsolete SVG; only this PNG is used by the exporter. The original generated JPEG in Downloads remains untouched.

Regenerate resource PNGs with FFmpeg installed, from the Android directory:

```bash
python3 artwork/generate_launcher_icons.py
```

This is image asset conversion, not a Gradle build. The [exporter](../../artwork/generate_launcher_icons.py) crops the opaque artwork with an antialias margin, exports seven transparent legacy density PNGs with the mark at 90% of the canvas, and creates a transparent 432px adaptive foreground with a 52dp mark on the 108dp layer. The former green background is transparent in both legacy/adaptive assets. Android 26+ and 33+ full-color adaptive icons reference that foreground; Android 33+ themed icons use the outline vector below at 105% of its earlier size. Normal and round manifest icons continue to reference `@mipmap/ic_launcher`. Both the application (installer/system label) and launcher activity use the language-independent label **مُختبِر**; in-app localized names remain unchanged. System-themed launchers can supply their own background/tint independently of the app's transparent artwork.

The Android 33+ themed icon now uses the dedicated [monochrome vector](../../app/src/main/res/drawable/ic_launcher_monochrome.xml). Its transparent interiors preserve the checklist and magnifier when launchers tint the alpha mask; the opaque full-color paper silhouette previously lost those details. Exporting the PNGs does not overwrite this vector. Full-color legacy/adaptive assets use the PNG master.

## Built-in edit prompts

First edit:

> Edit target: the supplied Gemini-generated quiz app icon. Remove ONLY the fake gray and white checkerboard background completely, replacing it with actual alpha transparency. Preserve the exact original icon artwork: light gray folded paper with dark navy outline, Arabic title 'مُختبر' as drawn in the source without changing its letterforms, green check, red cross, empty third checkbox, dark short lines, and blue-gray magnifying glass and handle. Preserve original colors, geometry and details, do not redesign or add elements. Clean antialiased cutout edges with no checkerboard pixels or white/gray fringe outside the artwork. Paper and lens interior stay opaque. Keep the whole icon including the handle, centered with small transparent padding suitable for an Android launcher foreground. Output a high-resolution transparent PNG.

Final edit:

> Edit the provided transparent quiz icon. Change ONLY the Arabic title diacritic: it must read exactly مُختبِر. Preserve the existing damma above م and add the missing kasra (a short diagonal black vowel stroke) beneath the letter ب, distinct from and next to/below its existing dot, placed correctly on the ba near the left half of the word just before the final ر. No other letters, words or artwork changes. Preserve the paper, folded corner, green check, red X, third checkbox, magnifier, outlines, colors and proportions. Remove any nearly transparent stray pixels outside the icon; alpha zero outside the clean cutout, no checkerboard or solid background. Center the complete icon including its magnifier handle on a square transparent canvas with about 6% padding on all sides. All paper/lens interior remains opaque. Output transparent PNG.

## Verification

2026-10-10: inspected the Gemini original, both edits and the exported 192px legacy icon. Checked real alpha on the master/foreground, all seven density dimensions, opaque legacy backgrounds, XML references, exporter Python AST and whitespace. No Gradle build, install or launcher/device verification. Rebuild/install before checking circle/squircle/themed launchers; existing launcher caches can retain an old icon.
