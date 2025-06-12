package com.example.diplom.models

import com.google.gson.annotations.SerializedName

// Основной класс, представляющий один пост на стене
data class PostItem(
    val id: Long,
    @SerializedName("owner_id")
    val ownerId: Long,
    val date: Long, // Дата в формате Unix time (секунды)
    val text: String,
    val attachments: List<Attachment>? // Список вложений, может отсутствовать
)

// Класс для одного вложения
data class Attachment(
    val type: String, // Тип вложения: "photo", "doc" и т.д.
    val photo: Photo?,
    val doc: Document?
)

// Класс для вложения "фото"
data class Photo(
    val sizes: List<PhotoSize> // У фото есть несколько размеров
) {
    // Вспомогательная функция для получения URL фото самого большого размера
    // VK отдает несколько размеров, отмеченных буквами: s, m, x, y, z, w.
    // 'x', 'y', 'z' обычно самые большие.
    fun getBestQualityUrl(): String? {
        return sizes.find { it.type == "x" }?.url
            ?: sizes.find { it.type == "y" }?.url
            ?: sizes.find { it.type == "z" }?.url
            ?: sizes.maxByOrNull { it.width }?.url
    }
}

// Класс, описывающий один размер фото
data class PhotoSize(
    val type: String,
    val url: String,
    val width: Int,
    val height: Int
)

// Класс для вложения "документ"
data class Document(
    val title: String,
    val url: String,
    val ext: String, // Расширение файла: "pdf", "docx"
    val size: Long // Размер в байтах
)