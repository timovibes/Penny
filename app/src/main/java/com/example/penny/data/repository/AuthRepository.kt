package com.example.penny.data.repository

import com.example.penny.data.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    suspend fun signUp(name: String, email: String, password: String): AuthResult {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            result.user?.updateProfile(profileUpdates)?.await()

            return AuthResult.Success

        } catch (e: FirebaseAuthUserCollisionException) {
            return AuthResult.Error("An account with this email already exists.")
        } catch (e: FirebaseAuthWeakPasswordException) {
            return AuthResult.Error("Password is too weak. Use at least 6 characters.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            return AuthResult.Error("That email address doesn't look valid.")
        } catch (e: Exception) {
            return AuthResult.Error("Something went wrong creating your account. Please try again.")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            return AuthResult.Success
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            return AuthResult.Error("Incorrect email or password.")
        } catch (e: Exception) {
            return AuthResult.Error("Couldn't sign you in. Please try again.")
        }
    }

    suspend fun forgotPassword(email: String): AuthResult {
        try {
            auth.sendPasswordResetEmail(email).await()
            return AuthResult.Success
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            return AuthResult.Error("That email address doesn't look valid.")
        } catch (e: Exception) {
            return AuthResult.Error("Couldn't send the reset email. Please try again.")
        }
    }

    fun signOut() {
        auth.signOut()
    }
}