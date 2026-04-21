package com.jdw.skillstestapp.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdw.skillstestapp.data.model.UserImg
import com.jdw.skillstestapp.repository.MyAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoogleMapScreenViewModel @Inject constructor(
    private val appRepository: MyAppRepository,
) : ViewModel() {

    private val _userImages: MutableStateFlow<List<UserImg>> = MutableStateFlow(emptyList())
    val userImages: StateFlow<List<UserImg>> = _userImages.asStateFlow()

    init {
        getAllImages()
    }

    // for image
    fun fetchImagesToDb() {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.fetchImages()
            getAllImages()
        }
    }

    fun getAllImages() {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.getAllImages().forEach { userImage ->
                if (!appRepository.imageIsExist(userImage.imageDataPath)) {
                    appRepository.deleteUserImage(userImage)
                }
            }

            _userImages.value =
                appRepository.getAllImages().sortedByDescending { userImg ->
                    userImg.imageDateTaken
                }
        }
    }
}