package com.sr.fixit106.ui.main.fragments.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.sr.fixit106.R
import com.sr.fixit106.ui.main.PostsViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.apply

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val postsViewModel: PostsViewModel by activityViewModels()

    private lateinit var toolbar: MaterialToolbar
    private var badge: BadgeDrawable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.home_toolbar)
        toolbar.title = "FixIt"
        toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.top_profile_menu)

        toolbar.menu.findItem(R.id.action_notifications)?.icon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.white)
        )

        val helloText: TextView = view.findViewById(R.id.home_hello)
        val reportBtn: MaterialButton = view.findViewById(R.id.home_report_btn)
        val cityFeedBtn: MaterialButton = view.findViewById(R.id.home_city_feed_btn)

        val myReportsRow: View = view.findViewById(R.id.home_my_reports_row)
        val call106Row: View = view.findViewById(R.id.home_call_106_row)

        val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Resident"
        helloText.text = "Hello, $userName,"

        reportBtn.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToReportGraph()
            )
        }

        cityFeedBtn.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToPostsListFragment()
            )
        }

        myReportsRow.setOnClickListener {
            val request = NavDeepLinkRequest.Builder
                .fromUri(Uri.parse("fixit106://my_reports"))
                .build()

            findNavController().navigate(request)
        }

        call106Row.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:106")
            }
            startActivity(intent)
        }

        setupBadge()
    }

    @OptIn(ExperimentalBadgeUtils::class)
    private fun setupBadge() {
        val b = BadgeDrawable.create(requireContext()).apply {
            isVisible = false
            number = 0
        }
        badge = b

        BadgeUtils.attachBadgeDrawable(b, toolbar, R.id.action_notifications)

        MainScope().launch {
            val unread = postsViewModel.getUnreadNotificationsCountForCurrentUser(requireContext())
            badge?.number = unread
            badge?.isVisible = unread > 0
        }
    }
}