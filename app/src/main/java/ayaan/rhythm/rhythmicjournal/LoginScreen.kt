package ayaan.rhythm.rhythmicjournal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import ayaan.rhythm.rhythmicjournal.ui.theme.JournalBlack
import ayaan.rhythm.rhythmicjournal.ui.theme.JournalWhite
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class GoogleUser(
    val idToken: String,
    val displayName: String?,
    val email: String?,
    val profilePictureUrl: String?
)

private enum class AuthAction {
    LOGIN,
    SIGN_UP
}

private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onCreateAccount: (GoogleUser) -> Unit
) {
    val imageResIds = remember {
        listOf(
            R.drawable.login_bg_1,
            R.drawable.login_bg_2,
            R.drawable.login_bg_3,
            R.drawable.login_bg_4,
            R.drawable.login_bg_5
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentImageIndex by remember { mutableIntStateOf(0) }
    var showTermsSheet by remember { mutableStateOf(false) }
    var activeAuthAction by remember { mutableStateOf<AuthAction?>(null) }
    var googleError by remember { mutableStateOf<String?>(null) }

    val isGoogleLoading = activeAuthAction != null

    suspend fun runGoogleAuthFlow(action: AuthAction) {
        googleError = null
        activeAuthAction = action

        val activity = context.findActivity()
        if (activity == null) {
            googleError = "Google sign-in could not start."
            activeAuthAction = null
            return
        }

        try {
            val googleUser = signInWithGoogle(activity)
            firebaseAuthWithGoogle(googleUser.idToken)

            when (action) {
                AuthAction.LOGIN -> onLogin()
                AuthAction.SIGN_UP -> onCreateAccount(googleUser)
            }
        } catch (_: GetCredentialCancellationException) {
            // User canceled the Google auth sheet.
        } catch (_: NoCredentialException) {
            googleError = "No Google account is available on this device."
        } catch (_: GoogleIdTokenParsingException) {
            googleError = "Google returned an invalid sign-in response."
        } catch (e: FirebaseAuthException) {
            googleError = e.localizedMessage ?: "Firebase sign-in failed."
        } catch (e: GetCredentialException) {
            googleError = e.message ?: "Google sign-in failed."
        } catch (e: Exception) {
            googleError = e.localizedMessage ?: "Google sign-in failed."
        } finally {
            activeAuthAction = null
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentImageIndex = (currentImageIndex + 1) % imageResIds.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JournalBlack)
    ) {
        Crossfade(
            targetState = currentImageIndex,
            animationSpec = tween(durationMillis = 900),
            modifier = Modifier.fillMaxSize(),
            label = "login_background_slideshow"
        ) { index ->
            Image(
                painter = painterResource(id = imageResIds[index]),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            JournalBlack.copy(alpha = 0.12f),
                            JournalBlack.copy(alpha = 0.08f),
                            JournalBlack.copy(alpha = 0.34f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            LoginBrand()

            Spacer(modifier = Modifier.size(24.dp))

            Text(
                text = "SLOW DOWN\nSAVE THE MOMENT",
                style = TextStyle(
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = JournalWhite,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.weight(1f))

            TermsLine(
                onTermsClick = { showTermsSheet = true }
            )

            Spacer(modifier = Modifier.size(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                color = JournalBlack,
                border = BorderStroke(0.dp, androidx.compose.ui.graphics.Color.Transparent),
                onClick = {
                    if (!isGoogleLoading) {
                        scope.launch {
                            runGoogleAuthFlow(AuthAction.LOGIN)
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeAuthAction == AuthAction.LOGIN) {
                            "CONNECTING..."
                        } else {
                            "LOG IN"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = JournalWhite
                    )
                }
            }

            Spacer(modifier = Modifier.size(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                color = JournalWhite,
                border = BorderStroke(0.dp, androidx.compose.ui.graphics.Color.Transparent),
                onClick = {
                    if (!isGoogleLoading) {
                        scope.launch {
                            runGoogleAuthFlow(AuthAction.SIGN_UP)
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeAuthAction == AuthAction.SIGN_UP) {
                            "CONNECTING..."
                        } else {
                            "SIGN UP"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = JournalBlack
                    )
                }
            }

            googleError?.let { error ->
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showTermsSheet) {
        TermsBottomSheet(
            onDismiss = { showTermsSheet = false }
        )
    }
}

@Composable
private fun LoginBrand() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        JournalMark(
            diameter = 38.dp,
            color = JournalWhite
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "RhythmicJournal",
            style = MaterialTheme.typography.headlineSmall,
            color = JournalWhite
        )
    }
}

@Composable
private fun TermsLine(
    onTermsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "By signing up, you agree to RhythmicJournal’s ",
            color = JournalWhite.copy(alpha = 0.9f),
            fontSize = 10.sp
        )

        Text(
            text = "Terms and Conditions",
            color = JournalWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onTermsClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            Text(
                text = "Terms and Conditions",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.size(18.dp))

            TermsSection(
                title = "1. Personal use",
                body = "RhythmicJournal is intended for personal journaling, memory keeping, and creative reflection. You are responsible for the content you choose to upload or write."
            )

            TermsSection(
                title = "2. Your content",
                body = "Photos, notes, profile details, and journal entries you create remain yours. You should only upload content you have the right to use."
            )

            TermsSection(
                title = "3. Respectful use",
                body = "You agree not to use the app to post harmful, abusive, unlawful, or misleading content."
            )

            TermsSection(
                title = "4. Privacy",
                body = "Your saved information may be stored locally on your device. You should avoid storing sensitive information unless you are comfortable keeping it on the device."
            )

            TermsSection(
                title = "5. App updates",
                body = "Features, appearance, and storage behavior may change as the app develops. Continued use means you accept those updates."
            )

            TermsSection(
                title = "6. Availability",
                body = "The app is provided as-is during development. Some features may be incomplete, unavailable, or changed without notice."
            )

            Spacer(modifier = Modifier.size(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.onBackground,
                border = BorderStroke(0.dp, androidx.compose.ui.graphics.Color.Transparent),
                onClick = onDismiss
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.background
                    )
                }
            }

            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TermsSection(
    title: String,
    body: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.size(6.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.size(16.dp))
    }
}

private suspend fun signInWithGoogle(activity: Activity): GoogleUser {
    val credentialManager = CredentialManager.create(activity)
    val webClientId = activity.getString(R.string.default_web_client_id)

    val googleSignInOption = GetSignInWithGoogleOption.Builder(webClientId).build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleSignInOption)
        .build()

    val result = credentialManager.getCredential(
        context = activity,
        request = request
    )

    val credential = result.credential

    if (credential is CustomCredential) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

        return GoogleUser(
            idToken = googleIdTokenCredential.idToken,
            displayName = googleIdTokenCredential.displayName,
            email = googleIdTokenCredential.id,
            profilePictureUrl = googleIdTokenCredential.profilePictureUri?.toString()
        )
    }

    throw IllegalStateException("Unexpected credential type returned from Google sign-in.")
}

private suspend fun firebaseAuthWithGoogle(idToken: String): FirebaseUser =
    suspendCancellableCoroutine { continuation ->
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener

                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        continuation.resume(user)
                    } else {
                        continuation.resumeWithException(
                            IllegalStateException("Firebase user was null after sign-in.")
                        )
                    }
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase sign-in failed.")
                    )
                }
            }
    }

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}