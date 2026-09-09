package com.open365.vpn.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** 主题选项：动态取色（Android 12+）+ 5 套固定配色 */
enum class AppTheme(val id: String, val label: String) {
    DYNAMIC("dynamic", "动态取色 · Material You"),
    FROST("frost", "霜青 · Frost"),
    AURORA("aurora", "极光 · Aurora"),
    SAKURA("sakura", "樱花 · Sakura"),
    OCEAN("ocean", "海洋 · Ocean"),
    SUNSET("sunset", "落日 · Sunset");

    companion object {
        fun fromId(id: String?): AppTheme {
            entries.firstOrNull { it.id == id }?.let { return it }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) DYNAMIC else FROST
        }
    }
}

private data class PaletteColors(
    val primary: Color, val onPrimary: Color,
    val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondary: Color, val onSecondary: Color,
    val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color,
    val tertiaryContainer: Color, val onTertiaryContainer: Color,
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color,
    val surfaceVariant: Color, val onSurfaceVariant: Color,
    val outline: Color,
)

private fun lightScheme(c: PaletteColors) = lightColorScheme(
    primary = c.primary, onPrimary = c.onPrimary,
    primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
    secondary = c.secondary, onSecondary = c.onSecondary,
    secondaryContainer = c.secondaryContainer, onSecondaryContainer = c.onSecondaryContainer,
    tertiary = c.tertiary, onTertiary = c.onTertiary,
    tertiaryContainer = c.tertiaryContainer, onTertiaryContainer = c.onTertiaryContainer,
    background = c.background, onBackground = c.onBackground,
    surface = c.surface, onSurface = c.onSurface,
    surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
    outline = c.outline,
)

private fun darkScheme(c: PaletteColors) = darkColorScheme(
    primary = c.primary, onPrimary = c.onPrimary,
    primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
    secondary = c.secondary, onSecondary = c.onSecondary,
    secondaryContainer = c.secondaryContainer, onSecondaryContainer = c.onSecondaryContainer,
    tertiary = c.tertiary, onTertiary = c.onTertiary,
    tertiaryContainer = c.tertiaryContainer, onTertiaryContainer = c.onTertiaryContainer,
    background = c.background, onBackground = c.onBackground,
    surface = c.surface, onSurface = c.onSurface,
    surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
    outline = c.outline,
)

// ── Frost 霜青（默认品牌色，teal） ──
private val FrostLight = PaletteColors(
    Color(0xFF006A60), Color(0xFFFFFFFF), Color(0xFF74F8E7), Color(0xFF00201C),
    Color(0xFF4A635F), Color(0xFFFFFFFF), Color(0xFFCCE8E2), Color(0xFF06201C),
    Color(0xFF456179), Color(0xFFFFFFFF), Color(0xFFCCE5FF), Color(0xFF001E31),
    Color(0xFFF4FBF8), Color(0xFF161D1C), Color(0xFFF4FBF8), Color(0xFF161D1C),
    Color(0xFFDAE5E1), Color(0xFF3F4947), Color(0xFF6F7977),
)
private val FrostDark = PaletteColors(
    Color(0xFF54DBC9), Color(0xFF003731), Color(0xFF005048), Color(0xFF74F8E7),
    Color(0xFFB0CCC6), Color(0xFF1C3531), Color(0xFF334B47), Color(0xFFCCE8E2),
    Color(0xFFADCAE6), Color(0xFF143349), Color(0xFF2D4961), Color(0xFFCCE5FF),
    Color(0xFF0E1514), Color(0xFFDDE4E2), Color(0xFF0E1514), Color(0xFFDDE4E2),
    Color(0xFF3F4947), Color(0xFFBEC9C6), Color(0xFF889391),
)

// ── Aurora 极光（紫，M3 baseline） ──
private val AuroraLight = PaletteColors(
    Color(0xFF6750A4), Color(0xFFFFFFFF), Color(0xFFEADDFF), Color(0xFF21005D),
    Color(0xFF625B71), Color(0xFFFFFFFF), Color(0xFFE8DEF8), Color(0xFF1D192B),
    Color(0xFF7D5260), Color(0xFFFFFFFF), Color(0xFFFFD8E4), Color(0xFF31111D),
    Color(0xFFFFFBFF), Color(0xFF1C1B1F), Color(0xFFFFFBFF), Color(0xFF1C1B1F),
    Color(0xFFE7E0EC), Color(0xFF49454F), Color(0xFF79747E),
)
private val AuroraDark = PaletteColors(
    Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF),
    Color(0xFFCCC2DC), Color(0xFF332D41), Color(0xFF4A4458), Color(0xFFE8DEF8),
    Color(0xFFEFB8C8), Color(0xFF492532), Color(0xFF633B48), Color(0xFFFFD8E4),
    Color(0xFF1C1B1F), Color(0xFFE6E1E5), Color(0xFF1C1B1F), Color(0xFFE6E1E5),
    Color(0xFF49454F), Color(0xFFCAC4D0), Color(0xFF938F99),
)

