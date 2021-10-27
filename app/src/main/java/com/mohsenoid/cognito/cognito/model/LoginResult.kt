package com.mohsenoid.cognito.cognito.model

sealed interface LoginResult {

    object Successful : LoginResult

    sealed interface Error : LoginResult {
        object NoConnection : Error
        object InvalidUsername : Error
        object InvalidPassword : Error
        data class UnknownError(val exception: Exception) : Error
    }
}