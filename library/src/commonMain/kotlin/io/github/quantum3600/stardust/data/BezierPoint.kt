package io.github.quantum3600.stardust.data

import androidx.compose.ui.geometry.Offset

/**
 * Represents a Bezier control point for advanced mesh gradient control,
 * similar to SwiftUI's MeshGradient.BezierPoint
 * Currently not used in the core library, but provided for future use.
 */
data class BezierPoint(
    val position: Offset,
    val leadingControlPoint: Offset = position,
    val topControlPoint: Offset = position,
    val trailingControlPoint: Offset = position,
    val bottomControlPoint: Offset = position
) {
    /**
     * Relative horizontal tangent handle pointing left (leading).
     */
    val uTangentLeading: Offset get() = leadingControlPoint - position

    /**
     * Relative horizontal tangent handle pointing right (trailing).
     */
    val uTangentTrailing: Offset get() = trailingControlPoint - position

    /**
     * Relative vertical tangent handle pointing up (top).
     */
    val vTangentTop: Offset get() = topControlPoint - position

    /**
     * Relative vertical tangent handle pointing down (bottom).
     */
    val vTangentBottom: Offset get() = bottomControlPoint - position
    /**
     * Creates a simple BezierPoint with just a position (no control points)
     */
    constructor(x: Float, y: Float) : this(Offset(x, y))

    /**
     * Creates a BezierPoint with symmetric control points
     */
    constructor(
        position: Offset,
        controlPointOffset: Offset
    ) : this(
        position = position,
        leadingControlPoint = position - controlPointOffset,
        topControlPoint = position - Offset(0f, controlPointOffset.y),
        trailingControlPoint = position + controlPointOffset,
        bottomControlPoint = position + Offset(0f, controlPointOffset.y)
    )

    companion object {
        /**
         * Creates a simple BezierPoint at the given position with no control point offsets
         */
        fun simple(x: Float, y: Float): BezierPoint = BezierPoint(Offset(x, y))

        /**
         * Creates a BezierPoint with custom control point offsets
         */
        fun withControls(
            x: Float,
            y: Float,
            leadingOffset: Offset = Offset.Companion.Zero,
            topOffset: Offset = Offset.Companion.Zero,
            trailingOffset: Offset = Offset.Companion.Zero,
            bottomOffset: Offset = Offset.Companion.Zero
        ): BezierPoint {
            val pos = Offset(x, y)
            return BezierPoint(
                position = pos,
                leadingControlPoint = pos + leadingOffset,
                topControlPoint = pos + topOffset,
                trailingControlPoint = pos + trailingOffset,
                bottomControlPoint = pos + bottomOffset
            )
        }

        /**
         * Creates a BezierPoint at [position] with directional tangent handle offsets.
         */
        fun withTangents(
            position: Offset,
            uTangentLeading: Offset = Offset.Zero,
            uTangentTrailing: Offset = Offset.Zero,
            vTangentTop: Offset = Offset.Zero,
            vTangentBottom: Offset = Offset.Zero
        ): BezierPoint {
            return BezierPoint(
                position = position,
                leadingControlPoint = position + uTangentLeading,
                topControlPoint = position + vTangentTop,
                trailingControlPoint = position + uTangentTrailing,
                bottomControlPoint = position + vTangentBottom
            )
        }
    }
}