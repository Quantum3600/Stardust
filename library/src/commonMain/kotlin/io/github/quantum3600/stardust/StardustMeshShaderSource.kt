package io.github.quantum3600.stardust

/**
 * Generates the fragment shader shared by AGSL (Android 33+) and SkSL (Skia targets).
 *
 * AGSL/SkSL have no vertex stage, so instead of deforming a tessellated grid on the GPU
 * (the old OpenGL approach) we do the *inverse*: for every pixel we find the (u,v) inside the mesh
 * cell that maps onto it, then evaluate the colour there with Catmull-Rom.
 *
 * Inverse bilinear mapping alone gives straight-edged quads, which is only C0 across cells and shows up
 * as low-poly creases. So the bilinear result is only a starting guess: it is refined with Newton
 * iterations against the real bicubic Catmull-Rom *position* surface, the same surface the OpenGL path
 * tessellates. Position and colour are then both C1-continuous across cell borders => smooth curves.
 *
 * Cells are unrolled at generation time so every uniform-array index is a compile-time constant
 * (SkSL/AGSL only guarantee constant / loop-index addressing).
 */
internal object MeshShaderSource {

    const val U_RESOLUTION = "uResolution"
    const val U_POINTS = "uPoints"
    const val U_COLORS = "uColors"

    private const val HELPERS = """
float4 cr(float4 p0, float4 p1, float4 p2, float4 p3, float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    return 0.5 * (
        (2.0 * p1) +
        (-p0 + p2) * t +
        (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 +
        (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3
    );
}

float2 cr2(float2 p0, float2 p1, float2 p2, float2 p3, float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    return 0.5 * (
        (2.0 * p1) +
        (-p0 + p2) * t +
        (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 +
        (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3
    );
}

// derivative of cr2 with respect to t
float2 crd2(float2 p0, float2 p1, float2 p2, float2 p3, float t) {
    float t2 = t * t;
    return 0.5 * (
        (-p0 + p2) +
        2.0 * (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t +
        3.0 * (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t2
    );
}

float cross2(float2 a, float2 b) { return a.x * b.y - a.y * b.x; }

float solveU(float2 h, float2 e, float2 f, float2 g, float v) {
    float2 den = e + g * v;
    float2 num = h - f * v;
    // use the better-conditioned axis (handles rotated / vertical edges)
    return abs(den.x) > abs(den.y) ? num.x / den.x : num.y / den.y;
}

// Straight-edged inverse bilinear mapping of p inside quad a(0,0) b(1,0) c(1,1) d(0,1).
// Only used as the starting guess for refine(). Returns (-2,-2) if there is no solution.
float2 invBilinear(float2 p, float2 a, float2 b, float2 c, float2 d) {
    float2 e = b - a;
    float2 f = d - a;
    float2 g = a - b + c - d;
    float2 h = p - a;
    float k2 = cross2(g, f);
    float k1 = cross2(e, f) + cross2(h, g);
    float k0 = cross2(h, e);
    if (abs(k2) < 0.000001) {
        if (abs(k1) < 0.000001) return float2(-2.0);
        float v = -k0 / k1;
        return float2(solveU(h, e, f, g, v), v);
    }
    float w = k1 * k1 - 4.0 * k0 * k2;
    if (w < 0.0) return float2(-2.0);
    w = sqrt(w);
    float ik2 = 0.5 / k2;
    float v = (-k1 - w) * ik2;
    float u = solveU(h, e, f, g, v);
    if (u < 0.0 || u > 1.0 || v < 0.0 || v > 1.0) {
        v = (-k1 + w) * ik2;
        u = solveU(h, e, f, g, v);
    }
    return float2(u, v);
}

// Newton refinement of uv so that the bicubic Catmull-Rom surface S(uv) lands on p.
// q0..q15 are the 4x4 control points around the cell (row-major, edge-clamped).
float2 refine(float2 p, float2 uv,
              float2 q0, float2 q1, float2 q2, float2 q3,
              float2 q4, float2 q5, float2 q6, float2 q7,
              float2 q8, float2 q9, float2 q10, float2 q11,
              float2 q12, float2 q13, float2 q14, float2 q15) {
    for (int i = 0; i < 3; i++) {
        float2 r0 = cr2(q0, q1, q2, q3, uv.x);
        float2 r1 = cr2(q4, q5, q6, q7, uv.x);
        float2 r2 = cr2(q8, q9, q10, q11, uv.x);
        float2 r3 = cr2(q12, q13, q14, q15, uv.x);
        float2 d0 = crd2(q0, q1, q2, q3, uv.x);
        float2 d1 = crd2(q4, q5, q6, q7, uv.x);
        float2 d2 = crd2(q8, q9, q10, q11, uv.x);
        float2 d3 = crd2(q12, q13, q14, q15, uv.x);

        float2 s = cr2(r0, r1, r2, r3, uv.y);
        float2 su = cr2(d0, d1, d2, d3, uv.y);
        float2 sv = crd2(r0, r1, r2, r3, uv.y);

        float2 f = s - p;
        float det = su.x * sv.y - su.y * sv.x;
        if (abs(det) < 0.00000001) break;
        float du = (f.x * sv.y - f.y * sv.x) / det;
        float dv = (su.x * f.y - su.y * f.x) / det;
        uv = clamp(uv - float2(du, dv), -0.1, 1.1);
    }
    return uv;
}

bool inside(float2 uv) {
    return uv.x >= -0.02 && uv.x <= 1.02 && uv.y >= -0.02 && uv.y <= 1.02;
}
"""