// ── Sakura 樱花（粉） ──
private val SakuraLight = PaletteColors(
    Color(0xFF984061), Color(0xFFFFFFFF), Color(0xFFFFD9E2), Color(0xFF3E001D),
    Color(0xFF74565F), Color(0xFFFFFFFF), Color(0xFFFFD9E2), Color(0xFF2B151C),
    Color(0xFF7C5635), Color(0xFFFFFFFF), Color(0xFFFFDCC1), Color(0xFF2E1500),
    Color(0xFFFFF8F8), Color(0xFF22191C), Color(0xFFFFF8F8), Color(0xFF22191C),
    Color(0xFFF2DDE1), Color(0xFF514347), Color(0xFF837377),
)
private val SakuraDark = PaletteColors(
    Color(0xFFFFB1C8), Color(0xFF5E1133), Color(0xFF7D2949), Color(0xFFFFD9E2),
    Color(0xFFE2BDC6), Color(0xFF422931), Color(0xFF5A3F44), Color(0xFFFFD9E2),
    Color(0xFFEFB9A0), Color(0xFF48290B), Color(0xFF633F28), Color(0xFFFFDCC1),
    Color(0xFF1A1113), Color(0xFFF0E4E6), Color(0xFF1A1113), Color(0xFFF0E4E6),
    Color(0xFF514347), Color(0xFFD5C2C6), Color(0xFF9D8C90),
)

// ── Ocean 海洋（蓝） ──
private val OceanLight = PaletteColors(
    Color(0xFF0061A4), Color(0xFFFFFFFF), Color(0xFFD1E4FF), Color(0xFF001D36),
    Color(0xFF535F70), Color(0xFFFFFFFF), Color(0xFFD7E3F7), Color(0xFF101C2B),
    Color(0xFF6B5778), Color(0xFFFFFFFF), Color(0xFFF2DAFF), Color(0xFF271430),
    Color(0xFFF8F9FF), Color(0xFF191C20), Color(0xFFF8F9FF), Color(0xFF191C20),
    Color(0xFFDDE3EA), Color(0xFF41474D), Color(0xFF71787F),
)
private val OceanDark = PaletteColors(
    Color(0xFFA0CAFD), Color(0xFF003258), Color(0xFF00497D), Color(0xFFD1E4FF),
    Color(0xFFBBC7DB), Color(0xFF253140), Color(0xFF3B4858), Color(0xFFD7E3F7),
    Color(0xFFD7BDE4), Color(0xFF3B2948), Color(0xFF523F5F), Color(0xFFF2DAFF),
    Color(0xFF111318), Color(0xFFE2E2E9), Color(0xFF111318), Color(0xFFE2E2E9),
    Color(0xFF41474D), Color(0xFFC3C9CF), Color(0xFF8B9199),
)

// ── Sunset 落日（橙） ──
private val SunsetLight = PaletteColors(
    Color(0xFF8B5000), Color(0xFFFFFFFF), Color(0xFFFFDCBE), Color(0xFF2C1600),
    Color(0xFF74593F), Color(0xFFFFFFFF), Color(0xFFFFDCBE), Color(0xFF2B1708),
    Color(0xFF5C6236), Color(0xFFFFFFFF), Color(0xFFE0EAB4), Color(0xFF191E00),
    Color(0xFFFFF8F4), Color(0xFF221A14), Color(0xFFFFF8F4), Color(0xFF221A14),
    Color(0xFFF4E0D0), Color(0xFF52443A), Color(0xFF857468),
)
private val SunsetDark = PaletteColors(
    Color(0xFFFFB86B), Color(0xFF4A2800), Color(0xFF6A3C00), Color(0xFFFFDCBE),
    Color(0xFFE6C0A4), Color(0xFF402C18), Color(0xFF58422C), Color(0xFFFFDCBE),
    Color(0xFFC4CC96), Color(0xFF2E3300), Color(0xFF454B23), Color(0xFFE0EAB4),
    Color(0xFF17120E), Color(0xFFECE0D8), Color(0xFF17120E), Color(0xFFECE0D8),
    Color(0xFF52443A), Color(0xFFD7C3B5), Color(0xFF9F8D80),
)

private val lightPalettes = mapOf(
    AppTheme.FROST to FrostLight,
    AppTheme.AURORA to AuroraLight,
    AppTheme.SAKURA to SakuraLight,
    AppTheme.OCEAN to OceanLight,
    AppTheme.SUNSET to SunsetLight,
)

private val darkPalettes = mapOf(
    AppTheme.FROST to FrostDark,
    AppTheme.AURORA to AuroraDark,
    AppTheme.SAKURA to SakuraDark,
    AppTheme.OCEAN to OceanDark,
    AppTheme.SUNSET to SunsetDark,
)

val AppTheme.lightScheme: ColorScheme
    get() = lightSchemeOf(lightPalettes.getValue(this))

val AppTheme.darkScheme: ColorScheme
    get() = darkSchemeOf(darkPalettes.getValue(this))

private fun lightSchemeOf(c: PaletteColors) = lightScheme(c)
private fun darkSchemeOf(c: PaletteColors) = darkScheme(c)

/** 主题选择器里的预览色点（primary / secondary / tertiary，浅色方案） */
val AppTheme.previewColors: List<Color>
    get() = lightPalettes.getValue(this).let { listOf(it.primary, it.secondary, it.tertiary) }
