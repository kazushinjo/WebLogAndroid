package com.weblog.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weblog.android.data.AppViewModel

@Composable
fun WebLogApp(vm: AppViewModel = viewModel()) {
    val toastMsg by vm.toastMessage.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
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
