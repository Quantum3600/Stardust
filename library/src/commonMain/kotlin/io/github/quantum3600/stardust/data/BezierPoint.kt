package io.github.quantum3600.stardust.data

import androidx.compose.ui.geometry.Offset

/**
 * Bezier control point, similar to SwiftUI's MeshGradient.BezierPoint.
 * Not used by the renderers yet.
 *
 */
data class BezierPoint(
    val position: Offset,
    val leadingControlPoint: Offset = position,
    val topControlPoint: Offset = position,
    val trailingControlPoint: Offset = position,
    val bottomControlPoint: Offset = position,
) {
    constructor(x: Float, y: Float) : this(Offset(x, y))

    companion object {
        fun simple(x: Float, y: Float): BezierPoint = BezierPoint(Offset(x, y))

        /**
         * Replaces the old secondary constructor `BezierPoint(position, controlPointOffset)`: it had the same
         * signature as the primary constructor's first two parameters, so it silently hijacked
         * `BezierPoint(pos, leadingControlPoint)` calls.
         */
        fun symmetric(position: Offset, controlPointOffset: Offset) = BezierPoint(
            position = position,
            leadingControlPoint = position - controlPointOffset,
            topControlPoint = position - Offset(0f, controlPointOffset.y),
            trailingControlPoint = position + controlPointOffset,
            bottomControlPoint = position + Offset(0f, controlPointOffset.y),
        )

        fun withControls(
            x: Float,
            y: Float,
            leadingOffset: Offset = Offset.Zero,
            topOffset: Offset = Offset.Zero,
            trailingOffset: Offset = Offset.Zero,
            bottomOffset: Offset = Offset.Zero,
        ): BezierPoint {
            val pos = Offset(x, y)
            return BezierPoint(
                position = pos,
                leadingControlPoint = pos + leadingOffset,
                topControlPoint = pos + topOffset,
                trailingControlPoint = pos + trailingOffset,
                bottomControlPoint = pos + bottomOffset,
            )
        }
    }
}
