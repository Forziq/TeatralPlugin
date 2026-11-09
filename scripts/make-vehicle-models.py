#!/usr/bin/env python3
"""Builds the tank and jeep models (shapes plus a painted 128x128 texture) for BetterModel.

Usage: python3 scripts/make-vehicle-models.py
Writes src/main/resources/models/tank.bbmodel and jeep.bbmodel. Needs Pillow (pip install pillow).
Re-running overwrites any changes made to those files in Blockbench, so edit this script instead.

Rules the shapes follow (BetterModel 2.2.0 / Minecraft item models):
- forward is -Z, units are pixels (16 per block), origin is the anchor's feet;
- a cube may turn about one axis only, by -45, -22.5, 22.5 or 45 degrees, so BetterModel keeps it in its bone;
- bone names and pivots the Java code relies on stay fixed (see VehicleModelTest).
"""
import base64
import io
import json
import math
import random
import uuid
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/models"
TEX = 128
NS = uuid.UUID("6b1d3c1e-35a0-4f7e-9d0c-6a3f0c1b7e35")

# ---------------------------------------------------------------- texture

# Atlas regions (x0, y0, x1, y1) in texture pixels.
R = {
    "olive_sq": (0, 0, 32, 32),
    "star": (32, 0, 96, 26),
    "metal_long": (32, 26, 96, 32),
    "engine": (96, 0, 128, 32),
    "wheel_road": (0, 32, 32, 64),
    "wheel_jeep": (32, 32, 64, 64),
    "dark_sq": (64, 32, 96, 64),
    "hatch": (96, 32, 112, 48),
    "metal": (112, 32, 128, 48),
    "rubber": (96, 48, 112, 64),
    "seat": (112, 48, 128, 64),
    "track_side_long": (0, 64, 128, 80),
    "track_top_long": (0, 80, 128, 88),
    "olive_long": (0, 88, 128, 104),
    "dark_long": (0, 104, 128, 112),
    "glass": (0, 112, 16, 128),
    "light": (16, 112, 32, 128),
    "track_sq": (32, 112, 48, 128),
    "sprocket": (48, 112, 64, 128),
    "olive_plain": (64, 112, 80, 128),
    "dark_steel": (80, 112, 96, 128),
    "grille": (96, 112, 112, 128),
    "tyre": (112, 112, 128, 128),
}

OLIVE = (76, 86, 48)
OLIVE_WORN = (128, 134, 86)
OLIVE_DARK = (50, 58, 33)
GRIME = (40, 38, 28)
RUBBER = (34, 33, 31)
STEEL = (58, 56, 51)
STEEL_LIGHT = (104, 100, 90)
RUST = (96, 68, 46)
GUNMETAL = (60, 66, 48)
RED = (176, 32, 30)
RED_DARK = (104, 18, 16)


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


