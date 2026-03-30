package com.sr.fixit.ui.main.fragments.post_details

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.sr.fixit.R
import com.sr.fixit.data.posts.PostWithSender
import com.sr.fixit.ui.main.PostsViewModel
import com.sr.fixit.utils.ImageUtils

class PostDetailsFragment : Fragment() {
    private val viewModel: PostsViewModel by activityViewModels()
    private val userId: String by lazy { FirebaseAuth.getInstance().currentUser!!.uid }

    private lateinit var postImageView: ImageView
    private lateinit var postTitleTextView: TextView
    private lateinit var postDescriptionTextView: TextView
    private lateinit var postUserNameTextView: TextView
    private lateinit var postLocationLngTextView: TextView
    private lateinit var postLocationLatTextView: TextView
    private lateinit var tagsSection: LinearLayout
    private lateinit var postTagsChipGroup: ChipGroup
    private lateinit var editButton: Button
    private lateinit var backButton: AppCompatImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postImageView = view.findViewById(R.id.post_details_image)
        postTitleTextView = view.findViewById(R.id.post_details_title_text_view)
        postDescriptionTextView = view.findViewById(R.id.post_details_description_text_view)
        postUserNameTextView = view.findViewById(R.id.post_details_username_text_view)
        tagsSection = view.findViewById(R.id.post_tags_section)
        postTagsChipGroup = view.findViewById(R.id.post_tags_chip_group)
        postLocationLngTextView = view.findViewById(R.id.post_details_location_lng_text_view)
        postLocationLatTextView = view.findViewById(R.id.post_details_location_lat_text_view)
        editButton = view.findViewById(R.id.edit_button)
        backButton = view.findViewById(R.id.back_button)

        val postId = arguments?.getString("post_id") ?: ""

        viewModel.getAllPosts().observe(viewLifecycleOwner) { posts ->
            if (posts.isEmpty()) viewModel.invalidatePosts()
            val currentPost = posts.find { it.post.id == postId }
            currentPost?.let { updateUI(it) }
        }

        editButton.setOnClickListener { clickedView ->
            val navController = Navigation.findNavController(clickedView)
            val bundle = bundleOf("post_id" to postId)

            val pkg = requireContext().packageName
            val actionId = resources.getIdentifier(
                "action_postDetailsFragment_to_editPostFragment",
                "id",
                pkg
            )

            if (actionId != 0) {
                navController.navigate(actionId, bundle)
                return@setOnClickListener
            }

            val editDestIdCandidates = listOf(
                "editPostFragment",
                "edit_post_fragment"
            )

            val editDestId = editDestIdCandidates
                .map { resources.getIdentifier(it, "id", pkg) }
                .firstOrNull { it != 0 } ?: 0

            if (editDestId != 0) {
                navController.navigate(editDestId, bundle)
            } else {
                Log.e(
                    "PostDetailsFragment",
                    "Could not find action or destination id for EditPostFragment in resources."
                )
            }
        }

        backButton.setOnClickListener { clickedView ->
            val navController = Navigation.findNavController(clickedView)

            var poppedBack = false
            while (navController.previousBackStackEntry != null) {
                val prev = navController.previousBackStackEntry!!.destination.displayName

                val isEditOrDetails =
                    prev.contains("editPostFragment", ignoreCase = true) ||
                            prev.contains("EditPostFragment", ignoreCase = true) ||
                            prev.contains("postDetailsFragment", ignoreCase = true) ||
                            prev.contains("PostDetailsFragment", ignoreCase = true)

                if (!isEditOrDetails) break

                poppedBack = true
                navController.popBackStack()
            }

            if (!poppedBack) {
                navController.popBackStack()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(post: PostWithSender) {
        postTitleTextView.text = post.post.title
        postDescriptionTextView.text = post.post.description
        postUserNameTextView.text = post.sender.name

        renderTags(post.post.tags)

        postLocationLngTextView.text = post.post.locationLng.toString()
        postLocationLatTextView.text = post.post.locationLat.toString()

        editButton.visibility = if (post.sender.id == userId) View.VISIBLE else View.GONE

        post.post.image?.let {
            val bitmap = ImageUtils.decodeBase64ToImage(it)
            postImageView.setImageBitmap(bitmap)
        }
    }

    private fun renderTags(tags: List<String>?) {
        postTagsChipGroup.removeAllViews()

        if (tags.isNullOrEmpty()) {
            tagsSection.visibility = View.GONE
            return
        }

        tagsSection.visibility = View.VISIBLE

        tags
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { tag ->
                postTagsChipGroup.addView(createTagChip(tag))
            }
    }

    private fun createTagChip(tag: String): Chip {
        return Chip(requireContext()).apply {
            text = tag
            isClickable = false
            isCheckable = false
            chipCornerRadius = resources.getDimension(R.dimen.post_details_tag_corner_radius)
            chipMinHeight = resources.getDimension(R.dimen.post_details_tag_min_height)
            setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.home_card_bg)
            chipStrokeWidth = resources.getDimension(R.dimen.post_details_tag_stroke_width)
            chipStrokeColor = ContextCompat.getColorStateList(context, R.color.brand_primary)
            setEnsureMinTouchTargetSize(false)
            textStartPadding = resources.getDimension(R.dimen.post_details_tag_horizontal_padding)
            textEndPadding = resources.getDimension(R.dimen.post_details_tag_horizontal_padding)
        }
    }
}