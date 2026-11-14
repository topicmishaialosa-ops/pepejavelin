#!/usr/bin/env python3
"""Original voxel blades and hand-painted pixel materials. No downloaded assets.

The exported parts also drive the live catalogue thumbnails, keeping the silhouettes
identical to the item models used in first and third person.
"""
import json
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/blade"
PALETTE = [0x302735, 0x6B586D, 0xD6E4EF, 0x302238, 0xA38451, 0x71E9F5, 0xEC719E, 0xD63C68]


def box(x, y, z, w, h, d, material):
    return {"center": [x, y, z], "size": [w, h, d], "material": material,
            "color": PALETTE[material]}


def weapon(kind, accent):
    parts = [box(8, 3.6, 8, 1.5, 5.3, 1.6, 3), box(8, 0.8, 8, 2, 0.9, 2, 4),
             box(8, 6.4, 8, 2.2, 0.7, 2.2, 4)]
    for i in range(5):
        parts.append(box(8, 1.5 + i * 0.87, 8.85, 1.55, 0.28, 0.12, accent))
    if kind == "katana":
        parts += [box(8, 7.1, 8, 4.8, 0.65, 3.3, 0), box(8, 7.46, 8, 3.6, 0.18, 2.6, 4)]
        for i in range(7):
            x = 8 + (i / 6) ** 2 * 2.1
            y = 9 + i * 2.65
            parts += [box(x, y, 8, 1.9 - i * 0.10, 2.8, 0.65, 1),
                      box(x + 0.65 - i * 0.035, y, 8.02, 0.42, 2.8, 0.70, 2),
                      box(x - 0.43, y, 8.36, 0.22, 2.6, 0.08, accent)]
        parts.append(box(10.2, 27.3, 8, 0.6, 1.5, 0.6, 2))
    elif kind == "greatsword":
        parts += [box(8, 7.4, 8, 8.4, 1.4, 2.1, 0), box(8, 7.5, 8, 6.8, 0.45, 2.3, accent),
                  box(8, 16.5, 8, 5.8, 16.7, 1.1, 0), box(8, 16.6, 8.6, 0.7, 15, 0.18, accent),
                  box(5.15, 16.4, 8, 0.48, 15.8, 1.2, 2), box(10.85, 16.4, 8, 0.48, 15.8, 1.2, 2),
                  box(8, 25.2, 8, 3.2, 1.2, 1.0, 1), box(8, 26.25, 8, 1.5, 1.1, 0.8, 2)]
        for side in [-1, 1]:
            parts.append(box(8 + side * 3.8, 8.25, 8, 1.2, 2.1, 1.5, 0))
    else:
        parts += [box(8, 7.4, 8, 6.4, 0.9, 1.8, 4), box(8, 15.7, 8, 2.8, 15.7, 0.9, 0),
                  box(6.7, 15.7, 8, 0.45, 15.5, 1, 2), box(9.3, 15.7, 8, 0.45, 15.5, 1, 2),
                  box(8, 15.6, 8.5, 0.6, 14.8, 0.15, accent), box(8, 24.1, 8, 1.6, 1.3, 0.8, 2)]
        for side in [-1, 1]:
            parts += [box(8 + side * 2.7, 8.4, 8, 0.8, 2.0, 1.4, 0),
                      box(8 + side * 1.8, 10.4, 8, 1.1, 2.3, 0.8, 1)]
    parts.append(box(8, 7.65, 9.2, 1.1, 1.1, 0.6, accent))
    return parts


def png(path):
    rows = bytearray()
    for y in range(16):
        rows.append(0)
        for x in range(64):
            index, local = x // 8, x % 8
            color = PALETTE[index]
            # Brushed metal, wrapped handles, and a bright polished rim, drawn pixel by pixel.
            light = 1.17 if local == 1 else 0.78 if local == 7 else 1.0
            if index == 3:
                light *= 1.25 if (local + y) % 6 == 0 else 0.9
            elif (local * 13 + y * 7) % 23 == 0:
                light *= 1.12
            rows.extend(min(255, int(((color >> shift) & 255) * light)) for shift in [16, 8, 0])
            rows.append(255)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 64, 16, 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b""))


def main():
    definitions = {"blood": weapon("sword", 7), "frost": weapon("sword", 5),
                   "gold": weapon("sword", 4), "netherite": weapon("greatsword", 5),
                   "katana_void": weapon("katana", 7), "katana_sakura": weapon("katana", 6),
                   "katana_frost": weapon("katana", 5)}
    for folder in ["cosmetics", "models/item/cosmetics", "items/cosmetics", "textures/item/cosmetics"]:
        (ROOT / folder).mkdir(parents=True, exist_ok=True)
    (ROOT / "cosmetics/weapons.json").write_text(json.dumps(definitions, indent=2) + "\n")
    png(ROOT / "textures/item/cosmetics/forged_palette.png")
    displays = {
        "gui": {"rotation": [0, -25, -35], "translation": [0, -3, 0], "scale": [0.52] * 3},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.60] * 3},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.60] * 3},
        "thirdperson_righthand": {"rotation": [0, -90, 0], "translation": [0, 4, 0], "scale": [0.64] * 3},
        "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 4, 0], "scale": [0.64] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.35] * 3},
        "fixed": {"rotation": [0, 180, -35], "translation": [0, -2, 0], "scale": [0.55] * 3},
    }
    for name, parts in definitions.items():
        elements = []
        for part in parts:
            c, size, material = part["center"], part["size"], part["material"]
            face = {"uv": [material * 2, 0, material * 2 + 2, 16], "texture": "#metal"}
            elements.append({"from": [round(c[i] - size[i] / 2, 4) for i in range(3)],
                             "to": [round(c[i] + size[i] / 2, 4) for i in range(3)],
                             "faces": {side: face for side in ["north", "south", "east", "west", "up", "down"]}})
        model = {"textures": {"metal": "blade:item/cosmetics/forged_palette", "particle": "blade:item/cosmetics/forged_palette"},
                 "gui_light": "front", "display": displays, "elements": elements}
        (ROOT / f"models/item/cosmetics/{name}.json").write_text(json.dumps(model, indent=2) + "\n")
        item = {"model": {"type": "minecraft:model", "model": f"blade:item/cosmetics/{name}"}}
        (ROOT / f"items/cosmetics/{name}.json").write_text(json.dumps(item, indent=2) + "\n")
    print(f"Generated {len(definitions)} original blade models, shared preview parts, and a 64x16 pixel material atlas.")


if __name__ == "__main__":
    main()
