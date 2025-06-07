package com.example.diplom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter : ListAdapter<ScheduleListItem, RecyclerView.ViewHolder>(ScheduleDiffCallback()) {

    private val VIEW_TYPE_HEADER = 1
    private val VIEW_TYPE_PAIR = 2
    private val VIEW_TYPE_NOTE = 3
    private val VIEW_TYPE_EMPTY = 4


    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ScheduleListItem.DayHeader -> VIEW_TYPE_HEADER
            is ScheduleListItem.PairItem -> VIEW_TYPE_PAIR
            is ScheduleListItem.SpecialNoteItem -> VIEW_TYPE_NOTE
            is ScheduleListItem.EmptyState -> VIEW_TYPE_EMPTY
            // Добавьте null на случай если getItem(position) вернет null, хотя с ListAdapter это маловероятно
            // если вы не передаете null в submitList и список не пуст.
            // Либо убедитесь, что ваш список никогда не содержит null, если такое возможно.
            null -> throw IllegalStateException("Null item found at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_day_header, parent, false))
            VIEW_TYPE_PAIR -> PairViewHolder(inflater.inflate(R.layout.item_schedule_pair, parent, false))
            VIEW_TYPE_NOTE -> NoteViewHolder(inflater.inflate(R.layout.item_special_note, parent, false))
            VIEW_TYPE_EMPTY -> EmptyViewHolder(inflater.inflate(R.layout.item_empty_state, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ScheduleListItem.DayHeader -> (holder as HeaderViewHolder).bind(item)
            is ScheduleListItem.PairItem -> (holder as PairViewHolder).bind(item)
            is ScheduleListItem.SpecialNoteItem -> (holder as NoteViewHolder).bind(item)
            is ScheduleListItem.EmptyState -> (holder as EmptyViewHolder).bind()
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayText: TextView = itemView.findViewById(R.id.textViewDayHeader)
        fun bind(item: ScheduleListItem.DayHeader) {
            dayText.text = item.day
        }
    }

    class PairViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.textViewPairTime)
        private val disciplineText: TextView = itemView.findViewById(R.id.textViewPairDiscipline)
        private val typeText: TextView = itemView.findViewById(R.id.textViewPairType)
        private val teacherText: TextView = itemView.findViewById(R.id.textViewPairTeacher)
        private val audienceText: TextView = itemView.findViewById(R.id.textViewPairAudience)

        fun bind(item: ScheduleListItem.PairItem) {
            timeText.text = item.time
            disciplineText.text = item.discipline
            typeText.text = item.type
            teacherText.text = "Преподаватель: ${item.teacher}"
            audienceText.text = "Аудитория: ${item.audience}"
        }
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val noteText: TextView = itemView.findViewById(R.id.textViewSpecialNote)
        fun bind(item: ScheduleListItem.SpecialNoteItem) {
            noteText.text = item.note
        }
    }
    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textViewEmptyMessage)
        fun bind() {
            // Можно настроить текст, если он будет меняться
            // messageText.text = "Данных нет"
        }
    }
}

class ScheduleDiffCallback : DiffUtil.ItemCallback<ScheduleListItem>() {
    override fun areItemsTheSame(oldItem: ScheduleListItem, newItem: ScheduleListItem): Boolean {
        return when {
            oldItem is ScheduleListItem.DayHeader && newItem is ScheduleListItem.DayHeader -> oldItem.day == newItem.day
            oldItem is ScheduleListItem.PairItem && newItem is ScheduleListItem.PairItem ->
                oldItem.time == newItem.time && oldItem.discipline == newItem.discipline // Простой пример, можно улучшить
            oldItem is ScheduleListItem.SpecialNoteItem && newItem is ScheduleListItem.SpecialNoteItem -> oldItem.note == newItem.note
            oldItem is ScheduleListItem.EmptyState && newItem is ScheduleListItem.EmptyState -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ScheduleListItem, newItem: ScheduleListItem): Boolean {
        return oldItem == newItem // Data classes сравнивают по содержимому
    }
}