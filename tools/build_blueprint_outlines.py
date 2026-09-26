"""Build blueprint guide textures and server masks from black-on-white drawings."""

import argparse
import base64
import json
from pathlib import Path

from PIL import Image


CANVAS_SIZE = 256
MASK_SIZE = 128
INK_LEVEL = 128
CELL_COVERAGE = 0.25


def luminance(pixel):
    red, green, blue = pixel[:3]
    return 0.299 * red + 0.587 * green + 0.114 * blue


def cut_paper(image):
    image = image.convert("RGBA").resize((CANVAS_SIZE, CANVAS_SIZE), Image.Resampling.LANCZOS)
    outline = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    source = image.load()
    target = outline.load()

    for y in range(CANVAS_SIZE):
        for x in range(CANVAS_SIZE):
            alpha = max(0, min(255, round(255 - luminance(source[x, y]))))
            if alpha:
                target[x, y] = (0, 0, 0, alpha)

    return outline


def build_mask(outline):
    step = CANVAS_SIZE // MASK_SIZE
    alpha = outline.getchannel("A").load()
    bits = bytearray(MASK_SIZE * MASK_SIZE // 8)
    filled = 0

    for cell_y in range(MASK_SIZE):
        for cell_x in range(MASK_SIZE):
            covered = 0
            for y in range(cell_y * step, cell_y * step + step):
                for x in range(cell_x * step, cell_x * step + step):
                    if alpha[x, y] >= 255 - INK_LEVEL:
                        covered += 1

            if covered / (step * step) >= CELL_COVERAGE:
                index = cell_y * MASK_SIZE + cell_x
                bits[index >> 3] |= 0x80 >> (index & 7)
                filled += 1

    return bits, filled


def build(source_root, texture_root, data_root, namespace):
    for source_path in sorted(source_root.rglob("*.png")):
        relative = source_path.relative_to(source_root).with_suffix("")
        texture_path = (texture_root / relative).with_suffix(".png")
        data_path = (data_root / relative).with_suffix(".json")
        texture_path.parent.mkdir(parents=True, exist_ok=True)
        data_path.parent.mkdir(parents=True, exist_ok=True)

        with Image.open(source_path) as source:
            outline = cut_paper(source)
        outline.save(texture_path)

        bits, filled = build_mask(outline)
        texture_id = f"{namespace}:textures/blueprint/{relative.as_posix()}.png"
        document = {
            "texture": texture_id,
            "resolution": MASK_SIZE,
            "cells": filled,
            "mask": base64.b64encode(bytes(bits)).decode("ascii"),
        }
        data_path.write_text(json.dumps(document, indent=2) + "\n", encoding="utf-8")
        print(f"{relative.as_posix()}: {filled} cells")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="Directory containing source PNG drawings")
    parser.add_argument("textures", type=Path, help="Output textures/blueprint directory")
    parser.add_argument("data", type=Path, help="Output data/<namespace>/blueprint_outlines directory")
    parser.add_argument("namespace", help="Resource namespace written to generated texture IDs")
    args = parser.parse_args()
    build(args.source, args.textures, args.data, args.namespace)


if __name__ == "__main__":
    main()
