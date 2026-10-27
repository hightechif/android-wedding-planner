package app.weddingplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.weddingplanner.core.data.SyncEngine
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.weddingplanner.core.datastore.UserPreferencesRepository
import app.weddingplanner.core.designsystem.WeddingTheme
import app.weddingplanner.navigation.WeddingNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var sync: SyncEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Pull from the sheet while the app is visible (TRD §5.7); stops in the background.
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { sync.pollWhileVisible() } }
        setContent {
            WeddingTheme {
                Surface(Modifier.fillMaxSize()) {
                    val prefs by preferences.preferences.collectAsStateWithLifecycle(initialValue = null)
                    // Wait for DataStore before choosing the start destination.
                    prefs?.let { WeddingNavHost(isLinked = it.isLinked) }
                }
            }
        }
    }
}
