"""Dessine les textures du mod, sans dépendance : `python tools/make_textures.py`.

Chaque texture est décrite par une fonction (x, y) -> (r, g, b, a) ; le script écrit des PNG RGBA.
Les textures de blocs font 16 x 16, celles du tableau 64 x 64.
"""
import math
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                    "assets", "cobblemon-league-screen", "textures")


def write_png(path, size, pixel):
    rows = b""
    for y in range(size):
        rows += b"\x00" + b"".join(bytes(pixel(x, y)) for x in range(size))

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(rows, 9))
           + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as out:
        out.write(png)


def rgb(text, alpha=255):
    text = text.lstrip("#")
    return int(text[0:2], 16), int(text[2:4], 16), int(text[4:6], 16), alpha


def border(x, y, size):
    return x == 0 or y == 0 or x == size - 1 or y == size - 1


# --- Blocs (16 x 16) -------------------------------------------------------------------------

def screen_front(x, y):
    # Dalle sombre avec un liseré discret : posée en mur, elle dessine un quadrillage de panneaux.
    return rgb("#101730") if border(x, y, 16) else rgb("#0A0F1F")


def screen_side(x, y):
    return rgb("#1C1F27") if border(x, y, 16) else rgb("#2A2E38")


def terminal_side(x, y):
    if border(x, y, 16):
        return rgb("#23262E")
    if 3 <= y <= 5:
        return rgb("#E5484D")          # bandeau rouge
    if 8 <= y <= 12 and 5 <= x <= 10:
        return rgb("#0A0F1F")          # petit écran
    return rgb("#3A3F4D")


def terminal_top(x, y):
    if border(x, y, 16):
        return rgb("#23262E")
    distance = math.hypot(x - 7.5, y - 7.5)
    if distance <= 1.6:
        return rgb("#F4F4F4")          # bouton central
    if distance <= 5.2:
        if abs(y - 7.5) < 1:
            return rgb("#1C1F27")      # ceinture de la Poké Ball
        return rgb("#E5484D") if y < 7.5 else rgb("#F4F4F4")
    return rgb("#3A3F4D")


def terminal_bottom(x, y):
    return rgb("#1C1F27") if border(x, y, 16) else rgb("#2A2E38")


# --- Tableau (64 x 64, en blanc : la couleur est donnée au moment du dessin) -------------------

def soft_disc(x, y, radius, edge=1.5, size=64):
    distance = math.hypot(x - (size - 1) / 2, y - (size - 1) / 2)
    return max(0.0, min(1.0, (radius - distance) / edge + 0.5))


def circle(x, y):
    return 255, 255, 255, int(255 * soft_disc(x, y, 30))


def ring(x, y):
    outer = soft_disc(x, y, 30)
    inner = soft_disc(x, y, 26)
    return 255, 255, 255, int(255 * max(0.0, outer - inner))


def halo(x, y):
    # Un dégradé doux qui déborde du disque de la case.
    distance = math.hypot(x - 31.5, y - 31.5)
    alpha = max(0.0, 1.0 - distance / 31.5)
    return 255, 255, 255, int(255 * alpha ** 1.5)


def ball_off(x, y):
    distance = math.hypot(x - 31.5, y - 31.5)
    if distance > 22:
        return 0, 0, 0, 0
    if distance <= 5:
        return rgb("#3A4670")
    if distance <= 8 or abs(y - 31.5) < 2.5 or distance > 19.5:
        return rgb("#0A0F1F")
    return rgb("#2A3558") if y < 31.5 else rgb("#232D4C")


def silhouette(x, y):
    head = math.hypot(x - 31.5, y - 24) <= 12
    shoulders = ((x - 31.5) / 26.0) ** 2 + ((y - 66) / 28.0) ** 2 <= 1
    return (255, 255, 255, 255) if head or shoulders else (0, 0, 0, 0)


def main():
    blocks = {
        "screen_front": screen_front, "screen_side": screen_side,
        "terminal_side": terminal_side, "terminal_top": terminal_top, "terminal_bottom": terminal_bottom,
    }
    for name, pixel in blocks.items():
        write_png(os.path.join(ROOT, "block", name + ".png"), 16, pixel)

    board = {"circle": circle, "ring": ring, "halo": halo, "ball_off": ball_off, "silhouette": silhouette}
    for name, pixel in board.items():
        write_png(os.path.join(ROOT, "board", name + ".png"), 64, pixel)

    print("textures écrites dans", os.path.normpath(ROOT))


if __name__ == "__main__":
    main()