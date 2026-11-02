package io.github.quantum3600.stardust

// language=AGSL
internal const val MeshGradientShaderSource = """
uniform float2 uResolution;
uniform int uWidth;
uniform int uHeight;
uniform float4 uPackedPoints[13];
uniform float4 uColors[25];
uniform float4 uUTangents[25];
uniform float4 uVTangents[25];
uniform float uGrainIntensity;
uniform float uGrainSeed;

int clampInt(int val, int minVal, int maxVal) {
    return val < minVal ? minVal : (val > maxVal ? maxVal : val);
}

// Procedural Stardust / Film Grain Hash
float hash(float2 p, float seed) {
    float3 p3 = fract(p.xyx * 0.1031 + seed);
    p3 += float3(dot(p3, p3.yzx + 33.33));
    return fract((p3.x + p3.y) * p3.z);
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

float4 uTangentAt(int x, int y) {
    int idx = pointIndex(x, y);
    for (int i = 0; i < 25; i++) {
        if (i == idx) return uUTangents[i];
    }
    return uUTangents[0];
}

float4 vTangentAt(int x, int y) {
    int idx = pointIndex(x, y);
    for (int i = 0; i < 25; i++) {
        if (i == idx) return uVTangents[i];
    }
    return uVTangents[0];
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
    float u = clamp(gridX - float(cellX), 0.0, 1.0);
    float v = clamp(gridY - float(cellY), 0.0, 1.0);

    float2 tl = pointAt(cellX, cellY);
    float2 tr = pointAt(cellX + 1, cellY);
    float2 bl = pointAt(cellX, cellY + 1);
    float2 br = pointAt(cellX + 1, cellY + 1);

    float4 tanU_tl = uTangentAt(cellX, cellY);
    float4 tanU_tr = uTangentAt(cellX + 1, cellY);
    float4 tanU_bl = uTangentAt(cellX, cellY + 1);
    float4 tanU_br = uTangentAt(cellX + 1, cellY + 1);

    float4 tanV_tl = vTangentAt(cellX, cellY);
    float4 tanV_tr = vTangentAt(cellX + 1, cellY);
    float4 tanV_bl = vTangentAt(cellX, cellY + 1);
    float4 tanV_br = vTangentAt(cellX + 1, cellY + 1);

    float oneMinusU = 1.0 - u;
    float bu0 = oneMinusU * oneMinusU * oneMinusU;
    float bu1 = 3.0 * oneMinusU * oneMinusU * u;
    float bu2 = 3.0 * oneMinusU * u * u;
    float bu3 = u * u * u;

    // Top horizontal curve point
    float2 p0 = bu0 * tl + bu1 * (tl + tanU_tl.zw) + bu2 * (tr + tanU_tr.xy) + bu3 * tr;
    // Bottom horizontal curve point
    float2 p1 = bu0 * bl + bu1 * (bl + tanU_bl.zw) + bu2 * (br + tanU_br.xy) + bu3 * br;

    // Interpolated vertical tangents along U
    float2 vTan0 = mix(tanV_tl.zw, tanV_tr.zw, u);
    float2 vTan1 = mix(tanV_bl.xy, tanV_br.xy, u);

    float oneMinusV = 1.0 - v;
    float bv0 = oneMinusV * oneMinusV * oneMinusV;
    float bv1 = 3.0 * oneMinusV * oneMinusV * v;
    float bv2 = 3.0 * oneMinusV * v * v;
    float bv3 = v * v * v;

    return bv0 * p0 + bv1 * (p0 + vTan0) + bv2 * (p1 + vTan1) + bv3 * p1;
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

    for (int iteration = 0; iteration < 5; iteration++) {
        float2 position = evaluatePosition(uv);
        float2 error = position - target;

        if (dot(error, error) < 1e-5) break;

        float stepX = (uv.x + epsilon > 1.0) ? -epsilon : epsilon;
        float2 dx = (evaluatePosition(uv + float2(stepX, 0.0)) - position) / stepX;
        
        float stepY = (uv.y + epsilon > 1.0) ? -epsilon : epsilon;
        float2 dy = (evaluatePosition(uv + float2(0.0, stepY)) - position) / stepY;
        
        float lambda = 0.025;
        
        float a = dot(dx, dx) + lambda;
        float b = dot(dx, dy);
        float c = dot(dy, dy) + lambda;
        
        float2 jte = float2(dot(dx, error), dot(dy, error));
        
        float det = a * c - b * b;
        if (abs(det) < 1e-5) break;
        float2 delta = float2(
            (c * jte.x - b * jte.y) / det,
            (a * jte.y - b * jte.x) / det
        );
        
        delta = clamp(delta, float2(-0.2, -0.2), float2(0.2, 0.2));
        
        uv -= delta;
        uv = clamp(uv, float2(0.0, 0.0), float2(1.0, 1.0));
    }

    half4 col = half4(clamp(evaluateColor(uv), float4(0.0, 0.0, 0.0, 0.0), float4(1.0, 1.0, 1.0, 1.0)));

    if (uGrainIntensity > 0.0) {
        float noise = hash(coord, uGrainSeed);
        col.rgb += half3(half(noise - 0.5) * half(uGrainIntensity));
        col.rgb = clamp(col.rgb, half3(0.0), half3(1.0));
    }

    half3 linearRgb = toLinear(col.rgb);

    return half4(linearRgb * col.a, col.a);
}
"""
