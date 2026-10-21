from __future__ import annotations


def loop_triangles(mesh) -> tuple:
    mesh.calc_loop_triangles()
    triangles = tuple(mesh.loop_triangles)
    if not triangles:
        raise RuntimeError(f"Mesh {mesh.name!r} has no triangles")
    return triangles
