package com.weblog.android.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weblog.android.data.AppViewModel

@Composable
fun WebLogApp(vm: AppViewModel = viewModel()) {
    val toastMsg by vm.toastMessage.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial)
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (up != null) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }
        }
    ) {
        MainScreen(vm = vm)

        toastMsg?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2000)
                vm.clearToast()
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.inverseSurface
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
