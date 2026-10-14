package com.example.rgbleddemo.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<STATE, EVENT, EFFECT>(
    initialState: STATE,
) : ViewModel(), UnidirectionalViewModel<STATE, EVENT, EFFECT> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<STATE> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<EFFECT>()
    override val effect: SharedFlow<EFFECT> = _effect.asSharedFlow()

    private val handledOneTimeEvents = mutableSetOf<EVENT>()

    protected fun updateState(update: (STATE) -> STATE) {
        _state.update(update)
    }

    protected fun emitEffect(effect: EFFECT) {
        viewModelScope.launch { _effect.emit(effect) }
    }

    /** Runs [block] only the first time [event] is seen, e.g. an initial load after recomposition. */
    protected fun handleOneTimeEvent(event: EVENT, block: () -> Unit) {
        if (handledOneTimeEvents.add(event)) block()
    }
}
