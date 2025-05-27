package com.example.kursovaya

import ImageHandler
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {
    lateinit var launcher: ActivityResultLauncher<Intent>
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        auth = Firebase.auth

        val signInWithGoogleButton: Button = findViewById(R.id.buttonSingInWithGoogle)
        val emailEditText: EditText = findViewById(R.id.editTextEmail)
        val passwordEditText: EditText = findViewById(R.id.editTextPassword)
        val signUpButton: ImageButton = findViewById(R.id.buttonSingUp)
        val loginButton: Button = findViewById(R.id.buttonLogIn)

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {

                val account = task.getResult(ApiException::class.java)
                if(account != null){
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException){
                Log.d("My log", "ApiException")
            }
        }
        checkAuthState()
        signInWithGoogleButton.setOnClickListener {
            signInWithGoogle()
        }
        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty()) {
                showErrorDialog("Enter Email")
            } else if (password.isEmpty()) {
                showErrorDialog("Enter Password")
            } else {
                signUpWithEmail(email, password)
            }

        }

        loginButton.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            this.finish()
        }

    }

    fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun getClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val signInClient = GoogleSignIn.getClient(this, gso)
        signInClient.signOut()
        return signInClient
    }

    private fun signInWithGoogle(){
        val signInClient = getClient()
        launcher.launch(signInClient.signInIntent)
    }


    private fun firebaseAuthWithGoogle(idToken: String){
        val credential = GoogleAuthProvider.getCredential(idToken,null)
        auth.signInWithCredential(credential).addOnCompleteListener{
            if (it.isSuccessful){
                Log.d("My log", "google sign is done")
                checkAuthState()
            }else{
                Log.d("My log", "google sign is error")
            }
        }

    }

    private fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) {
            task ->
            if (task.isSuccessful) {
                Log.d("My log", "sign is done")
                checkAuthState()
            } else {
                Log.w("My log", "Registration failed", task.exception)
                showErrorDialog("Registration failed: ${task.exception?.message}")
            }
        }
    }

    private fun checkAuthState() {
        if (auth.currentUser != null) {
            // Проверяем, существует ли пользователь в Firestore
            val userId = auth.currentUser?.uid ?: return
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        showErrorDialog("User already registered")
                        auth.signOut()
                    } else {
                        createUserRecord()
                        startActivity(Intent(this, SignUpNameActivity::class.java))
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("My log", "Error checking user existence", e)
                }
        }
    }

    private fun createUserRecord() {
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val imageBase64 = ImageHandler().encodeImageToBase64FromResourse(this,R.drawable.user_unknown)
        val userData = hashMapOf(
            "name" to "",
            "tags" to listOf<String>(),
            "ImageProfile" to imageBase64,
            "description" to "",
            "admin" to false
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Log.d("My log", "User record created successfully")
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error creating user record", e)
            }
    }
}