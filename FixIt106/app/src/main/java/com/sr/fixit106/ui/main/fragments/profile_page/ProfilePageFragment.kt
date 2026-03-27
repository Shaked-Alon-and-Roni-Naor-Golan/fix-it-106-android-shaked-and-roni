package com.sr.fixit106.ui.main.fragments.profile_page

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.sr.fixit106.R
import com.sr.fixit106.data.users.UserRole
import com.sr.fixit106.ui.auth.AuthActivity
import com.sr.fixit106.ui.main.PostsViewModel
import com.sr.fixit106.utils.ImageUtils
import kotlin.collections.joinToString
import kotlin.collections.plusAssign
import kotlin.jvm.java
import kotlin.text.ifBlank
import kotlin.text.isNotBlank

class ProfilePageFragment : Fragment(R.layout.fragment_profile_page) {

    private val postsViewModel: PostsViewModel by activityViewModels()

    private val userId: String? by lazy { FirebaseAuth.getInstance().currentUser?.uid }

    private lateinit var profileImage: ImageView
    private lateinit var usernameTv: TextView
    private lateinit var cityTv: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = userId
        if (uid == null) {
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
            return
        }

        val toolbar = view.findViewById<MaterialToolbar>(R.id.profile_toolbar)
        profileImage = view.findViewById(R.id.profile_picture)
        usernameTv = view.findViewById(R.id.profile_username)
        cityTv = view.findViewById(R.id.profile_city)

        val editRow = view.findViewById<View>(R.id.row_edit_profile)
        val myPostsRow = view.findViewById<View>(R.id.row_my_posts)
        val aboutRow = view.findViewById<View>(R.id.row_about_fixit)
        val logoutRow = view.findViewById<View>(R.id.row_logout)

        toolbar.title = "My Profile"
        toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.top_profile_menu)

        toolbar.menu.findItem(R.id.action_notifications)?.icon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.white)
        )

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        ImageUtils.makeImageViewCircular(profileImage)

        postsViewModel.getUserById(uid).observe(viewLifecycleOwner) { user ->
            if (user != null) {
                renderProfile(
                    name = user.name.ifBlank { "User" },
                    city = user.city,
                    profilePicture = user.profile_picture,
                    role = user.role
                )
            }
        }

        editRow.setOnClickListener {
            findNavController().navigate(
                ProfilePageFragmentDirections.actionProfilePageFragmentToEditProfileFragment()
            )
        }

        myPostsRow.setOnClickListener {
            findNavController().navigate(
                ProfilePageFragmentDirections.actionProfilePageFragmentToMyPostsFragment()
            )
        }

        aboutRow.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("About FixIt106")
                .setMessage(
                    "FixIt106 is a municipal 106 support app for reporting construction and infrastructure issues in your city. Residents can open reports, track updates, and stay connected with the city support center."
                )
                .setPositiveButton("OK", null)
                .show()
        }

        logoutRow.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun renderProfile(
        name: String,
        city: String,
        profilePicture: String,
        role: String
    ) {
        usernameTv.text = name.ifBlank { "User" }

        val parts = mutableListOf<String>()
        if (UserRole.isRepresentative(role)) {
            parts plusAssign "106 Representative"
        }
        if (city.isNotBlank()) {
            parts plusAssign "$city Resident"
        } else if (!UserRole.isRepresentative(role)) {
            parts plusAssign "Resident"
        }

        cityTv.text = parts.joinToString(" • ")

        if (profilePicture.isNotBlank()) {
            profileImage.setImageBitmap(ImageUtils.decodeBase64ToImage(profilePicture))
        } else {
            profileImage.setImageResource(R.drawable.empty_profile_picture)
        }

        ImageUtils.makeImageViewCircular(profileImage)
    }
}