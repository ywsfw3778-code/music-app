package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

val AppFontFamily = FontFamily(
    Font(R.font.app_font_regular, FontWeight.Light),
    Font(R.font.app_font_regular, FontWeight.Normal),
    Font(R.font.app_font_medium, FontWeight.Medium),
    Font(R.font.app_font_medium, FontWeight.SemiBold),
    Font(R.font.app_font_bold, FontWeight.Bold),
    Font(R.font.app_font_extrabold, FontWeight.ExtraBold),
    Font(R.font.app_font_extrabold, FontWeight.Black)
)

private val BaseTypography = Typography()

val AppTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = AppFontFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = AppFontFamily),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = AppFontFamily),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = AppFontFamily),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = AppFontFamily),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = AppFontFamily),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = AppFontFamily),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = AppFontFamily),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = AppFontFamily),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = AppFontFamily),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = AppFontFamily),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = AppFontFamily),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = AppFontFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = AppFontFamily),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = AppFontFamily)
)
