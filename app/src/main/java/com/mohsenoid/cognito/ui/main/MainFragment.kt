package com.mohsenoid.cognito.ui.main

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mohsenoid.cognito.cognito.CognitoHelper
import com.mohsenoid.cognito.cognito.model.FetchTokenResult
import com.mohsenoid.cognito.databinding.MainFragmentBinding
import org.koin.android.ext.android.inject

class MainFragment : Fragment() {

    private val cognitoHelper: CognitoHelper by inject()

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshToken()

        with(binding) {
            tokenInfoMainFragment.movementMethod = ScrollingMovementMethod()

            refreshMainFragment.setOnClickListener {
                refreshToken()
            }

            logoutMainFragment.setOnClickListener {
                cognitoHelper.userLogout()
                proceed()
            }
        }
    }

    private fun refreshToken() {
        lifecycleScope.launchWhenResumed {
            loadingUI(true)
            val fetchTokenResult = cognitoHelper.getLatestAuthToken()
            loadingUI(false)

            if (fetchTokenResult is FetchTokenResult.Successful) {
                fetchTokenResult.updateAccessTokenInfo()
            } else {
                Toast.makeText(requireContext(), "Error: $fetchTokenResult", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun FetchTokenResult.Successful.updateAccessTokenInfo() {
        val info = listOf(
            "Current User" to cognitoHelper.currentUser,
            "ID Token Issued At" to idToken.issuedAt.toString(),
            "ID Token Expiration" to idToken.expiration.toString(),
            "Access Token Expiration" to accessToken.expiration.toString(),
            "Refresh Token" to refreshToken.token,
        ).joinToString("\n-----------\n") { "${it.first}\n${it.second}" }

        binding.tokenInfoMainFragment.text = info
    }

    private fun loadingUI(isLoading: Boolean) {
        with(binding) {
            progressMainFragment.isVisible = isLoading

            refreshMainFragment.isEnabled = !isLoading
            logoutMainFragment.isEnabled = !isLoading
        }
    }

    private fun proceed() {
        findNavController().navigate(MainFragmentDirections.actionMainFragmentToLoginFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}