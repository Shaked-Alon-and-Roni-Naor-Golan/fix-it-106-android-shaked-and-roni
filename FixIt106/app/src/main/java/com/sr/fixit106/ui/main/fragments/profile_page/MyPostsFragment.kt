package com.sr.fixit106.ui.main.fragments.profile_page

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.sr.fixit106.R
import com.sr.fixit106.ui.main.PostsViewModel
import com.sr.fixit106.ui.main.fragments.posts_list.PostsAdapter
import kotlin.text.orEmpty

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private val postsViewModel: PostsViewModel by activityViewModels()
    private lateinit var adapter: PostsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.my_posts_toolbar)
        val recycler = view.findViewById<RecyclerView>(R.id.my_posts_recycler)

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
}
