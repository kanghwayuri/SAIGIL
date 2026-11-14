package com.gomsoo.saigil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gomsoo.saigil.screen.Event
import com.gomsoo.saigil.ui.SaigilApp
import com.gomsoo.saigil.ui.theme.SaigilTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SaigilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SaigilTheme {
                val ui by viewModel.ui.collectAsStateWithLifecycle()
                SaigilApp(ui, viewModel::send)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // The microphone belongs to the foreground only.
        if (!isChangingConfigurations) viewModel.send(Event.PageHidden)
    }
}
