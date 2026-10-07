"""Draws the ParkourTracks banner: the icon on the left, the name and the tagline in the Minecraft font on the right."""
import re

from icon import hex_color, icon, texture
from textures import texture as jar_texture

WIDTH, HEIGHT = 1200, 400
FONT = jar_texture('font/ascii')


def glyph(char):
    """The pixels of a character in the 16x16 grid of 8x8 cells, and how far the next one starts."""
    code = ord(char)
    if char == ' ':
        return [], 4
    cx, cy = (code % 16) * 8, (code // 16) * 8
    pixels = [(x, y) for y in range(8) for x in range(8) if FONT[cy + y][cx + x][3] > 0]
    width = max(x for x, _ in pixels) + 1
    return pixels, width + 1


def text_width(text, scale):
    return sum(glyph(char)[1] for char in text) * scale - scale


def text(content, x, y, scale, color, shadow):
    """Minecraft-style text: every pixel a square, with the shadow one pixel down and right."""
    out = []
    for colour, offset in ((shadow, scale), (color, 0)):
        cursor = x
        rects = []
        for char in content:
            pixels, advance = glyph(char)
            rects += ['M%d %dh%dv%dh-%dz' % (cursor + px * scale + offset, y + py * scale + offset, scale, scale,
                                            scale) for px, py in pixels]
            cursor += advance * scale
        out.append('<path d="%s" fill="%s"/>' % (''.join(rects), colour))
    return out


def background_pattern(tex, size):
    cell = size / 16
    rects = ''.join('<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s"/>' % (
        u * cell, v * cell, cell + 0.3, cell + 0.3, hex_color(tex[v][u])) for v in range(16) for u in range(16))
    return '<pattern id="stone" patternUnits="userSpaceOnUse" width="%d" height="%d">%s</pattern>' % (
        size, size, rects)


def banner():
    inner = re.sub(r'^<svg[^>]*>', '', icon()).rsplit('</svg>', 1)[0]
    icon_size, icon_x = 360, 30

    lines = ['Timed parkour tracks with', 'checkpoints and medals for Paper']
    text_x = icon_x + icon_size + 30
    # As large as the free width allows
    title_scale = min(11, (WIDTH - 40 - text_x) // (text_width('ParkourTracks', 1) + 1))
    line_scale = 4
    title_y = 92
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d">' % (
        WIDTH, HEIGHT, WIDTH, HEIGHT),
        '<defs>', background_pattern(texture('block/stone'), 64),
        '<linearGradient id="fade" x1="0" x2="1"><stop offset="0" stop-color="#0e0f12" stop-opacity="0.55"/>'
        '<stop offset="1" stop-color="#0e0f12" stop-opacity="0.92"/></linearGradient>',
        '<radialGradient id="warm" cx="0.18" cy="0.55" r="0.5">'
        '<stop offset="0" stop-color="#ffd23f" stop-opacity="0.3"/>'
        '<stop offset="1" stop-color="#ffd23f" stop-opacity="0"/></radialGradient>',
        '</defs>',
        '<rect width="%d" height="%d" fill="url(#stone)"/>' % (WIDTH, HEIGHT),
        '<rect width="%d" height="%d" fill="url(#fade)"/>' % (WIDTH, HEIGHT),
        '<rect width="%d" height="%d" fill="url(#warm)"/>' % (WIDTH, HEIGHT),
        '<svg x="%d" y="%d" width="%d" height="%d" viewBox="0 0 512 512">%s</svg>' % (
            icon_x, (HEIGHT - icon_size) // 2, icon_size, icon_size, inner)]
    parts += text('ParkourTracks', text_x, title_y, title_scale, '#55ff55', '#153f15')
    line_y = title_y + 8 * title_scale + 42
    for line in lines:
        parts += text(line, text_x, line_y, line_scale, '#e6e6e6', '#393939')
        line_y += 10 * line_scale + 6
    parts.append('</svg>')
    return '\n'.join(parts)
