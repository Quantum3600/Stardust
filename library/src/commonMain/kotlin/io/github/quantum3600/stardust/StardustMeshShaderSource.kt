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
uniform float uDustIntensity;
uniform float uDustPhase;

float hash(float2 p, float seed) {
    float3 p3 = fract(p.xyx * 0.1031 + seed);
    p3 += float3(dot(p3, p3.yzx + 33.33));
    return fract((p3.x + p3.y) * p3.z);
}

float CatmullRom(float p0, float p1, float p2, float p3, float t) {
    float v0 = (p2 - p0) * 0.5;
    float v1 = (p3 - p1) * 0.5;
    float t2 = t * t;
    float t3 = t2 * t;
    return (2.0 * p1 - 2.0 * p2 + v0 + v1) * t3 +
           (-3.0 * p1 + 3.0 * p2 - 2.0 * v0 - v1) * t2 +
           v0 * t + p1;
}

float4 CatmullRomColor(float4 c0, float4 c1, float4 c2, float4 c3, float t) {
    float4 v0 = (c2 - c0) * 0.5;
    float4 v1 = (c3 - c1) * 0.5;
    float t2 = t * t;
    float t3 = t2 * t;
    return (2.0 * c1 - 2.0 * c2 + v0 + v1) * t3 +
           (-3.0 * c1 + 3.0 * c2 - 2.0 * v0 - v1) * t2 +
           v0 * t + c1;
}

float CubicBezier1D(float p0, float p1, float p2, float p3, float t) {
    float u = 1.0 - t;
    float tt = t * t;
    float uu = u * u;
    float uuu = uu * u;
    float ttt = tt * t;

    return uuu * p0 +
           3.0 * uu * t * p1 +
           3.0 * u * tt * p2 +
           ttt * p3;
}

float4 CubicBezier4D(float4 c0, float4 c1, float4 c2, float4 c3, float t) {
    float u = 1.0 - t;
    float tt = t * t;
    float uu = u * u;
    float uuu = uu * u;
    float ttt = tt * t;

    return uuu * c0 +
           3.0 * uu * t * c1 +
           3.0 * u * tt * c2 +
           ttt * c3;
}

float2 getControlPoint(int index) {
    int vecIdx = index / 2;
    int component = index % 2;
    float4 packedVec = uPackedPoints[vecIdx];
    return (component == 0) ? packedVec.xy : packedVec.zw;
}

half4 main(float2 fragCoord) {
    float2 st = fragCoord / uResolution;

    float stepX = 1.0 / float(uWidth - 1);
    float stepY = 1.0 / float(uHeight - 1);

    int cellX = int(clamp(st.x / stepX, 0.0, float(uWidth - 2)));
    int cellY = int(clamp(st.y / stepY, 0.0, float(uHeight - 2)));

    float localU = clamp((st.x - float(cellX) * stepX) / stepX, 0.0, 1.0);
    float localV = clamp((st.y - float(cellY) * stepY) / stepY, 0.0, 1.0);

    float2 interpPos[4];
    float4 interpCol[4];

    for (int rowOffset = 0; rowOffset < 4; rowOffset++) {
        int r = clamp(cellY - 1 + rowOffset, 0, uHeight - 1);

        int idx0 = r * uWidth + clamp(cellX - 1, 0, uWidth - 1);
        int idx1 = r * uWidth + cellX;
        int idx2 = r * uWidth + cellX + 1;
        int idx3 = r * uWidth + clamp(cellX + 2, 0, uWidth - 1);

        float2 p0 = getControlPoint(idx0);
        float2 p1 = getControlPoint(idx1);
        float2 p2 = getControlPoint(idx2);
        float2 p3 = getControlPoint(idx3);

        float4 uTang1 = uUTangents[idx1];
        float4 uTang2 = uUTangents[idx2];

        float2 P0 = p1;
        float2 P1 = p1 + uTang1.zw;
        float2 P2 = p2 + uTang2.xy;
        float2 P3 = p2;

        interpPos[rowOffset] = float2(
            CubicBezier1D(P0.x, P1.x, P2.x, P3.x, localU),
            CubicBezier1D(P0.y, P1.y, P2.y, P3.y, localU)
        );

        interpCol[rowOffset] = CatmullRomColor(
            uColors[idx0],
            uColors[idx1],
            uColors[idx2],
            uColors[idx3],
            localU
        );
    }

    float2 finalPos;
    float4 finalColor;

    int r0 = cellY;
    int r1 = cellY + 1;

    int idxR0_C1 = r0 * uWidth + cellX;
    int idxR0_C2 = r0 * uWidth + cellX + 1;
    int idxR1_C1 = r1 * uWidth + cellX;
    int idxR1_C2 = r1 * uWidth + cellX + 1;

    float4 vTang_R0_C1 = uVTangents[idxR0_C1];
    float4 vTang_R0_C2 = uVTangents[idxR0_C2];
    float4 vTang_R1_C1 = uVTangents[idxR1_C1];
    float4 vTang_R1_C2 = uVTangents[idxR1_C2];

    float2 vLeading_R0 = mix(vTang_R0_C1.xy, vTang_R0_C2.xy, localU);
    float2 vTrailing_R0 = mix(vTang_R0_C1.zw, vTang_R0_C2.zw, localU);
    float2 vLeading_R1 = mix(vTang_R1_C1.xy, vTang_R1_C2.xy, localU);
    float2 vTrailing_R1 = mix(vTang_R1_C1.zw, vTang_R1_C2.zw, localU);

    float2 V0 = interpPos[1];
    float2 V1 = interpPos[1] + vTrailing_R0;
    float2 V2 = interpPos[2] + vLeading_R1;
    float2 V3 = interpPos[2];

    finalPos = float2(
        CubicBezier1D(V0.x, V1.x, V2.x, V3.x, localV),
        CubicBezier1D(V0.y, V1.y, V2.y, V3.y, localV)
    );

    finalColor = CatmullRomColor(
        interpCol[0],
        interpCol[1],
        interpCol[2],
        interpCol[3],
        localV
    );

    float alpha = finalColor.a;
    if (alpha <= 0.0) {
        return half4(0.0, 0.0, 0.0, 0.0);
    }

    half3 rgb = half3(finalColor.rgb / alpha);

    if (uDustIntensity > 0.0) {
        float noiseVal = hash(fragCoord, uDustPhase);
        float dither = (noiseVal - 0.5) * uDustIntensity;
        rgb = clamp(rgb + half3(dither), half3(0.0), half3(1.0));
    }

    return half4(rgb * half(alpha), half(alpha));
}
"""
