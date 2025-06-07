package com.example.diplom // Или com.example.diplom.model

sealed class ScheduleListItemPrepod {
    data class DayHeader(val day: String) : ScheduleListItemPrepod()
    data class PairItem(
        val time: String,
        val discipline: String,
        val type: String, // e.g., "Пр. занятие", "Лекция"
        val teacher: String,
        val audience: String,
        val group: String // Добавлено поле для группы
    ) : ScheduleListItemPrepod()
    object EmptyState : ScheduleListItemPrepod() // Для случая "Ничего не найдено"
    data class SpecialNoteItem(val message: String) : ScheduleListItemPrepod() // Для ошибок или сообщений
}