class Painter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rng = random.Random(seed)

    def put(self, x, y, c, a=255):
        self.px[x, y] = tuple(max(0, min(255, int(round(v)))) for v in c) + (a,)

    def noisy(self, c, amt=5):
        d = self.rng.uniform(-amt, amt)
        return tuple(v + d for v in c)

    def panel(self, name, base, edge, dark, rivets=False, chips=0.02, grime=0.25, noise=5):
        """Painted plate: worn lighter rim, dirt towards the bottom, a few paint chips."""
        x0, y0, x1, y1 = R[name]
        h = y1 - y0
        for y in range(y0, y1):
            t = (y - y0) / max(1, h - 1)
            for x in range(x0, x1):
                d = min(x - x0, x1 - 1 - x, y - y0, y1 - 1 - y)
                c = mix(base, dark, grime * t * t)
                if d == 0:
                    c = mix(c, edge, 0.7 if self.rng.random() < 0.85 else 0.25)
                elif d == 1:
                    c = mix(c, edge, 0.28)
                if self.rng.random() < chips:
                    c = mix(c, edge, 0.55)
                self.put(x, y, self.noisy(c, noise))
        if rivets:
            for x in range(x0 + 3, x1 - 2, 6):
                for y in (y0 + 3, y1 - 4):
                    self.put(x, y, mix(base, GRIME, 0.5))
                    self.put(x - 1, y - 1, mix(base, edge, 0.6))

    def star(self):
        name = "star"
        self.panel(name, OLIVE, OLIVE_WORN, OLIVE_DARK)
        x0, y0, x1, y1 = R[name]
        # The face this lands on is about 22 x 8 px, the region 64 x 26: draw the star in face space so it isn't squashed.
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
        ro = 10.5
        pts = []
        for k in range(10):
            ang = -math.pi / 2 + k * math.pi / 5
            r = ro if k % 2 == 0 else ro * 0.42
            pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))

        def inside(px, py):
            n, ok = len(pts), False
            for i in range(n):
                xa, ya = pts[i]
                xb, yb = pts[(i + 1) % n]
                if (ya > py) != (yb > py) and px < (xb - xa) * (py - ya) / (yb - ya) + xa:
                    ok = not ok
            return ok

        for y in range(y0, y1):
            for x in range(x0, x1):
                if inside(x + 0.5, y + 0.5):
                    edge = not all(inside(x + 0.5 + dx, y + 0.5 + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                    c = RED_DARK if edge else RED
                    if not edge and self.rng.random() < 0.06:
                        c = mix(RED, OLIVE_WORN, 0.5)  # worn paint
                    self.put(x, y, self.noisy(c, 6))

    def disc(self, name, colour_at):
        """Round part seen from the side; colour_at(r, angle_deg) with r = 1 at the region's half width."""
        x0, y0, x1, y1 = R[name]
        cx, cy, half = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2
        for y in range(y0, y1):
            for x in range(x0, x1):
                dx, dy = (x + 0.5 - cx) / half, (y + 0.5 - cy) / half
                r = math.hypot(dx, dy)
                ang = math.degrees(math.atan2(dy, dx)) % 360
                self.put(x, y, self.noisy(colour_at(r, ang), 4))

    def fill(self, name, fn):
        x0, y0, x1, y1 = R[name]
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = fn(x - x0, y - y0, x1 - x0, y1 - y0)
                if len(c) == 4:
                    self.put(x, y, c[:3], c[3])
                else:
                    self.put(x, y, c)


def road_wheel(r, ang):
    # Rubber tyre, steel rim, olive disc with eight holes, hub. Eight-fold symmetric so the octagon pieces line up.
    if r > 0.80:
        return mix(RUBBER, STEEL_LIGHT, 0.25) if r < 0.84 else RUBBER
    if r > 0.74:
        return mix(OLIVE, OLIVE_WORN, 0.6)
    if r > 0.30:
        a = math.radians(((ang + 22.5) % 45) - 22.5)
        hx, hy = 0.55 * math.cos(a) - 0.0, 0.55 * math.sin(a)
        rx, ry = r * math.cos(a), r * math.sin(a)
        if math.hypot(rx - 0.55, ry) < 0.10:
            return (24, 26, 20)
        return mix(OLIVE, OLIVE_DARK, 0.15 + 0.2 * (r - 0.3))
    if r > 0.20:
        return mix(OLIVE_DARK, OLIVE_WORN, 0.2)
    if r > 0.12 and int(ang // 22.5) % 2 == 0:
        return STEEL_LIGHT
    return STEEL


def jeep_wheel(r, ang):
    if r > 0.62:
        if r > 0.86 and int(ang // 11.25) % 2 == 0:
            return (22, 22, 21)  # tread blocks
        return (38, 37, 35)
    if r > 0.56:
        return (60, 60, 56)
    if r > 0.40:
        return OLIVE
    if r > 0.30:
        return mix(OLIVE, OLIVE_WORN, 0.45)
    if r > 0.14:
        return OLIVE_DARK
    return STEEL


def sprocket(r, ang):
    if r > 0.72:
        return STEEL_LIGHT if (ang % 22.5) < 11 else RUBBER
    if r > 0.30:
        return mix(OLIVE_DARK, STEEL, 0.3)
    return STEEL


def paint():
    p = Painter(35)
    p.panel("olive_sq", OLIVE, OLIVE_WORN, OLIVE_DARK, rivets=True)
    p.star()
    p.panel("olive_long", OLIVE, OLIVE_WORN, OLIVE_DARK, rivets=True)
    p.panel("olive_plain", OLIVE, OLIVE, OLIVE_DARK, chips=0.01, grime=0.1)
    p.panel("dark_sq", OLIVE_DARK, mix(OLIVE_DARK, OLIVE_WORN, 0.5), GRIME)
    p.panel("dark_long", OLIVE_DARK, mix(OLIVE_DARK, OLIVE_WORN, 0.5), GRIME)
    p.panel("hatch", OLIVE, OLIVE_WORN, OLIVE_DARK)
    x0, y0, _, _ = R["hatch"]
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 5.2 < r < 6.4:
                p.put(x0 + x, y0 + y, p.noisy(mix(OLIVE, OLIVE_WORN, 0.55)))
            elif r <= 5.2 and 6 <= y <= 8 and 4 <= x <= 11 and y != 7:
                p.put(x0 + x, y0 + y, STEEL)  # handle
    p.panel("metal", GUNMETAL, mix(GUNMETAL, STEEL_LIGHT, 0.5), GRIME, chips=0.03, grime=0.1)
    p.fill("metal_long", lambda x, y, w, h: p.noisy(
        mix(GUNMETAL, STEEL_LIGHT, 0.45) if y == 1 else mix(GUNMETAL, GRIME, 0.35) if y == h - 1 else GUNMETAL, 3))
    p.fill("dark_steel", lambda x, y, w, h: p.noisy((40, 42, 38), 4))
    p.fill("rubber", lambda x, y, w, h: p.noisy(RUBBER, 3))
    p.fill("tyre", lambda x, y, w, h: p.noisy((22, 22, 21) if (x + y) % 6 < 2 else (38, 37, 35), 3))
    p.fill("seat", lambda x, y, w, h: p.noisy((104, 92, 62) if x % 5 == 0 or y % 5 == 0 else (124, 110, 74), 4))
    p.fill("glass", lambda x, y, w, h: ((200, 220, 225, 170) if abs(x - y - 3) < 2 else (150, 176, 184, 115)))
    p.disc("light", lambda r, a: (60, 60, 56) if r > 0.85 else (226, 220, 170) if r > 0.3 else (250, 246, 210))

    def track_link(x, y, w, h, top):
        lx = x % 8
        if lx == 7:
            return (26, 25, 23)  # gap between links
        c = STEEL
        if top:
            if lx in (2, 3) or (x // 8 % 2 == 0 and 3 <= y <= 4 and lx in (4, 5)):
                c = STEEL_LIGHT  # grouser and guide horn
        else:
            if y <= 1 or y >= h - 2:
                c = mix(STEEL, STEEL_LIGHT, 0.6)  # worn edge
            elif lx == 3 and y in (h // 2 - 1, h // 2):
                c = (26, 25, 23)  # pin
        if p.rng.random() < 0.05:
            c = RUST
        return p.noisy(c, 5)

    p.fill("track_side_long", lambda x, y, w, h: track_link(x, y, w, h, False))
    p.fill("track_top_long", lambda x, y, w, h: track_link(x, y, w, h, True))
    p.fill("track_sq", lambda x, y, w, h: track_link(x, y, w, h, False))
    p.panel("engine", OLIVE, OLIVE_WORN, OLIVE_DARK, rivets=True)
    x0, y0, _, _ = R["engine"]
    for y in range(8, 24):
        for x in range(5, 27):
            if y % 3 == 0:
                p.put(x0 + x, y0 + y, p.noisy(GRIME, 3))  # louvres
            elif y % 3 == 1:
                p.put(x0 + x, y0 + y, p.noisy(mix(OLIVE, OLIVE_WORN, 0.35), 3))
    p.disc("wheel_road", road_wheel)
    p.disc("wheel_jeep", jeep_wheel)
    p.disc("sprocket", sprocket)
    p.panel("grille", OLIVE, OLIVE_WORN, OLIVE_DARK, chips=0.0)
    x0, y0, _, _ = R["grille"]
    for y in range(3, 13):
        for x in range(4, 13):
            if x % 2 == 0:
                p.put(x0 + x, y0 + y, (20, 22, 18))  # the slots
    for cx in (2, 13):
        for y in range(4, 8):
            for x in range(cx - 1, cx + 2):
                p.put(x0 + x, y0 + y, (226, 220, 170) if (x, y) != (cx - 1, 4) else (250, 246, 210))
    return p.img


def png_data_uri(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


# ---------------------------------------------------------------- geometry

TAN225 = math.tan(math.radians(22.5))


class Model:
    def __init__(self, name):
        self.name = name
        self.elements = []
        self.groups = []
        self.roots = []
        self.count = 0

    def uid(self, key):
        self.count += 1
        return str(uuid.uuid5(NS, f"{self.name}/{key}/{self.count}"))

    def group(self, name, origin, parent=None):
        g = {"uuid": self.uid("group:" + name), "export": True, "locked": False, "origin": list(origin),
             "rotation": [0, 0, 0], "color": 0, "name": name, "children": [], "reset": False, "shade": True,
             "mirror_uv": False, "selected": False, "visibility": True, "autouv": 0, "isOpen": True}
        self.groups.append(g)
        node = {"uuid": g["uuid"], "isOpen": True, "children": [], "_origin": origin}
        (parent["children"] if parent else self.roots).append(node)
        return node

    def cube(self, parent, name, frm, to, mat="olive", rot=None, origin=None, faces=None):
        """mat: material for every face; faces: {face: material or (region, uv, rotation)} overrides."""
        if origin is None:
            origin = parent["_origin"]
        el = {"name": name, "box_uv": False, "render_order": "default", "locked": False,
              "allow_mirror_modeling": True, "from": [r6(v) for v in frm], "to": [r6(v) for v in to],
              "autouv": 0, "color": 0, "origin": [r6(v) for v in origin], "faces": {}, "type": "cube",
              "uuid": self.uid(name)}
        if rot is not None:
            el["rotation"] = list(rot)
        dx, dy, dz = (to[i] - frm[i] for i in range(3))
        size = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}
        for f, (du, dv) in size.items():
            m = (faces or {}).get(f, mat)
            el["faces"][f] = face(m, du, dv)
        self.elements.append(el)
        parent["children"].append(el["uuid"])
        return el

    def json(self):
        def clean(nodes):
            out = []
            for n in nodes:
                if isinstance(n, str):
                    out.append(n)
                else:
                    out.append({"uuid": n["uuid"], "isOpen": True, "children": clean(n["children"])})
            return out

        img = paint()
        return {
            "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
            "name": self.name, "model_identifier": "", "visible_box": [1, 1, 0], "variable_placeholders": "",
            "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {},
            "resolution": {"width": TEX, "height": TEX},
            "elements": self.elements, "groups": self.groups, "outliner": clean(self.roots),
            "textures": [{
                "name": self.name + ".png", "path": "", "folder": "", "namespace": "", "id": "0", "group": "",
                "width": TEX, "height": TEX, "uv_width": TEX, "uv_height": TEX, "particle": False,
                "use_as_default": True, "layers_enabled": False, "sync_to_project": "", "render_mode": "default",
                "render_sides": "auto", "pbr_channel": "color", "frame_time": 1, "frame_order_type": "loop",
                "frame_order": "", "frame_interpolate": False, "visible": True, "internal": True, "saved": False,
                "uuid": str(uuid.uuid5(NS, self.name + "/texture")), "source": png_data_uri(img)}],
            "animations": [],
        }


def r6(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


MATERIALS = {
    # name: (square-ish region, long region or None)
    "olive": ("olive_sq", "olive_long"),
    "dark": ("dark_sq", "dark_long"),
    "track_side": ("track_sq", "track_side_long"),
    "track_top": ("track_sq", "track_top_long"),
    "metal": ("metal", "metal_long"),
}


def face(m, du, dv):
    if isinstance(m, tuple):  # (region, uv, rotation) given directly
        region, uv, rotation = m
        f = {"uv": [r6(v) for v in uv], "texture": 0}
        if rotation:
            f["rotation"] = rotation
        return f
    sq, long_ = MATERIALS.get(m, (m, None))
    region = sq
    rotation = 0
    lo, hi = min(du, dv), max(du, dv)
    if long_ and lo > 0 and hi / lo >= 3:
        region = long_
        rotation = 90 if dv > du else 0
    elif m == "olive" and hi <= 3:
        region = "olive_plain"
    x0, y0, x1, y1 = R[region]
    f = {"uv": [x0, y0, x1, y1], "texture": 0}
    if rotation:
        f["rotation"] = rotation
    return f


def wheel(model, parent, name, axis, centre, radius, x_range, side_region, tread="rubber"):
    """Round part as four overlapping bars at 0, 45, 90 and 135 degrees: together they make an exact octagon.
    axis 'x': the wheel turns about X (road wheels); axis 'z': about Z (the jeep's spare). The side picture is
    eight-fold symmetric, so the turned bars can reuse the same part of it."""
    e = radius * TAN225  # half an octagon edge
    x0, y0, x1, y1 = R[side_region]
    cu, cv, half = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2
    bars = [(radius, e, 0), (e, radius, 0), (radius, e, 45), (e, radius, 45)]
    a, b = x_range
    for i, (h1, h2, angle) in enumerate(bars):
        inset = 0.05 * i  # keeps overlapping side faces off the same plane
        uv = (cu - h1 / radius * half, cv - h2 / radius * half, cu + h1 / radius * half, cv + h2 / radius * half)
        side = (side_region, uv, 0)
        if axis == "x":
            c_y, c_z = centre
            frm = (a + inset, c_y - h2, c_z - h1)
            to = (b - inset, c_y + h2, c_z + h1)
            faces = {"east": side, "west": side}
            rot = [angle, 0, 0] if angle else None
            origin = (0, c_y, c_z)
        else:
            c_x, c_y = centre
            frm = (c_x - h1, c_y - h2, a + inset)
            to = (c_x + h1, c_y + h2, b - inset)
            faces = {"north": side, "south": side}
            rot = [0, 0, angle] if angle else None
            origin = (c_x, c_y, 0)
        model.cube(parent, f"{name}_{i}", frm, to, tread, rot=rot, origin=origin, faces=faces)


def tank():
    m = Model("tank")
    hull = m.group("hull", (0, 16, 0))
    m.cube(hull, "lower_hull", (-15, 3, -33), (15, 14, 35), "dark")
    m.cube(hull, "deck", (-23.5, 14, -22), (23.5, 16, 34))
    # Sloped front: a thick plate tipped 22.5 degrees down from the deck's front edge; its end face is the lower nose.
    m.cube(hull, "glacis", (-15, 8, -39.3), (15, 16, -22), rot=[-22.5, 0, 0], origin=(0, 16, -22))
    for s in (-1, 1):
        lo, hi = sorted((s * 15, s * 23.5))
        m.cube(hull, "glacis_side", (lo, 14, -27), (hi, 16, -22), rot=[-22.5, 0, 0], origin=(0, 16, -22))
        flo, fhi = sorted((s * 15, s * 24))
        m.cube(hull, "fender_front", (flo, 14, -40.5), (fhi, 15, -26))
        m.cube(hull, "fender_rear", (flo, 14, 34), (fhi, 15, 40.5))
        tlo, thi = sorted((s * 19.5, s * 23.5))
        m.cube(hull, "fuel_tank", (tlo, 16, 22), (thi, 19.5, 33), "dark")
    m.cube(hull, "rear_plate", (-15, 6, 34), (15, 16, 39.4), rot=[22.5, 0, 0], origin=(0, 16, 34))
    m.cube(hull, "engine_deck", (-10, 16, 18), (10, 16.5, 32), "olive", faces={"up": "engine"})
    m.cube(hull, "exhaust", (-9, 8, 36.5), (-6, 11, 41), "dark_steel")
    m.cube(hull, "exhaust", (6, 8, 36.5), (9, 11, 41), "dark_steel")
    on_glacis = dict(rot=[-22.5, 0, 0], origin=(0, 16, -22))
    m.cube(hull, "driver_hatch", (-11, 16, -31), (-3, 16.75, -25), "olive", faces={"up": "hatch"}, **on_glacis)
    m.cube(hull, "headlight", (-13, 16, -36), (-10, 17.5, -34), "dark_steel", faces={"north": "light"}, **on_glacis)
    m.cube(hull, "mg_ball", (5, 16, -33), (9, 17.5, -29), "dark", **on_glacis)
    m.cube(hull, "mg_barrel", (6.5, 16.4, -37), (7.5, 17.1, -33), "dark_steel", **on_glacis)
    for s in (-1, 1):
        x0, x1 = sorted((s * 15.5, s * 24))
        track = {"up": "track_top", "down": "track_top", "east": "track_side", "west": "track_side"}
        m.cube(hull, "track_bottom", (x0, 0, -32), (x1, 2, 33), "track_side", faces=track)
        m.cube(hull, "track_top", (x0, 12, -39), (x1, 14, 39), "track_side", faces=track)
        m.cube(hull, "track_front_slope", (x0, 0, -40.5), (x1, 2, -32), "track_sq", rot=[45, 0, 0], origin=(0, 1, -32))
        m.cube(hull, "track_front", (x0, 6, -40), (x1, 12, -38), "track_sq")
        m.cube(hull, "track_rear_slope", (x0, 0, 33), (x1, 2, 41.5), "track_sq", rot=[-45, 0, 0], origin=(0, 1, 33))
        m.cube(hull, "track_rear", (x0, 6, 38), (x1, 12, 40), "track_sq")
        wx = sorted((s * 16, s * 23))
        wheel(m, hull, "idler", "x", (8, -35), 3, wx, "wheel_road", "dark_steel")
        wheel(m, hull, "sprocket", "x", (8, 35), 3, wx, "sprocket", "dark_steel")
    side_names = {-1: "l", 1: "r"}
    for s in (-1, 1):
        for i, zc in enumerate((-26, -13, 0, 13, 26), start=1):
            g = m.group(f"wheel_{side_names[s]}{i}", (s * 19.5, 7.25, zc), hull)
            wheel(m, g, f"wheel_{side_names[s]}{i}", "x", (7.25, zc), 5.25, sorted((s * 16, s * 23)), "wheel_road")

    turret = m.group("turret", (0, 16, 0), hull)
    m.cube(turret, "turret_ring", (-9, 16, -9), (9, 17, 9), "dark")
    m.cube(turret, "turret_body", (-9, 17, -10), (9, 25, 12), "olive", faces={"east": "star", "west": "star"})
    m.cube(turret, "turret_nose", (-7, 17, -13), (7, 24, -10))
    m.cube(turret, "turret_bustle", (-7, 18, 12), (7, 24.5, 16))
    m.cube(turret, "turret_roof", (-8.5, 25, -9.5), (8.5, 26, 11.5))
    m.cube(turret, "hatch_opening", (-3, 26, -3), (3, 26.2, 3), "dark_steel")
    m.cube(turret, "hatch_lid", (-2.5, 26, 3.2), (3.5, 26.75, 9), "olive", faces={"up": "hatch"})
    m.cube(turret, "cupola", (-8, 26, 4), (-3, 28.5, 10), "olive", faces={"up": "hatch"})
    m.group("sp_gunner", (0, 18, 0), turret)  # at the turret pivot, so turning the turret doesn't move the seat

    barrel = m.group("barrel", (0, 21.5, -12), turret)
    m.cube(barrel, "mantlet", (-4.5, 18.5, -16), (4.5, 24.5, -11.5))
    m.cube(barrel, "gun_sleeve", (-2, 19.5, -22), (2, 23.5, -16))
    m.cube(barrel, "gun_tube", (-1.25, 20.25, -51), (1.25, 22.75, -22), "metal")
    m.cube(barrel, "muzzle_brake", (-1.75, 19.75, -54), (1.75, 23.25, -51), "dark_steel")
    m.group("muzzle", (0, 21.5, -54), barrel)  # TurretModel: barrel pivot -12, muzzle 42 px further

    m.group("p_driver", (-7, 14, -27.5), hull)  # in the driver's hatch, front left of the sloped front
    return m


def jeep():
    m = Model("jeep")
    body = m.group("body", (0, 9, 0))
    m.cube(body, "chassis", (-9, 3, -26), (9, 5, 24), "dark")
    m.cube(body, "floor", (-11, 5, -7), (11, 6, 24), "dark")
    for s in (-1, 1):
        a, b = sorted((s * 11, s * 10))
        m.cube(body, "side_low", (a, 6, -7), (b, 9, 24))
        m.cube(body, "side_rear", (a, 9, 5), (b, 12, 24))
        m.cube(body, "side_post", (a, 9, -7), (b, 12, -5))
        fa, fb = sorted((s * 15.5, s * 8))
        m.cube(body, "front_fender", (fa, 10, -25), (fb, 11, -9))
        m.cube(body, "front_fender_lip", (fa, 8, -25.5), (fb, 10, -24.5))
        ra, rb = sorted((s * 16, s * 11))
        m.cube(body, "rear_fender", (ra, 11, 10), (rb, 12, 24))
    m.cube(body, "rear_wall", (-10, 6, 23), (10, 12, 24))
    m.cube(body, "dash", (-10, 6, -8), (10, 13, -7))
    m.cube(body, "bonnet", (-8, 5, -26), (8, 13, -8))
    m.cube(body, "grille", (-8, 4, -27.5), (8, 13, -26), "olive", faces={"north": "grille"})
    m.cube(body, "bumper", (-13, 3, -29), (13, 5, -27.5), "dark")
    # Flat windscreen frame with two panes.
    m.cube(body, "screen_bottom", (-11, 13, -8), (11, 14, -7))
    m.cube(body, "screen_top", (-11, 20, -8), (11, 21, -7))
    m.cube(body, "screen_post", (-11, 14, -8), (-10, 20, -7))
    m.cube(body, "screen_post", (10, 14, -8), (11, 20, -7))
    m.cube(body, "screen_middle", (-0.5, 14, -8), (0.5, 20, -7))
    m.cube(body, "glass", (-10, 14, -7.75), (-0.5, 20, -7.25), "glass")
    m.cube(body, "glass", (0.5, 14, -7.75), (10, 20, -7.25), "glass")
    m.cube(body, "steering_wheel", (-8.5, 11, -5.5), (-2.5, 15, -5), "dark_steel", rot=[-22.5, 0, 0], origin=(-5.5, 13, -5.25))
    m.cube(body, "steering_column", (-6, 9, -7), (-5, 12, -5.5), "dark_steel")
    for x0, x1 in ((-9.5, -2), (2, 9.5)):
        m.cube(body, "front_seat", (x0, 6, -3), (x1, 8.5, 4), "seat")
        m.cube(body, "front_backrest", (x0, 8.5, 3), (x1, 14, 5), "seat")
    m.cube(body, "rear_seat", (-9.5, 6, 12), (9.5, 8.5, 20), "seat")
    m.cube(body, "rear_backrest", (-9.5, 8.5, 20), (9.5, 13, 22), "seat")
    m.cube(body, "jerry_can", (-9, 6, 24), (-4.5, 13, 27.5), "dark")
    m.cube(body, "spare_bracket", (0, 9.5, 24), (4, 11.5, 24.5), "dark_steel")
    wheel(m, body, "spare_wheel", "z", (2, 10.5), 5, (24.5, 28.5), "wheel_jeep", "tyre")
    for name, x, z in (("wheel_fl", -14, -17), ("wheel_fr", 14, -17), ("wheel_rl", -14, 17), ("wheel_rr", 14, 17)):
        g = m.group(name, (x, 5, z), body)
        wheel(m, g, name, "x", (5, z), 5, sorted((x - 2, x + 2)), "wheel_jeep", "tyre")
    m.group("p_driver", (-5.75, 8.5, 1), body)
    m.group("sp_passenger1", (5.75, 8.5, 1), body)  # front passenger
    m.group("sp_passenger2", (0, 8.5, 16), body)  # back seat
    return m


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for model in (tank(), jeep()):
        path = OUT / f"{model.name}.bbmodel"
        path.write_text(json.dumps(model.json(), separators=(",", ":")) + "\n", encoding="utf-8")
        print(f"wrote {path.relative_to(ROOT)} ({path.stat().st_size // 1024} KB, {len(model.elements)} cubes)")


if __name__ == "__main__":
    main()
