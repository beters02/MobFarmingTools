#!/usr/bin/env python3
"""
slice_atlas.py - Split a texture atlas into individual tiles.

Examples:
  python slice_atlas.py atlas.png out --tile 32 32
  python slice_atlas.py atlas.png out --tile 32 32 --margin 1 1 --spacing 2 2
  python slice_atlas.py atlas.png out --tile 32 32 --no-skip-transparent
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path
from typing import Tuple

from PIL import Image


def parse_pair(values: list[int], name: str) -> Tuple[int, int]:
    if len(values) != 2:
        raise ValueError(f"{name} must have exactly 2 integers (x y).")
    return int(values[0]), int(values[1])


def is_fully_transparent(img: Image.Image) -> bool:
    """Return True if all pixels have alpha == 0 (requires RGBA)."""
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    alpha = img.getchannel("A")
    bbox = alpha.getbbox()  # bounding box of non-zero alpha
    return bbox is None


def slice_atlas(
    atlas_path: Path,
    out_dir: Path,
    tile_w: int = 32,
    tile_h: int = 32,
    margin_x: int = 0,
    margin_y: int = 0,
    spacing_x: int = 0,
    spacing_y: int = 0,
    skip_transparent: bool = True,
    prefix: str = "tile",
) -> int:
    atlas = Image.open(atlas_path)
    atlas_rgba = atlas.convert("RGBA")
    W, H = atlas_rgba.size

    out_dir.mkdir(parents=True, exist_ok=True)

    # How many tiles fit (integer grid) with margin + spacing
    usable_w = W - 2 * margin_x
    usable_h = H - 2 * margin_y
    if usable_w <= 0 or usable_h <= 0:
        raise ValueError("Margins are too large for the atlas size.")

    step_x = tile_w + spacing_x
    step_y = tile_h + spacing_y

    cols = (usable_w + spacing_x) // step_x
    rows = (usable_h + spacing_y) // step_y

    if cols <= 0 or rows <= 0:
        raise ValueError("No tiles fit. Check tile size / margin / spacing.")

    saved = 0
    for r in range(rows):
        for c in range(cols):
            left = margin_x + c * step_x
            upper = margin_y + r * step_y
            right = left + tile_w
            lower = upper + tile_h

            # Safety: don't crop outside image
            if right > W - margin_x or lower > H - margin_y:
                continue

            tile = atlas_rgba.crop((left, upper, right, lower))

            if skip_transparent and is_fully_transparent(tile):
                continue

            filename = f"{prefix}_r{r:02d}_c{c:02d}.png"
            tile.save(out_dir / filename)
            saved += 1

    return saved


def main() -> None:
    ap = argparse.ArgumentParser(description="Split a texture atlas into tiles.")
    ap.add_argument("atlas", type=Path, help="Path to the atlas image (png recommended).")
    ap.add_argument("out", type=Path, help="Output folder for tiles.")

    ap.add_argument("--tile", nargs=2, type=int, default=[32, 32], metavar=("W", "H"),
                    help="Tile width and height in pixels (e.g., --tile 32 32).")

    ap.add_argument("--margin", nargs=2, type=int, default=[0, 0], metavar=("X", "Y"),
                    help="Margin (pixels) from left/right and top/bottom (default 0 0).")

    ap.add_argument("--spacing", nargs=2, type=int, default=[0, 0], metavar=("X", "Y"),
                    help="Spacing (pixels) between tiles (default 0 0).")

    ap.add_argument("--prefix", default="tile", help="Output filename prefix (default: tile).")

    ap.add_argument("--no-skip-transparent", action="store_true",
                    help="Do NOT skip fully transparent tiles (by default they are skipped).")

    args = ap.parse_args()

    tile_w, tile_h = parse_pair(args.tile, "--tile")
    margin_x, margin_y = parse_pair(args.margin, "--margin")
    spacing_x, spacing_y = parse_pair(args.spacing, "--spacing")

    saved = slice_atlas(
        atlas_path=args.atlas,
        out_dir=args.out,
        tile_w=tile_w,
        tile_h=tile_h,
        margin_x=margin_x,
        margin_y=margin_y,
        spacing_x=spacing_x,
        spacing_y=spacing_y,
        skip_transparent=not args.no_skip_transparent,
        prefix=args.prefix,
    )

    print(f"Saved {saved} tiles to: {args.out.resolve()}")


if __name__ == "__main__":
    # Pillow install: pip install pillow
    main()
