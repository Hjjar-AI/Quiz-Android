"""Export a shaped Arabic launch wordmark as an Android vector (host Pango/Cairo)."""
from pathlib import Path
import ctypes
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
NAME = next(node.text for node in ET.parse(RES / "values/strings.xml").getroot()
            if node.attrib.get("name") == "launcher_name")
fontconfig = ctypes.CDLL("libfontconfig.so.1")
fontconfig.FcConfigAppFontAddFile.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
assert fontconfig.FcConfigAppFontAddFile(None, str(RES / "font/amiri.ttf").encode())

cairo = ctypes.CDLL("libcairo.so.2")
pango = ctypes.CDLL("libpango-1.0.so.0")
pangocairo = ctypes.CDLL("libpangocairo-1.0.so.0")
ptr, integer, double = ctypes.c_void_p, ctypes.c_int, ctypes.c_double

def bind(library, name, result, *arguments):
    function = getattr(library, name)
    function.restype, function.argtypes = result, arguments
    return function

surface = bind(cairo, "cairo_image_surface_create", ptr, integer, integer, integer)(0, 288, 288)
context = bind(cairo, "cairo_create", ptr, ptr)(surface)
layout = bind(pangocairo, "pango_cairo_create_layout", ptr, ptr)(context)
font = bind(pango, "pango_font_description_from_string", ptr, ctypes.c_char_p)(b"Amiri")
bind(pango, "pango_font_description_set_absolute_size", None, ptr, double)(font, 80 * 1024)
bind(pango, "pango_layout_set_font_description", None, ptr, ptr)(layout, font)
bind(pango, "pango_layout_set_text", None, ptr, ctypes.c_char_p, integer)(layout, NAME.encode(), -1)
bind(pangocairo, "pango_cairo_layout_path", None, ptr, ptr)(context, layout)
extents = [double() for _ in range(4)]
# Ink bounds exclude empty glyph-advance moves (Amiri has generous font metrics).
bind(cairo, "cairo_fill_extents", None, ptr, *([ctypes.POINTER(double)] * 4))(
    context, *(ctypes.byref(value) for value in extents))
left, top, right, bottom = (value.value for value in extents)

def point(x, y):
    return f"{144 + (x - (left + right) / 2) * scale:.3f},{144 + (y - (top + bottom) / 2) * scale:.3f}"

class Header(ctypes.Structure):
    _fields_ = [("kind", integer), ("length", integer)]

class Point(ctypes.Structure):
    _fields_ = [("x", double), ("y", double)]

class PathData(ctypes.Union):
    _fields_ = [("header", Header), ("point", Point)]

class CairoPath(ctypes.Structure):
    _fields_ = [("status", integer), ("data", ctypes.POINTER(PathData)), ("count", integer)]

outline = bind(cairo, "cairo_copy_path", ctypes.POINTER(CairoPath), ptr)(context)
assert outline.contents.status == 0
# Fit the actual calligraphy to the circular mask, keeping Amiri's tall vowel marks.
coords, index = [], 0
while index < outline.contents.count:
    entry = outline.contents.data[index].header
    coords.extend((outline.contents.data[index + offset].point.x,
                   outline.contents.data[index + offset].point.y)
                  for offset in range(1, entry.length))
    index += entry.length
radius = max(((x - (left + right) / 2) ** 2 + (y - (top + bottom) / 2) ** 2) ** 0.5
             for x, y in coords)
# The system's 192dp circular mask leaves a small margin for outlines and the pen.
scale = min(184 / (right - left), 94 / radius)
reveal_left = 144 - scale * (right - left) / 2
reveal_right = 144 + scale * (right - left) / 2
commands, index = [], 0
while index < outline.contents.count:
    entry = outline.contents.data[index].header
    if entry.kind == 3:
        commands.append("Z")
    else:
        prefix = {0: "M", 1: "L", 2: "C"}[entry.kind]
        points = [outline.contents.data[index + offset].point for offset in range(1, entry.length)]
        commands.append(prefix + " ".join(point(value.x, value.y) for value in points))
    index += entry.length
bind(cairo, "cairo_path_destroy", None, ctypes.POINTER(CairoPath))(outline)
path = " ".join(commands)
(RES / "drawable/startup_wordmark.xml").write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Shaped {NAME}; regenerate with /usr/bin/python3 artwork/generate_startup_wordmark.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="288dp" android:height="288dp"
    android:viewportWidth="288" android:viewportHeight="288">
    <group>
        <clip-path android:name="reveal" android:pathData="M48,48 L240,48 L240,240 L48,240 Z" />
        <path android:fillColor="@color/startup_foreground" android:pathData="{path}" />
    </group>
    <group android:name="pen" android:translateX="{reveal_right:.3f}">
        <path android:name="penDot" android:fillColor="@color/startup_foreground" android:fillAlpha="0"
            android:pathData="M-3.5,152 A3.5,3.5 0,1 0,3.5,152 A3.5,3.5 0,1 0,-3.5,152 Z" />
    </group>
</vector>
''')
(RES / "animator/startup_reveal.xml").write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"
    android:propertyName="pathData" android:valueType="pathType"
    android:valueFrom="M{reveal_right:.3f},48 L240,48 L240,240 L{reveal_right:.3f},240 Z"
    android:valueTo="M{reveal_left:.3f},48 L240,48 L240,240 L{reveal_left:.3f},240 Z"
    android:duration="@integer/startup_reveal_duration" android:interpolator="@android:interpolator/linear" />
''')
(RES / "animator/startup_pen_motion.xml").write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"
    android:propertyName="translateX" android:valueType="floatType"
    android:valueFrom="{reveal_right:.3f}" android:valueTo="{reveal_left:.3f}"
    android:duration="@integer/startup_reveal_duration" android:interpolator="@android:interpolator/linear" />
''')
print(f"Exported {NAME}: {scale * (right-left):.1f} × {scale * (bottom-top):.1f} within the system splash mask.")
