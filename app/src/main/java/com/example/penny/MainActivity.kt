package com.example.penny

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.penny.ui.navigation.AppNavGraph
import com.example.penny.ui.screens.AppLockGate
import com.example.penny.ui.theme.PennyTheme

// Changed from ComponentActivity to FragmentActivity — androidx.biometric.BiometricPrompt
// requires a FragmentActivity to show the system fingerprint dialog.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PennyTheme {
                AppLockGate(activity = this) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}