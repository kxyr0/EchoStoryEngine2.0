"""Original procedural light textures. Requires Pillow; no third-party VFX assets."""
from pathlib import Path
import math
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "teleport-magic-forge-1.20.1/src/main/resources/assets/echo_teleport_magic/textures/effect"
OUT.mkdir(parents=True, exist_ok=True)


def texture(kind):
    image = Image.new("RGBA", (128, 128))
    pixels = image.load()
    for y in range(128):
        for x in range(128):
            dx, dy = (x - 63.5) / 63.5, (y - 63.5) / 63.5
            angle = math.atan2(dy, dx)
            radius = math.hypot(dx, dy)
            if kind == "ground_glow":
                radius *= 1 + .06 * math.sin(5 * angle) + .035 * math.cos(9 * angle)
            if kind == "gold_orb":
                halo = falloff(radius, 2) * .35
                middle = falloff(radius * 2.6 / 1.5, 2) * .8
                core = falloff(radius * 2.6 / .65, 4.5)
                alpha = min(1, halo + middle + core)
                center = math.exp(-120 * radius * radius)
                pixels[x, y] = (255, round(255 * (.8 + .18 * center)), round(255 * .9 * center), round(alpha * 255))
            else:
                power = {"soft_orb": 2.0, "soft_core": 4.5, "spark": 9.0, "ground_glow": 2.8}[kind]
                center = math.exp(-18 * radius * radius)
                pixels[x, y] = (255, round(255 * (.82 + .16 * center)), round(255 * .9 * center), round(falloff(radius, power) * 255))
    return image


def falloff(radius, power):
    edge = max(0.0, min(1.0, (1 - radius) / .24))
    edge = edge * edge * (3 - 2 * edge)
    return math.exp(-power * radius * radius) * edge if radius < 1 else 0


atlas = Image.new("RGBA", (640, 128))
for index, name in enumerate(("soft_orb", "soft_core", "spark", "ground_glow", "gold_orb")):
    asset = texture(name)
    asset.save(OUT / f"{name}.png")
    atlas.paste(asset, (index * 128, 0))
atlas.save(OUT / "atlas.png")
Image.new("RGBA", (2, 2), (255, 255, 255, 255)).save(OUT / "body.png")
print(OUT)
