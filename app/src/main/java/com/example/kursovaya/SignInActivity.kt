package com.example.kursovaya

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

class SignInActivity : AppCompatActivity() {
    lateinit var launcher: ActivityResultLauncher<Intent>
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        auth = Firebase.auth

        val loginButton: Button = findViewById(R.id.buttonLogIn)
        val signInButton: ImageButton = findViewById(R.id.buttonSingIn)
        val emailEditText: EditText = findViewById(R.id.editTextEmail)
        val passwordEditText: EditText = findViewById(R.id.editTextPassword)
        val signInWithGoogleButton: Button = findViewById(R.id.buttonSingInWithGoogle)

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


        loginButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            this.finish()
        }

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty()) {
                showErrorDialog("Enter Email")
            } else if (password.isEmpty()) {
                showErrorDialog("Enter Password")
            } else {
                signInWithEmail(email, password)
            }
        }


    }

    fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }


    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("My log", "sign in is successful")
                checkAuthState()
            } else {
                Log.w("My log", "sign in failed", task.exception)
                showErrorDialog("Authentication failed: ${task.exception?.message}")
            }
        }
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(currentUser.uid)

            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.getBoolean("admin") == true) {
                    val intent = Intent(this, AdminPanelActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                finish()
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error getting user document", e)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
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
    private fun signInWithGoogle(){
        val signInClient = getClient()
        launcher.launch(signInClient.signInIntent)
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
}