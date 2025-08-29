package prototype.one.mtlw.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prototype.one.mtlw.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onSave: (String, LocalDate) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState()
    val selectedDateMillis = datePickerState.selectedDateMillis
    val selectedDate = selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val isDateInPast = selectedDate != null && selectedDate.isBefore(LocalDate.now())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF43A047))
                .height(80.dp)
        ) {
            // Profile image and bee logo can be added here if needed
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Manual Date",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF1E824C),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            text = "Manually date the expiration of your ingredients to be reminded in 3 days before they expire!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF444444),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Name of food:",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1E824C),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            placeholder = { Text("Please enter name of food", color = Color(0xFFAAAAAA)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Select the expiration date:",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1E824C),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Button(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = selectedDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "Pick a date",
                color = Color.White,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (selectedDate != null) {
            Text(
                text = "Selected expiration date: " + selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                color = Color(0xFF1E824C),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (itemName.isNotBlank() && selectedDate != null && !isDateInPast) {
                    showConfirmDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E824C)),
            shape = RoundedCornerShape(10.dp),
            enabled = itemName.isNotBlank() && selectedDate != null && !isDateInPast
        ) {
            Icon(
                painter = painterResource(id = R.drawable.save_icon),
                contentDescription = "Save",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Expiration Date", color = Color.White, fontSize = 18.sp)
        }
        if (isDateInPast) {
            Text(
                text = "Expiry date cannot be in the past.",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK", color = Color(0xFF1E824C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color(0xFF222222))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF222222),
                headlineContentColor = Color(0xFF222222),
                weekdayContentColor = Color(0xFF222222),
                dayContentColor = Color(0xFF222222),
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = Color(0xFF1E824C),
                todayContentColor = Color(0xFF1E824C),
                todayDateBorderColor = Color(0xFF1E824C),
                navigationContentColor = Color(0xFF1E824C),
                yearContentColor = Color(0xFF222222),
                disabledYearContentColor = Color(0xFFBBBBBB),
                currentYearContentColor = Color(0xFF1E824C),
                selectedYearContentColor = Color.White,
                disabledSelectedYearContentColor = Color.White,
                selectedYearContainerColor = Color(0xFF1E824C),
                disabledSelectedYearContainerColor = Color(0xFFBBBBBB),
                disabledDayContentColor = Color(0xFFBBBBBB),
                disabledSelectedDayContainerColor = Color(0xFFBBBBBB),
                dayInSelectionRangeContentColor = Color(0xFF222222),
                dayInSelectionRangeContainerColor = Color(0xFF1E824C),
                dividerColor = Color(0xFF1E824C)
            ),
            content = {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF222222),
                        headlineContentColor = Color(0xFF222222),
                        weekdayContentColor = Color(0xFF222222),
                        dayContentColor = Color(0xFF222222),
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = Color(0xFF1E824C),
                        todayContentColor = Color(0xFF1E824C),
                        todayDateBorderColor = Color(0xFF1E824C),
                        navigationContentColor = Color(0xFF1E824C),
                        yearContentColor = Color(0xFF222222),
                        disabledYearContentColor = Color(0xFFBBBBBB),
                        currentYearContentColor = Color(0xFF1E824C),
                        selectedYearContentColor = Color.White,
                        disabledSelectedYearContentColor = Color.White,
                        selectedYearContainerColor = Color(0xFF1E824C),
                        disabledSelectedYearContainerColor = Color(0xFFBBBBBB),
                        disabledDayContentColor = Color(0xFFBBBBBB),
                        disabledSelectedDayContainerColor = Color(0xFFBBBBBB),
                        dayInSelectionRangeContentColor = Color(0xFF222222),
                        dayInSelectionRangeContainerColor = Color(0xFF1E824C),
                        dividerColor = Color(0xFF1E824C)
                    )
                )
            }
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Save", color = Color.Black) },
            text = {
                Text(
                    "Are you sure you want to save this expiration date?",
                    color = Color.Black
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    if (itemName.isNotBlank() && selectedDate != null) {
                        onSave(itemName, selectedDate)
                    }
                }) {
                    Text("Yes", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("No", color = Color.Black)
                }
            },
            containerColor = Color.White
        )
    }
} 