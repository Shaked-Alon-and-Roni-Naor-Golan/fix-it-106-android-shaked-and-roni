package com.sr.fixit.ui.main.fragments.create_post

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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.sr.fixit.R
import com.sr.fixit.data.posts.PostModel
import com.sr.fixit.data.posts.PostStatus
import com.sr.fixit.ui.main.PostsViewModel
import com.sr.fixit.utils.CityLocationUtils
import com.sr.fixit.utils.ImageUtils
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class CreatePostFragment : Fragment(R.layout.fragment_create_post) {

    private val postsViewModel: PostsViewModel by activityViewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var addPhotoCard: View
    private lateinit var imagePreview: ImageView
    private lateinit var overlay: LinearLayout

    private lateinit var locationCard: View
    private lateinit var locationValue: TextView

    private lateinit var titleEt: TextInputEditText
    private lateinit var descriptionEt: TextInputEditText
    private lateinit var submitBtn: MaterialButton
    private lateinit var loadingOverlay: View

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

        toolbar = view.findViewById(R.id.report_toolbar)
        addPhotoCard = view.findViewById(R.id.add_photo_card)
        imagePreview = view.findViewById(R.id.report_image_preview)
        overlay = view.findViewById(R.id.add_photo_overlay)

        locationCard = view.findViewById(R.id.location_card)
        locationValue = view.findViewById(R.id.location_value)

        titleEt = view.findViewById(R.id.report_title_input)
        descriptionEt = view.findViewById(R.id.report_description_input)
        submitBtn = view.findViewById(R.id.btn_submit_report)
        loadingOverlay = view.findViewById(R.id.create_post_loading_overlay)

        applyToolbarTopInset(toolbar)

        toolbar.title = "Report an Issue"
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

        renderPhotoState()
        renderLocationState()

        addPhotoCard.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add photo")
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
                CreatePostFragmentDirections.actionCreatePostFragmentToPickLocationFragment()
            )
        }

        submitBtn.setOnClickListener { submitReport() }
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

    private fun resetForm() {
        titleEt.setText("")
        descriptionEt.setText("")
        selectedImageBase64 = null
        selectedLat = null
        selectedLng = null
        renderPhotoState()
        renderLocationState()
    }

    private fun setLoading(isLoading: Boolean) {
        submitBtn.isEnabled = !isLoading
        titleEt.isEnabled = !isLoading
        descriptionEt.isEnabled = !isLoading
        addPhotoCard.isEnabled = !isLoading
        locationCard.isEnabled = !isLoading
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun submitReport() {
        val title = titleEt.text?.toString().orEmpty().trim()
        val desc = descriptionEt.text?.toString().orEmpty().trim()

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageBase64.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please add a photo", Toast.LENGTH_SHORT).show()
            return
        }

        if (desc.isBlank()) {
            Toast.makeText(requireContext(), "Description is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLat == null || selectedLng == null) {
            Toast.makeText(requireContext(), "Please choose a location", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = postsViewModel.getCurrentUserIdOrEmpty()
        if (userId.isBlank()) {
            Toast.makeText(requireContext(), "Please sign in again.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val lat = selectedLat ?: run {
                setLoading(false)
                return@launch
            }
            val lng = selectedLng ?: run {
                setLoading(false)
                return@launch
            }

            val userCity = postsViewModel.getCurrentUserCityOrEmpty()
            val city = CityLocationUtils.getCityNameFromCoordinates(requireContext(), lat, lng)
                .ifBlank { userCity }

            val tags = postsViewModel.generateTagsForPost(title, desc)

            val post = PostModel(
                title = title,
                description = desc,
                userId = userId,
                locationLat = lat,
                locationLng = lng,
                image = selectedImageBase64,
                tags = tags.ifEmpty { null },
                status = PostStatus.NEW,
                city = city
            )

            postsViewModel.addPost(post) { success, _ ->
                setLoading(false)

                if (!success) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to submit report.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addPost
                }

                resetForm()

                findNavController().navigate(
                    CreatePostFragmentDirections.actionCreatePostFragmentToChatFragment(post.id)
                )
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
}