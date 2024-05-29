package com.example.talkmoreapp.model

data class User (val username: String, val password: String)

data class UserResponse(val username: String, val password: String, val domain:String)

data class ApiError(val message:String)