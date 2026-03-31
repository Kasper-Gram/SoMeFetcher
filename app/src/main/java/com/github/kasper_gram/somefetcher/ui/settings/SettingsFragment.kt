package com.github.kasper_gram.somefetcher.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kasper_gram.somefetcher.R
import com.github.kasper_gram.somefetcher.databinding.FragmentSettingsBinding
import com.github.kasper_gram.somefetcher.worker.DigestScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import android.widget.LinearLayout as AndroidLinearLayout

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var sourcesAdapter: FeedSourcesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSourcesList()
        setupDigestTime()
        setupNotificationAccess()
        setupAddSourceButton()
    }

    private fun setupSourcesList() {
        sourcesAdapter = FeedSourcesAdapter(
            onToggle = { source -> viewModel.toggleSource(source) },
            onDelete = { source -> viewModel.deleteSource(source) }
        )
        binding.recyclerSources.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            adapter = sourcesAdapter
        }
        viewModel.sources.observe(viewLifecycleOwner) { sources ->
            sourcesAdapter.submitList(sources)
        }
    }

    private fun setupDigestTime() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val hour = prefs.getInt("digest_hour", 8)
        val minute = prefs.getInt("digest_minute", 0)
        updateTimeDisplay(hour, minute)

        binding.buttonPickTime.setOnClickListener {
            val currentHour = prefs.getInt("digest_hour", 8)
            val currentMinute = prefs.getInt("digest_minute", 0)
            TimePickerDialog(requireContext(), { _, h, m ->
                prefs.edit().putInt("digest_hour", h).putInt("digest_minute", m).apply()
                updateTimeDisplay(h, m)
                DigestScheduler.schedule(requireContext(), h, m)
                Snackbar.make(binding.root, R.string.digest_time_saved, Snackbar.LENGTH_SHORT).show()
            }, currentHour, currentMinute, true).show()
        }
    }

    private fun updateTimeDisplay(hour: Int, minute: Int) {
        binding.textDigestTime.text = getString(R.string.digest_time_format, hour, minute)
    }

    private fun setupNotificationAccess() {
        binding.buttonNotificationAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun setupAddSourceButton() {
        binding.fabAddSource.setOnClickListener {
            showAddSourceDialog()
        }
    }

    private fun showAddSourceDialog() {
        val dialogLayout = AndroidLinearLayout(requireContext()).apply {
            orientation = AndroidLinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_padding)
            setPadding(padding, padding, padding, padding)
        }

        val nameLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.feed_name_hint)
        }
        val nameInput = TextInputEditText(requireContext())
        nameLayout.addView(nameInput)

        val urlLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.feed_url_hint)
        }
        val urlInput = TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        urlLayout.addView(urlInput)

        val progressIndicator = CircularProgressIndicator(requireContext()).apply {
            isIndeterminate = true
            visibility = View.GONE
            layoutParams = AndroidLinearLayout.LayoutParams(
                AndroidLinearLayout.LayoutParams.WRAP_CONTENT,
                AndroidLinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = resources.getDimensionPixelSize(R.dimen.dialog_padding) / 2
            }
        }

        dialogLayout.addView(nameLayout)
        dialogLayout.addView(urlLayout)
        dialogLayout.addView(progressIndicator)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_feed_source)
            .setView(dialogLayout)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(R.string.cancel, null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = nameInput.text?.toString()?.trim().orEmpty()
            val url = urlInput.text?.toString()?.trim().orEmpty()
            nameLayout.error = null
            urlLayout.error = null
            if (name.isEmpty()) {
                nameLayout.error = getString(R.string.error_feed_name_required)
                return@setOnClickListener
            }
            if (url.isEmpty()) {
                urlLayout.error = getString(R.string.error_feed_url_required)
                return@setOnClickListener
            }
            viewModel.addSource(name, url)
        }

        val collectJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.addSourceState.collect { state ->
                val positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                when (state) {
                    is AddSourceState.Idle -> {
                        progressIndicator.visibility = View.GONE
                        positiveBtn?.isEnabled = true
                        negativeBtn?.isEnabled = true
                    }
                    is AddSourceState.Validating -> {
                        progressIndicator.visibility = View.VISIBLE
                        positiveBtn?.isEnabled = false
                        negativeBtn?.isEnabled = false
                    }
                    is AddSourceState.Success -> {
                        dialog.dismiss()
                        viewModel.resetAddSourceState()
                    }
                    is AddSourceState.Invalid -> {
                        progressIndicator.visibility = View.GONE
                        urlLayout.error = getString(
                            when (state.reason) {
                                AddSourceError.INVALID_URL -> R.string.error_invalid_url_format
                                AddSourceError.UNREACHABLE -> R.string.error_feed_unreachable
                                AddSourceError.NOT_A_FEED -> R.string.error_not_a_valid_feed
                            }
                        )
                        positiveBtn?.isEnabled = true
                        negativeBtn?.isEnabled = true
                    }
                }
            }
        }

        dialog.setOnDismissListener {
            collectJob.cancel()
            viewModel.resetAddSourceState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
