# Trigger Wand: формы

ED1 работает в block snap mode. Точка соответствует выбранной block cell.

## Box

Требует 2 точки. Первая и вторая точки считаются противоположными углами.
Preview и source нормализуют итоговые `Min` и `MaxExclusive`.

Если выбран block `205, 8, 205`, max-exclusive для этой cell: `206, 9, 206`.

## Sphere

Требует 2 точки:

- center;
- radius point.

Radius считается как distance между center и radius point. Radius должен быть больше 0.

## Cylinder

Требует 3 точки:

- center;
- radius point;
- height point.

X/Z берутся от center, radius считается в горизонтальной плоскости, `minY` и `maxY` выводятся явно.

## Polygon

Требует минимум 3 footprint points. ED1 ограничивает polygon 64 точками.

Если все точки на одном Y, default height равен 3 blocks:

```text
minY = selectedY
maxY = selectedY + 3
```

Если Y различается, `maxY` становится upper-exclusive:

```text
maxY = max(selectedY) + 1
```

Generated source использует настоящий `PolygonArea` validator Echo Trigger.
