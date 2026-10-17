package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// DAV Cheeka InfoExchange — Minimalist Precision Slate & Monochrome Theme
// ============================================================================

// Canvas & Deep Slate Backgrounds
val DavCanvasDark = Color(0xFF0A0F1D)           // Deepest scholastic slate background
val DavSurfaceCard = Color(0xFF131D31)          // Focused precision card surface
val DavSurfaceElevated = Color(0xFF1C2840)      // Elevated interactive slate card
val DavSurfaceHover = Color(0xFF243352)         // Active/pressed surface
val DavBorder = Color(0xFF28395A)               // Sharp precision border (1.dp)
val DavBorderSubtle = Color(0xFF1E2C46)         // Subtle divider

// Crisp High-Contrast Typography
val DavTextPrimary = Color(0xFFF8FAFC)          // Crisp pure white/slate-50
val DavTextSecondary = Color(0xFF94A3B8)        // Neutral cool slate-400
val DavTextMuted = Color(0xFF64748B)            // Muted caption slate-500

// Precision Accents (Sky Blue, Pure Monochrome, Scholastic Gold)
val DavAccentSky = Color(0xFF38BDF8)            // Sky 400 precision active indicator
val DavAccentIndigo = Color(0xFF6366F1)         // Scholastic indigo brand accent
val DavAccentGold = Color(0xFFF59E0B)           // Institutional DAV crest gold
val DavAccentEmerald = Color(0xFF10B981)        // Live connection green
val DavAccentCrimson = Color(0xFFEF4444)        // Administrative alert crimson

// ============================================================================
// Semantic Compatibility Tokens (Theme Continuity)
// ============================================================================
val VhoqxethDarkBg = DavCanvasDark
val VhoqxethSurface = DavSurfaceCard
val VhoqxethSurfaceElevated = DavSurfaceElevated
val VhoqxethSurfaceHighlight = DavSurfaceHover
val VhoqxethBorder = DavBorder
val VhoqxethBorderLightStrong = DavBorder
val VhoqxethDeepBlack = DavCanvasDark

val VhoqxethPrimaryViolet = DavAccentIndigo
val VhoqxethPrimaryVioletBright = DavAccentSky
val VhoqxethPrimaryVioletGlow = DavAccentIndigo
val VhoqxethCyanAccent = DavAccentSky
val VhoqxethCyanBright = Color(0xFF7DD3FC)
val VhoqxethNeonMagenta = DavAccentGold
val VhoqxethIndigo600 = DavAccentIndigo

val VhoqxethTextPrimary = DavTextPrimary
val VhoqxethTextSecondary = DavTextSecondary
val VhoqxethTextMuted = DavTextMuted

val VhoqxethOnlineGreen = DavAccentEmerald
val VhoqxethUnreadBadge = DavAccentSky
val VhoqxethSlate900 = DavCanvasDark
val VhoqxethSlate600 = DavTextSecondary
