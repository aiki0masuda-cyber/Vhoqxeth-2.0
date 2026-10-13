package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Clean & Readable Palette (Crisp White Canvas, Warm-Gray Cards, Deep Indigo)
// ============================================================================

// Canvas & Surfaces (Light Theme First)
val VhoqxethCanvasLight = Color(0xFFF8FAFC)          // Clean slate-50 background canvas
val VhoqxethCardLight = Color(0xFFFFFFFF)            // Crisp pure white elevated cards
val VhoqxethCardLightAlt = Color(0xFFF1F5F9)         // Soft warm slate-100 card highlight
val VhoqxethCardLightHover = Color(0xFFE2E8F0)       // Interactive slate-200 state
val VhoqxethBorderLight = Color(0xFFE2E8F0)          // Clean slate-200 subtle border
val VhoqxethBorderLightStrong = Color(0xFFCBD5E1)    // Slate-300 prominent border

// Accents (Deep Indigo & Crisp Cobalt - zero neon glow)
val VhoqxethIndigo600 = Color(0xFF4F46E5)            // Primary brand deep indigo
val VhoqxethIndigo500 = Color(0xFF6366F1)            // Indigo lighter accent
val VhoqxethIndigo700 = Color(0xFF4338CA)            // Indigo deeper accent
val VhoqxethCobalt600 = Color(0xFF2563EB)            // Secondary crisp cobalt blue
val VhoqxethCobalt500 = Color(0xFF3B82F6)            // Secondary cobalt light
val VhoqxethRose600 = Color(0xFFE11D48)              // Clean rose accent (replacing neon magenta)

// High-Contrast Readable Typography
val VhoqxethSlate900 = Color(0xFF0F172A)             // Primary high-contrast text
val VhoqxethSlate600 = Color(0xFF475569)             // Secondary readable text
val VhoqxethSlate500 = Color(0xFF64748B)             // Muted / caption text
val VhoqxethSlate400 = Color(0xFF94A3B8)             // Subtle placeholder text

// Functional Status Indicators
val VhoqxethEmerald500 = Color(0xFF10B981)           // Active / online status
val VhoqxethRed500 = Color(0xFFEF4444)               // Error / delete status
val VhoqxethAmber500 = Color(0xFFF59E0B)             // Warning status

// ============================================================================
// App-Wide Semantic Tokens (Clean Light Defaults)
// ============================================================================
val VhoqxethDarkBg = VhoqxethCanvasLight             // Clean canvas background
val VhoqxethSurface = VhoqxethCardLight              // Clean white card surface
val VhoqxethSurfaceElevated = VhoqxethCardLightAlt   // Soft warm-gray elevated surface
val VhoqxethSurfaceHighlight = VhoqxethCardLightHover// Subtle highlight surface
val VhoqxethBorder = VhoqxethBorderLight             // Clean 1.dp border
val VhoqxethDeepBlack = VhoqxethSlate900             // High-contrast slate

val VhoqxethPrimaryViolet = VhoqxethIndigo600        // Clean Deep Indigo
val VhoqxethPrimaryVioletBright = VhoqxethIndigo500  // Indigo 500
val VhoqxethPrimaryVioletGlow = VhoqxethIndigo700    // Deep Indigo
val VhoqxethCyanAccent = VhoqxethCobalt600           // Crisp Cobalt
val VhoqxethCyanBright = VhoqxethCobalt500           // Cobalt 500
val VhoqxethNeonMagenta = VhoqxethRose600            // Clean Rose 600

val VhoqxethTextPrimary = VhoqxethSlate900           // Deep Slate 900
val VhoqxethTextSecondary = VhoqxethSlate600         // Slate 600
val VhoqxethTextMuted = VhoqxethSlate500             // Slate 500

val VhoqxethOnlineGreen = VhoqxethEmerald500         // Emerald online pill
val VhoqxethUnreadBadge = VhoqxethIndigo600          // Deep Indigo badge
