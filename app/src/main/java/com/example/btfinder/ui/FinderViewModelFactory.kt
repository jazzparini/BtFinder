package com.example.btfinder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.btfinder.data.BluetoothRepository
import com.example.btfinder.data.PreferencesRepository
import com.example.btfinder.util.BeepPlayer
import com.example.btfinder.util.VibratorHelper

class FinderViewModelFactory(
    private val repository: BluetoothRepository,
    private val preferences: PreferencesRepository,
    private val beepPlayer: BeepPlayer,
    private val vibratorHelper: VibratorHelper
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        return FinderViewModel(
            repository,
            preferences,
            beepPlayer,
            vibratorHelper
        ) as T
    }
}
