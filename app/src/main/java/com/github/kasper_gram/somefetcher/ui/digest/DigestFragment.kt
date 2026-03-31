package com.github.kasper_gram.somefetcher.ui.digest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kasper_gram.somefetcher.R
import com.github.kasper_gram.somefetcher.databinding.FragmentDigestBinding

class DigestFragment : Fragment() {

    private var _binding: FragmentDigestBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DigestViewModel by viewModels()
    private lateinit var adapter: DigestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDigestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DigestAdapter { item -> viewModel.markRead(item) }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            adapter = this@DigestFragment.adapter
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_digest, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_mark_all_read -> {
                        viewModel.markAllRead()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
