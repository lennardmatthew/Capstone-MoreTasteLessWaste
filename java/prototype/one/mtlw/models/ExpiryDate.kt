package prototype.one.mtlw.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ExpiryDate(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val itemName: String = "",
    val expiryDate: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val notes: String = ""
) {
    // No-arg constructor for Firestore/serialization
    constructor() : this("", "", "", Timestamp.now(), Timestamp.now(), true, "")
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "itemName" to itemName,
        "expiryDate" to expiryDate,
        "createdAt" to createdAt,
        "isActive" to isActive,
        "notes" to notes
    )
} 