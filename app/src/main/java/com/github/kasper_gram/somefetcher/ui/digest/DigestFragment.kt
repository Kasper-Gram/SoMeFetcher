package com.github.kasper_gram.somefetcher.ui.digest

import android.content.Intent
import android.net.Uri
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kasper_gram.somefetcher.R
import com.github.kasper_gram.somefetcher.databinding.FragmentDigestBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        adapter = DigestAdapter(
            onItemClick = { item ->
                viewModel.markRead(item)
                if (item.link.isNotEmpty()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
                    } catch (_: Exception) {
                        Snackbar.make(binding.root, R.string.error_open_link, Snackbar.LENGTH_SHORT).show()
                    }
                }
            },
            onStarClick = { item ->
                viewModel.toggleStar(item)
                val messageRes = if (item.isStarred) R.string.item_unstarred else R.string.item_starred
                Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            adapter = this@DigestFragment.adapter
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            binding.swipeRefresh.isRefreshing = refreshing
        }

        viewModel.failedSourceCount.observe(viewLifecycleOwner) { failed ->
            if (failed > 0) {
                Snackbar.make(
                    binding.root,
                    resources.getQuantityString(R.plurals.error_refresh_partial, failed, failed),
                    Snackbar.LENGTH_LONG
                ).show()
                viewModel.acknowledgeRefreshError()
            }
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                checkedIds.contains(R.id.chip_all) -> DigestFilter.ALL
                checkedIds.contains(R.id.chip_saved) -> DigestFilter.STARRED
                else -> DigestFilter.UNREAD
            }
            viewModel.setFilter(filter)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pagingItems.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collect { loadState ->
                    val isEmpty = loadState.source.refresh is LoadState.NotLoading &&
                            adapter.itemCount == 0
                    binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
                }
            }
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
                    R.id.action_refresh -> {
                        viewModel.refresh()
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
