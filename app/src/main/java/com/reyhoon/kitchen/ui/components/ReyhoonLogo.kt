package com.reyhoon.kitchen.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.R
import com.reyhoon.kitchen.ui.theme.GreenPale
import com.reyhoon.kitchen.ui.theme.GreenLight

@Composable
fun ReyhoonLogo(
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(GreenPale, GreenLight.copy(alpha = 0.35f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_reyhoon_logo),
            contentDescription = "لوگوی آشپزخانه ریحون",
            modifier = Modifier.size(size * 0.92f),
            contentScale = ContentScale.Fit
        )
    }
}
