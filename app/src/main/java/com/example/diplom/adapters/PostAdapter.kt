package com.example.diplom.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.diplom.R
import com.example.diplom.models.PostItem
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter : ListAdapter<PostItem, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    // Создает новый ViewHolder (контейнер для одного элемента списка)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_post, parent, false)
        return PostViewHolder(view)
    }

    // Заполняет ViewHolder данными из конкретного поста
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    // Внутренний класс, который хранит ссылки на View-элементы макета list_item_post.xml
    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val context: Context = itemView.context
        private val postDate: TextView = itemView.findViewById(R.id.postDate)
        private val postText: TextView = itemView.findViewById(R.id.postText)
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val postDocument: Button = itemView.findViewById(R.id.postDocument)

        // Функция, которая связывает данные из объекта PostItem с View-элементами
        fun bind(post: PostItem) {
            // Дата и время: VK отдает время в секундах, а Date ждет миллисекунды
            val date = Date(post.date * 1000)
            val format = SimpleDateFormat("d MMMM yyyy 'в' HH:mm", Locale("ru"))
            postDate.text = format.format(date)

            // Текст поста
            postText.text = post.text
            postText.isVisible = post.text.isNotBlank() // Показываем, только если не пустой

            // Сбрасываем видимость вложений перед настройкой
            postImage.isVisible = false
            postDocument.isVisible = false

            // Обработка вложений
            post.attachments?.forEach { attachment ->
                when (attachment.type) {
                    "photo" -> {
                        // Если есть фото, получаем URL лучшего качества и загружаем через Glide
                        attachment.photo?.getBestQualityUrl()?.let { imageUrl ->
                            postImage.isVisible = true
                            Glide.with(context)
                                .load(imageUrl)
                                .into(postImage)
                        }
                    }
                    "doc" -> {
                        // Если есть документ, настраиваем кнопку для его открытия в браузере
                        attachment.doc?.let { doc ->
                            postDocument.isVisible = true
                            postDocument.text = "${doc.title} (.${doc.ext})"
                            postDocument.setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.url))
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Вспомогательный класс для эффективного обновления списка
class PostDiffCallback : DiffUtil.ItemCallback<PostItem>() {
    override fun areItemsTheSame(oldItem: PostItem, newItem: PostItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PostItem, newItem: PostItem): Boolean {
        return oldItem == newItem
    }
}