"""Generate the compact full-NPC smooth-skinning acceptance mesh."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = ROOT / "animation-forge-1.20.1/src/main/resources/assets/echo_animation/echo_animation/models/tests"
vertices, triangles = [], []


def matrix(x=0, y=0, z=0):
    return [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, x, y, z, 1]


def skin(indices, weights=None):
    return indices, weights or [1] * len(indices)


def vertex(point, normal, uv, influence):
    indices, weights = influence
    return {"position": point, "normal": normal, "uv": [uv[0] / 64, uv[1] / 64],
            "boneIndices": indices, "boneWeights": weights}


def quad(points, normal, uvs, influences):
    start = len(vertices)
    for point, uv, influence in zip(points, uvs, influences):
        vertices.append(vertex(point, normal, uv, influence))
    triangles.extend([start, start + 1, start + 2, start, start + 2, start + 3])


def rect(u0, v0, u1, v1):
    return [(u1, v0), (u0, v0), (u0, v1), (u1, v1)]


def cuboid(x0, y0, z0, x1, y1, z1, uv, bone):
    u, v, width, height, depth = uv
    fixed = [skin([bone])] * 4
    faces = [
        ([(x1, y0, z1), (x0, y0, z1), (x0, y0, z0), (x1, y0, z0)], (0, -1, 0), rect(u + depth, v, u + depth + width, v + depth)),
        ([(x1, y1, z0), (x0, y1, z0), (x0, y1, z1), (x1, y1, z1)], (0, 1, 0), rect(u + depth + width, v, u + depth + 2 * width, v + depth)),
        ([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)], (-1, 0, 0), rect(u, v + depth, u + depth, v + depth + height)),
        ([(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)], (0, 0, -1), rect(u + depth, v + depth, u + depth + width, v + depth + height)),
        ([(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)], (1, 0, 0), rect(u + depth + width, v + depth, u + 2 * depth + width, v + depth + height)),
        ([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], (0, 0, 1), rect(u + 2 * depth + width, v + depth, u + 2 * (depth + width), v + depth + height)),
    ]
    for points, normal, uvs in faces:
        quad(points, normal, uvs, fixed)


def blend(parent, child, parent_weight):
    if parent_weight >= 1:
        return skin([parent])
    if parent_weight <= 0:
        return skin([child])
    return skin([parent, child], [parent_weight, 1 - parent_weight])


def limb(x0, x1, y0, y1, z0, z1, uv, parent, child):
    u, v, width, height, depth = uv
    span = y1 - y0
    rings = [(y0, 1), (y0 + span * .25, .75), (y0 + span * .5, .5),
             (y0 + span * .75, .25), (y1, 0)]
    sides = [
        (lambda y: [(x0, y, z0), (x0, y, z1)], (-1, 0, 0), u, u + depth),
        (lambda y: [(x1, y, z0), (x0, y, z0)], (0, 0, -1), u + depth, u + depth + width),
        (lambda y: [(x1, y, z1), (x1, y, z0)], (1, 0, 0), u + depth + width, u + 2 * depth + width),
        (lambda y: [(x0, y, z1), (x1, y, z1)], (0, 0, 1), u + 2 * depth + width, u + 2 * (depth + width)),
    ]
    for points, normal, ua, ub in sides:
        for (ya, wa), (yb, wb) in zip(rings, rings[1:]):
            va = v + depth + (ya - y0) * height / span
            vb = v + depth + (yb - y0) * height / span
            pair_a, pair_b = points(ya), points(yb)
            quad([pair_a[1], pair_a[0], pair_b[0], pair_b[1]], normal,
                 rect(ua, va, ub, vb), [blend(parent, child, wa)] * 2 + [blend(parent, child, wb)] * 2)
    fixed_parent, fixed_child = [skin([parent])] * 4, [skin([child])] * 4
    quad([(x1, y0, z1), (x0, y0, z1), (x0, y0, z0), (x1, y0, z0)], (0, -1, 0),
         rect(u + depth, v, u + depth + width, v + depth), fixed_parent)
    quad([(x1, y1, z0), (x0, y1, z0), (x0, y1, z1), (x1, y1, z1)], (0, 1, 0),
         rect(u + depth + width, v, u + depth + 2 * width, v + depth), fixed_child)


bones_spec = [
    ("root", -1, (0, 0, 0)), ("body", 0, (0, 0, 0)), ("head", 1, (0, 0, 0)),
    ("left_shoulder", 1, (5, 2, 0)), ("left_upper_arm", 3, (0, 0, 0)),
    ("left_forearm", 4, (0, 4, 0)), ("right_shoulder", 1, (-5, 2, 0)),
    ("right_upper_arm", 6, (0, 0, 0)), ("right_forearm", 7, (0, 4, 0)),
    ("left_thigh", 0, (2, 12, 0)), ("left_shin", 9, (0, 6, 0)),
    ("right_thigh", 0, (-2, 12, 0)), ("right_shin", 11, (0, 6, 0)),
]
def make_bones():
    globals_xyz = [(0, 0, 0)] * len(bones_spec)
    bones = []
    for index, (name, parent, local) in enumerate(bones_spec):
        base = (0, 0, 0) if parent < 0 else globals_xyz[parent]
        global_xyz = tuple(base[axis] + local[axis] for axis in range(3))
        globals_xyz[index] = global_xyz
        bones.append({"name": name, "parent": parent, "bindLocal": matrix(*local),
                      "inverseBind": matrix(*(-value for value in global_xyz))})
    return bones


def generate(slim):
    global vertices, triangles
    vertices, triangles = [], []
    arm_width = 3 if slim else 4
    arm_edge = 4 + arm_width
    cuboid(-4, -8, -4, 4, 0, 4, (0, 0, 8, 8, 8), 2)
    cuboid(-4, 0, -2, 4, 12, 2, (16, 16, 8, 12, 4), 1)
    limb(4, arm_edge, 0, 12, -2, 2, (32, 48, arm_width, 12, 4), 4, 5)
    limb(-arm_edge, -4, 0, 12, -2, 2, (40, 16, arm_width, 12, 4), 7, 8)
    # The entity pose stack mirrors X; atlas halves therefore map to the opposite mesh side.
    limb(0, 4, 12, 24, -2, 2, (0, 16, 4, 12, 4), 9, 10)
    limb(-4, 0, 12, 24, -2, 2, (16, 48, 4, 12, 4), 11, 12)

    # Vanilla second layer; slim sleeves use the three-pixel arm atlas.
    cuboid(-4.5, -8.5, -4.5, 4.5, 0.5, 4.5, (32, 0, 8, 8, 8), 2)
    cuboid(-4.25, -0.25, -2.25, 4.25, 12.25, 2.25, (16, 32, 8, 12, 4), 1)
    limb(3.75, arm_edge + .25, -.25, 12.25, -2.25, 2.25,
         (48, 48, arm_width, 12, 4), 4, 5)
    limb(-arm_edge - .25, -3.75, -.25, 12.25, -2.25, 2.25,
         (40, 32, arm_width, 12, 4), 7, 8)
    limb(-.25, 4.25, 11.75, 24.25, -2.25, 2.25, (0, 32, 4, 12, 4), 9, 10)
    limb(-4.25, .25, 11.75, 24.25, -2.25, 2.25, (0, 48, 4, 12, 4), 11, 12)

    suffix = "_slim" if slim else ""
    output = MODEL_DIR / f"full_humanoid{suffix}.skin.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({"formatVersion": 2, "bones": make_bones(),
                                  "meshes": [{"name": "full_humanoid", "vertices": vertices,
                                              "triangles": triangles}]}, indent=2) + "\n", encoding="utf-8")
    print(f"generated {output} vertices={len(vertices)} triangles={len(triangles) // 3}")


generate(False)
generate(True)
