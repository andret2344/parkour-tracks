"""Draws the ParkourTracks icon as a pure vector SVG from Minecraft textures: a grass block with a light weighted
pressure plate, the usual checkpoint of parkour maps, and a clock in front of it."""
import math

from textures import texture as jar_texture

SIZE = 512
# The block's texel; the clock's texel, drawn flat like an item in the inventory
TEXEL = 10.4
CLOCK_TEXEL = 15
# The minecraft:plains grass colour, which the game multiplies the grey grass texture by
GRASS_TINT = (0x91, 0xbd, 0x59)


def texture(name):
    # Animated textures are vertical strips of square frames; the first frame is enough
    pixels = jar_texture(name)
    return pixels[:len(pixels[0])]


def tinted(tex, tint):
    return [[(r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255, alpha) for r, g, b, alpha in row]
            for row in tex]


def hex_color(rgba, shade=1.0):
    r, g, b, _ = rgba
    return '#%02x%02x%02x' % tuple(min(255, round(c * shade)) for c in (r, g, b))


def face(tex, origin, du, dv, shade):
    """Draws a texture of any size on the parallelogram origin + u * du + v * dv."""
    out = []
    for v, row in enumerate(tex):
        for u, rgba in enumerate(row):
            if rgba[3] == 0:
                continue
            corners = [(u, v), (u + 1, v), (u + 1, v + 1), (u, v + 1)]
            points = ' '.join('%.2f,%.2f' % (origin[0] + cu * du[0] + cv * dv[0],
                                             origin[1] + cu * du[1] + cv * dv[1]) for cu, cv in corners)
            color = hex_color(rgba, shade)
            # The stroke in the same colour hides the hairline seams between neighbouring texels
            out.append('<polygon points="%s" fill="%s" stroke="%s" stroke-width="0.6"/>' % (points, color, color))
    return out


def box(top, left, right, cx, y, texel, shades=(1.0, 0.82, 0.64)):
    """An isometric box whose top face's back corner is at (cx, y); its size follows the textures."""
    a, b = texel * math.cos(math.radians(30)), texel * math.sin(math.radians(30))
    depth, width = len(top), len(top[0])
    left_corner = (cx - depth * a, y + depth * b)
    front = (cx - depth * a + width * a, y + depth * b + width * b)
    out = []
    out += face(top, left_corner, (a, -b), (a, b), shades[0])
    out += face(left, left_corner, (a, b), (0, texel), shades[1])
    out += face(right, front, (a, -b), (0, texel), shades[2])
    return out


def flat(tex, x, y, texel):
    """An item as the inventory shows it: the texture flat, every texel a square."""
    out = []
    for v, row in enumerate(tex):
        for u, rgba in enumerate(row):
            if rgba[3] > 0:
                out.append('<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s"/>' % (
                    x + u * texel, y + v * texel, texel + 0.3, texel + 0.3, hex_color(rgba)))
    return out


def icon():
    a, b = TEXEL * math.cos(math.radians(30)), TEXEL * math.sin(math.radians(30))
    grass_top = tinted(texture('block/grass_block_top'), GRASS_TINT)
    grass_side = texture('block/grass_block_side')
    gold = texture('block/gold_block')
    clock = texture('item/clock_00')

    cx, top_y = 214, 68
    block = box(grass_top, grass_side, grass_side, cx, top_y, TEXEL)
    # The plate as the game models it: 14x14 texels, one high, in the middle of the block's top
    plate_top = [row[1:15] for row in gold[1:15]]
    plate_side = [gold[15][1:15]]
    plate = box(plate_top, plate_side, plate_side, cx, top_y + 2 * b - TEXEL, TEXEL)
    clock_x = SIZE - 16 * CLOCK_TEXEL - 14
    clock_y = SIZE - 16 * CLOCK_TEXEL - 14

    parts = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d">' % (
        SIZE, SIZE, SIZE, SIZE)]
    parts.append('<g>%s</g>' % ''.join(block))
    parts.append('<g>%s</g>' % ''.join(plate))
    # crispEdges keeps the clock's texels sharp and seamless at any size
    parts.append('<g shape-rendering="crispEdges">%s</g>' % ''.join(flat(clock, clock_x, clock_y, CLOCK_TEXEL)))
    parts.append('</svg>')
    return '\n'.join(parts)
