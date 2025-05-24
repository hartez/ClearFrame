package com.ezhart.clearframe.ui.screens

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezhart.clearframe.MainActivity
import com.ezhart.clearframe.model.Photo
import com.ezhart.clearframe.ui.screens.SlideDirection.Forward
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

sealed interface SlideDirection {
    data object Forward : SlideDirection
    data object Backward : SlideDirection
}

class SlideshowViewModel(val photos: List<Photo>) : ViewModel() {
    private var currentIndex: Int = 0
    private var autoAdvance: Boolean = true
    private var autoAdvanceInterval: Long = 10

    // Keep track of this so we can cancel it and start another auto-advance job when the user
    // manually moves the slideshow forward or backward
    var autoAdvanceJob: Job? = null

    var currentPhoto: String by mutableStateOf(photos[currentIndex].filename)
    var previousPhoto: String by mutableStateOf(currentPhoto)
    var direction: SlideDirection by mutableStateOf(Forward)

    init {
        EventBus.getDefault().register(this)

        if (autoAdvance) {
            play()
        }
    }

    @Subscribe
    fun handleRemoteButton(event: MainActivity.RemoteKeyPressEvent) {
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> handleManualAdvance(::next)
            KeyEvent.KEYCODE_DPAD_LEFT -> handleManualAdvance(::prev)
            KeyEvent.KEYCODE_DPAD_CENTER -> togglePlayPause()
        }
    }

    private fun handleManualAdvance(advance: () -> Unit) {
        if (autoAdvance) {
            autoAdvanceJob?.cancel()
        }

        advance()

        if (autoAdvance) {
            play()
        }
    }

    fun next() {
        currentIndex += 1
        if (currentIndex >= photos.size) {
            currentIndex = 0
        }
        previousPhoto = currentPhoto
        currentPhoto = photos[currentIndex].filename
        direction = SlideDirection.Forward
    }

    fun prev() {
        currentIndex -= 1
        if (currentIndex < 0) {
            currentIndex = photos.size - 1
        }
        previousPhoto = currentPhoto
        currentPhoto = photos[currentIndex].filename
        direction = SlideDirection.Backward
    }

    private fun play() {
        autoAdvance = true
        autoAdvanceJob = viewModelScope.launch {
            while (autoAdvance) {
                delay(autoAdvanceInterval * 1000)
                if (autoAdvance) {
                    next()
                }
            }
        }
    }

    private fun pause() {
        autoAdvance = false
    }

    private fun togglePlayPause() = when {
        autoAdvance -> {
            pause()
        }

        else -> {
            play()
        }
    }

    fun cleanup() {
        EventBus.getDefault().unregister(this)
    }
}