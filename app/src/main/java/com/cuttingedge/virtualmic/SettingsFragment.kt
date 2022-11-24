package com.cuttingedge.virtualmic

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cuttingedge.virtualmic.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val binding by viewBinding(FragmentSettingsBinding::bind)

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            lifecycleScope.launch {
                settingsManager.themeTypeFlow.collectLatest {
                    when (it) {
                        "SYSTEM_DEFAULT" -> {
                            toggleButton.check(buttonDefault.id)
                        }
                        "LIGHT" -> {
                            toggleButton.check(buttonLight.id)
                        }
                        "DARK" -> {
                            toggleButton.check(buttonDark.id)
                        }
                    }
                }
            }
            toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
                lifecycleScope.launch {
                    when (checkedId) {
                        buttonDefault.id -> {
                            if (isChecked) {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                                settingsManager.setTheme(ThemeType.SYSTEM_DEFAULT)
                            }
                        }
                        buttonLight.id -> {
                            if (isChecked) {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                settingsManager.setTheme(ThemeType.LIGHT)
                            }
                        }
                        buttonDark.id -> {
                            if (isChecked) {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                settingsManager.setTheme(ThemeType.DARK)
                            }
                        }
                    }
                }
            }
        }
    }
}