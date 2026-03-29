package com.sr.fixit.data.users

object UserRole {
    const val RESIDENT = "RESIDENT"
    const val REPRESENTATIVE = "REPRESENTATIVE"

    val all = listOf(RESIDENT, REPRESENTATIVE)

    fun isRepresentative(role: String?): Boolean {
        return role == REPRESENTATIVE
    }
}