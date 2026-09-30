package com.unicornwhodev.visiondatasetstudio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.unicornwhodev.visiondatasetstudio.R

val StudioFont = FontFamily(Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium), Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold))

val Typography = Typography(
    displayLarge = TextStyle(fontFamily=StudioFont,fontWeight=FontWeight.SemiBold,fontSize=44.sp,lineHeight=50.sp),
    displayMedium = TextStyle(fontFamily=StudioFont,fontWeight=FontWeight.SemiBold,fontSize=36.sp,lineHeight=42.sp),
    displaySmall = TextStyle(fontFamily=StudioFont,fontWeight=FontWeight.SemiBold,fontSize=30.sp,lineHeight=36.sp),
    headlineLarge = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily=StudioFont,fontWeight=FontWeight.SemiBold,fontSize=22.sp,lineHeight=28.sp),
    titleLarge = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    labelLarge = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = StudioFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
)
