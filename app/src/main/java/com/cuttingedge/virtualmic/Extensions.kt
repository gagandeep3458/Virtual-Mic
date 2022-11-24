package com.cuttingedge.virtualmic

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment

fun Fragment.hideKeyBoard(root: View) {
    val imm = getSystemService(requireContext(),InputMethodManager::class.java)
    imm?.hideSoftInputFromWindow(root.windowToken, 0)
}