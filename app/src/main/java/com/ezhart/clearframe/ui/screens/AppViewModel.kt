package com.ezhart.clearframe.ui.screens

import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ezhart.clearframe.ClearFrameApplication
import com.ezhart.clearframe.MainActivity
import com.ezhart.clearframe.data.PhotoRepository
import com.ezhart.clearframe.sync.ReloadRequest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

private const val TAG = "ViewModel"

sealed interface AppUiState {
    data object Error : AppUiState
    data object Loading : AppUiState
    data object Empty : AppUiState

    data class Running(val slideShowViewModel: SlideshowViewModel) : AppUiState
}

class AppViewModel(private val photoRepository: PhotoRepository) : ViewModel() {
    var uiState: AppUiState by mutableStateOf(AppUiState.Loading)
    private var slideshowViewModel: SlideshowViewModel? = null

    init {
        EventBus.getDefault().register(this)
        getPhotos()
    }

    fun getPhotos() {
        uiState = AppUiState.Loading
        viewModelScope.launch {
            try {
                val photos = photoRepository.getPhotos()

                uiState = if (photos.isEmpty()) {
                    AppUiState.Empty
                } else {

                    // If we're reloading the slideshow for some reason (new photos, rotation)
                    // then we need to make sure the old one is cleaned up (unsubscribed from the event bus)
                    if (slideshowViewModel != null) {
                        slideshowViewModel?.cleanup()
                    }

                    val vm = SlideshowViewModel(photos)
                    slideshowViewModel = vm

                    AppUiState.Running(vm)
                }

            } catch (e: Exception) {
                uiState = AppUiState.Error
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ClearFrameApplication)
                val photoRepository = application.container.photoRepository
                AppViewModel(photoRepository = photoRepository)
            }
        }
    }

    @Subscribe
    public fun handleRemoteButton(event: MainActivity.RemoteKeyPressEvent) {
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK -> reload()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public fun handleReloadRequest(event: ReloadRequest) {
        Log.d(TAG, "Reload requested ${event.reason}")
        reload()
    }

    private fun reload() {
        getPhotos()
    }
}

