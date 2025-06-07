package com.example.diplom

sealed class ScheduleListItem {
    data class DayHeader(val day: String) : ScheduleListItem()
    data class PairItem(
        val time: String,
        val discipline: String,
        val type: String,
        val teacher: String,
        val audience: String
    ) : ScheduleListItem()
    data class SpecialNoteItem(val note: String) : ScheduleListItem()
    object EmptyState : ScheduleListItem() // Для случая "Ничего не найдено"
}