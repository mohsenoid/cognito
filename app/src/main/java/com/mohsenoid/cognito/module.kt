package com.mohsenoid.cognito

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.regions.Regions
import com.mohsenoid.cognito.cognito.CognitoHelper
import com.mohsenoid.cognito.cognito.internal.CognitoUserPoolsAuthProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

    single {
        CognitoUserPool(
            androidContext(),
            BuildConfig.COGNITO_USER_POOL_ID,
            BuildConfig.COGNITO_CLIENT_ID,
            BuildConfig.COGNITO_CLIENT_SECRET,
            Regions.fromName(BuildConfig.AWS_REGION)
        )
    }

    single {
        CognitoUserPoolsAuthProvider(userPool = get())
    }

    single {
        CognitoHelper(
            userPool = get(),
            authProvider = get()
        )
    }
}