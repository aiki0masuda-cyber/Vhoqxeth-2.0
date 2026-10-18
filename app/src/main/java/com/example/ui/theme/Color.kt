package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// DAV InfoExchange — Clean Scholastic White, Royal Blue & Slate Light Theme
// ============================================================================

// Canvas & Scholastic Clean Light Surfaces
val DavCanvasLight = Color(0xFFF8FAFC)          // Airy clean slate-50 background
val DavSurfaceCard = Color(0xFFFFFFFF)          // Pure white crisp card surface
val DavSurfaceElevated = Color(0xFFF1F5F9)      // Elevated interactive slate-100 card
val DavSurfaceHover = Color(0xFFE2E8F0)         // Active/pressed slate-200 surface
val DavBorder = Color(0xFFE2E8F0)               // Clean subtle slate-200 border (1.dp)
val DavBorderSubtle = Color(0xFFF1F5F9)         // Subtle divider

// Crisp High-Contrast Scholastic Typography
val DavTextPrimary = Color(0xFF0F172A)          // High-contrast deep slate-900 / navy
val DavTextSecondary = Color(0xFF475569)        // Legible neutral slate-600
val DavTextMuted = Color(0xFF64748B)            // Muted caption slate-500

// Scholastic Brand Accents (DAV Royal Blue, Sky Cyan, Institutional Gold)
val DavBrandRoyalBlue = Color(0xFF1E2A78)       // Official DAV crest royal navy
val DavAccentSky = Color(0xFF0284C7)            // Sky 600 precision active indicator
val DavAccentIndigo = Color(0xFF4338CA)         // Scholastic indigo-700
val DavAccentGold = Color(0xFFD97706)           // Institutional DAV crest gold / amber-600
val DavAccentEmerald = Color(0xFF16A34A)        // Live connection green-600
val DavAccentCrimson = Color(0xFFDC2626)        // Alert crimson red-600

// ============================================================================
// Semantic Compatibility Tokens (Theme Continuity mapped to Light Theme)
// ============================================================================
val VhoqxethDarkBg = DavCanvasLight
val VhoqxethSurface = DavSurfaceCard
val VhoqxethSurfaceElevated = DavSurfaceElevated
val VhoqxethSurfaceHighlight = DavSurfaceHover
val VhoqxethBorder = DavBorder
val VhoqxethBorderLightStrong = DavBorder
val VhoqxethDeepBlack = DavCanvasLight

val VhoqxethPrimaryViolet = DavBrandRoyalBlue
val VhoqxethPrimaryVioletBright = DavAccentSky
val VhoqxethPrimaryVioletGlow = DavAccentIndigo
val VhoqxethCyanAccent = DavAccentSky
val VhoqxethCyanBright = Color(0xFF0369A1)
val VhoqxethNeonMagenta = DavAccentGold
val VhoqxethIndigo600 = DavBrandRoyalBlue

val VhoqxethTextPrimary = DavTextPrimary
val VhoqxethTextSecondary = DavTextSecondary
val VhoqxethTextMuted = DavTextMuted

val VhoqxethOnlineGreen = DavAccentEmerald
val VhoqxethUnreadBadge = DavAccentSky
val VhoqxethSlate900 = DavSurfaceElevated
val VhoqxethSlate600 = DavTextSecondary

// Backward compatibility alias
val DavCanvasDark = DavCanvasLight
