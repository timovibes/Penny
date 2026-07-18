package com.example.penny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.penny.data.AuthResult

@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    resetState: AuthResult?,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    val isLoading = resetState is AuthResult.Loading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset your password") },
        text = {
            Column {
                Text(
                    text = "Enter the email address linked to your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email address") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                when (resetState) {
                    is AuthResult.Error -> Text(
                        text = resetState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    is AuthResult.Success -> Text(
                        text = "Reset email sent — check your inbox.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    else -> Unit
                }
            }
        },
        confirmButton = {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                TextButton(onClick = { onSubmit(email) }) {
                    Text("Send reset link")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (resetState is AuthResult.Success) "Done" else "Cancel")
            }
        }
    )
}