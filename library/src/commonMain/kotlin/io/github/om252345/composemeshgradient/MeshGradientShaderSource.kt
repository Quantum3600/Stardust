package io.github.om252345.composemeshgradient

internal const val MeshGradientShaderSource = """
uniform float2 uResolution;
uniform int uWidth;
uniform int uHeight;
uniform float4 uPackedPoints[13];
uniform float4 uColors[25];

int clampInt(int val, int minVal, int maxVal) {
    return val < minVal ? minVal : (val > maxVal ? maxVal : val);
}

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
    return clampInt(y, 0, uHeight - 1) * uWidth + clampInt(x, 0, uWidth - 1);
}

float2 pointAt(int x, int y) {
    int idx = pointIndex(x, y);
    int pairIndex = idx / 2;
    float4 packedPair = uPackedPoints[0];
    for (int i = 0; i < 13; i++) {
        if (i == pairIndex) {
            packedPair = uPackedPoints[i];
            break;
        }
    }
    return ((pairIndex * 2) == idx) ? packedPair.xy : packedPair.zw;
}

float4 colorAt(int x, int y) {
    int idx = pointIndex(x, y);
    for (int i = 0; i < 25; i++) {
        if (i == idx) return uColors[i];
    }
    return uColors[0];
}

// Perform color mixing in perceptual gamma space to match raw GLES20 vertex blending
half4 blendMeshColors(half4 c1, half4 c2, half4 c3, half4 c4, float u, float v) {
    half4 top = mix(c1, c2, half(u));
    half4 bottom = mix(c3, c4, half(u));
    return mix(top, bottom, half(v));
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
    int cellX = clampInt(int(floor(gridX)), 0, uWidth - 2);
    int cellY = clampInt(int(floor(gridY)), 0, uHeight - 2);
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
    int cellX = clampInt(int(floor(gridX)), 0, uWidth - 2);
    int cellY = clampInt(int(floor(gridY)), 0, uHeight - 2);
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

float srgbToLinear(float c) {
    return (c <= 0.04045) ? (c / 12.92) : pow((c + 0.055) / 1.055, 2.4);
}

half3 toLinear(half3 c) {
    return half3(srgbToLinear(float(c.r)), srgbToLinear(float(c.g)), srgbToLinear(float(c.b)));
}

half4 main(float2 coord) {
    float2 target = clamp(coord / uResolution, float2(0.0, 0.0), float2(1.0, 1.0));
    float2 uv = target;
    float epsilon = 0.002;

    // Invert the warped bicubic surface using Levenberg-Marquardt iteration.
    // Increased iteration limit for aggressive noise folding where LM falls back 
    // to gradient descent and takes smaller steps.
    for (int iteration = 0; iteration < 5; iteration++) {
        float2 position = evaluatePosition(uv);
        float2 error = position - target;

        if (dot(error, error) < 1e-5) break;

        float stepX = (uv.x + epsilon > 1.0) ? -epsilon : epsilon;
        float2 dx = (evaluatePosition(uv + float2(stepX, 0.0)) - position) / stepX;
        
        float stepY = (uv.y + epsilon > 1.0) ? -epsilon : epsilon;
        float2 dy = (evaluatePosition(uv + float2(0.0, stepY)) - position) / stepY;
        
        // Slightly higher lambda to heavily dampen oscillations in complex folds
        float lambda = 0.025;
        
        // J^T * J
        float a = dot(dx, dx) + lambda;
        float b = dot(dx, dy);
        float c = dot(dy, dy) + lambda;
        
        // J^T * error
        float2 jte = float2(dot(dx, error), dot(dy, error));
        
        // Inverse of (J^T * J + lambda * I)
        float det = a * c - b * b;
        float2 delta = float2(
            (c * jte.x - b * jte.y) / det,
            (a * jte.y - b * jte.x) / det
        );
        
        // Limit the step size strictly to 20% of UV space to prevent 
        // the solver from ping-ponging out of a fold crease.
        delta = clamp(delta, float2(-0.2, -0.2), float2(0.2, 0.2));
        
        uv -= delta;
        uv = clamp(uv, float2(0.0, 0.0), float2(1.0, 1.0));
    }

    // Evaluate patch colors in perceptual gamma / sRGB space (un-premultiplied)
    half4 col = half4(clamp(evaluateColor(uv), float4(0.0, 0.0, 0.0, 0.0), float4(1.0, 1.0, 1.0, 1.0)));

    // Convert perceptual sRGB to linear working space so Skia's output to sRGB framebuffer
    // preserves the exact original non-linear GLES20 color blending curve without washing out.
    half3 linearRgb = toLinear(col.rgb);

    // Premultiply alpha right before returning from main()
    return half4(linearRgb * col.a, col.a);
}
"""
