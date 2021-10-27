package com.mohsenoid.cognito.cognito.internal

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.exceptions.CognitoInternalErrorException
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.mohsenoid.cognito.cognito.model.FetchTokenResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class CognitoUserPoolsAuthProvider(private val userPool: CognitoUserPool) {

    var userSession: CognitoUserSession? = null

    suspend fun getLatestAuthToken(): FetchTokenResult {
        if (userPool.currentUser.userId == null) return FetchTokenResult.Error.UserPoolsIsNotSignedIn

        userSession?.run {
            if (isValidForThreshold) return FetchTokenResult.Successful(
                refreshToken = refreshToken,
                accessToken = accessToken,
                idToken = idToken,
            )
        }

        return fetchToken()
    }

    /**
     * Fetches token from the Cognito User Pools client for the current user.
     */
    private suspend fun fetchToken(): FetchTokenResult = suspendCoroutine { continuation ->
        userPool.currentUser.getSessionInBackground(object : AuthenticationHandler {
            override fun onSuccess(userSession: CognitoUserSession, newDevice: CognitoDevice?) {
                this@CognitoUserPoolsAuthProvider.userSession = userSession
                continuation.resume(
                    FetchTokenResult.Successful(
                        refreshToken = userSession.refreshToken,
                        accessToken = userSession.accessToken,
                        idToken = userSession.idToken,
                    )
                )
            }

            override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation, userId: String?) {
                continuation.resume(FetchTokenResult.Error.UserPoolsIsNotSignedIn)
            }

            override fun getMFACode(authenticationContinuation: MultiFactorAuthenticationContinuation) {
                continuation.resume(FetchTokenResult.Error.UserPoolsIsNotSignedIn)
            }

            override fun authenticationChallenge(challengeContinuation: ChallengeContinuation) {
                continuation.resume(FetchTokenResult.Error.UserPoolsIsNotSignedIn)
            }

            override fun onFailure(exception: Exception) {
                // check if no connection is not allowing cognito to retrieve session!
                if (userPool.currentUser.userId != null && exception.isNetworkError()
                ) {
                    continuation.resume(FetchTokenResult.Error.NoConnection)
                    return
                }

                continuation.resume(FetchTokenResult.Error.Unknown(exception))
            }
        })
    }

    fun Exception.isNetworkError(): Boolean =
        this is CognitoInternalErrorException && this.cause?.message?.contentEquals("Unable to execute HTTP request", true) == true

    fun signOut() {
        userSession = null
    }
}