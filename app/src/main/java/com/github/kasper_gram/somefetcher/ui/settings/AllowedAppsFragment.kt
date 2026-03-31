package com.github.kasper_gram.somefetcher.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kasper_gram.somefetcher.databinding.FragmentAllowedAppsBinding

class AllowedAppsFragment : Fragment() {

    private var _binding: FragmentAllowedAppsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AllowedAppsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllowedAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AllowedAppsAdapter { packageName, allowed ->
            viewModel.setAppAllowed(packageName, allowed)
        }

        binding.recyclerAllowedApps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
            this.adapter = adapter
        }

        viewModel.apps.observe(viewLifecycleOwner) { apps ->
            adapter.submitList(apps)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
