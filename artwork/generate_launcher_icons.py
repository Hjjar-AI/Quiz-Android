#!/usr/bin/env python3
"""Export enlarged transparent launcher assets from the master PNG; no Android build.

Requires ffmpeg. Run: python3 artwork/generate_launcher_icons.py
The edited master stays unchanged; FFmpeg handles cropping/scaling/compositing.
All Android versions use the density PNGs directly. Do not add adaptive-icon
overrides or roundIcon: those opt into launcher-controlled masks/backgrounds.
"""
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
SOURCE = ROOT / 'artwork/app_icon.png'
DENSITIES = {'ldpi': 36, 'mdpi': 48, 'tvdpi': 64, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}


def run_ffmpeg(*args):
    return subprocess.run(
        ['ffmpeg', '-hide_banner', *map(str, args)],
        check=True, capture_output=True, text=True,
    )


def artwork_crop():
    # Find the opaque artwork rather than retaining empty generated margins.
    # Four surrounding pixels retain antialiased edges at the source resolution.
    result = run_ffmpeg('-i', SOURCE, '-vf', 'alphaextract,bbox=min_val=128',
                        '-frames:v', '1', '-f', 'null', '-')
    match = re.search(r'x1:(\d+) x2:(\d+) y1:(\d+) y2:(\d+) w:(\d+) h:(\d+)', result.stderr)
    if not match:
        raise RuntimeError('Transparent master has no opaque icon bounds.')
    x1, x2, y1, y2, _, _ = map(int, match.groups())
    dimensions = re.search(r'Video:.*? (\d+)x(\d+)[, ]', result.stderr)
    if not dimensions:
        raise RuntimeError('Cannot read source dimensions.')
    width, height = map(int, dimensions.groups())
    left, top = max(0, x1 - 4), max(0, y1 - 4)
    right, bottom = min(width, x2 + 5), min(height, y2 + 5)
    return f'crop={right - left}:{bottom - top}:{left}:{top}'


def main():
    if not shutil.which('ffmpeg'):
        raise RuntimeError('Install ffmpeg to export the launcher assets.')
    if not SOURCE.is_file():
        raise RuntimeError(f'Missing transparent master: {SOURCE}')
    crop = artwork_crop()
    for density, size in DENSITIES.items():
        mark_size = round(size * .9)
        filters = (f'{crop},scale={mark_size}:{mark_size}:force_original_aspect_ratio=decrease:flags=lanczos,'
                   f'format=rgba,pad={size}:{size}:(ow-iw)/2:(oh-ih)/2:color=black@0,format=rgba')
        target = RES / f'mipmap-{density}/ic_launcher.png'
        target.parent.mkdir(parents=True, exist_ok=True)
        run_ffmpeg('-v', 'error', '-y', '-i', SOURCE, '-vf', filters,
                    '-frames:v', '1', '-threads', '1', target)
        print(f'{density}: {size}x{size}')
    # Reference adaptive foreground only; not selected by the manifest. The
    # app uses the transparent density PNGs on every Android version.
    # Android adaptive foreground: 52dp artwork on the 108dp layer. The
    # asymmetric paper/handle needs more mask clearance than a circular mark.
    target = RES.parents[1] / 'shared/res/drawable-nodpi/ic_launcher_artwork.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    filters = (f'{crop},scale=208:208:force_original_aspect_ratio=decrease:flags=lanczos,'
               'format=rgba,pad=432:432:(ow-iw)/2:(oh-ih)/2:color=black@0,format=rgba')
    run_ffmpeg('-v', 'error', '-y', '-i', SOURCE, '-vf', filters,
                '-frames:v', '1', '-threads', '1', target)
    print('adaptive foreground: 432x432, transparent')


if __name__ == '__main__':
    main()
