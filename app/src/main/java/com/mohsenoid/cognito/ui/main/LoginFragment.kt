package com.mohsenoid.cognito.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mohsenoid.cognito.cognito.CognitoHelper
import com.mohsenoid.cognito.cognito.model.FetchTokenResult
import com.mohsenoid.cognito.cognito.model.LoginResult
import com.mohsenoid.cognito.databinding.LoginFragmentBinding
import org.koin.android.ext.android.inject

class LoginFragment : Fragment() {

    private val cognitoHelper: CognitoHelper by inject()

    private var _binding: LoginFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LoginFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restoreSession()

        binding.usernameLoginFragment.setText(cognitoHelper.currentUser)

        binding.loginLoginFragment.setOnClickListener { loginSession() }
    }

    private fun restoreSession() {
        lifecycleScope.launchWhenResumed {
            loadingUI(true)
            val fetchTokenResult = cognitoHelper.getLatestAuthToken()
            loadingUI(false)

            if (fetchTokenResult is FetchTokenResult.Successful || fetchTokenResult is FetchTokenResult.Error.NoConnection) {
                proceed()
            }
        }
    }

    private fun loginSession() {
        val username = binding.usernameLoginFragment.value("invalid username") { it.isNotEmpty() }
        if (username !is Value.Ok) return

        val password = binding.passwordLoginFragment.value("invalid password, at least 6 character") { it.length >= 6 }
        if (password !is Value.Ok) return

        lifecycleScope.launchWhenResumed {
            loadingUI(true)
            val loginResult = cognitoHelper.userLogin(username.value, password.value)
            loadingUI(false)

            handleLoginResult(loginResult)
        }
    }

    private fun EditText.value(validationErrorMessage: String, validator: (String) -> Boolean): Value<String> {
        val value = text.toString().trim()
        val result = if (validator(value)) Value.Ok(value) else Value.Error

        if (result is Value.Error) {
            error = validationErrorMessage
            requestFocus()
        } else {
            error = null
        }

        return result
    }

    private fun loadingUI(isLoading: Boolean) {
        with(binding) {
            progressLoginFragment.isVisible = isLoading

            usernameLoginFragment.isEnabled = !isLoading
            passwordLoginFragment.isEnabled = !isLoading
            loginLoginFragment.isEnabled = !isLoading
        }
    }

    private fun handleLoginResult(loginResult: LoginResult) {
        when (loginResult) {
            is LoginResult.Successful -> {
                proceed()
            }
            LoginResult.Error.NoConnection -> {
                Toast.makeText(requireContext(), "please check your internet connection", Toast.LENGTH_LONG).show()
            }
            LoginResult.Error.InvalidUsername -> {
                with(binding.usernameLoginFragment) {
                    error = "invalid username"
                    requestFocus()
                }
            }
            LoginResult.Error.InvalidPassword -> {
                with(binding.passwordLoginFragment) {
                    error = "invalid password"
                    requestFocus()
                }
            }
            is LoginResult.Error.UnknownError -> {
                Toast.makeText(requireContext(), "LoginError!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun proceed() {
        findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToMainFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    sealed interface Value<out T> {
        data class Ok<out T>(val value: T) : Value<T>
        object Error : Value<Nothing>
    }
}