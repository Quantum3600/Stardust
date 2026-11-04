package io.github.quantum3600.stardust

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

internal class StardustMeshRendererHelper(
    private val gridWidth: Int,
    private val gridHeight: Int,
    private val globalSubdivisions: Int = 32,
) {
    private lateinit var indexBuffer: IntBuffer
    private lateinit var uvBuffer: FloatBuffer
    private lateinit var posArray: FloatArray
    private lateinit var colorArray: FloatArray
    private var indexCount = 0
    private var program = 0

    private var aGridUv = -1
    private var uGridWidth = -1
    private var uGridHeight = -1
    private var uBezierPositions = -1
    private var uBezierColors = -1

    fun initBuffers() {
        val totalX = (gridWidth - 1) * globalSubdivisions + 1
        val totalY = (gridHeight - 1) * globalSubdivisions + 1

        val uvList = FloatArray(totalX * totalY * 2)
        var uvi = 0
        for (y in 0 until totalY) {
            val v = y.toFloat() / (totalY - 1)
            for (x in 0 until totalX) {
                uvList[uvi++] = x.toFloat() / (totalX - 1)
                uvList[uvi++] = v
            }
        }
        uvBuffer = ByteBuffer.allocateDirect(uvList.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(uvList).also { it.position(0) }

        // 32-bit indices (ES 3.0): the old ShortArray overflowed above 65 535 vertices (e.g. 5x5 grid @ 64 subdivisions)
        indexCount = (totalX - 1) * (totalY - 1) * 6
        val indices = IntArray(indexCount)
        var k = 0
        for (y in 0 until totalY - 1) {
            val row = y * totalX
            val next = (y + 1) * totalX
            for (x in 0 until totalX - 1) {
                val tl = row + x
                val tr = row + x + 1
                val bl = next + x
                val br = next + x + 1
                indices[k++] = tl; indices[k++] = tr; indices[k++] = bl
                indices[k++] = tr; indices[k++] = br; indices[k++] = bl
            }
        }
        indexBuffer = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder())
            .asIntBuffer().put(indices).also { it.position(0) }

        program = compileShaders()

        aGridUv = GLES20.glGetAttribLocation(program, "a_Grid_UV")
        uGridWidth = GLES20.glGetUniformLocation(program, "u_GridWidth")
        uGridHeight = GLES20.glGetUniformLocation(program, "u_GridHeight")
        uBezierPositions = GLES20.glGetUniformLocation(program, "u_BezierPositions")
        uBezierColors = GLES20.glGetUniformLocation(program, "u_BezierColors")

        val controlCount = gridWidth * gridHeight
        posArray = FloatArray(controlCount * 2)
        colorArray = FloatArray(controlCount * 4)
    }

    fun draw(points: FloatArray, colors: FloatArray) {
        GLES20.glUseProgram(program)

        GLES20.glEnableVertexAttribArray(aGridUv)
        GLES20.glVertexAttribPointer(aGridUv, 2, GLES20.GL_FLOAT, false, 0, uvBuffer)

        GLES20.glUniform1i(uGridWidth, gridWidth)
        GLES20.glUniform1i(uGridHeight, gridHeight)

        System.arraycopy(points, 0, posArray, 0, points.size)
        System.arraycopy(colors, 0, colorArray, 0, colors.size)
        GLES20.glUniform2fv(uBezierPositions, gridWidth * gridHeight, posArray, 0)
        GLES20.glUniform4fv(uBezierColors, gridWidth * gridHeight, colorArray, 0)

        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer)

        GLES20.glDisableVertexAttribArray(aGridUv)
    }

    private fun compileShaders(): Int {
        val vertexShader = """#version 300 es
in vec2 a_Grid_UV;

uniform vec2 u_BezierPositions[${gridWidth * gridHeight}];
uniform vec4 u_BezierColors[${gridWidth * gridHeight}];
uniform int u_GridWidth;
uniform int u_GridHeight;

out vec4 v_Color;

float catmullRom(float p0, float p1, float p2, float p3, float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    return 0.5 * (
        (2.0 * p1) +
        (-p0 + p2) * t +
        (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 +
        (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3
    );
}

vec2 getPosition(int x, int y) {
    int cx = clamp(x, 0, u_GridWidth - 1);
    int cy = clamp(y, 0, u_GridHeight - 1);
    return u_BezierPositions[cy * u_GridWidth + cx];
}

vec4 getColor(int x, int y) {
    int cx = clamp(x, 0, u_GridWidth - 1);
    int cy = clamp(y, 0, u_GridHeight - 1);
    return u_BezierColors[cy * u_GridWidth + cx];
}

void main() {
    float gx = a_Grid_UV.x * float(u_GridWidth - 1);
    float gy = a_Grid_UV.y * float(u_GridHeight - 1);
    int cx = int(floor(gx));
    int cy = int(floor(gy));
    float localU = fract(gx);
    float localV = fract(gy);

    vec2 rowPos[4];
    vec4 rowCol[4];
    for (int i = 0; i < 4; i++) {
        vec2 p0 = getPosition(cx - 1, cy - 1 + i);
        vec2 p1 = getPosition(cx,     cy - 1 + i);
        vec2 p2 = getPosition(cx + 1, cy - 1 + i);
        vec2 p3 = getPosition(cx + 2, cy - 1 + i);
        vec4 c0 = getColor(cx - 1, cy - 1 + i);
        vec4 c1 = getColor(cx,     cy - 1 + i);
        vec4 c2 = getColor(cx + 1, cy - 1 + i);
        vec4 c3 = getColor(cx + 2, cy - 1 + i);

        rowPos[i].x = catmullRom(p0.x, p1.x, p2.x, p3.x, localU);
        rowPos[i].y = catmullRom(p0.y, p1.y, p2.y, p3.y, localU);
        rowCol[i].r = catmullRom(c0.r, c1.r, c2.r, c3.r, localU);
        rowCol[i].g = catmullRom(c0.g, c1.g, c2.g, c3.g, localU);
        rowCol[i].b = catmullRom(c0.b, c1.b, c2.b, c3.b, localU);
        rowCol[i].a = catmullRom(c0.a, c1.a, c2.a, c3.a, localU);
    }

    vec2 pos;
    pos.x = catmullRom(rowPos[0].x, rowPos[1].x, rowPos[2].x, rowPos[3].x, localV);
    pos.y = catmullRom(rowPos[0].y, rowPos[1].y, rowPos[2].y, rowPos[3].y, localV);

    vec4 col;
    col.r = catmullRom(rowCol[0].r, rowCol[1].r, rowCol[2].r, rowCol[3].r, localV);
    col.g = catmullRom(rowCol[0].g, rowCol[1].g, rowCol[2].g, rowCol[3].g, localV);
    col.b = catmullRom(rowCol[0].b, rowCol[1].b, rowCol[2].b, rowCol[3].b, localV);
    col.a = catmullRom(rowCol[0].a, rowCol[1].a, rowCol[2].a, rowCol[3].a, localV);

    v_Color = clamp(col, 0.0, 1.0);

    // Compose convention: y = 0 is the TOP, so flip into GL clip space.
    gl_Position = vec4(pos.x * 2.0 - 1.0, 1.0 - pos.y * 2.0, 0.0, 1.0);
}
"""

        val fragmentShader = """#version 300 es
precision mediump float;
in vec4 v_Color;
out vec4 fragColor;
void main() {
    fragColor = v_Color;
}
"""

        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        val status = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(prog)
            GLES20.glDeleteProgram(prog)
            error("Mesh gradient program link failed: $log")
        }
        return prog
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("Mesh gradient shader compile failed: $log")
        }
        return shader
    }
}
