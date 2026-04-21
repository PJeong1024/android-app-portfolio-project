package com.jdw.skillstestapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jdw.skillstestapp.screens.viewmodel.FireBaseAuthViewModel


@Composable
fun FirebaseAuthScreen(
    navController: NavController,
    viewModel: FireBaseAuthViewModel,
    paddingValues: PaddingValues
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        LoginScreen(viewModel = viewModel)
    }
}

@Composable
fun LoginScreen(viewModel: FireBaseAuthViewModel) {
    var bottomBarVisible by remember { mutableStateOf(false) }
    var bottomBarState by remember { mutableStateOf(FASBottomBarState.EmptyState) }
    val userEmail by remember { mutableStateOf(viewModel.getCurrentUser()?.email ?: "") }

    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty()) {
            bottomBarState = FASBottomBarState.SignInBottomBarState
        }
    }

    Scaffold(
        bottomBar = {
            when (bottomBarState) {
                FASBottomBarState.EmptyState -> {
                    bottomBarVisible = false
                }

                FASBottomBarState.SignUpBottomBarState -> {
                    bottomBarVisible = true
                    SignUpScreen(viewModel = viewModel,
                        changeBottomBarState = { state ->
                            bottomBarState = state
                        }
                    )
                }

                FASBottomBarState.SignInBottomBarState -> {
                    bottomBarVisible = true
                    SucceedSignScreen(viewModel = viewModel,
                        changeBottomBarState = { state ->
                            bottomBarState = state
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            LoginPage(viewModel,
                changeBottomBarState = { state ->
                    bottomBarState = state
                }
            )
        }
    }
}

@Composable
fun LoginPage(viewModel: FireBaseAuthViewModel, changeBottomBarState: (FASBottomBarState) -> Unit) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Focus Requesters to handle tab navigation
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    // FocusManager to move focus out of text fields when login/sign in is pressed
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Email TextField
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    passwordFocusRequester.requestFocus()
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password TextField
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus() // Hide the keyboard when done
                    // Add login action here
                }
            ),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        Button(
            onClick = {
                focusManager.clearFocus() // Hide the keyboard
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }

                viewModel.firebaseSignIn(email, password) { signInSuccess ->
                    email = ""
                    password = ""
                    if (signInSuccess) {
                        Toast.makeText(context, "Success to Sign In", Toast.LENGTH_SHORT).show()
                        changeBottomBarState(FASBottomBarState.SignInBottomBarState)
                    } else {
                        Toast.makeText(context, "Failed to Sign In", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Button
        OutlinedButton(
            onClick = {
                focusManager.clearFocus() // Hide the keyboard
                changeBottomBarState(FASBottomBarState.SignUpBottomBarState)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Sign Up")
        }
    }
}


@Composable
fun SignUpScreen(
    viewModel: FireBaseAuthViewModel,
    changeBottomBarState: (FASBottomBarState) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    // Focus requesters to manage focus between fields
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }

    // Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val context = LocalContext.current
        // Email TextField
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester),
            keyboardActions = KeyboardActions(
                onNext = {
                    passwordFocusRequester.requestFocus()
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password TextField
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester),
            keyboardActions = KeyboardActions(
                onNext = {
                    nameFocusRequester.requestFocus()
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name TextField
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester),
            keyboardActions = KeyboardActions(
                onDone = {
                    // Handle login here
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Add the login button here, and connect it to Firebase authentication
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.firebaseSignUp(email, password) { signUpSuccess ->
                    if (signUpSuccess) {
                        viewModel.createUser(name)
                        Toast.makeText(
                            context,
                            "Success to Sign Up. Please Sign In again.",
                            Toast.LENGTH_SHORT
                        ).show()
                        changeBottomBarState(FASBottomBarState.EmptyState)
                    } else {
                        Toast.makeText(context, "Failed to Sign Up", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                changeBottomBarState(FASBottomBarState.EmptyState)
            }) {
            Text("Cancel")
        }
    }
}

@Composable
fun SucceedSignScreen(
    viewModel: FireBaseAuthViewModel,
    changeBottomBarState: (FASBottomBarState) -> Unit
) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val user = viewModel.getCurrentUser()
            Text(text = "Sign In Success : ${user?.email ?: "Unknown"}")
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.firebaseSignOut() {
                        Toast.makeText(
                            context,
                            "Success to Sign Out",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    changeBottomBarState(FASBottomBarState.EmptyState)
                }) {
                Text("Sign Out")
            }
        }

    }
}

enum class FASBottomBarState {
    EmptyState,
    SignInBottomBarState,
    SignUpBottomBarState,
}


