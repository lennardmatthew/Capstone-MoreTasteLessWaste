package prototype.one.mtlw.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import prototype.one.mtlw.R

@Composable
fun PasswordChangeSuccessScreen(
    navController: NavController
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Logo at the top right with safe area padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, end = 16.dp)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Password Change",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Your password has been changed successfully",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = { 
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in now")
            }
        }
    }
} 