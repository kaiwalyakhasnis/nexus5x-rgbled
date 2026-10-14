package com.example.rgbleddemo.ui.led

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.rgbleddemo.R
import com.example.rgbleddemo.ui.led.LedContract.Effect
import com.example.rgbleddemo.ui.led.LedContract.Event
import com.example.rgbleddemo.ui.mvi.observe
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedScreen(
    viewModel: LedContract.ViewModel,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.ShowFailure -> scope.launch {
                snackbarHostState.showSnackbar(
                    "${effect.action} failed. See logcat RgbLedService/RgbLedHal.",
                )
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.LoadState)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        LedContent(
            state = state.value,
            onEvent = dispatch,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
