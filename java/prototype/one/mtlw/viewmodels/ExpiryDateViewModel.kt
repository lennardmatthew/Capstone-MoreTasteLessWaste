package prototype.one.mtlw.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import prototype.one.mtlw.models.ExpiryDate
import prototype.one.mtlw.notifications.ExpiryNotificationHelper
import prototype.one.mtlw.notifications.NotificationPermissionHelper
import java.time.LocalDate
import java.time.ZoneId

sealed class ExpiryDateUiState {
    object Loading : ExpiryDateUiState()
    data class Success(val expiryDates: List<ExpiryDate>) : ExpiryDateUiState()
    data class Error(val message: String) : ExpiryDateUiState()
}

class ExpiryDateViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val expiryDatesCollection = firestore.collection("expiry_dates")
    private var expiryDatesListener: ListenerRegistration? = null
    
    private val _uiState = MutableStateFlow<ExpiryDateUiState>(ExpiryDateUiState.Loading)
    val uiState: StateFlow<ExpiryDateUiState> = _uiState

    init {
        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            expiryDatesListener?.remove()
            if (firebaseAuth.currentUser != null) {
                listenToExpiryDatesRealtime()
            } else {
                _uiState.value = ExpiryDateUiState.Loading
            }
        }
    }

    fun listenToExpiryDatesRealtime() {
        expiryDatesListener?.remove()
        val currentUser = auth.currentUser ?: return
        expiryDatesListener = expiryDatesCollection
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("isActive", true)
            .orderBy(FieldPath.documentId())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ExpiryDateViewModel", "Error loading expiry dates: ", error)
                    _uiState.value = ExpiryDateUiState.Error("Failed to load expiry dates: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val expiryDates = snapshot.toObjects(ExpiryDate::class.java)
                    _uiState.value = ExpiryDateUiState.Success(expiryDates)
                } else {
                    _uiState.value = ExpiryDateUiState.Success(emptyList())
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        expiryDatesListener?.remove()
    }

    fun saveExpiryDate(itemName: String, expiryDate: LocalDate, notes: String = "", context: Context) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
                
                // Validate input
                if (expiryDate.isBefore(LocalDate.now())) {
                    _uiState.value = ExpiryDateUiState.Error("Expiry date cannot be in the past")
                    return@launch
                }
                
                val instant = expiryDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val timestamp = Timestamp(instant.epochSecond, instant.nano)
                
                // Use a default name if empty
                val finalItemName = if (itemName.isBlank()) {
                    "Item expiring on ${expiryDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                } else {
                    itemName
                }
                
                val expiryDateObj = ExpiryDate(
                    userId = currentUser.uid,
                    itemName = finalItemName,
                    expiryDate = timestamp,
                    notes = notes,
                    isActive = true
                )
                
                val docRef = expiryDatesCollection.add(expiryDateObj.toMap()).await()
                
                // Schedule notifications 7 and 3 days before expiry (handled by helper)
                if (NotificationPermissionHelper.hasNotificationPermission(context)) {
                    ExpiryNotificationHelper.scheduleExpiryNotification(context, docRef.id, finalItemName, expiryDate)
                }
            } catch (e: Exception) {
                Log.e("ExpiryDateViewModel", "Error saving expiry date: ${e.message}")
                _uiState.value = ExpiryDateUiState.Error(e.message ?: "Failed to save expiry date")
                throw e // Re-throw to be handled by the caller
            }
        }
    }

    fun deleteExpiryDate(expiryDateId: String, context: Context) {
        if (expiryDateId.isBlank()) {
            Log.e("ExpiryDateViewModel", "deleteExpiryDate called with blank ID")
            return
        }
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
                val expiryDateRef = expiryDatesCollection.document(expiryDateId)
                val expiryDate = expiryDateRef.get().await().toObject(ExpiryDate::class.java)
                    ?: throw IllegalStateException("Expiry date not found")
                if (expiryDate.userId != currentUser.uid) {
                    throw IllegalStateException("You can only delete your own expiry dates")
                }
                // Soft delete: set isActive to false
                expiryDateRef.update("isActive", false).await()
                // Cancel notification only if permission is granted
                if (NotificationPermissionHelper.hasNotificationPermission(context)) {
                    ExpiryNotificationHelper.cancelExpiryNotification(context, expiryDateId)
                }
            } catch (e: Exception) {
                Log.e("ExpiryDateViewModel", "Error deleting expiry date: ${e.message}")
                _uiState.value = ExpiryDateUiState.Error(e.message ?: "Failed to delete expiry date")
            }
        }
    }

}