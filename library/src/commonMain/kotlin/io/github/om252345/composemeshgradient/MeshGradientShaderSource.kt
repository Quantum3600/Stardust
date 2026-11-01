package io.github.om252345.composemeshgradient

internal const val MeshGradientShaderSource = """
uniform float2 uResolution;
uniform int uWidth;
uniform int uHeight;
uniform float4 uPoints[64];
uniform float4 uColors[64];

float catmullRom(float p0, float p1, float p2, float p3, float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    return 0.5 * (
        2.0 * p1 +
        (-p0 + p2) * t +
        (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 +
        (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3
    );
}

int pointIndex(int x, int y) {
    return clamp(y, 0, uHeight - 1) * uWidth + clamp(x, 0, uWidth - 1);
}

float2 pointAt(int x, int y) {
    return uPoints[pointIndex(x, y)].xy;
}

float4 colorAt(int x, int y) {
    return uColors[pointIndex(x, y)];
}

float2 interpolatePointRow(int x, int y, float t) {
    float2 p0 = pointAt(x - 1, y);
    float2 p1 = pointAt(x, y);
    float2 p2 = pointAt(x + 1, y);
    float2 p3 = pointAt(x + 2, y);
    return float2(
        catmullRom(p0.x, p1.x, p2.x, p3.x, t),
        catmullRom(p0.y, p1.y, p2.y, p3.y, t)
    );
}

float4 interpolateColorRow(int x, int y, float t) {
    float4 c0 = colorAt(x - 1, y);
    float4 c1 = colorAt(x, y);
    float4 c2 = colorAt(x + 1, y);
    float4 c3 = colorAt(x + 2, y);
    return float4(
        catmullRom(c0.r, c1.r, c2.r, c3.r, t),
        catmullRom(c0.g, c1.g, c2.g, c3.g, t),
        catmullRom(c0.b, c1.b, c2.b, c3.b, t),
        catmullRom(c0.a, c1.a, c2.a, c3.a, t)
    );
}

float2 evaluatePosition(float2 uv) {
    float gridX = uv.x * float(uWidth - 1);
    float gridY = uv.y * float(uHeight - 1);
    int cellX = clamp(int(floor(gridX)), 0, uWidth - 2);
    int cellY = clamp(int(floor(gridY)), 0, uHeight - 2);
    float localU = clamp(gridX - float(cellX), 0.0, 1.0);
    float localV = clamp(gridY - float(cellY), 0.0, 1.0);
    float2 row0 = interpolatePointRow(cellX, cellY - 1, localU);
    float2 row1 = interpolatePointRow(cellX, cellY, localU);
    float2 row2 = interpolatePointRow(cellX, cellY + 1, localU);
    float2 row3 = interpolatePointRow(cellX, cellY + 2, localU);
    return float2(
        catmullRom(row0.x, row1.x, row2.x, row3.x, localV),
        catmullRom(row0.y, row1.y, row2.y, row3.y, localV)
    );
}

float4 evaluateColor(float2 uv) {
    float gridX = uv.x * float(uWidth - 1);
    float gridY = uv.y * float(uHeight - 1);
    int cellX = clamp(int(floor(gridX)), 0, uWidth - 2);
    int cellY = clamp(int(floor(gridY)), 0, uHeight - 2);
    float localU = clamp(gridX - float(cellX), 0.0, 1.0);
    float localV = clamp(gridY - float(cellY), 0.0, 1.0);
    float4 row0 = interpolateColorRow(cellX, cellY - 1, localU);
    float4 row1 = interpolateColorRow(cellX, cellY, localU);
    float4 row2 = interpolateColorRow(cellX, cellY + 1, localU);
    float4 row3 = interpolateColorRow(cellX, cellY + 2, localU);
    return float4(
        catmullRom(row0.r, row1.r, row2.r, row3.r, localV),
        catmullRom(row0.g, row1.g, row2.g, row3.g, localV),
        catmullRom(row0.b, row1.b, row2.b, row3.b, localV),
        catmullRom(row0.a, row1.a, row2.a, row3.a, localV)
    );
}

half4 main(float2 coord) {
    float2 target = clamp(coord / uResolution, 0.0, 1.0);
    float2 uv = target;
    float epsilon = 0.002;

    // Invert the warped bicubic surface with a few Newton iterations. This keeps the draw as one
    // full-bounds rectangle while preserving the control-point deformation per fragment.
    for (int iteration = 0; iteration < 5; iteration++) {
        float2 position = evaluatePosition(uv);
        float2 dx = (evaluatePosition(clamp(uv + float2(epsilon, 0.0), 0.0, 1.0)) - position) / epsilon;
        float2 dy = (evaluatePosition(clamp(uv + float2(0.0, epsilon), 0.0, 1.0)) - position) / epsilon;
        float determinant = dx.x * dy.y - dx.y * dy.x;
        if (abs(determinant) < 1e-5) break;
        float2 error = position - target;
        float2 delta = float2(
            (error.x * dy.y - error.y * dy.x) / determinant,
            (dx.x * error.y - dx.y * error.x) / determinant
        );
        uv -= delta;
        uv = clamp(uv, 0.0, 1.0);
        if (dot(delta, delta) < 1e-6) break;
    }

    return half4(evaluateColor(uv));
}
"""
