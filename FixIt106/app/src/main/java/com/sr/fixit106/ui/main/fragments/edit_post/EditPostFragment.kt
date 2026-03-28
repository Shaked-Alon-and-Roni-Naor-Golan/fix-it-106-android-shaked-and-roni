package com.sr.fixit106.ui.main.fragments.edit_post

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.sr.fixit106.R
import com.sr.fixit106.data.posts.PostModel
import com.sr.fixit106.ui.main.PostsViewModel
import com.sr.fixit106.ui.main.fragments.create_post.PickLocationFragment
import com.sr.fixit106.utils.ImageUtils
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.collections.ifEmpty
import kotlin.isInitialized
import kotlin.isNaN
import kotlin.let
import kotlin.run
import kotlin.takeIf
import kotlin.text.format
import kotlin.text.ifBlank
import kotlin.text.isBlank
import kotlin.text.isNullOrBlank
import kotlin.text.orEmpty
import kotlin.text.trim
import kotlin.to

class EditPostFragment : Fragment(R.layout.fragment_edit_post) {

    private val postsViewModel: PostsViewModel by activityViewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var addPhotoCard: View
    private lateinit var imagePreview: ImageView
    private lateinit var overlay: LinearLayout

    private lateinit var locationCard: View
    private lateinit var locationValue: TextView

    private lateinit var titleEt: TextInputEditText
    private lateinit var descriptionEt: TextInputEditText
    private lateinit var saveBtn: MaterialButton
    private lateinit var loadingOverlay: View

    private var postId: String = ""
    private var currentPost: PostModel? = null

