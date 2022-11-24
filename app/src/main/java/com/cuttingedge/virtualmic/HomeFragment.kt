package com.cuttingedge.virtualmic

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.InetAddresses
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cuttingedge.virtualmic.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<HomeViewModel>()
    private var mAppState: AppState = AppState.NotStreaming

    @Inject
    lateinit var settingsManager: SettingsManager

    private val permissionRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startStreamingOnIP(binding)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            settingsManager.themeTypeFlow.collectLatest {
                when (it) {
                    "SYSTEM_DEFAULT" -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    "LIGHT" -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    "DARK" -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                }
            }
        }

        binding.apply {
            textInputLayoutIPAddress.apply {
                editText?.doBeforeTextChanged { _, _, _, _ ->
                    textInputLayoutIPAddress.isErrorEnabled = false
                }
                savedInstanceState?.let {
                    editText?.setText(it.getString("IP"))
                }
            }
            buttonToggleStream.setOnClickListener {
                hideKeyBoard(binding.root)
                when (mAppState) {
                    AppState.NotStreaming -> {
                        startStreamingOnIP(this)
                    }
                    AppState.Streaming -> {
                        viewModel.stopStreaming()
                    }
                }
            }
            buttonSettings.setOnClickListener {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
                )
            }
        }

        with(viewModel) {
            appState.observe(viewLifecycleOwner) { state ->
                mAppState = state
                binding.apply {
                    when (state) {
                        AppState.Streaming -> {
                            textInputLayoutIPAddress.isEnabled = false
                            buttonToggleStream.let {
                                it.backgroundTintList = ColorStateList.valueOf(
                                    resources.getColor(R.color.elegant_red, null)
                                )
                                it.icon = ResourcesCompat.getDrawable(
                                    resources,
                                    R.drawable.ic_baseline_stop_24,
                                    null
                                )
                                it.text = resources.getString(R.string.stop_streaming)
                            }
                        }
                        AppState.NotStreaming -> {
                            textInputLayoutIPAddress.isEnabled = true
                            buttonToggleStream.let {
                                it.backgroundTintList = ColorStateList.valueOf(
                                    resources.getColor(R.color.elegant_green, null)
                                )
                                it.icon = ResourcesCompat.getDrawable(
                                    resources,
                                    R.drawable.ic_baseline_play_arrow_24,
                                    null
                                )
                                it.text = resources.getString(R.string.start_streaming)
                            }
                        }
                    }
                }
            }

            lifecycleScope.launch {
                homeEventsFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collectLatest {
                    when (it) {
                        is HomeViewModel.HomeEvents.Error -> {
                            Snackbar.make(binding.root, it.message, Snackbar.LENGTH_SHORT)
                                .show()
                        }
                        is HomeViewModel.HomeEvents.InfoMessage -> {
                            Snackbar.make(binding.root, it.message, Snackbar.LENGTH_SHORT)
                                .show()
                        }
                        is HomeViewModel.HomeEvents.NavigateTo -> {
                            Timber.i("Navigation to ${it.destination} Fragment")
                            findNavController().navigate(it.destination)
                        }
                    }
                }
            }
        }
    }

    private fun startStreamingOnIP(binding: FragmentHomeBinding) {
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED)
        {
            binding.apply {
                val string = textInputLayoutIPAddress.editText?.text.toString()
                if (validateIP(string)) {
                    viewModel.startStreaming(string)
                } else {
                    textInputLayoutIPAddress.error = "Enter Valid IP Address"
                }
            }
        } else {
            permissionRequestLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun validateIP(string: String): Boolean {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            InetAddresses.isNumericAddress(string)
        } else {
            Patterns.IP_ADDRESS.matcher(string).matches()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("IP", viewModel.ip)

    }
}