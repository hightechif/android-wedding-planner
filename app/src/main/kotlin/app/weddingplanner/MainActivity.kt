package app.weddingplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.weddingplanner.core.datastore.UserPreferencesRepository
import app.weddingplanner.core.designsystem.WeddingTheme
import app.weddingplanner.navigation.WeddingNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferences: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
