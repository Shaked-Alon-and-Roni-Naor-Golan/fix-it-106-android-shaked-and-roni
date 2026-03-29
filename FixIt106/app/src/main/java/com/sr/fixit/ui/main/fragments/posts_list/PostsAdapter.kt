package com.sr.fixit.ui.main.fragments.posts_list

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.sr.fixit.R
import com.sr.fixit.data.posts.PostStatus
import com.sr.fixit.data.posts.PostWithSender
import com.sr.fixit.utils.ImageUtils

class PostsAdapter(
    private val onPostClick: (PostWithSender) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private var posts: List<PostWithSender> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.post_list_item, parent, false)
        return PostViewHolder(v)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusChip: TextView = itemView.findViewById(R.id.status_chip)
        private val postImage: ImageView = itemView.findViewById(R.id.post_image_view)
        private val postTitle: TextView = itemView.findViewById(R.id.post_row_title_text_view)
        private val tagsGroup: ChipGroup = itemView.findViewById(R.id.post_row_tags_group)
        private val subtitle: TextView = itemView.findViewById(R.id.post_row_subtitle_text_view)

        @SuppressLint("SetTextI18n")
        fun bind(post: PostWithSender) {
            val status = post.post.status.ifBlank { PostStatus.NEW }
            statusChip.text = when (status) {
                PostStatus.IN_PROGRESS -> "IN PROGRESS"
                PostStatus.RESOLVED -> "RESOLVED"
                else -> "NEW"
            }

            val chipColorRes = when (status) {
                PostStatus.IN_PROGRESS -> R.color.status_in_progress
                PostStatus.RESOLVED -> R.color.status_resolved
                else -> R.color.status_new
            }
            statusChip.backgroundTintList =
                ContextCompat.getColorStateList(itemView.context, chipColorRes)

            postTitle.text = post.post.title.ifBlank { "Issue report" }

            renderTags(post.post.tags)

            val rel = DateUtils.getRelativeTimeSpanString(
                post.post.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            val name = post.sender.name.ifBlank { "Resident" }
            subtitle.text = "$name · $rel"

            if (!post.post.image.isNullOrEmpty()) {
                postImage.setImageBitmap(ImageUtils.decodeBase64ToImage(post.post.image))
            } else {
                postImage.setImageResource(R.drawable.add_image_icon)
            }

            itemView.setOnClickListener { onPostClick(post) }
        }

        private fun renderTags(tags: List<String>?) {
            tagsGroup.removeAllViews()

            val validTags = tags
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.take(3)
                .orEmpty()

            tagsGroup.visibility = if (validTags.isEmpty()) View.GONE else View.VISIBLE
            if (validTags.isEmpty()) return

            validTags.forEach { tag ->
                tagsGroup.addView(createTagChip(tag))
            }
        }

        private fun createTagChip(tag: String): Chip {
            return Chip(itemView.context).apply {
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

                chipStartPadding = dpToPx(6)
                chipEndPadding = dpToPx(6)
                textStartPadding = dpToPx(8)
                textEndPadding = dpToPx(8)
                textSize = 12f
            }
        }

        private fun dpToPx(dp: Int): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                itemView.resources.displayMetrics
            )
        }
    }

    fun updatePosts(newPosts: List<PostWithSender>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}