from __future__ import annotations

import re


_LOCAL = re.compile(r"[^a-z0-9_.-]+")
_RESOURCE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


def sanitize_name(value: str, fallback: str = "animation") -> str:
    value = _LOCAL.sub("_", value.strip().lower().replace(" ", "_"))
    value = re.sub(r"_+", "_", value).strip("_.-")
    return value or fallback


def resource_id(value: str, namespace: str = "jdh") -> str:
    raw = value.strip().lower()
    if ":" in raw:
        owner, local = raw.split(":", 1)
        return f"{sanitize_name(owner, namespace)}:{sanitize_name(local)}"
    return f"{namespace}:{sanitize_name(raw)}"


def valid_resource_id(value: str) -> bool:
    return bool(_RESOURCE.fullmatch(value.strip()))


def class_name(value: str, suffix: str) -> str:
    local = value.split(":", 1)[-1]
    words = [part for part in re.split(r"[^A-Za-z0-9]+", local) if part]
    result = "".join(word[:1].upper() + word[1:] for word in words) or "Animation"
    if result[0].isdigit():
        result = "Animation" + result
    return result + suffix


def identifier(value: str) -> str:
    words = [part for part in re.split(r"[^A-Za-z0-9]+", value) if part]
    result = words[0].lower() + "".join(word.title() for word in words[1:])
    return "item" + result.title() if result[:1].isdigit() else result


def semantic_names(source: str, active: str, target: str) -> dict[str, str]:
    source = sanitize_name(source, "idle")
    active = sanitize_name(active, "active")
    target = sanitize_name(target, "idle")
    return {
        "enter": f"{source}_to_{active}",
        "loop": f"{active}_loop",
        "exit": f"{active}_to_{target}",
        "main": f"{active}_one_shot",
        "hold": f"{active}_pose",
    }
