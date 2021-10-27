package com.mohsenoid.cognito.cognito.model

import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoAccessToken
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoIdToken
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoRefreshToken

sealed interface FetchTokenResult {

    data class Successful(
        val refreshToken: CognitoRefreshToken,
        val accessToken: CognitoAccessToken,
        val idToken: CognitoIdToken
    ) : FetchTokenResult

    sealed interface Error {
        object NoConnection : FetchTokenResult
        object UserPoolsIsNotSignedIn : FetchTokenResult
        data class Unknown(val exception: Exception) : FetchTokenResult
    }
}