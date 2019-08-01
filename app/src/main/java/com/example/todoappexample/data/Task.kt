package com.example.todoappexample.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.*

@Entity(tableName = "tasks")
data class Task @JvmOverloads constructor(
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "description") var description: String = "",
    @ColumnInfo(name = "completed") var isCompleted: Boolean = false,
    @ColumnInfo(name = "entryId")var id: String = UUID.randomUUID().toString()
) {

    val isActive
    get() = !isCompleted

    val isEmpty
    get() = title.isEmpty() || description.isEmpty()

}