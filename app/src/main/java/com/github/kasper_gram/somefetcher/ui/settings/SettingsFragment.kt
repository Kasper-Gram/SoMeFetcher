package com.github.kasper_gram.somefetcher.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
import java.io.File
import android.widget.LinearLayout as AndroidLinearLayout

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var sourcesAdapter: FeedSourcesAdapter

    private val importOpmlLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return@registerForActivityResult
        viewModel.importOpml(inputStream)
    }

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
        setupOpmlButtons()
        observeOpmlImportState()
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
        binding.buttonAllowedApps.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_allowed_apps)
        }
    }

    private fun setupAddSourceButton() {
        binding.fabAddSource.setOnClickListener {
            showAddSourceDialog()
        }
    }

    private fun setupOpmlButtons() {
        binding.buttonExportOpml.setOnClickListener {
            exportOpml()
        }
        binding.buttonImportOpml.setOnClickListener {
            importOpmlLauncher.launch(
                arrayOf("text/xml", "application/xml", "text/x-opml", "*/*")
            )
        }
    }

    private fun exportOpml() {
        viewLifecycleOwner.lifecycleScope.launch {
            val opmlContent = viewModel.buildExportOpml()
            if (opmlContent == null) {
                Snackbar.make(binding.root, R.string.opml_export_empty, Snackbar.LENGTH_SHORT).show()
                return@launch
            }
            try {
                // Write to a temporary file in the app cache and share via FileProvider
                val cacheDir = requireContext().cacheDir
                val opmlFile = File(cacheDir, getString(R.string.opml_export_filename))
                opmlFile.writeText(opmlContent)
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    opmlFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = getString(R.string.opml_export_mime)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.opml_export_filename))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.opml_export_button)))
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.opml_export_error, e.message ?: "Unknown error"),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun observeOpmlImportState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.opmlImportState.collect { state ->
                when (state) {
                    is OPMLImportState.Idle -> { /* nothing */ }
                    is OPMLImportState.Importing -> { /* could show a progress indicator */ }
                    is OPMLImportState.Success -> {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.opml_import_success, state.imported, state.skipped),
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.resetOpmlImportState()
                    }
                    is OPMLImportState.Error -> {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.opml_import_error, state.message),
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.resetOpmlImportState()
                    }
                }
            }
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

