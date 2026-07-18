package com.example.penny.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.penny.viewmodel.ChangePasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordSheet(
    onDismiss: () -> Unit,
    viewModel: ChangePasswordViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetFields()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Change Password",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )

            OutlinedTextField(
                value = viewModel.currentPassword,
                onValueChange = { viewModel.currentPassword = it },
                label = { Text("Current password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.newPassword,
                onValueChange = { viewModel.newPassword = it },
                label = { Text("New password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.confirmPassword,
                onValueChange = { viewModel.confirmPassword = it },
                label = { Text("Confirm new password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (viewModel.error != null) {
                Text(
                    viewModel.error!!,
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    viewModel.changePassword(onSuccess = {
                        viewModel.resetFields()
                        onDismiss()
                    })
                },
                enabled = !viewModel.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.onPrimary
                    )
                } else {
                    Text("Update Password", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}