from __future__ import annotations

from itertools import product


def armature_for_lattice(lattice_object):
    targets = {
        modifier.object
        for modifier in lattice_object.modifiers
        if modifier.type == "ARMATURE" and modifier.object is not None
    }
    if len(targets) != 1:
        return None
    armature = next(iter(targets))
    return armature if armature.type == "ARMATURE" else None


def lattice_modifier_for_mesh(mesh_object, armature=None):
    candidates = []
    for modifier in mesh_object.modifiers:
        lattice = modifier.object if modifier.type == "LATTICE" else None
        if lattice is None or lattice.type != "LATTICE" or not modifier.show_viewport:
            continue
        target = armature_for_lattice(lattice)
        if target is not None and (armature is None or target == armature):
            candidates.append((modifier, lattice))
    if len(candidates) > 1:
        raise RuntimeError(
            f"Mesh {mesh_object.name} uses more than one armature-driven Lattice modifier"
        )
    return candidates[0] if candidates else None


def _axis_blend(value: float, coordinates: list[float]):
    if len(coordinates) == 1 or value <= coordinates[0]:
        return ((coordinates[0], 1.0),)
    if value >= coordinates[-1]:
        return ((coordinates[-1], 1.0),)
    for index in range(len(coordinates) - 1):
        lower = coordinates[index]
        upper = coordinates[index + 1]
        if lower <= value <= upper:
            amount = (value - lower) / (upper - lower)
            return ((lower, 1.0 - amount), (upper, amount))
    return ((coordinates[-1], 1.0),)


def lattice_group_weights(mesh_object, vertex, lattice_object) -> dict[str, float]:
    data = lattice_object.data
    interpolation = (
        data.interpolation_type_u,
        data.interpolation_type_v,
        data.interpolation_type_w,
    )
    if interpolation != ("KEY_LINEAR", "KEY_LINEAR", "KEY_LINEAR"):
        raise RuntimeError(
            f"Lattice {lattice_object.name} must use Linear interpolation on U/V/W "
            "for deterministic Echo soft-weight transfer"
        )

    local = (
        lattice_object.matrix_world.inverted_safe()
        @ mesh_object.matrix_world
        @ vertex.co
    )
    points = list(data.points)
    axes = [
        sorted({round(float(point.co[axis]), 8) for point in points})
        for axis in range(3)
    ]
    point_by_coordinate = {
        tuple(round(float(point.co[axis]), 8) for axis in range(3)): point
        for point in points
    }
    blends = [
        _axis_blend(float(local[axis]), axes[axis])
        for axis in range(3)
    ]

    result: dict[str, float] = {}
    for x, y, z in product(*blends):
        coordinate = (round(x[0], 8), round(y[0], 8), round(z[0], 8))
        point = point_by_coordinate.get(coordinate)
        if point is None:
            raise RuntimeError(
                f"Lattice {lattice_object.name} has an irregular point grid at {coordinate}"
            )
        coefficient = x[1] * y[1] * z[1]
        if coefficient <= 0.0:
            continue
        for assignment in point.groups:
            if assignment.weight <= 0.0:
                continue
            name = lattice_object.vertex_groups[assignment.group].name
            result[name] = result.get(name, 0.0) + coefficient * float(assignment.weight)
    return result
