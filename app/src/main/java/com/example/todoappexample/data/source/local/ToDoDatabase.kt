package com.example.todoappexample.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.todoappexample.data.Task


/**
 * Room database that contains task table
 *
 * exportSchema should be true in production database
 */

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class ToDoDatabase: RoomDatabase() {
    abstract fun taskDao(): TasksDao
}