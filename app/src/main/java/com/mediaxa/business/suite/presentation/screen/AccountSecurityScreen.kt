package com.mediaxa.business.suite.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.entity.User
import com.mediaxa.business.suite.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecurityScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Keamanan Akun", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isAdmin = currentUser?.role == "ADMIN"
            
            if (isAdmin) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Akun Saya") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Kelola Kasir") }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (selectedTab == 0 || !isAdmin) {
                    OwnProfileSecuritySection(authViewModel = authViewModel)
                } else {
                    AdminManageStaffSection(authViewModel = authViewModel, currentUser = currentUser, allUsers = allUsers)
                }
            }
        }
    }
}

@Composable
fun OwnProfileSecuritySection(authViewModel: AuthViewModel) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Card Username
        var usernameOldPass by remember { mutableStateOf("") }
        var newUsername by remember { mutableStateOf(currentUser?.username ?: "") }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ubah Username", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Username Baru") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = usernameOldPass,
                    onValueChange = { usernameOldPass = it },
                    label = { Text("Password Lama") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (newUsername.isBlank()) {
                            Toast.makeText(context, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (usernameOldPass.isBlank()) {
                            Toast.makeText(context, "Password lama harus diisi", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        authViewModel.changeOwnUsername(usernameOldPass, newUsername) { msg, success ->
                            Toast.makeText(context, msg ?: (if (success) "Username berhasil diubah" else "Gagal mengubah username"), Toast.LENGTH_LONG).show()
                            if (success) {
                                usernameOldPass = ""
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ubah Username")
                }
            }
        }

        // Card Password
        var passwordOldPass by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmNewPassword by remember { mutableStateOf("") }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ubah Password Login", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = passwordOldPass,
                    onValueChange = { passwordOldPass = it },
                    label = { Text("Password Lama") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Password Baru (Min. 6 karakter)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text("Konfirmasi Password Baru") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (passwordOldPass.isBlank()) {
                            Toast.makeText(context, "Password lama harus diisi", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            Toast.makeText(context, "Password baru minimal 6 karakter", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword != confirmNewPassword) {
                            Toast.makeText(context, "Konfirmasi password baru tidak cocok", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        authViewModel.changeOwnPassword(passwordOldPass, newPassword) { msg, success ->
                            Toast.makeText(context, msg ?: (if (success) "Password berhasil diubah" else "Gagal mengubah password"), Toast.LENGTH_LONG).show()
                            if (success) {
                                passwordOldPass = ""
                                newPassword = ""
                                confirmNewPassword = ""
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ubah Password")
                }
            }
        }

        // Card PIN
        var pinOldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmNewPin by remember { mutableStateOf("") }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ubah PIN Login/Admin", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                val hasOldPin = !currentUser?.pin.isNullOrEmpty()
                if (hasOldPin) {
                    OutlinedTextField(
                        value = pinOldPin,
                        onValueChange = { pinOldPin = it },
                        label = { Text("PIN Lama") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("PIN Baru (4-6 digit angka)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { confirmNewPin = it },
                    label = { Text("Konfirmasi PIN Baru") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (hasOldPin && pinOldPin.isBlank()) {
                            Toast.makeText(context, "PIN lama harus diisi", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!newPin.matches(Regex("^\\d{4,6}$"))) {
                            Toast.makeText(context, "PIN baru harus berupa 4-6 digit angka", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPin != confirmNewPin) {
                            Toast.makeText(context, "Konfirmasi PIN baru tidak cocok", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        authViewModel.changeOwnPin(pinOldPin, newPin) { msg, success ->
                            Toast.makeText(context, msg ?: (if (success) "PIN berhasil diubah" else "Gagal mengubah PIN"), Toast.LENGTH_LONG).show()
                            if (success) {
                                pinOldPin = ""
                                newPin = ""
                                confirmNewPin = ""
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ubah PIN")
                }
            }
        }
    }
}

@Composable
fun AdminManageStaffSection(
    authViewModel: AuthViewModel,
    currentUser: User?,
    allUsers: List<User>
) {
    val context = LocalContext.current
    val staffUsers = allUsers.filter { it.uuid != currentUser?.uuid }

    if (staffUsers.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Tidak ada user kasir/staf lain.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        staffUsers.forEach { user ->
            var isResetPasswordDialogOpen by remember { mutableStateOf(false) }
            var isResetPinDialogOpen by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(user.username, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Peran: ${user.role}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (user.isActive) "Aktif" else "Nonaktif",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (user.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = user.isActive,
                                onCheckedChange = { checked ->
                                    authViewModel.adminToggleUserActive(user.uuid, checked) { msg, success ->
                                        Toast.makeText(context, msg ?: "Operasi berhasil", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isResetPasswordDialogOpen = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Reset Password", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isResetPinDialogOpen = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Reset PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Dialog Reset Password
            if (isResetPasswordDialogOpen) {
                var newPasswordByAdmin by remember { mutableStateOf("") }
                var confirmNewPasswordByAdmin by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { isResetPasswordDialogOpen = false },
                    title = { Text("Reset Password User: ${user.username}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newPasswordByAdmin,
                                onValueChange = { newPasswordByAdmin = it },
                                label = { Text("Password Baru (Min. 6 karakter)") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmNewPasswordByAdmin,
                                onValueChange = { confirmNewPasswordByAdmin = it },
                                label = { Text("Konfirmasi Password Baru") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newPasswordByAdmin.length < 6) {
                                    Toast.makeText(context, "Password baru minimal 6 karakter", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (newPasswordByAdmin != confirmNewPasswordByAdmin) {
                                    Toast.makeText(context, "Konfirmasi password baru tidak cocok", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                authViewModel.adminResetPassword(user.uuid, newPasswordByAdmin) { msg, success ->
                                    Toast.makeText(context, msg ?: "Password berhasil direset", Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        isResetPasswordDialogOpen = false
                                    }
                                }
                            }
                        ) {
                            Text("Reset")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isResetPasswordDialogOpen = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Dialog Reset PIN
            if (isResetPinDialogOpen) {
                var newPinByAdmin by remember { mutableStateOf("") }
                var confirmNewPinByAdmin by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { isResetPinDialogOpen = false },
                    title = { Text("Reset PIN User: ${user.username}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newPinByAdmin,
                                onValueChange = { newPinByAdmin = it },
                                label = { Text("PIN Baru (4-6 digit angka)") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmNewPinByAdmin,
                                onValueChange = { confirmNewPinByAdmin = it },
                                label = { Text("Konfirmasi PIN Baru") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (!newPinByAdmin.matches(Regex("^\\d{4,6}$"))) {
                                    Toast.makeText(context, "PIN baru harus 4-6 digit angka", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (newPinByAdmin != confirmNewPinByAdmin) {
                                    Toast.makeText(context, "Konfirmasi PIN baru tidak cocok", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                authViewModel.adminResetPin(user.uuid, newPinByAdmin) { msg, success ->
                                    Toast.makeText(context, msg ?: "PIN berhasil direset", Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        isResetPinDialogOpen = false
                                    }
                                }
                            }
                        ) {
                            Text("Reset")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isResetPinDialogOpen = false }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}
