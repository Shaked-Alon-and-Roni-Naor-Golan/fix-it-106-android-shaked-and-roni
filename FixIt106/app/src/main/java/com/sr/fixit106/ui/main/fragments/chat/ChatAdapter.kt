package com.sr.fixit106.ui.main.fragments.chat

import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sr.fixit106.R
import com.sr.fixit106.data.comments.CommentWithSender
import com.sr.fixit106.data.users.UserRole
import com.sr.fixit106.utils.ImageUtils

class ChatAdapter(
    private var items: List<CommentWithSender>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_SYSTEM = 0
        const val TYPE_ME = 1
        const val TYPE_OTHER = 2
        const val SYSTEM_USER_ID = "rep_106"
    }

    fun submitList(newItems: List<CommentWithSender>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]

        if (item.comment.userId == SYSTEM_USER_ID) {
            return TYPE_SYSTEM
        }

        val isMine = item.comment.userId == currentUserId
        return if (isMine) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_SYSTEM -> {
                val v = inflater.inflate(R.layout.item_chat_message_system, parent, false)
                SystemVH(v)
            }

            TYPE_ME -> {
                val v = inflater.inflate(R.layout.item_chat_message_me, parent, false)
                MeVH(v)
            }

            else -> {
                val v = inflater.inflate(R.layout.item_chat_message_rep, parent, false)
                OtherVH(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = items[position]
        when (holder) {
            is MeVH -> holder.bind(model)
            is OtherVH -> holder.bind(model)
            is SystemVH -> holder.bind(model)
        }
    }

    override fun getItemCount(): Int = items.size

    class MeVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.msg_container)
        private val bubble: TextView = itemView.findViewById(R.id.msg_bubble)
        private val time: TextView = itemView.findViewById(R.id.msg_time)
        private val avatar: ImageView? = itemView.findViewById(R.id.msg_avatar)

        fun bind(model: CommentWithSender) {
            val context = itemView.context
            val isRepresentative = UserRole.isRepresentative(model.sender.role)

            bubble.text = model.comment.content
            container.background = ContextCompat.getDrawable(
                context,
                if (isRepresentative) R.drawable.bg_chat_me_rep else R.drawable.bg_chat_me_resident
            )
            bubble.setTextColor(ContextCompat.getColor(context, R.color.gray_900))
            bubble.setTypeface(
                bubble.typeface,
                if (isRepresentative) Typeface.BOLD else Typeface.NORMAL
            )

            time.text = DateUtils.getRelativeTimeSpanString(
                model.comment.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            time.setTextColor(ContextCompat.getColor(context, R.color.gray_500))

            avatar?.let { imageView ->
                val profilePicture = model.sender.profile_picture
                if (profilePicture.isNotBlank()) {
                    val bitmap = ImageUtils.decodeBase64ToImage(profilePicture)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageResource(R.drawable.empty_profile_picture)
                    }
                } else {
                    imageView.setImageResource(R.drawable.empty_profile_picture)
                }
                ImageUtils.makeImageViewCircular(imageView)
            }
        }
    }

    class OtherVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.msg_container)
        private val bubble: TextView = itemView.findViewById(R.id.msg_bubble)
        private val time: TextView = itemView.findViewById(R.id.msg_time)
        private val avatar: ImageView = itemView.findViewById(R.id.msg_avatar)

        fun bind(model: CommentWithSender) {
            val context = itemView.context
            val isRepresentative = UserRole.isRepresentative(model.sender.role)

            bubble.text = model.comment.content
            container.background = ContextCompat.getDrawable(
                context,
                if (isRepresentative) R.drawable.bg_chat_other_rep else R.drawable.bg_chat_other_resident
            )
            bubble.setTextColor(ContextCompat.getColor(context, R.color.gray_900))
            bubble.setTypeface(
                bubble.typeface,
                if (isRepresentative) Typeface.BOLD else Typeface.NORMAL
            )

            time.text = DateUtils.getRelativeTimeSpanString(
                model.comment.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            time.setTextColor(ContextCompat.getColor(context, R.color.gray_500))

            val profilePicture = model.sender.profile_picture
            if (profilePicture.isNotBlank()) {
                val bitmap = ImageUtils.decodeBase64ToImage(profilePicture)
                if (bitmap != null) {
                    avatar.setImageBitmap(bitmap)
                } else {
                    avatar.setImageResource(R.drawable.empty_profile_picture)
                }
            } else {
                avatar.setImageResource(R.drawable.empty_profile_picture)
            }

            ImageUtils.makeImageViewCircular(avatar)
        }
    }

    class SystemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bubble: TextView = itemView.findViewById(R.id.msg_bubble)
        private val time: TextView = itemView.findViewById(R.id.msg_time)

        fun bind(model: CommentWithSender) {
            bubble.text = model.comment.content
            time.text = DateUtils.getRelativeTimeSpanString(
                model.comment.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
        }
    }
}