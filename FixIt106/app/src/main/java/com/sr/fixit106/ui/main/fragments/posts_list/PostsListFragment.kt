package com.sr.fixit106.ui.main.fragments.posts_list

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.sr.fixit106.R
import com.sr.fixit106.data.posts.PostStatus
import com.sr.fixit106.ui.main.PostsViewModel

class PostsListFragment : Fragment(R.layout.fragment_posts_list) {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var postsList: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tabs: TabLayout
    private lateinit var initialLoadingView: View
    private lateinit var paginationLoadingView: View

    private val postsViewModel: PostsViewModel by activityViewModels()
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var postsLayoutManager: LinearLayoutManager

    private var currentStatus: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.city_feed_toolbar)
        swipeRefreshLayout = view.findViewById(R.id.post_list_view)
        tabs = view.findViewById(R.id.city_feed_tabs)
        postsList = view.findViewById(R.id.posts_list)
        initialLoadingView = view.findViewById(R.id.posts_initial_loading)
        paginationLoadingView = view.findViewById(R.id.posts_pagination_loading)

        toolbar.title = "City Feed"
        toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        context?.let { initPostsList(it) }
        setupTabs()
        bindObservers()

        swipeRefreshLayout.setOnRefreshListener {
            postsViewModel.refreshFeed()
        }

        if (savedInstanceState == null) {
            bindToStatus(null)
        }
    }

    private fun setupTabs() {
        tabs.removeAllTabs()
        tabs.addTab(tabs.newTab().setText("All"), true)
        tabs.addTab(tabs.newTab().setText("New"))
        tabs.addTab(tabs.newTab().setText("In Progress"))
        tabs.addTab(tabs.newTab().setText("Resolved"))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val status = when (tab.position) {
                    1 -> PostStatus.NEW
                    2 -> PostStatus.IN_PROGRESS
                    3 -> PostStatus.RESOLVED
                    else -> null
                }
                bindToStatus(status)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun bindObservers() {
        postsViewModel.observeFeedPosts().observe(viewLifecycleOwner) { posts ->
            postsAdapter.updatePosts(posts)
            swipeRefreshLayout.isRefreshing = false
        }

        postsViewModel.observeFeedInitialLoading().observe(viewLifecycleOwner) { isLoading ->
            initialLoadingView.visibility =
                if (isLoading && postsAdapter.itemCount == 0) View.VISIBLE else View.GONE

            if (!isLoading) {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        postsViewModel.observeFeedLoadingMore().observe(viewLifecycleOwner) { isLoadingMore ->
            paginationLoadingView.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
        }
    }

    private fun bindToStatus(status: String?) {
        currentStatus = status
        postsList.scrollToPosition(0)
        postsViewModel.resetAndLoadFeed(status)
    }

    private fun initPostsList(context: Context) {
        postsAdapter = PostsAdapter(
            onPostClick = { post ->
                findNavController().navigate(
                    PostsListFragmentDirections.actionPostsListFragmentToChatFragment(post.post.id)
                )
            }
        )

        postsLayoutManager = LinearLayoutManager(context)

        postsList.apply {
            layoutManager = postsLayoutManager
            adapter = postsAdapter
            clipToPadding = false
            setPadding(
                paddingLeft,
                paddingTop,
                paddingRight,
                resources.getDimensionPixelSize(R.dimen.feed_bottom_padding)
            )
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return

                    val totalItems = postsLayoutManager.itemCount
                    val lastVisible = postsLayoutManager.findLastVisibleItemPosition()

                    if (totalItems > 0 && lastVisible >= totalItems - 5) {
                        postsViewModel.loadNextFeedPage()
                    }
                }
            })
        }
    }
}