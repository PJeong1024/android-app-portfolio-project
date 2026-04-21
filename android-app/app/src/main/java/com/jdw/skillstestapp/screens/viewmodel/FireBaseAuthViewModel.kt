package com.jdw.skillstestapp.screens.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.jdw.skillstestapp.data.model.LogInUser
import com.jdw.skillstestapp.repository.MyAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FireBaseAuthViewModel @Inject constructor(
    private val appRepository: MyAppRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFireStore: FirebaseFirestore
) : ViewModel() {

    // firebase methods
    fun firebaseSignIn(
        email: String, password: String,
        signInSuccess: (Boolean) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                signInSuccess(task.isSuccessful)
            }
    }

    fun firebaseSignUp(
        email: String, password: String,
        signUpSuccess: (Boolean) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                signUpSuccess(task.isSuccessful)
            }
    }

    fun firebaseSignOut(signOutSuccess: () -> Unit) {
        if (firebaseAuth.currentUser != null) {
            firebaseAuth.signOut()
            signOutSuccess()
        }
    }

    fun createUser(displayName: String?) {
        val userId = firebaseAuth.currentUser?.uid
        val user = LogInUser(
            id = userId,
            email = firebaseAuth.currentUser?.email.toString(),
            name = displayName.toString(),
        ).toMap()

        firebaseFireStore.collection("users").add(user)
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}