    fun build(gridWidth: Int, gridHeight: Int): String {
        require(gridWidth >= 2 && gridHeight >= 2) { "Mesh needs at least 2x2 control points" }
        val n = gridWidth * gridHeight
        fun idx(x: Int, y: Int) =
            y.coerceIn(0, gridHeight - 1) * gridWidth + x.coerceIn(0, gridWidth - 1)

        return buildString {
            appendLine("uniform float2 $U_RESOLUTION;")
            appendLine("uniform float2 $U_POINTS[$n];")
            appendLine("uniform float4 $U_COLORS[$n];")
            append(HELPERS)
            appendLine("half4 main(float2 fragCoord) {")
            appendLine("    float2 p = fragCoord / $U_RESOLUTION;")
            appendLine("    float bestD = 1000.0;")
            appendLine("    float4 best = float4(0.0);")
            for (cy in 0 until gridHeight - 1) {
                for (cx in 0 until gridWidth - 1) {
                    appendLine("    {")
                    appendLine(
                        "        float2 uv = invBilinear(p, " +
                                "$U_POINTS[${idx(cx, cy)}], $U_POINTS[${idx(cx + 1, cy)}], " +
                                "$U_POINTS[${idx(cx + 1, cy + 1)}], $U_POINTS[${idx(cx, cy + 1)}]);"
                    )
                    appendLine("        if (uv.x > -0.3 && uv.x < 1.3 && uv.y > -0.3 && uv.y < 1.3) {")
                    val q = (0..3).flatMap { r -> (0..3).map { c -> "$U_POINTS[${idx(cx - 1 + c, cy - 1 + r)}]" } }
                    appendLine("            uv = refine(p, uv,")
                    q.chunked(4).forEachIndexed { i, row ->
                        appendLine("                " + row.joinToString(", ") + if (i < 3) "," else ");")
                    }
                    // distance outside the cell's [0,1] square (<= 0 means inside)
                    appendLine("            float d = max(max(-uv.x, uv.x - 1.0), max(-uv.y, uv.y - 1.0));")
                    appendLine("            if (d < 0.02 || d < bestD) {")
                    appendLine("                float2 cuv = clamp(uv, 0.0, 1.0);")
                    for (r in 0..3) {
                        val y = cy - 1 + r
                        appendLine(
                            "                float4 r$r = cr($U_COLORS[${idx(cx - 1, y)}], $U_COLORS[${idx(cx, y)}], " +
                                    "$U_COLORS[${idx(cx + 1, y)}], $U_COLORS[${idx(cx + 2, y)}], cuv.x);"
                        )
                    }
                    appendLine("                float4 c = clamp(cr(r0, r1, r2, r3, cuv.y), 0.0, 1.0);")
                    appendLine("                if (d < 0.02) return half4(half3(c.rgb * c.a), half(c.a));")
                    appendLine("                bestD = d;")
                    appendLine("                best = c;")
                    appendLine("            }")
                    appendLine("        }")
                    appendLine("    }")
                }
            }
            // No cell contained the pixel (folded mesh): use the nearest candidate instead of a hole
            appendLine("    if (bestD < 0.35) return half4(half3(best.rgb * best.a), half(best.a));")
            appendLine("    return half4(0.0);")
            appendLine("}")
        }
    }
}