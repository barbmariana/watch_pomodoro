package com.example.moves.domain

enum class ReminderType {
    Move, Water;

    fun next(): ReminderType = if (this == Move) Water else Move

    companion object {
        fun fromString(name: String?): ReminderType =
            entries.firstOrNull { it.name == name } ?: Water
    }
}
