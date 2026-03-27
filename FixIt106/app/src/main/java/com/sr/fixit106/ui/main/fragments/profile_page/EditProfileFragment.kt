package com.sr.fixit106.ui.main.fragments.profile_page

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.sr.fixit106.R
import com.sr.fixit106.data.users.UserModel
import com.sr.fixit106.ui.main.PostsViewModel
import com.sr.fixit106.utils.ImageUtils
import java.io.ByteArrayOutputStream
import kotlin.apply
import kotlin.run
import kotlin.runCatching
import kotlin.text.ifBlank
import kotlin.text.isBlank
import kotlin.text.isNotBlank
import kotlin.text.orEmpty
import kotlin.text.trim

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private val postsViewModel: PostsViewModel by activityViewModels()

    private val uid: String? by lazy { FirebaseAuth.getInstance().currentUser?.uid }

    private lateinit var profileImage: ImageView
    private lateinit var editProfileImageBtn: View
    private lateinit var nameEt: TextInputEditText
    private lateinit var cityEt: TextInputEditText
    private lateinit var saveBtn: MaterialButton
    private lateinit var loadingOverlay: View

    private var base64Image: String = ""
    private var cachedUser: UserModel? = null

    private val pickFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                base64Image = ImageUtils.convertImageToBase64(uri, requireContext()).ifBlank { "" }
                if (base64Image.isNotBlank()) {
                    profileImage.setImageBitmap(ImageUtils.decodeBase64ToImage(base64Image))
                } else {
                    profileImage.setImageResource(R.drawable.empty_profile_picture)
                }
            }
        }

    private val takePhotoPreview =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                base64Image = bitmapToBase64(bitmap)
                profileImage.setImageBitmap(bitmap)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.edit_profile_toolbar)
        profileImage = view.findViewById(R.id.edit_profile_picture)
        editProfileImageBtn = view.findViewById(R.id.edit_profile_picture_button)
        nameEt = view.findViewById(R.id.edit_profile_name)
        cityEt = view.findViewById(R.id.edit_profile_city)
        saveBtn = view.findViewById(R.id.edit_profile_save_btn)
        loadingOverlay = view.findViewById(R.id.edit_profile_loading_overlay)

        toolbar.title = "Edit Profile"
        toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val id = uid ?: run {
            findNavController().navigateUp()
            return
        }

        postsViewModel.getUserById(id).observe(viewLifecycleOwner) { user ->
            if (user != null) {
                cachedUser = user
                nameEt.setText(user.name)
                cityEt.setText(user.city)

                if (base64Image.isBlank()) {
                    if (user.profile_picture.isNotBlank()) {
                        base64Image = user.profile_picture
                        profileImage.setImageBitmap(
                            ImageUtils.decodeBase64ToImage(user.profile_picture)
                        )
                    } else {
                        base64Image = ""
                        profileImage.setImageResource(R.drawable.empty_profile_picture)
                    }
                }
            }
        }

        profileImage.setOnClickListener { showImagePickerOptions() }
        editProfileImageBtn.setOnClickListener { showImagePickerOptions() }

        saveBtn.setOnClickListener {
            val newName = nameEt.text?.toString().orEmpty().trim()
            val newCity = cityEt.text?.toString().orEmpty().trim()

            if (newName.isBlank()) {
                Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newCity.isBlank()) {
                Toast.makeText(requireContext(), "City is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = cachedUser
            if (user == null) {
                Toast.makeText(requireContext(), "User not loaded yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updated = user.copy(
                name = newName,
                profile_picture = base64Image,
                city = newCity
            )

            setLoading(true)

            postsViewModel.updateUser(updated) { success, _ ->
                setLoading(false)

                if (!success) {
                    Toast.makeText(requireContext(), "Failed to save profile", Toast.LENGTH_SHORT)
                        .show()
                    return@updateUser
                }

                runCatching {
                    findNavController()
                        .getBackStackEntry(R.id.profilePageFragment)
                        .savedStateHandle[PROFILE_UPDATED_KEY] = Bundle().apply {
                        putString(BUNDLE_NAME, updated.name)
                        putString(BUNDLE_CITY, updated.city)
                        putString(BUNDLE_PROFILE_PICTURE, updated.profile_picture)
                    }
                }

                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        saveBtn.isEnabled = !isLoading
        nameEt.isEnabled = !isLoading
        cityEt.isEnabled = !isLoading
        profileImage.isEnabled = !isLoading
        editProfileImageBtn.isEnabled = !isLoading
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showImagePickerOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change profile photo")
            .setItems(arrayOf("Take photo", "Choose from gallery")) { _, which ->
                when (which) {
                    0 -> takePhotoPreview.launch(null)
                    1 -> pickFromGallery.launch("image/*")
                }
            }
            .show()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    companion object {
        const val PROFILE_UPDATED_KEY = "profile_updated_key"
        const val BUNDLE_NAME = "name"
        const val BUNDLE_CITY = "city"
        const val BUNDLE_PROFILE_PICTURE = "profile_picture"
    }
}