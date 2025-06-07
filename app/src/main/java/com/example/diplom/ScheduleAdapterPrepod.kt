package com.example.diplom // Или com.example.diplom.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.cardview.widget.CardView // Для карточек

class ScheduleAdapterPrepod : ListAdapter<ScheduleListItemPrepod, RecyclerView.ViewHolder>(ScheduleDiffCallback()) {

    private val VIEW_TYPE_DAY_HEADER = 0
    private val VIEW_TYPE_PAIR_ITEM = 1
    private val VIEW_TYPE_EMPTY_STATE = 2
    private val VIEW_TYPE_SPECIAL_NOTE = 3

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ScheduleListItemPrepod.DayHeader -> VIEW_TYPE_DAY_HEADER
            is ScheduleListItemPrepod.PairItem -> VIEW_TYPE_PAIR_ITEM
            is ScheduleListItemPrepod.EmptyState -> VIEW_TYPE_EMPTY_STATE
            is ScheduleListItemPrepod.SpecialNoteItem -> VIEW_TYPE_SPECIAL_NOTE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DAY_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day_header_prepod, parent, false)
                DayHeaderViewHolder(view)
            }
            VIEW_TYPE_PAIR_ITEM -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prepod_lesson_prepod, parent, false) // Используйте новый макет
                PairItemViewHolder(view)
            }
            VIEW_TYPE_EMPTY_STATE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_empty_state_prepod, parent, false)
                EmptyStateViewHolder(view)
            }
            VIEW_TYPE_SPECIAL_NOTE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_special_note_prepod, parent, false)
                SpecialNoteViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ScheduleListItemPrepod.DayHeader -> (holder as DayHeaderViewHolder).bind(item.day)
            is ScheduleListItemPrepod.PairItem -> (holder as PairItemViewHolder).bind(item)
            is ScheduleListItemPrepod.EmptyState -> (holder as EmptyStateViewHolder).bind()
            is ScheduleListItemPrepod.SpecialNoteItem -> (holder as SpecialNoteViewHolder).bind(item.message)
        }
    }

    class DayHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayHeaderTextView: TextView = itemView.findViewById(R.id.dayHeaderTextView)
        fun bind(day: String) {
            dayHeaderTextView.text = day
        }
    }

    class PairItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val teacherNameTextView: TextView = itemView.findViewById(R.id.teacherNameTextView) // Новое поле для ФИО
        private val groupTextView: TextView = itemView.findViewById(R.id.groupTextView)
        private val audienceTextView: TextView = itemView.findViewById(R.id.audienceTextView)
        private val disciplineTextView: TextView = itemView.findViewById(R.id.disciplineTextView) // Добавлено

        fun bind(pair: ScheduleListItemPrepod.PairItem) {
            timeTextView.text = pair.time
            teacherNameTextView.text = "Преподаватель: ${pair.teacher}" // Здесь ФИО преподавателя, которое мы ищем
            disciplineTextView.text = pair.discipline // Дисциплина
            groupTextView.text = "Группа: ${pair.group}"
            audienceTextView.text = "Аудитория: ${pair.audience}"
        }
    }

    class EmptyStateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.emptyStateTextView)
        fun bind() {
            messageTextView.text = "Ничего не найдено"
        }
    }

    class SpecialNoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.specialNoteTextView)
        fun bind(message: String) {
            messageTextView.text = message
        }
    }

    private class ScheduleDiffCallback : DiffUtil.ItemCallback<ScheduleListItemPrepod>() {
        override fun areItemsTheSame(oldItem: ScheduleListItemPrepod, newItem: ScheduleListItemPrepod): Boolean {
            return when {
                oldItem is ScheduleListItemPrepod.DayHeader && newItem is ScheduleListItemPrepod.DayHeader -> oldItem.day == newItem.day
                oldItem is ScheduleListItemPrepod.PairItem && newItem is ScheduleListItemPrepod.PairItem ->
                    oldItem.time == newItem.time && oldItem.discipline == newItem.discipline &&
                            oldItem.teacher == newItem.teacher && oldItem.group == newItem.group
                oldItem is ScheduleListItemPrepod.EmptyState && newItem is ScheduleListItemPrepod.EmptyState -> true
                oldItem is ScheduleListItemPrepod.SpecialNoteItem && newItem is ScheduleListItemPrepod.SpecialNoteItem -> oldItem.message == newItem.message
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ScheduleListItemPrepod, newItem: ScheduleListItemPrepod): Boolean {
            return oldItem == newItem // Data classes handle content equality automatically
        }
    }
}