package br.com.aditum.paytrial.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.aditum.paytrial.R

/*
 * Aditum Pay Trial Design System (aprovado em 23/09/2026). Material 3 por baixo, identidade própria por cima:
 * paleta de #2F2256 (esquema "fidelity" da biblioteca de cores do Material, com a cor principal fixada na semente no
 * tema claro), Nunito Sans, cantos de 2 a 12 dp (padrão 6), Tabler Icons preenchidos. Técnico, sóbrio e amigável.
 */

private val Light = lightColorScheme(
    primary = Color(0xFF2F2256), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFE7DEFB), onPrimaryContainer = Color(0xFF1A0B41),
    secondary = Color(0xFF615B73), onSecondary = Color(0xFFFFFFFF), secondaryContainer = Color(0xFFE6E1EE), onSecondaryContainer = Color(0xFF1E1A2B),
    tertiary = Color(0xFF7C5800), onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFFFDEA6), onTertiaryContainer = Color(0xFF271900),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDF8FD), onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFDF8FD), onSurface = Color(0xFF1C1B1F), surfaceVariant = Color(0xFFE6E1E6), onSurfaceVariant = Color(0xFF48454F),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF7F2F7), surfaceContainer = Color(0xFFF1ECF2),
    surfaceContainerHigh = Color(0xFFECE7EC), surfaceContainerHighest = Color(0xFFE6E1E6), surfaceDim = Color(0xFFDDD8DE), surfaceBright = Color(0xFFFDF8FD),
    outline = Color(0xFF797580), outlineVariant = Color(0xFFCAC4D0), inverseSurface = Color(0xFF313034), inverseOnSurface = Color(0xFFF4EFF5),
    inversePrimary = Color(0xFFCDBDFC), scrim = Color(0xFF000000),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFCDBDFC), onPrimary = Color(0xFF34275C), primaryContainer = Color(0xFF463A70), onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFFCBC2DE), onSecondary = Color(0xFF332D43), secondaryContainer = Color(0xFF49435A), onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFF5BF48), onTertiary = Color(0xFF412D00), tertiaryContainer = Color(0xFF5D4200), onTertiaryContainer = Color(0xFFFFDEA6),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141316), onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF141316), onSurface = Color(0xFFE6E1E6), surfaceVariant = Color(0xFF48454F), onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF0F0E11), surfaceContainerLow = Color(0xFF1C1B1F), surfaceContainer = Color(0xFF201F23),
    surfaceContainerHigh = Color(0xFF2B292D), surfaceContainerHighest = Color(0xFF363438), surfaceDim = Color(0xFF141316), surfaceBright = Color(0xFF3A383D),
    outline = Color(0xFF938F9A), outlineVariant = Color(0xFF48454F), inverseSurface = Color(0xFFE6E1E6), inverseOnSurface = Color(0xFF313034),
    inversePrimary = Color(0xFF63568D), scrim = Color(0xFF000000),
)

/** Cores de estado, independentes da marca: sucesso, alerta, info (o erro é o do `ColorScheme`). */
@Immutable
data class PtStatusColors(
    val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val info: Color, val onInfo: Color, val infoContainer: Color, val onInfoContainer: Color,
)

private val LightStatus = PtStatusColors(
    Color(0xFF1E6B3A), Color.White, Color(0xFFB7F1C6), Color(0xFF00210C),
    Color(0xFF7C5800), Color.White, Color(0xFFFFDEA6), Color(0xFF271900),
    Color(0xFF0B57D0), Color.White, Color(0xFFD8E2FF), Color(0xFF001A41),
)
private val DarkStatus = PtStatusColors(
    Color(0xFF8BD8A0), Color(0xFF00391A), Color(0xFF005228), Color(0xFFB7F1C6),
    Color(0xFFF5BF48), Color(0xFF412D00), Color(0xFF5D4200), Color(0xFFFFDEA6),
    Color(0xFFADC6FF), Color(0xFF002E69), Color(0xFF0842A0), Color(0xFFD8E2FF),
)

val LocalPtStatus = staticCompositionLocalOf { LightStatus }

val NunitoSans = FontFamily(
    Font(R.font.nunito_sans_regular, FontWeight.Normal), Font(R.font.nunito_sans_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_sans_bold, FontWeight.Bold), Font(R.font.nunito_sans_extrabold, FontWeight.ExtraBold),
)
/** Comprovante, NSU, JSON e console. */
val PtMono = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal), Font(R.font.jetbrains_mono_medium, FontWeight.Medium))

private fun style(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0) =
    TextStyle(fontFamily = NunitoSans, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = spacing.sp)

