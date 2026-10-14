package com.example.rgbleddemo.ui.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// Same shape as Thunderbird for Android's core/ui/contract mvi package (Apache-2.0).

/**
 * A view model with a single [STATE], driven only by [EVENT]s, that can emit one-off [EFFECT]s.
 */
interface UnidirectionalViewModel<STATE, EVENT, EFFECT> {
    val state: StateFlow<STATE>
    val effect: SharedFlow<EFFECT>

    fun event(event: EVENT)
}

data class StateDispatch<STATE, EVENT>(
    val state: State<STATE>,
    val dispatch: (EVENT) -> Unit,
)

/** Collects the state lifecycle-aware and forwards each effect to [handleEffect]. */
@Composable
inline fun <reified STATE, EVENT, EFFECT> UnidirectionalViewModel<STATE, EVENT, EFFECT>.observe(
    crossinline handleEffect: (EFFECT) -> Unit,
): StateDispatch<STATE, EVENT> {
    val collectedState = state.collectAsStateWithLifecycle()
    LaunchedEffect(key1 = effect) {
        effect.collect { handleEffect(it) }
    }
    return StateDispatch(state = collectedState, dispatch = { event(it) })
}
