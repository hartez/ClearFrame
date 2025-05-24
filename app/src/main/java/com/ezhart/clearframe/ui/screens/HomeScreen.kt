package com.ezhart.clearframe.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ezhart.clearframe.R
import com.ezhart.clearframe.ui.theme.ClearFrameTheme

@Composable
fun HomeScreen(
    uiState: AppUiState,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AppUiState.Loading -> LoadingScreen(modifier = modifier.fillMaxSize())
        is AppUiState.Running -> SlideScreen(
            uiState, modifier = modifier.fillMaxWidth()
        )

        is AppUiState.Error -> ErrorScreen(modifier = modifier.fillMaxSize())
        AppUiState.Empty -> EmptyScreen(modifier = modifier.fillMaxSize())
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier.size(200.dp),
        painter = painterResource(R.drawable.loading),
        contentDescription = stringResource(R.string.loading)
    )
}

@Composable
fun ErrorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.connection_error), contentDescription = ""
        )
        Text(text = stringResource(R.string.loading_failed), modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun EmptyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = modifier.size(200.dp),
            painter = painterResource(id = R.drawable.empty), contentDescription = ""
        )
        Text(text = stringResource(R.string.empty), modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun SlideScreen(
    uiState: AppUiState.Running,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(colorResource(R.color.black))
            .fillMaxSize(), contentAlignment = Alignment.Center
    ) {

        AnimatedContent(
            targetState = uiState.slideShowViewModel.currentPhoto,
            label = "animated content",
            transitionSpec = {

                val slideDirection = when (uiState.slideShowViewModel.direction) {
                    SlideDirection.Forward -> AnimatedContentTransitionScope.SlideDirection.Left
                    SlideDirection.Backward -> AnimatedContentTransitionScope.SlideDirection.Right
                }

                val slideDuration = 1500
                val fadeDuration = 1000

                slideIntoContainer(
                    towards = slideDirection,
                    animationSpec = tween(slideDuration)
                ) + fadeIn(animationSpec = tween(fadeDuration)) togetherWith
                        slideOutOfContainer(
                            towards = slideDirection,
                            animationSpec = tween(slideDuration)
                        ) + fadeOut(animationSpec = tween(fadeDuration))

            }
        ) { filename ->
            if (filename == uiState.slideShowViewModel.currentPhoto) {
                AsyncImage(
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(uiState.slideShowViewModel.currentPhoto)
                        .build(),
                    error = painterResource(R.drawable.broken_image),
                    contentDescription = stringResource(R.string.photo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(uiState.slideShowViewModel.previousPhoto)
                        .build(),
                    placeholder = painterResource(R.drawable.loading),
                    error = painterResource(R.drawable.broken_image),
                    contentDescription = stringResource(R.string.photo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

}


@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    ClearFrameTheme {
        LoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorScreenPreview() {
    ClearFrameTheme {
        ErrorScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyScreenPreview() {
    ClearFrameTheme {
        EmptyScreen()
    }
}
