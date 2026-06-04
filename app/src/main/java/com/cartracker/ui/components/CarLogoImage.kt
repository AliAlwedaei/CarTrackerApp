package com.cartracker.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.cartracker.ui.theme.OnSurfaceSecondary

private const val CDN = "https://raw.githubusercontent.com/filippofilip95/car-logos-dataset/master/logos/optimized"

private fun makeSlug(make: String): String = make
    .trim()
    .lowercase()
    .replace("mercedes-benz", "mercedes-benz")
    .replace("land rover", "land-rover")
    .replace("aston martin", "aston-martin")
    .replace("alfa romeo", "alfa-romeo")
    .replace("rolls-royce", "rolls-royce")
    .replace(" ", "-")

@Composable
fun CarLogoImage(
    make: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    fallbackTint: Color = OnSurfaceSecondary
) {
    val url = "$CDN/${makeSlug(make)}.png"
    val padding = Modifier.fillMaxSize().padding(5.dp)

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(300)
            .build(),
        contentDescription = "$make logo",
        modifier = modifier,
        contentScale = ContentScale.Fit
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success ->
                SubcomposeAsyncImageContent(
                    modifier = padding,
                    colorFilter = ColorFilter.tint(tint, BlendMode.SrcIn)
                )
            else ->
                Icon(
                    Icons.Filled.DirectionsCar, null,
                    tint = if (painter.state is AsyncImagePainter.State.Loading)
                        fallbackTint.copy(alpha = 0.3f) else fallbackTint,
                    modifier = padding
                )
        }
    }
}
