package io.github.quantum3600.stardust

// language=AGSL
internal const val StardustMeshShaderSource = """
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

float4 getPackedPoint(int index) {
    if (index <= 0) return uPackedPoints[0];
    if (index == 1) return uPackedPoints[1];
    if (index == 2) return uPackedPoints[2];
    if (index == 3) return uPackedPoints[3];
    if (index == 4) return uPackedPoints[4];
    if (index == 5) return uPackedPoints[5];
    if (index == 6) return uPackedPoints[6];
    if (index == 7) return uPackedPoints[7];
    if (index == 8) return uPackedPoints[8];
    if (index == 9) return uPackedPoints[9];
    if (index == 10) return uPackedPoints[10];
    if (index == 11) return uPackedPoints[11];
    return uPackedPoints[12];
}

float2 getControlPoint(int index) {
    int vecIdx = index / 2;
    int component = index - (vecIdx * 2);
    float4 packedVec = getPackedPoint(vecIdx);
    return (component == 0) ? packedVec.xy : packedVec.zw;
}

float4 getColor(int index) {
    if (index <= 0) return uColors[0];
    if (index == 1) return uColors[1];
    if (index == 2) return uColors[2];
    if (index == 3) return uColors[3];
    if (index == 4) return uColors[4];
    if (index == 5) return uColors[5];
    if (index == 6) return uColors[6];
    if (index == 7) return uColors[7];
    if (index == 8) return uColors[8];
    if (index == 9) return uColors[9];
    if (index == 10) return uColors[10];
    if (index == 11) return uColors[11];
    if (index == 12) return uColors[12];
    if (index == 13) return uColors[13];
    if (index == 14) return uColors[14];
    if (index == 15) return uColors[15];
    if (index == 16) return uColors[16];
    if (index == 17) return uColors[17];
    if (index == 18) return uColors[18];
    if (index == 19) return uColors[19];
    if (index == 20) return uColors[20];
    if (index == 21) return uColors[21];
    if (index == 22) return uColors[22];
    if (index == 23) return uColors[23];
    return uColors[24];
}

float hash(float2 p, float seed) {
    float3 p3 = fract(p.xyx * 0.1031 + seed);
    p3 += float3(dot(p3, p3.yzx + 33.33));
    return fract((p3.x + p3.y) * p3.z);
}

float catmullRom1D(float p0, float p1, float p2, float p3, float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    return 0.5 * (
        (2.0 * p1) +
        (-p0 + p2) * t +
        (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 +
        (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3
    );
}

float4 catmullRomColor(float4 c0, float4 c1, float4 c2, float4 c3, float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    float4 col = 0.5 * (
        (2.0 * c1) +
        (-c0 + c2) * t +
        (2.0 * c0 - 5.0 * c1 + 4.0 * c2 - c3) * t2 +
        (-c0 + 3.0 * c1 - 3.0 * c2 + c3) * t3
    );
    float4 minCol = min(c1, c2);
    float4 maxCol = max(c1, c2);
    return clamp(col, minCol, maxCol);
}

float2 getPatchPos(int cx, int cy, float u, float v) {
    float2 interp_row[4];
    for (int i = 0; i < 4; i++) {
        int r = cy - 1 + i;
        float2 p0 = getControlPoint(clampInt(cx - 1, 0, uWidth - 1) + clampInt(r, 0, uHeight - 1) * uWidth);
        float2 p1 = getControlPoint(clampInt(cx, 0, uWidth - 1) + clampInt(r, 0, uHeight - 1) * uWidth);
        float2 p2 = getControlPoint(clampInt(cx + 1, 0, uWidth - 1) + clampInt(r, 0, uHeight - 1) * uWidth);
        float2 p3 = getControlPoint(clampInt(cx + 2, 0, uWidth - 1) + clampInt(r, 0, uHeight - 1) * uWidth);

        interp_row[i].x = catmullRom1D(p0.x, p1.x, p2.x, p3.x, u);
        interp_row[i].y = catmullRom1D(p0.y, p1.y, p2.y, p3.y, u);
    }

    float2 finalPos;
    finalPos.x = catmullRom1D(interp_row[0].x, interp_row[1].x, interp_row[2].x, interp_row[3].x, v);
    finalPos.y = catmullRom1D(interp_row[0].y, interp_row[1].y, interp_row[2].y, interp_row[3].y, v);
    return finalPos;
}

float4 getPatchColor(int cx, int cy, float u, float v) {
    float4 interp_color_row[4];
    for (int i = 0; i < 4; i++) {
        int r = cy - 1 + i;
        float4 c0 = getColor(clampInt(cx - 1, 0, uWidth - 1) + clampInt(r, 0, uHeight - 1) * uWidth);
        float4 c1 = getColor(clampInt(cx, 0, uWidth - 1) + clampInt(r, 0, uHeight - 1) * uWidth);
        float4 c2 = getColor(clampInt(cx + 1, 0, uWidth - 1) + clampInt(r, 0, uHeight - 1) * uWidth);
        float4 c3 = getColor(clampInt(cx + 2, 0, uWidth - 1) + clampInt(r, 0, uHeight - 1) * uWidth);

        interp_color_row[i] = catmullRomColor(c0, c1, c2, c3, u);
    }

    return catmullRomColor(interp_color_row[0], interp_color_row[1], interp_color_row[2], interp_color_row[3], v);
}

float2 getGlobalBicubicPosition(float gU, float gV) {
    int cx = clampInt(int(floor(gU)), 0, uWidth - 2);
    int cy = clampInt(int(floor(gV)), 0, uHeight - 2);
    float localU = clamp(gU - float(cx), 0.0, 1.0);
    float localV = clamp(gV - float(cy), 0.0, 1.0);
    return getPatchPos(cx, cy, localU, localV);
}

float4 getGlobalBicubicColor(float gU, float gV) {
    int cx = clampInt(int(floor(gU)), 0, uWidth - 2);
    int cy = clampInt(int(floor(gV)), 0, uHeight - 2);
    float localU = clamp(gU - float(cx), 0.0, 1.0);
    float localV = clamp(gV - float(cy), 0.0, 1.0);
    return getPatchColor(cx, cy, localU, localV);
}

half4 main(float2 fragCoord) {
    float2 st = fragCoord / uResolution;

    float gU = clamp(st.x, 0.0, 1.0) * float(uWidth - 1);
    float gV = clamp(st.y, 0.0, 1.0) * float(uHeight - 1);

    float2 pos = getGlobalBicubicPosition(gU, gV);
    float2 shift = pos - st;

    float warpedGU = clamp(gU - shift.x * float(uWidth - 1), 0.0, float(uWidth - 1));
    float warpedGV = clamp(gV - shift.y * float(uHeight - 1), 0.0, float(uHeight - 1));

    float4 finalColor = clamp(getGlobalBicubicColor(warpedGU, warpedGV), 0.0, 1.0);

    float alpha = clamp(finalColor.a, 0.0, 1.0);
    if (alpha <= 0.0001) {
        return half4(0.0, 0.0, 0.0, 0.0);
    }

    half3 rgb = clamp(half3(finalColor.rgb / alpha), half3(0.0), half3(1.0));

    if (uGrainIntensity > 0.0) {
        float noiseVal = hash(fragCoord, uGrainSeed);
        float dither = (noiseVal - 0.5) * uGrainIntensity;
        rgb = clamp(rgb + half3(dither), half3(0.0), half3(1.0));
    }

    return half4(rgb * half(alpha), half(alpha));
}
"""
