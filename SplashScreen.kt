package com.example.memopwd.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memopwd.ui.theme.BrandBlue
import com.example.memopwd.ui.theme.BrandDeep
import com.example.memopwd.ui.theme.BrandDim
import com.example.memopwd.ui.theme.BrandSlate
import kotlinx.coroutines.delay

/**
 * 对应原 Kivy 版时序：
 *  0.2s 整页淡入 + logo 从 55dp 弹回 42dp（out_bounce）
 *  1.2s 底部标语淡入
 *  3.0s 整页淡出 → 回调跳转
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var logoVisible by remember { mutableStateOf(false) }
    var footVisible by remember { mutableStateOf(false) }
    var fadeOut by remember { mutableStateOf(false) }

    val logoSize by animateDpAsState(
        targetValue = if (logoVisible) 42.dp else 55.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoSize"
    )

    val overallAlpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        finishedListener = { if (fadeOut) onSplashFinished() },
        label = "overallAlpha"
    )

    LaunchedEffect(Unit) {
        delay(200)
        logoVisible = true
        delay(1_000)
        footVisible = true
        delay(1_800)
        fadeOut = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrandDeep)
            .alpha(overallAlpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "JS",
                fontSize = logoSize.value.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlue,
                textAlign = TextAlign.Center,
                lineHeight = logoSize.value.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .wrapContentHeight(Alignment.CenterVertically)
            )

            Text(
                text = "JS STUDIO",
                fontSize = 14.sp,
                color = BrandSlate,
                textAlign = TextAlign.Center,
                letterSpacing = 3.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
                    .wrapContentHeight(Alignment.CenterVertically)
            )

            AnimatedVisibility(
                visible = footVisible,
                enter = fadeIn(animationSpec = tween(600)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
            ) {
                Text(
                    text = "Your world, your rules",
                    fontSize = 12.sp,
                    color = BrandDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.wrapContentHeight(Alignment.CenterVertically)
                )
            }
        }
    }
}
