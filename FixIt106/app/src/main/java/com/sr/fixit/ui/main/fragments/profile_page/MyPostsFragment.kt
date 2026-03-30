package com.sr.fixit.ui.main.fragments.profile_page

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.sr.fixit.R
import com.sr.fixit.ui.main.PostsViewModel
import com.sr.fixit.ui.main.fragments.posts_list.PostsAdapter

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private val postsViewModel: PostsViewModel by activityViewModels()
    private lateinit var adapter: PostsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.my_posts_toolbar)
        val recycler = view.findViewById<RecyclerView>(R.id.my_posts_recycler)

        applyToolbarTopInset(toolbar)

        toolbar.title = "My Reports"
        toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = PostsAdapter(
            onPostClick = { post ->
                findNavController().navigate(
                    MyPostsFragmentDirections.actionMyPostsFragmentToChatFragment(post.post.id)
                )
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
        recycler.clipToPadding = false
        recycler.setPadding(
            recycler.paddingLeft,
            recycler.paddingTop,
            recycler.paddingRight,
            resources.getDimensionPixelSize(R.dimen.my_posts_bottom_padding)
        )
        recycler.addItemDecoration(
            DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        )

        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        postsViewModel.getPostsByUserId(uid).observe(viewLifecycleOwner) { posts ->
            adapter.updatePosts(posts)
        }
    }

    private fun applyToolbarTopInset(toolbar: MaterialToolbar) {
        val initialTopPadding = toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = initialTopPadding + systemBars.top)
            insets
        }
        ViewCompat.requestApplyInsets(toolbar)
    }
}