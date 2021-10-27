package com.mohsenoid.cognito.cognito

import com.amazonaws.AmazonClientException
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.exceptions.CognitoInternalErrorException
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException
import com.mohsenoid.cognito.cognito.internal.CognitoUserPoolsAuthProvider
import com.mohsenoid.cognito.cognito.model.FetchTokenResult
import com.mohsenoid.cognito.cognito.model.LoginResult
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CognitoHelper internal constructor(private val userPool: CognitoUserPool, private val authProvider: CognitoUserPoolsAuthProvider) {

    private var userSession: CognitoUserSession? = null

    val currentUser: String? = userPool.currentUser.userId

    suspend fun userLogin(userId: String, password: String): LoginResult =
        userPool.getUser(userId).getSession(password)

    private suspend fun CognitoUser.getSession(password: String): LoginResult = suspendCoroutine { continuation ->
        getSessionInBackground(object : AuthenticationHandler {

            override fun onSuccess(userSession: CognitoUserSession, newDevice: CognitoDevice?) {
                this@CognitoHelper.userSession = userSession
                continuation.resume(LoginResult.Successful)
            }

            override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation, userId: String?) {
                // The API needs user sign-in credentials to continue
                val authenticationDetails = AuthenticationDetails(userId, password, null)
                // Pass the user sign-in credentials to the continuation
                authenticationContinuation.setAuthenticationDetails(authenticationDetails)
                // Allow the sign-in to continue
                authenticationContinuation.continueTask()
            }

            override fun getMFACode(multiFactorAuthenticationContinuation: MultiFactorAuthenticationContinuation) {
                error("Multi Factor Authentication")
            }

            override fun authenticationChallenge(challengeContinuation: ChallengeContinuation) {
                error("Authentication Challenge")
            }

            override fun onFailure(exception: Exception) {
                logcat(LogPriority.ERROR) { exception.asLog() }

                // check if no connection is not allowing cognito to retrieve session!
                if (userPool.currentUser.userId != null && exception is CognitoInternalErrorException) {
                    continuation.resume(LoginResult.Successful)
                    return
                }

                // Sign-in failed, check exception for the cause
                val error = if (exception is AmazonClientException) exception.mapToLoginResultError() else LoginResult.Error.UnknownError(exception)
                continuation.resume(error)
            }
        })
    }

    fun AmazonClientException.mapToLoginResultError(): LoginResult.Error =
        when (this) {
            is UserNotFoundException -> LoginResult.Error.InvalidUsername
            is NotAuthorizedException -> LoginResult.Error.InvalidPassword
            else -> {
                message?.let {
                    if (it.contains("Unable to execute HTTP request", true)) LoginResult.Error.NoConnection
                    else LoginResult.Error.UnknownError(this)
                } ?: LoginResult.Error.UnknownError(this)
            }
        }

    suspend fun getLatestAuthToken(): FetchTokenResult =
        authProvider.getLatestAuthToken()

    fun userLogout() {
        authProvider.signOut()
        userPool.currentUser.signOut()
    }
}