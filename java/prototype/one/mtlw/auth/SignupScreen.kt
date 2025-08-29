@file:Suppress("DEPRECATION")

package prototype.one.mtlw.auth

import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import prototype.one.mtlw.R

@Composable
fun SignupScreen(
    navController: NavController,
    onGoogleSignInClick: () -> Unit,
    onSignupClick: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showTermsAcceptanceDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val termsText = """
TERMS AND CONDITIONS

Effective Date: January 2026

Welcome to More Taste, Less Waste!

By using the app, you agree to comply with and be bound by the following Terms and Conditions. If you do not agree to these terms, please do not use the app.

1. Acceptance of Terms
By downloading, installing, or using More Taste, Less Waste ("App"), you agree to these Terms and Conditions, as well as any additional terms or policies that may be provided within the App. These Terms and Conditions may be updated at any time, and it is your responsibility to review them periodically.

2. Camera Usage and Permissions
- The App requires access to your device's camera to provide features such as scanning expiry dates and taking photos for food tracking.
- Camera permission will be requested upon first use of the camera feature.
- You may disable camera access at any time through your device's settings, but this may limit your ability to use certain features.
- The App may capture images via the camera, which will be used only for the intended purpose within the App. Captured media will not be stored or shared without your explicit consent unless stated otherwise in our Privacy Policy.

3. Privacy and Data Security
- Your privacy is important to us. Please review our Privacy Policy, which explains how we collect, use, and protect your data.
- We may collect personal data or camera media for functionality or analysis purposes, and we will only share this data in accordance with our Privacy Policy.

4. User-Generated Content
- You are responsible for any content (including images, text, or other data) you create, upload, or share using the App.
- You agree not to upload or share content that is unlawful, offensive, or infringes on the rights of others.
- We reserve the right to remove any content that violates these terms or is otherwise objectionable.

5. Usage Restrictions
- You agree to use the App and its camera functionality in accordance with all applicable laws and regulations.
- You may not use the App to capture images or videos of others without their consent, or to create, share, or distribute offensive or inappropriate content.

6. Intellectual Property
- All content, features, and functionality in the App (except user-generated content) are the exclusive property of More Taste, Less Waste and its licensors.
- You may not copy, modify, distribute, or create derivative works from any part of the App without our prior written consent.

7. Third-Party Services
- The App may contain links to third-party websites or services that are not under our control. We are not responsible for the content, privacy policies, or practices of any third-party websites or services.

8. Disclaimers
- We make no guarantees regarding the functionality of the camera feature on all devices, as compatibility may vary.
- The App is provided on an "as is" and "as available" basis. We do not guarantee that the camera feature or any other part of the App will be uninterrupted or error-free.

9. Limitation of Liability
- To the maximum extent permitted by law, More Taste, Less Waste is not liable for any damages arising out of the use of the App, including but not limited to data loss, privacy breaches, or any indirect, incidental, or consequential damages.

10. Termination
- We reserve the right to suspend or terminate your access to the App at any time, without notice, for conduct that we believe violates these Terms and Conditions or is otherwise harmful to other users or the App.

11. Governing Law
- These Terms and Conditions are governed by and construed in accordance with the laws of the United States, without regard to its conflict of law provisions.

12. Changes to Terms
- We reserve the right to modify or update these Terms and Conditions at any time. Any changes will be reflected in the updated Terms and Conditions, and the "Effective Date" will be revised accordingly. Continued use of the App after any changes constitutes your acceptance of the updated Terms.

13. Contact Us
If you have any questions or concerns about these Terms and Conditions or the use of the camera feature, please contact us at support@mtlwapp.com.

---

PRIVACY POLICY

Effective Date: January 2026

More Taste, Less Waste ("we", "us", or "our") values your privacy and is committed to protecting your personal information. This Privacy Policy explains how we collect, use, and share your data when you use our mobile application (the "App") and the camera feature within it. By using the App, you agree to the collection and use of information in accordance with this Privacy Policy.

1. Information We Collect
- Camera Data: When you use the camera feature within the App, we may collect images that you capture through the camera. These files are stored temporarily and are used solely for the intended purpose of the App's features, such as scanning expiry dates or tracking food items.
- Device Information: We may collect information about your device, such as the type of device, operating system version, device identifiers (e.g., UDID), and other technical data related to how the App operates on your device.
- Usage Data: We collect data on how the App is accessed and used, including the pages or features you interact with, the time spent on the App, and error reports.

2. How We Use Your Information
- To enable the functionality of the camera feature, such as taking photos for expiry tracking.
- To improve the performance and user experience of the App.
- To communicate with you, if necessary, regarding the App's features or updates.
- To analyze usage patterns and troubleshoot issues.

3. Camera Permissions
- The App requires access to your device's camera to provide certain features. Upon first use of the camera feature, we will request permission to access your camera. You may choose to deny this permission, but this may limit the functionality of the App.
- Data Retention: Any media captured by the camera will be retained only as long as necessary to fulfill the purpose for which it was collected. We do not store or share your photos or videos without your explicit consent unless required by law.

4. How We Share Your Information
- We respect your privacy and do not sell, rent, or trade your personal information. However, we may share your data in the following circumstances:
    - Service Providers: We may engage third-party companies or individuals to assist with the operation of the App, such as hosting, analytics, or customer support. These service providers may have access to your information but are obligated not to disclose or use it for any other purpose.
    - Legal Compliance: We may disclose your information if required to do so by law or in response to a legal request (e.g., subpoena, court order, etc.).

5. Data Security
- We implement reasonable security measures to protect your data from unauthorized access, alteration, or disclosure. However, please be aware that no method of transmission over the internet or method of electronic storage is 100% secure, and we cannot guarantee absolute security.

6. Your Choices and Control
- Camera Access: You can enable or disable the App's access to your camera through your device's settings at any time. Disabling camera access will limit some features of the App.
- Data Deletion: You may request the deletion of any media captured by the camera through the App, and we will make reasonable efforts to honor your request. However, certain data may be retained for legal or technical reasons.

7. Children's Privacy
- The App is not intended for use by children under the age of 13. We do not knowingly collect personal data from children under 13. If we learn that we have inadvertently collected such data, we will take steps to delete it as soon as possible.

8. International Transfers
- Your information may be transferred to and maintained on computers located outside your state, province, country, or other governmental jurisdiction where the data protection laws may differ. If you are located outside the United States, please note that we may transfer your information to the United States and process it there.

9. Changes to This Privacy Policy
- We may update this Privacy Policy from time to time to reflect changes in our practices or legal requirements. When we make changes, we will post the updated policy in the App and update the "Effective Date" at the top of this page. We encourage you to review this Privacy Policy periodically.

10. Contact Us
If you have any questions or concerns about this Privacy Policy or the way we handle your personal data, please contact us at support@mtlwapp.com.
"""

    fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isValidPassword(password: String): Boolean =
        password.length >= 8 &&
        password.any { it.isUpperCase() } &&
        password.any { it.isDigit() }

    val validatePasswords = {
        when {
            password.length < 8 -> {
                passwordError = "Password must be at least 8 characters long"
                false
            }
            password != confirmPassword -> {
                passwordError = "Passwords do not match"
                false
            }
            else -> {
                passwordError = null
                true
            }
        }
    }

    // Handle system back button
    BackHandler {
        navController.navigateUp()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Back button at the top left - moved outside the scrollable content
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .padding(top = 36.dp, start = 8.dp)
                .size(48.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF222222),
                modifier = Modifier.size(24.dp)
            )
        }

        // Logo at the top right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, end = 16.dp)
                .align(Alignment.TopEnd)
        ) {
            Image(
                painter = painterResource(id = R.drawable.repo_green),
                contentDescription = "MTLW Logo",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopEnd)
            )
        }

        // Main content - scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 100.dp)  // Add padding to account for the back button
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF222222),
                maxLines = 1
            )
            Text(
                text = "Sign up to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666),
                maxLines = 1
            )

            Text(
                text = "Name",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF222222),
                maxLines = 1
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Please enter your name", color = Color(0x66222222)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF222222),
                    focusedBorderColor = Color(0xFF1E824C)
                )
            )

            Text(
                text = "Email",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF222222),
                maxLines = 1
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Please enter your email", color = Color(0x66222222)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF222222),
                    focusedBorderColor = Color(0xFF1E824C)
                )
            )

            Text(
                text = "Password",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF222222),
                maxLines = 1
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                placeholder = { Text("Please enter your password", color = Color(0x66222222)) },
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(text = it, color = Color(0xFF222222)) } },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF222222)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF222222),
                    focusedBorderColor = Color(0xFF1E824C)
                )
            )
            Text(
                text = "At least 8 characters, one uppercase, one number. Example: MyPassword123",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF888888),
                modifier = Modifier.padding(top = 0.dp, bottom = 8.dp, start = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Confirm Password",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF222222),
                maxLines = 1
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordError = null
                },
                placeholder = { Text("Please confirm your password", color = Color(0x66222222)) },
                isError = passwordError != null,
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF222222)
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF222222),
                    focusedBorderColor = Color(0xFF1E824C)
                )
            )
            Text(
                text = "At least 8 characters, one uppercase, one number. Example: MyPassword123",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF888888),
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isValidEmail(email)) {
                        Toast.makeText(context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!isValidPassword(password)) {
                        Toast.makeText(context, "Password must be at least 8 characters, include an uppercase letter and a number.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (validatePasswords()) {
                        showTermsAcceptanceDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E824C)
                ),
                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                Text("Sign Up", color = Color.White)
            }

            if (showTermsDialog) {
                Dialog(onDismissRequest = { showTermsDialog = false }) {
                    Surface(
                        color = Color.White,
                        border = BorderStroke(2.dp, Color(0xFF101010)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .heightIn(max = 520.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                "Terms of Service & Privacy Policy",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF232020),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Surface(
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = termsText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF222222),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showTermsDialog = false }) { Text("Close") }
                            }
                        }
                    }
                }
            }

            if (showTermsAcceptanceDialog) {
                Dialog(onDismissRequest = { showTermsAcceptanceDialog = false }) {
                    Surface(
                        color = Color.White,
                        border = BorderStroke(2.dp, Color(0xFF101010)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .heightIn(max = 520.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                "Accept Terms & Privacy Policy",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF232020),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                "By clicking 'Accept', you agree to our Terms of Service and Privacy Policy. Please review them carefully before proceeding.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF222222),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            TextButton(
                                onClick = { showTermsDialog = true },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text("Read Terms & Privacy Policy", color = Color(0xFF1E824C))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showTermsAcceptanceDialog = false }) { Text("Decline") }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = {
                                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                        return@TextButton
                                    }
                                    if (!isValidEmail(email)) {
                                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                        return@TextButton
                                    }
                                    if (!isValidPassword(password)) {
                                        Toast.makeText(context, "Password must be at least 8 characters, include an uppercase letter and a number", Toast.LENGTH_SHORT).show()
                                        return@TextButton
                                    }
                                    if (!validatePasswords()) {
                                        return@TextButton
                                    }
                                    showTermsAcceptanceDialog = false
                                    onSignupClick(name, email, password)
                                }) { Text("Accept") }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ClickableText(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF444444)))
                    {append("By continuing, you agree to More Taste, Less Waste ")}
                    pushStringAnnotation(tag = "terms", annotation = "terms")
                    withStyle(SpanStyle(color = Color(0xFF1E824C), fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                        append("Terms of Service")
                    }
                    pop()
                    withStyle(SpanStyle(color = Color(0xFF444444))) { append(" and ") }
                    pushStringAnnotation(tag = "privacy", annotation = "privacy")
                    withStyle(SpanStyle(color = Color(0xFF1E824C), fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                        append("Privacy Policy")
                    }
                    pop()
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                onClick = { offset ->
                    val annotations = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF444444)))
                        {append("By continuing, you agree to More Taste, Less Waste ")}
                        pushStringAnnotation(tag = "terms", annotation = "terms")
                        withStyle(SpanStyle(color = Color(0xFF1E824C), fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                            append("Terms of Service")
                        }
                        pop()
                        withStyle(SpanStyle(color = Color(0xFF444444))) { append(" and ") }
                        pushStringAnnotation(tag = "privacy", annotation = "privacy")
                        withStyle(SpanStyle(color = Color(0xFF1E824C), fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                            append("Privacy Policy")
                        }
                        pop()
                    }.getStringAnnotations(start = offset, end = offset)
                    
                    if (annotations.any { it.tag == "terms" || it.tag == "privacy" }) {
                        showTermsDialog = true
                    }
                }
            )

            Text(
                text = "Or continue with",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF222222),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            OutlinedButton(
                onClick = onGoogleSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF222222)
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign up with Google", color = Color(0xFF222222))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    maxLines = 1
                )
                TextButton(
                    onClick = { navController.navigate("login") },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF222222),
                        maxLines = 1
                    )
                }
            }
        }
    }
}