private val PtTypography = Typography(
    displaySmall = style(40, 44, FontWeight.ExtraBold),          // valor
    headlineSmall = style(24, 30, FontWeight.ExtraBold),         // resultado
    titleLarge = style(18, 24, FontWeight.Bold),                 // título da barra
    titleMedium = style(16, 22, FontWeight.Bold),
    titleSmall = style(14, 20, FontWeight.Bold),
    bodyLarge = style(15, 22, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(13, 18, FontWeight.Normal),
    labelLarge = style(14, 18, FontWeight.Bold),                 // botões
    labelMedium = style(12, 16, FontWeight.Bold),
    labelSmall = style(11, 16, FontWeight.ExtraBold, 0.8),       // sobretítulo em caixa alta
)

/** "Levemente arredondado, mais quadrado que redondo": nada em pílula. */
val PtShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp), small = RoundedCornerShape(4.dp), medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp), extraLarge = RoundedCornerShape(12.dp),
)

val MonoStyle = TextStyle(fontFamily = PtMono, fontSize = 12.sp, lineHeight = 16.sp)

/**
 * Cores do estabelecimento (`getMerchantCustomization`, 3.0.0): primária e secundária do portal. Nulas = as do Pay Trial.
 * Cores de estado (sucesso, alerta, erro) não mudam com a marca.
 */
@Immutable
data class PtBrand(val primary: Color? = null, val secondary: Color? = null) {
    companion object {
        /** `#RRGGBB` (ou `#AARRGGBB`) do serviço; texto inválido ou vazio = sem cor. */
        fun parse(hex: String?): Color? = hex?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { Color(android.graphics.Color.parseColor(if (it.startsWith("#")) it else "#$it")) }.getOrNull() }
    }
}

private fun contrast(a: Color, b: Color): Float {
    val (l1, l2) = a.luminance() to b.luminance()
    return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
}
/** A cor da marca, escurecida ou clareada só o bastante para ler sobre `bg` (a do portal pode ser um amarelo-claro). */
private fun readable(seed: Color, bg: Color, min: Float, towards: Color): Color {
    var t = 0f; var c = seed
    while (contrast(c, bg) < min && t < 1f) { t += 0.05f; c = lerp(seed, towards, t) }
    return c
}
private fun onColor(c: Color): Color = if (contrast(c, Color.White) >= contrast(c, Color(0xFF1C1B1F))) Color.White else Color(0xFF1C1B1F)

/** Troca primária e secundária do esquema pelas da marca, com as variações (container, "on") derivadas da semente. */
private fun ColorScheme.branded(brand: PtBrand, dark: Boolean): ColorScheme {
    var s = this
    brand.primary?.let { seed ->
        val primary = if (dark) readable(lerp(seed, Color.White, 0.35f), surface, 4.5f, Color.White) else readable(seed, surface, 3f, Color.Black)
        val container = if (dark) lerp(seed, Color.Black, 0.5f) else lerp(seed, Color.White, 0.82f)
        s = s.copy(primary = primary, onPrimary = onColor(primary), primaryContainer = container,
            onPrimaryContainer = readable(if (dark) lerp(seed, Color.White, 0.8f) else lerp(seed, Color.Black, 0.55f), container, 4.5f, if (dark) Color.White else Color.Black),
            inversePrimary = if (dark) readable(seed, Light.surface, 3f, Color.Black) else lerp(seed, Color.White, 0.55f), surfaceTint = primary)
    }
    brand.secondary?.let { seed ->
        val secondary = if (dark) readable(lerp(seed, Color.White, 0.35f), surface, 4.5f, Color.White) else readable(seed, surface, 3f, Color.Black)
        val container = if (dark) lerp(seed, Color.Black, 0.5f) else lerp(seed, Color.White, 0.82f)
        s = s.copy(secondary = secondary, onSecondary = onColor(secondary), secondaryContainer = container,
            onSecondaryContainer = readable(if (dark) lerp(seed, Color.White, 0.8f) else lerp(seed, Color.Black, 0.55f), container, 4.5f, if (dark) Color.White else Color.Black))
    }
    return s
}

@Composable
fun PtTheme(dark: Boolean = isSystemInDarkTheme(), brand: PtBrand? = null, content: @Composable () -> Unit) {
    val colors: ColorScheme = remember(dark, brand) { (if (dark) Dark else Light).let { base -> brand?.let { base.branded(it, dark) } ?: base } }
    CompositionLocalProvider(LocalPtStatus provides if (dark) DarkStatus else LightStatus) {
        MaterialTheme(colorScheme = colors, typography = PtTypography, shapes = PtShapes, content = content)
    }
}

/** Acesso curto às cores de estado: `PtTheme.status.success`. */
object PtTheme { val status: PtStatusColors @Composable get() = LocalPtStatus.current }
