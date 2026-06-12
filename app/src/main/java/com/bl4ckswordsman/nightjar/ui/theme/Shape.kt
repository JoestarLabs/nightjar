package com.bl4ckswordsman.nightjar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Nightjar Shapes ───────────────────────────────────────────────────────────
// M3 Expressive: more generous rounding than default M3 — softer, more playful.
// Zen aesthetic: smooth, deliberate curves.
val NightjarShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
