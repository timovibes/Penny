package com.example.penny.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.penny.ui.screens.HomeScreen
import com.example.penny.ui.screens.PrivacyPolicyScreen
import com.example.penny.ui.screens.ProfileScreen
import com.example.penny.ui.screens.SignInScreen
import com.example.penny.ui.screens.SignUpScreen
import com.example.penny.ui.screens.TermsOfServiceScreen
import com.example.penny.viewmodel.HomeViewModel
import com.example.penny.viewmodel.ProfileViewModel
import com.example.penny.viewmodel.SignInViewModel
import com.example.penny.viewmodel.SignUpViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph(navController: NavHostController) {
    val isLoggedIn = remember { FirebaseAuth.getInstance().currentUser != null }
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "signin"
    ) {

        composable("signin") {
            val viewModel: SignInViewModel = viewModel()
            SignInScreen(
                email = viewModel.email.collectAsState().value,
                password = viewModel.password.collectAsState().value,
                authState = viewModel.authState.collectAsState().value,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onSignInClick = viewModel::signIn,
                onForgotPasswordClick = viewModel::forgotPassword,
                onSignUpClick = { navController.navigate("signup") },
                onSignInSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("signup") {
            val viewModel: SignUpViewModel = viewModel()
            SignUpScreen(
                name = viewModel.name.collectAsState().value,
                email = viewModel.email.collectAsState().value,
                password = viewModel.password.collectAsState().value,
                isTermsAccepted = viewModel.isTermsAccepted.collectAsState().value,
                authState = viewModel.authState.collectAsState().value,
                onNameChange = viewModel::onNameChange,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onTermsCheckedChange = viewModel::onTermsCheckedChange,
                onSignUpClick = viewModel::signUp,
                onLoginClick = { navController.navigate("signin") },
                onTermsClick = { navController.navigate("terms") },
                onPrivacyClick = { navController.navigate("privacy") },
                onSignUpSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("terms") {
            TermsOfServiceScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("privacy") {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("home") {
            val viewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onProfileClick = {
                    navController.navigate("profile")
                }
            )
        }

        composable("profile") {
            val viewModel: ProfileViewModel = viewModel()
            val currentCurrency by viewModel.currencyCode.collectAsState()
            val biometricEnabled by viewModel.biometricEnabled.collectAsState()

            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                state = viewModel.loadState(),
                currentCurrency = currentCurrency,
                onCurrencySelected = viewModel::setCurrency,
                biometricEnabled = biometricEnabled,
                onFaceIdToggle = viewModel::setBiometricEnabled,
                onPrivacyPolicyClick = { navController.navigate("privacy") },
                onLogoutClick = {
                    viewModel.logout()
                    navController.navigate("signin") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }


    }
}