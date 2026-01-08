package com.mintly.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mintly.app.data.MintlyRepository

class MintlyViewModelFactory(private val repository: MintlyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MintlyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MintlyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
