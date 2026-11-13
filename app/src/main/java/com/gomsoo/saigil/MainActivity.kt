package com.gomsoo.saigil

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gomsoo.saigil.session.Event
import com.gomsoo.saigil.ui.SaigilApp
import com.gomsoo.saigil.ui.theme.SaigilTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The Recents thumbnail would otherwise keep a picture of the draft after the draft is cleared.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        setContent {
            SaigilTheme {
                val model by viewModel.model.collectAsStateWithLifecycle()
                SaigilApp(model = model, send = viewModel::send, close = ::finish)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) viewModel.send(Event.PageHidden)
    }
}
