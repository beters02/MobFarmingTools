"""
resize_images.py
Resize all images in a directory to a fixed size using nearest-neighbor scaling.

Examples:
  python resize_images.py input_dir --size 32 32
  python resize_images.py input_dir --size 64 64 --out output_dir
"""

from __future__ import annotations

import argparse
from pathlib import Path
from PIL import Image


SUPPORTED_EXTS = {".png", ".jpg", ".jpeg", ".bmp", ".tga", ".webp"}


def resize_image(
    img_path: Path,
    out_path: Path,
    width: int,
    height: int,
) -> None:
    with Image.open(img_path) as img:
        # Preserve alpha & pixel-art sharpness
        img = img.convert("RGBA")
        resized = img.resize((width, height), resample=Image.NEAREST)
        resized.save(out_path)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Resize all images in a directory using nearest-neighbor scaling."
    )

    parser.add_argument(
        "input",
        type=Path,
        help="Input directory containing images",
    )

    parser.add_argument(
        "--size",
        nargs=2,
        type=int,
        required=True,
        metavar=("W", "H"),
        help="Target width and height (e.g. --size 32 32)",
    )

    parser.add_argument(
        "--out",
        type=Path,
        help="Optional output directory (default: overwrite originals)",
    )

    args = parser.parse_args()

    input_dir: Path = args.input
    out_dir: Path = args.out if args.out else input_dir

    width, height = args.size

    if not input_dir.is_dir():
        raise SystemExit(f"Input path is not a directory: {input_dir}")

    out_dir.mkdir(parents=True, exist_ok=True)

    processed = 0
    for file in input_dir.iterdir():
        if file.suffix.lower() not in SUPPORTED_EXTS:
            continue

        out_path = out_dir / file.name
        resize_image(file, out_path, width, height)
        processed += 1

    print(f"Resized {processed} images to {width}x{height} using nearest neighbor.")
    print(f"Output directory: {out_dir.resolve()}")


if __name__ == "__main__":
    main()