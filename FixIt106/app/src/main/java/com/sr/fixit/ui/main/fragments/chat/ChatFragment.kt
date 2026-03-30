package com.sr.fixit.ui.main.fragments.chat

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.ActionMenuView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.sr.fixit.R
import com.sr.fixit.data.posts.PostStatus
import com.sr.fixit.data.users.UserRole
import com.sr.fixit.ui.main.ChatViewModel
import com.sr.fixit.ui.main.PostsViewModel
import com.sr.fixit.utils.ImageUtils
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private val postsViewModel: PostsViewModel by activityViewModels()
    private val chatViewModel: ChatViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var headerImage: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var headerDescription: TextView
    private lateinit var headerStatus: TextView
    private lateinit var headerTagsGroup: ChipGroup

    private lateinit var recycler: RecyclerView
    private lateinit var input: TextInputEditText
    private lateinit var sendBtn: View

    private lateinit var adapter: ChatAdapter

    private var canResolve: Boolean = false
    private var canEdit: Boolean = false
    private var canDelete: Boolean = false
    private var currentPostId: String = ""
    private var isCurrentUserRepresentative: Boolean = false
    private var autoTakeOwnershipHandled: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = ChatFragmentArgs.fromBundle(requireArguments())
        currentPostId = args.postId

        toolbar = view.findViewById(R.id.chat_toolbar)
        headerImage = view.findViewById(R.id.chat_post_image)
        headerTitle = view.findViewById(R.id.chat_post_title)
        headerDescription = view.findViewById(R.id.chat_post_description)
        headerStatus = view.findViewById(R.id.chat_post_status)
        headerTagsGroup = view.findViewById(R.id.chat_post_tags_group)

        recycler = view.findViewById(R.id.chat_recycler)
        input = view.findViewById(R.id.chat_input)
        sendBtn = view.findViewById(R.id.chat_send_btn)

        applyToolbarTopInset(toolbar)

        toolbar.title = "106 Representative"
        toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.chat_menu)
        compactToolbarActionItems()
        toolbar.menu.findItem(R.id.action_edit)?.isVisible = false
        toolbar.menu.findItem(R.id.action_delete)?.isVisible = false
        toolbar.menu.findItem(R.id.action_resolve)?.isVisible = false

        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_edit -> {
                    if (canEdit && currentPostId.isNotBlank()) {
                        navigateToEditPost()
                    }
                    true
                }

                R.id.action_delete -> {
                    if (canDelete && currentPostId.isNotBlank()) {
                        confirmDeletePost()
                    }
                    true
                }

                R.id.action_resolve -> {
                    if (canResolve && currentPostId.isNotBlank()) {
                        postsViewModel.updatePostStatus(currentPostId, PostStatus.RESOLVED)
                    }
                    true
                }

                else -> false
            }
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        adapter = ChatAdapter(emptyList(), currentUserId = currentUid)
        recycler.adapter = adapter

        postsViewModel.getCurrentUserLive().observe(viewLifecycleOwner) { user ->
            isCurrentUserRepresentative = UserRole.isRepresentative(user?.role)
        }

        postsViewModel.getPostByIdLive(currentPostId).observe(viewLifecycleOwner) { postWithSender ->
            val editItem = toolbar.menu.findItem(R.id.action_edit)
            val deleteItem = toolbar.menu.findItem(R.id.action_delete)
            val resolveItem = toolbar.menu.findItem(R.id.action_resolve)

            if (postWithSender != null) {
                headerTitle.text = postWithSender.post.title.ifBlank { "Issue report" }
                headerDescription.text = postWithSender.post.description.ifBlank { "" }
                renderHeaderTags(postWithSender.post.tags)

                val status = postWithSender.post.status.ifBlank { PostStatus.NEW }
                headerStatus.text = status

                val chipColorRes = when (status) {
                    PostStatus.IN_PROGRESS -> R.color.status_in_progress
                    PostStatus.RESOLVED -> R.color.status_resolved
                    else -> R.color.status_new
                }

                headerStatus.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(), chipColorRes))

                val img = postWithSender.post.image
                if (!img.isNullOrBlank()) {
                    headerImage.setImageBitmap(ImageUtils.decodeBase64ToImage(img))
                } else {
                    headerImage.setImageResource(R.drawable.add_image_icon)
                }

                val isOwner = postWithSender.post.userId == currentUid
                val isAlreadyResolved = status == PostStatus.RESOLVED

                canEdit = isOwner
                canDelete = isOwner
                canResolve = isOwner && !isAlreadyResolved

                editItem?.isVisible = canEdit
                deleteItem?.isVisible = canDelete
                resolveItem?.isVisible = canResolve
            } else {
                headerTitle.text = "Chat"
                headerDescription.text = ""
                headerStatus.text = PostStatus.NEW
                headerImage.setImageResource(R.drawable.add_image_icon)
                headerTagsGroup.removeAllViews()
                headerTagsGroup.visibility = View.GONE

                canEdit = false
                canDelete = false
                canResolve = false

                editItem?.isVisible = false
                deleteItem?.isVisible = false
                resolveItem?.isVisible = false
            }

            compactToolbarActionItems()

            if (postWithSender != null) {
                val status = postWithSender.post.status.ifBlank { PostStatus.NEW }
                if (isCurrentUserRepresentative && status == PostStatus.NEW && !autoTakeOwnershipHandled) {
                    autoTakeOwnershipHandled = true
                    chatViewModel.onRepresentativeOpenedNewIssue(currentPostId)
                }
            }
        }

        chatViewModel.observeComments(currentPostId).observe(viewLifecycleOwner) { comments ->
            adapter.submitList(comments)
            if (comments.isNotEmpty()) {
                recycler.post {
                    recycler.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }

        sendBtn.setOnClickListener {
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener

            if (isCurrentUserRepresentative) {
                chatViewModel.sendRepMessage(currentPostId, text)
            } else {
                chatViewModel.sendUserMessage(currentPostId, text)
            }
            input.setText("")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            postsViewModel.markPostChatAsRead(requireContext(), currentPostId)
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

    private fun navigateToEditPost() {
        findNavController().navigate(
            ChatFragmentDirections.actionChatFragmentToEditPostFragment(currentPostId)
        )
    }

    private fun confirmDeletePost() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete post")
            .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteCurrentPost()
            }
            .show()
    }

    private fun deleteCurrentPost() {
        postsViewModel.deletePostById(currentPostId) {
            if (isAdded) {
                findNavController().navigateUp()
            }
        }
    }

    private fun renderHeaderTags(tags: List<String>?) {
        headerTagsGroup.removeAllViews()

        val validTags = tags
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.take(5)
            .orEmpty()

        headerTagsGroup.visibility = if (validTags.isEmpty()) View.GONE else View.VISIBLE
        if (validTags.isEmpty()) return

        validTags.forEach { tag ->
            headerTagsGroup.addView(createHeaderTagChip(tag))
        }
    }

    private fun createHeaderTagChip(tag: String): Chip {
        return Chip(requireContext()).apply {
            text = tag
            isClickable = false
            isCheckable = false
            setEnsureMinTouchTargetSize(false)

            chipCornerRadius = 12f
            chipMinHeight = 24f
            chipStrokeWidth = 0f

            setTextColor(ContextCompat.getColor(context, R.color.gray_900))
            chipBackgroundColor =
                ContextCompat.getColorStateList(context, R.color.light_gray)

            chipStartPadding = dpToPxFloat(toolbar, 6)
            chipEndPadding = dpToPxFloat(toolbar, 6)
            textStartPadding = dpToPxFloat(toolbar, 8)
            textEndPadding = dpToPxFloat(toolbar, 8)
            textSize = 12f
        }
    }

    private fun compactToolbarActionItems() {
        toolbar.post {
            if (!isAdded || view == null) return@post

            val actionMenuView = findActionMenuView(toolbar) ?: return@post

            val horizontalPadding = dpToPx(toolbar, 6)
            val minItemWidth = dpToPx(toolbar, 36)

            for (i in 0 until actionMenuView.childCount) {
                val child = actionMenuView.getChildAt(i)
                child.minimumWidth = minItemWidth
                child.setPadding(
                    horizontalPadding,
                    child.paddingTop,
                    horizontalPadding,
                    child.paddingBottom
                )

                val lp = child.layoutParams
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                child.layoutParams = lp
            }
        }
    }

    private fun findActionMenuView(parent: ViewGroup): ActionMenuView? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ActionMenuView) return child
            if (child is ViewGroup) {
                val nested = findActionMenuView(child)
                if (nested != null) return nested
            }
        }
        return null
    }

    private fun dpToPx(view: View, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            view.resources.displayMetrics
        ).toInt()
    }

    private fun dpToPxFloat(view: View, dp: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            view.resources.displayMetrics
        )
    }
}