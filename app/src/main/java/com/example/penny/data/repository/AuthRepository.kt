package com.example.penny.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.penny.data.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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

    // Shrinks and compresses the picked image, then stores it as text inside the
    // user's own Firestore document — no Firebase Storage (paid plan) needed.
    suspend fun updateProfilePicture(context: Context, imageUri: Uri): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("You're not signed in.")
        try {
            val base64Image = compressImageToBase64(context, imageUri)
                ?: return AuthResult.Error("Couldn't read that image. Please try another one.")

            firestore.collection("users")
                .document(user.uid)
                .set(mapOf("profilePictureBase64" to base64Image), SetOptions.merge())
                .await()

            return AuthResult.Success
        } catch (e: Exception) {
            return AuthResult.Error("Couldn't save your profile picture. Please try again.")
        }
    }

    suspend fun getProfilePictureBase64(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.getString("profilePictureBase64")
        } catch (e: Exception) {
            null
        }
    }

    private fun compressImageToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Keep this small — Firestore documents cap at 1MB, and a big image
            // is wasted on a small circular avatar anyway.
            val maxDimension = 300
            val scale = maxDimension.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
            val resizedBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else originalBitmap

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun signOut() {
        auth.signOut()
    }
}