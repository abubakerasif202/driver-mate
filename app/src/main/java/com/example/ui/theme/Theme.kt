package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PremiumGold,
    secondary = MutedGold,
    tertiary = TrueGold,
    background = PremiumBlack,
    surface = PremiumDarkSurface,
    onPrimary = PremiumBlack,
    onSecondary = PureWhite,
    onBackground = PureWhite,
    onSurface = PureWhite,
    surfaceVariant = PremiumCardBorder,
    onSurfaceVariant = PremiumGoldLight
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PremiumGoldDark,
    secondary = Slate700,
    tertiary = PremiumGold,
    background = Color(0xFFFAF9F6), // Warm Alabaster/Off-white
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Slate900,
    onBackground = PremiumBlack,
    onSurface = PremiumBlack,
    surfaceVariant = Color(0xFFF1EFEA), // Light soft warm gray/gold
    onSurfaceVariant = Slate700
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to let our bespoke Black & Gold branding shine!
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

