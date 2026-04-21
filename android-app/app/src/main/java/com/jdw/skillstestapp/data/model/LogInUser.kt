package com.jdw.skillstestapp.data.model

data class LogInUser(
    val id : String?,
    val email: String,
    val name: String
) {
    fun toMap(): MutableMap<String, Any> {
        return mutableMapOf(
            "user_id" to id.toString(),
            "email" to email,
            "name" to name
        )
    }
}