package com.sr.fixit106.ui.auth

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.sr.fixit106.R
import com.sr.fixit106.data.users.UserRole
import com.sr.fixit106.utils.ImageUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AuthActivity : AppCompatActivity() {

    private enum class AuthMode { SIGN_IN, SIGN_UP }

    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        signInLauncher =
            registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
                lifecycleScope.launch { onSignInResult(result) }
            }

        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d("AUTH", "onCreate currentUser=${currentUser?.uid}, email=${currentUser?.email}")

        if (currentUser != null) {
            toApp()
            return
        }

        val signInBtn = findViewById<Button>(R.id.auth_sign_in_btn)
        val signUpBtn = findViewById<Button>(R.id.auth_sign_up_btn)

        signInBtn.setOnClickListener { launchFirebaseUi(AuthMode.SIGN_IN) }
        signUpBtn.setOnClickListener { launchFirebaseUi(AuthMode.SIGN_UP) }
    }

    private fun launchFirebaseUi(mode: AuthMode) {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder()
                .setAllowNewAccounts(mode == AuthMode.SIGN_UP)
                .setRequireName(mode == AuthMode.SIGN_UP)
                .build()
        )

        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setAlwaysShowSignInMethodScreen(true)
            .setIsSmartLockEnabled(false)
            .setLogo(R.drawable.fix_it_106)
            .setTheme(R.style.Theme_fixit106)
            .build()

        Log.d("AUTH", "Launching FirebaseUI, mode=$mode")
        signInLauncher.launch(intent)
    }

    private suspend fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        val error = response?.error

        Log.d(
            "AUTH",
            "onSignInResult resultCode=${result.resultCode}, provider=${response?.providerType}, email=${response?.email}, isNewUser=${response?.isNewUser}"
        )

        if (result.resultCode != RESULT_OK) {
            Log.e(
                "AUTH",
                "Sign-in failed. provider=${response?.providerType}, email=${response?.email}, errorCode=${error?.errorCode}, message=${error?.localizedMessage}",
                error
            )

            Toast.makeText(
                this,
                error?.localizedMessage ?: "Sign-in failed. Please check Logcat.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Log.e("AUTH", "RESULT_OK was returned, but Firebase currentUser is null")
            Toast.makeText(
                this,
                "Authentication did not complete correctly. Please try again.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Log.d(
            "AUTH",
            "Firebase currentUser uid=${firebaseUser.uid}, email=${firebaseUser.email}, displayName=${firebaseUser.displayName}"
        )

        val existingUser = try {
            authViewModel.getUserByUid(firebaseUser.uid)
        } catch (e: Exception) {
            Log.e("AUTH", "Failed reading user by uid from repository", e)
            Toast.makeText(
                this,
                "Failed to load user data. Please try again.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (existingUser != null) {
            Log.d("AUTH", "Existing user found by uid=${firebaseUser.uid}, navigating to app")
            toApp()
            return
        }

        Log.d("AUTH", "No existing user found by uid=${firebaseUser.uid}, continuing registration flow")

        val userImage = try {
            getUserImage()
        } catch (e: Exception) {
            Log.e("AUTH", "Failed to get user image", e)
            Toast.makeText(
                this,
                "Failed to prepare profile image.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val signUpData = collectSignUpData() ?: run {
            Log.d("AUTH", "Sign-up dialog cancelled, signing out current user")
            FirebaseAuth.getInstance().signOut()
            return
        }

        try {
            authViewModel.register(
                onFinishUi = ::toApp,
                userImage = userImage,
                role = signUpData.role,
                city = "Tel Aviv"
            )
        } catch (e: Exception) {
            Log.e("AUTH", "Registration failed", e)
            Toast.makeText(
                this,
                "Registration failed. Please try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private suspend fun getUserImage(): String {
        val user = FirebaseAuth.getInstance().currentUser
        val photoUrl = user?.photoUrl

        return if (photoUrl != null) {
            ImageUtils.convertPhotoUrlToBase64(photoUrl.toString())
        } else {
            ImageUtils.convertDrawableToBase64(
                this,
                R.drawable.empty_profile_picture
            )
        }
    }

    private suspend fun collectSignUpData(): SignUpData? =
        suspendCancellableCoroutine { continuation ->

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val pad = (20 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            }

            var selectedRole = UserRole.RESIDENT

            data class RoleCardRefs(
                val card: MaterialCardView,
                val titleView: MaterialTextView,
                val descView: MaterialTextView,
                val role: String
            )

            fun createCard(
                title: String,
                description: String,
                role: String
            ): RoleCardRefs {
                val outerMargin = (8 * resources.displayMetrics.density).toInt()
                val innerPadding = (20 * resources.displayMetrics.density).toInt()

                val card = MaterialCardView(this).apply {
                    radius = 24f
                    strokeWidth = 2
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = outerMargin
                    }
                }

                val layout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(innerPadding, innerPadding, innerPadding, innerPadding)
                }

                val titleView = MaterialTextView(this).apply {
                    text = title
                    textSize = 18f
                }

                val descView = MaterialTextView(this).apply {
                    text = description
                    textSize = 14f
                }

                layout.addView(titleView)
                layout.addView(descView)
                card.addView(layout)

                return RoleCardRefs(card, titleView, descView, role)
            }

            val residentRefs = createCard(
                "Resident",
                "Report issues in the city and track their resolution",
                UserRole.RESIDENT
            )

            val repRefs = createCard(
                "106 Representative",
                "Manage reports and communicate with residents",
                UserRole.REPRESENTATIVE
            )

            val allCards = listOf(residentRefs, repRefs)

            fun renderSelection(selected: String) {
                val selectedStroke = ContextCompat.getColor(this, R.color.brand_primary)
                val selectedBg = ContextCompat.getColor(this, R.color.brand_secondary)

                val normalStroke = ContextCompat.getColor(this, R.color.dark_gray)
                val normalBg = ContextCompat.getColor(this, R.color.white)

                val selectedText = ContextCompat.getColor(this, R.color.brand_primary)
                val normalText = ContextCompat.getColor(this, R.color.black)
                val normalDesc = ContextCompat.getColor(this, R.color.gray)

                allCards.forEach { refs ->
                    val isSelected = refs.role == selected

                    refs.card.strokeColor = if (isSelected) selectedStroke else normalStroke
                    refs.card.setCardBackgroundColor(if (isSelected) selectedBg else normalBg)

                    refs.titleView.setTextColor(if (isSelected) selectedText else normalText)
                    refs.titleView.setTypeface(
                        refs.titleView.typeface,
                        if (isSelected) Typeface.BOLD else Typeface.NORMAL
                    )

                    refs.descView.setTextColor(if (isSelected) selectedText else normalDesc)
                }
            }

            residentRefs.card.setOnClickListener {
                selectedRole = UserRole.RESIDENT
                renderSelection(selectedRole)
            }

            repRefs.card.setOnClickListener {
                selectedRole = UserRole.REPRESENTATIVE
                renderSelection(selectedRole)
            }

            container.addView(residentRefs.card)
            container.addView(repRefs.card)

            renderSelection(selectedRole)

            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Select user type")
                .setView(container)
                .setNegativeButton("Cancel") { _, _ ->
                    if (continuation.isActive) continuation.resume(null)
                }
                .setPositiveButton("Continue") { _, _ ->
                    if (continuation.isActive) {
                        continuation.resume(SignUpData(selectedRole))
                    }
                }
                .create()

            dialog.show()
        }

    private fun toApp() {
        Log.d("AUTH", "Navigating to MainActivity")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private data class SignUpData(
        val role: String
    )
}