    private var selectedImageBase64: String? = null
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    private val pickFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageBase64 = ImageUtils.convertImageToBase64(uri, requireContext())
                renderPhotoState()
            }
        }

    private val takePhotoPreview =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                selectedImageBase64 = bitmapToBase64(bitmap)
                renderPhotoState()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(PickLocationFragment.RESULT_KEY) { _, bundle ->
            val lat = bundle.getDouble(PickLocationFragment.BUNDLE_LAT, Double.NaN)
            val lng = bundle.getDouble(PickLocationFragment.BUNDLE_LNG, Double.NaN)

            if (!lat.isNaN() && !lng.isNaN()) {
                selectedLat = lat
                selectedLng = lng
                renderLocationState()
                renderPhotoState()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = EditPostFragmentArgs.fromBundle(requireArguments())
        postId = args.postId

        if (postId.isBlank()) {
            findNavController().navigateUp()
            return
        }

        toolbar = view.findViewById(R.id.edit_post_toolbar)
        addPhotoCard = view.findViewById(R.id.edit_add_photo_card)
        imagePreview = view.findViewById(R.id.edit_post_image_preview)
        overlay = view.findViewById(R.id.edit_add_photo_overlay)

        locationCard = view.findViewById(R.id.edit_location_card)
        locationValue = view.findViewById(R.id.edit_location_value)

        titleEt = view.findViewById(R.id.edit_post_title_input)
        descriptionEt = view.findViewById(R.id.edit_post_description_input)
        saveBtn = view.findViewById(R.id.btn_save_post)
        loadingOverlay = view.findViewById(R.id.edit_post_loading_overlay)

        toolbar.title = "Edit Post"
        toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        savedInstanceState?.let { st ->
            selectedImageBase64 = st.getString("selectedImageBase64")
            selectedLat = st.getDouble("selectedLat", Double.NaN).takeIf { !it.isNaN() }
            selectedLng = st.getDouble("selectedLng", Double.NaN).takeIf { !it.isNaN() }

            titleEt.setText(st.getString("draftTitle").orEmpty())
            descriptionEt.setText(st.getString("draftDesc").orEmpty())
        }

        postsViewModel.getPostByIdLive(postId).observe(viewLifecycleOwner) { postWithSender ->
            val p = postWithSender?.post ?: return@observe
            currentPost = p

            if (titleEt.text.isNullOrBlank()) {
                titleEt.setText(p.title)
            }
            if (descriptionEt.text.isNullOrBlank()) {
                descriptionEt.setText(p.description)
            }

            if (selectedLat == null) selectedLat = p.locationLat
            if (selectedLng == null) selectedLng = p.locationLng
            if (selectedImageBase64.isNullOrBlank()) selectedImageBase64 = p.image

            renderLocationState()
            renderPhotoState()
        }

        renderPhotoState()
        renderLocationState()

        addPhotoCard.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change photo")
                .setItems(arrayOf("Take photo", "Choose from gallery")) { _, which ->
                    when (which) {
                        0 -> takePhotoPreview.launch(null)
                        1 -> pickFromGallery.launch("image/*")
                    }
                }
                .show()
        }

        locationCard.setOnClickListener {
            findNavController().navigate(
                EditPostFragmentDirections.actionEditPostFragmentToPickLocationFragment()
            )
        }

        saveBtn.setOnClickListener { saveChanges() }
    }

    private fun renderPhotoState() {
        if (!this::imagePreview.isInitialized || !this::overlay.isInitialized) return

        if (!selectedImageBase64.isNullOrBlank()) {
            imagePreview.setImageBitmap(ImageUtils.decodeBase64ToImage(selectedImageBase64))
            overlay.visibility = View.GONE
        } else {
            imagePreview.setImageDrawable(null)
            overlay.visibility = View.VISIBLE
        }
    }

    private fun renderLocationState() {
        if (!this::locationValue.isInitialized) return

        if (selectedLat != null && selectedLng != null) {
            locationValue.text = "Selected: %.5f, %.5f".format(selectedLat, selectedLng)
        } else {
            locationValue.text = "Tap to select"
        }
    }

    private fun setLoading(isLoading: Boolean) {
        saveBtn.isEnabled = !isLoading
        titleEt.isEnabled = !isLoading
        descriptionEt.isEnabled = !isLoading
        addPhotoCard.isEnabled = !isLoading
        locationCard.isEnabled = !isLoading
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun saveChanges() {
        val p = currentPost ?: run {
            Toast.makeText(requireContext(), "Post not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }

        val title = titleEt.text?.toString().orEmpty().trim()
        val desc = descriptionEt.text?.toString().orEmpty().trim()

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (desc.isBlank()) {
            Toast.makeText(requireContext(), "Description is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageBase64.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please add a photo", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val finalLat = selectedLat ?: p.locationLat
            val finalLng = selectedLng ?: p.locationLng

            val userCity = postsViewModel.getCurrentUserCityOrEmpty()
            val resolvedCity = CityLocationUtils.getCityNameFromCoordinates(
                requireContext(),
                finalLat,
                finalLng
            ).ifBlank { userCity.ifBlank { p.city } }

            val tags = postsViewModel.generateTagsForPost(title, desc)

            val updated = p.copy(
                title = title,
                description = desc,
                locationLat = finalLat,
                locationLng = finalLng,
                image = selectedImageBase64,
                tags = tags.ifEmpty { p.tags },
                city = resolvedCity,
                lastActivityTimestamp = System.currentTimeMillis()
            )

            postsViewModel.editPost(updated) { success, _ ->
                setLoading(false)

                if (!success) {
                    Toast.makeText(requireContext(), "Failed to save post", Toast.LENGTH_SHORT)
                        .show()
                    return@editPost
                }

                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("selectedImageBase64", selectedImageBase64)
        outState.putDouble("selectedLat", selectedLat ?: Double.NaN)
        outState.putDouble("selectedLng", selectedLng ?: Double.NaN)

        val draftTitle = if (this::titleEt.isInitialized) {
            titleEt.text?.toString().orEmpty()
        } else {
            ""
        }
        val draftDesc = if (this::descriptionEt.isInitialized) {
            descriptionEt.text?.toString().orEmpty()
        } else {
            ""
        }

        outState.putString("draftTitle", draftTitle)
        outState.putString("draftDesc", draftDesc)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    companion object {
        fun args(postId: String) = bundleOf("post_id" to postId)
    }
}