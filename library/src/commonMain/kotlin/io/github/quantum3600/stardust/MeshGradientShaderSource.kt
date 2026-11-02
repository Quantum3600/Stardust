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
    return (val < minVal) ? minVal : ((val > maxVal) ? maxVal : val);
}

// Procedural Stardust / Film Grain Hash
float hash(float2 p, float seed) {
    float3 p3 = fract(p.xyx * 0.1031 + seed);
    p3 += float3(dot(p3, p3.yzx + 33.33));
    return fract((p3.x + p3.y) * p3.z);
}

float4 catmullRom(float4 p0, float4 p1, float4 p2, float4 p3, float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    return 0.5 * (
        (2.0 * p1) +
        (-p0 + p2) * t +
        (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 +
        (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3
    );
}

int pointIndex(int x, int y) {
    return clampInt(y, 0, uHeight - 1) * uWidth + clampInt(x, 0, uWidth - 1);
}

float4 getPackedPoint(int index) {
    for (int i = 0; i < 13; i++) {
        if (i == index) return uPackedPoints[i];
    }
    return float4(0.0);
}

float4 getColor(int index) {
    for (int i = 0; i < 25; i++) {
        if (i == index) return uColors[i];
    }
    return float4(0.0);
}

float4 getUTangent(int index) {
    for (int i = 0; i < 25; i++) {
        if (i == index) return uUTangents[i];
    }
    return float4(0.0);
}

float4 getVTangent(int index) {
    for (int i = 0; i < 25; i++) {
        if (i == index) return uVTangents[i];
    }
    return float4(0.0);
}

float2 pointAt(int x, int y) {
    int idx = pointIndex(x, y);
    int pairIndex = idx / 2;
    float4 packedPair = getPackedPoint(pairIndex);
    return ((pairIndex * 2) == idx) ? packedPair.xy : packedPair.zw;
}

float4 colorAt(int x, int y) {
    return getColor(pointIndex(x, y));
}

float4 uTangentAt(int x, int y) {
    return getUTangent(pointIndex(x, y));
}

float4 vTangentAt(int x, int y) {
    return getVTangent(pointIndex(x, y));
}

float4 interpolateColorRow(int x, int y, float t) {
    float4 c0 = colorAt(x - 1, y);
    float4 c1 = colorAt(x, y);
    float4 c2 = colorAt(x + 1, y);
    float4 c3 = colorAt(x + 2, y);
    return catmullRom(c0, c1, c2, c3, t);
}

float2 evaluatePositionAndDerivatives(float2 uv, out float2 dx, out float2 dy) {
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

    float dbu0 = -3.0 * oneMinusU * oneMinusU;
    float dbu1 = 3.0 * oneMinusU * (1.0 - 3.0 * u);
    float dbu2 = 3.0 * u * (2.0 - 3.0 * u);
    float dbu3 = 3.0 * u * u;

    // Top horizontal curve point and derivative
    float2 p0 = bu0 * tl + bu1 * (tl + tanU_tl.zw) + bu2 * (tr + tanU_tr.xy) + bu3 * tr;
    float2 dp0_du = dbu0 * tl + dbu1 * (tl + tanU_tl.zw) + dbu2 * (tr + tanU_tr.xy) + dbu3 * tr;

    // Bottom horizontal curve point and derivative
    float2 p1 = bu0 * bl + bu1 * (bl + tanU_bl.zw) + bu2 * (br + tanU_br.xy) + bu3 * br;
    float2 dp1_du = dbu0 * bl + dbu1 * (bl + tanU_bl.zw) + dbu2 * (br + tanU_br.xy) + dbu3 * br;

    // Interpolated vertical tangents along U and derivatives
    float2 vTan0 = mix(tanV_tl.zw, tanV_tr.zw, u);
    float2 dvTan0_du = tanV_tr.zw - tanV_tl.zw;

    float2 vTan1 = mix(tanV_bl.xy, tanV_br.xy, u);
    float2 dvTan1_du = tanV_br.xy - tanV_bl.xy;

    float oneMinusV = 1.0 - v;
    float bv0 = oneMinusV * oneMinusV * oneMinusV;
    float bv1 = 3.0 * oneMinusV * oneMinusV * v;
    float bv2 = 3.0 * oneMinusV * v * v;
    float bv3 = v * v * v;

    float dbv0 = -3.0 * oneMinusV * oneMinusV;
    float dbv1 = 3.0 * oneMinusV * (1.0 - 3.0 * v);
    float dbv2 = 3.0 * v * (2.0 - 3.0 * v);
    float dbv3 = 3.0 * v * v;

    float2 pos = bv0 * p0 + bv1 * (p0 + vTan0) + bv2 * (p1 + vTan1) + bv3 * p1;

    dx = (bv0 * dp0_du + bv1 * (dp0_du + dvTan0_du) + bv2 * (dp1_du + dvTan1_du) + bv3 * dp1_du) * float(uWidth - 1);
    dy = (dbv0 * p0 + dbv1 * (p0 + vTan0) + dbv2 * (p1 + vTan1) + dbv3 * p1) * float(uHeight - 1);

    return pos;
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
    return catmullRom(row0, row1, row2, row3, localV);
}

half3 toLinear(half3 c) {
    return c * c;
}

half4 main(float2 coord) {
    float2 target = clamp(coord / uResolution, float2(0.0, 0.0), float2(1.0, 1.0));
    float2 uv = target;

    for (int iteration = 0; iteration < 5; iteration++) {
        float2 dx, dy;
        float2 position = evaluatePositionAndDerivatives(uv, dx, dy);
        float2 error = position - target;

        if (dot(error, error) < 1e-6) break;

        float lambda = 0.025;
        float a = dot(dx, dx) + lambda;
        float b = dot(dx, dy);
        float c = dot(dy, dy) + lambda;

        float2 jte = float2(dot(dx, error), dot(dy, error));
        float det = a * c - b * b;
        if (abs(det) < 1e-6) break;

        float2 delta = float2(
            (c * jte.x - b * jte.y) / det,
            (a * jte.y - b * jte.x) / det
        );

        delta = clamp(delta, float2(-0.25, -0.25), float2(0.25, 0.25));
        uv = clamp(uv - delta, float2(0.0, 0.0), float2(1.0, 1.0));
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
