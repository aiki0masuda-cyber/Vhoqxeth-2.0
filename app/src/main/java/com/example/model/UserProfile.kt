package com.example.model

/**
 * User profile entity stored in Firestore under /users/{userId}.
 * Extended with DAV InfoExchange Student vs Teacher role architecture.
 * All properties provide default values to support Firestore no-arg deserialization.
 */
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val customUserId: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val role: String = ROLE_STUDENT,            // "STUDENT" or "TEACHER"
    val designation: String = "",             // Teacher: e.g. "Senior PGT Lecturer"
    val department: String = "",              // Teacher: e.g. "Department of Mathematics"
    val employeeId: String = "",              // Teacher: e.g. "DAV-FAC-204"
    val grade: String = "",                   // Student: e.g. "10", "12"
    val section: String = "",                 // Student: e.g. "A", "B"
    val subject: String = "",                 // Subject specialization / interest
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val description: String = "",
    val hasCompletedOptionalSetup: Boolean = false,
    val associatedIps: List<String> = emptyList(),
    val currentIp: String = "",
    val createdAt: String = ""
) {
    val isTeacher: Boolean
        get() = role.equals(ROLE_TEACHER, ignoreCase = true)

    val isStudent: Boolean
        get() = !isTeacher

    val isAdmin: Boolean
        get() = customUserId.trim().removePrefix("@").equals("admin", ignoreCase = true)

    /**
     * Computed scholastic badge label:
     * - Teachers: "Faculty • Department" or "Designation"
     * - Students: "Class Grade-Section" or "Scholar"
     */
    val scholasticBadge: String
        get() = if (isTeacher) {
            when {
                designation.isNotBlank() -> designation.trim()
                department.isNotBlank() -> "Faculty • ${department.trim()}"
                else -> "Verified Faculty"
            }
        } else {
            val g = grade.trim()
            val s = section.trim()
            when {
                g.isNotBlank() && s.isNotBlank() -> "Class $g-$s"
                g.isNotBlank() -> "Class $g"
                s.isNotBlank() -> "Sec $s"
                else -> "Scholar"
            }
        }

    companion object {
        const val ROLE_STUDENT = "STUDENT"
        const val ROLE_TEACHER = "TEACHER"
    }
}
