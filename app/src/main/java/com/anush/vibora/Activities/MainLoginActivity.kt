package com.anush.vibora.Activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.anush.vibora.Dialogs.CountryPickerDialog
import com.anush.vibora.Helpers.SharedPrefs
import com.anush.vibora.Models.User
import com.anush.vibora.R
import com.anush.vibora.Utils.Constants
import com.anush.vibora.databinding.ActivityMainLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.wang.avi.AVLoadingIndicatorView

class MainLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainLoginBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private var isPasswordVisible = false
    private var countryName: String? = Constants.FIELD_COUNTRY_NAME
    private var countryCode: String? = null

    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainLoginBinding.inflate(layoutInflater)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initGoogleLogin()
        initListeners()
        toggleLayoutsAndPasswords()
    }

    private fun toggleLayoutsAndPasswords() {
        binding.signUptogglePass.setOnClickListener { togglePasswordVisibility(binding.signUpPassEdt, binding.signUptogglePass) }
        binding.togglePass.setOnClickListener { togglePasswordVisibility(binding.loginPassEdt, binding.togglePass) }
    }

    private fun togglePasswordVisibility(editText: View, toggleButton: View) {
        isPasswordVisible = !isPasswordVisible

        if (editText is androidx.appcompat.widget.AppCompatEditText && toggleButton is androidx.appcompat.widget.AppCompatImageView) {
            editText.inputType = if (isPasswordVisible) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            toggleButton.setImageResource(if (isPasswordVisible) R.drawable.eye else R.drawable.eye_closed)
            editText.setSelection(editText.text?.length ?: 0)
        }
    }

    private fun initListeners() {
        binding.chooseCountryCode.setOnClickListener {
            val dialog = CountryPickerDialog(this, countryName, object : CountryPickerDialog.OnCountrySelectedListener {
                override fun onCountrySelected(code: String, name: String) {
                    countryName = name
                    countryCode = code
                    binding.chooseCountryCode.text = code
                }
            })
            dialog.show()
        }

        binding.loginBtn.setOnClickListener { loginUser() }
        binding.signUpBtn.setOnClickListener { registerUser() }
        binding.googleLogin.setOnClickListener { signInWithGoogle() }
        binding.switchLays.setOnClickListener { switchLayouts() }
    }

    private fun initGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignIn(task)
        }
    }

    private fun signInWithGoogle() {
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun handleGoogleSignIn(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            task.result?.let { account -> firebaseAuthWithGoogle(account) }
        } else {
            showToast("Google sign-in failed")
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        showLoading("Signing in...")

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                hideLoading()
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    checkIfUserExists(user.uid, account)
                } else {
                    showToast("Authentication failed: ${task.exception?.localizedMessage}")
                }
            }
    }

    private fun checkIfUserExists(userId: String, account: GoogleSignInAccount) {
        db.collection(Constants.USERS_COLLECTION).document(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    goto(MainActivity::class.java)
                } else {
                    createUserProfile(userId, account)
                }
            }
            .addOnFailureListener {
                showToast("Failed to check user existence")
            }
    }

    private fun createUserProfile(userId: String, account: GoogleSignInAccount) {
        showLoading("Creating profile...")

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val user = hashMapOf(
                "userId" to userId,
                "name" to (account.displayName ?: ""),
                "email" to (account.email ?: ""),
                "password" to Constants.DEFAULT_PASSWORD,
                "fcmToken" to token,
                "number" to Constants.DEFAULT_PHONE_NUMBER,
                "countryCode" to Constants.DEFAULT_COUNTRY_CODE,
                Constants.TAG_PROFILE_IMAGE to (account.photoUrl?.toString() ?: "")
            )

            db.collection(Constants.USERS_COLLECTION).document(userId).set(user)
                .addOnSuccessListener {
                    hideLoading()
                    goto(MainActivity::class.java)
                }
                .addOnFailureListener { e ->
                    hideLoading()
                    showToast("Failed to create profile: ${e.message}")
                }
        }.addOnFailureListener {
            hideLoading()
            showToast("Failed to get FCM token")
        }
    }

    private fun loginUser() {
        val email = binding.loginEmailEdt.text.toString().trim()
        val password = binding.loginPassEdt.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showToast("All fields are required!")
            return
        }

        showLoading("Logging in...")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                hideLoading()
                if (task.isSuccessful) {
                    SharedPrefs.putBoolean(Constants.PREF_IS_LOGGED_IN, true)
                    goto(MainActivity::class.java)
                } else {
                    handleLoginErrors(task.exception)
                }
            }
    }

    private fun registerUser() {
        val name = binding.nameSignUpEdt.text.toString().trim()
        val email = binding.signUpEmailEdt.text.toString().trim()
        val password = binding.signUpPassEdt.text.toString().trim()
        val number = binding.signUpNumEdt.text.toString().trim()
        val countryCodeText = binding.chooseCountryCode.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || number.isEmpty()) {
            showToast("All fields are required!")
            return
        }

        showLoading("Creating account...")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val userId = auth.currentUser!!.uid
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    val newUser = User(userId, name, email, password, token, number, countryCodeText)
                    db.collection(Constants.USERS_COLLECTION).document(userId).set(newUser)
                        .addOnSuccessListener {
                            // Send verification email
                            auth.currentUser?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    hideLoading()
                                    showToast("Verification email sent!")
                                    // Redirect user to UploadImageActivity
                                    goto(ImageUploadActivity::class.java)
                                }
                                ?.addOnFailureListener { e ->
                                    hideLoading()
                                    showToast("Failed to send verification email: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            hideLoading()
                            showToast("Failed to save user: ${e.message}")
                        }
                }.addOnFailureListener {
                    hideLoading()
                    showToast("Failed to get FCM token")
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                showToast("Signup failed: ${e.localizedMessage}")
            }
    }


    private fun switchLayouts() {
        val loginVisible = binding.loginLay.isVisible
        if (loginVisible) {
            binding.loginLay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left))
            binding.signupLay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right))
        } else {
            binding.signupLay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right))
            binding.loginLay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left))
        }
        binding.loginLay.visibility = if (loginVisible) View.GONE else View.VISIBLE
        binding.signupLay.visibility = if (loginVisible) View.VISIBLE else View.GONE
        setSwitchText(!loginVisible)
    }

    private fun setSwitchText(isLoginVisible: Boolean) {
        val base = if (isLoginVisible) getString(R.string.don_t_have_an_account) else getString(R.string.already_have_account)
        val highlight = if (isLoginVisible) getString(R.string.sign_up) else getString(R.string.login)

        val spannable = SpannableString(base + highlight).apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(this@MainLoginActivity, R.color.purple)), base.length, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), base.length, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.switchLays.text = spannable
    }

    private fun showLoading(message: String) {
        if (loadingDialog == null) {
            loadingDialog = AlertDialog.Builder(this)
                .setView(R.layout.dialog_loading)
                .setCancelable(false)
                .create()
        }

        // Access the TextView and set the message
        loadingDialog?.findViewById<TextView>(R.id.loadingMessage)?.text = message

        // Access the AVLoadingIndicatorView in the dialog and show it
        val loading = loadingDialog?.findViewById<AVLoadingIndicatorView>(R.id.loading)
        loading?.show()

        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleLoginErrors(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> showToast("No account found. Contact Admin.")
            is FirebaseAuthInvalidCredentialsException -> showToast("Invalid credentials. Try again.")
            else -> showToast("Login failed: ${exception?.localizedMessage}")
        }
    }

    private fun goto(target: Class<*>) {
        startActivity(Intent(this, target))
        finish()
    }
}
