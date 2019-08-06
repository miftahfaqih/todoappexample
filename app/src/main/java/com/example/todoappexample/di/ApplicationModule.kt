package com.example.todoappexample.di

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import com.example.todoappexample.data.source.DefaultTasksRepository
import com.example.todoappexample.data.source.TasksDataSource
import com.example.todoappexample.data.source.TasksRepository
import com.example.todoappexample.data.source.local.TasksLocalDataSource
import com.example.todoappexample.data.source.local.ToDoDatabase
import com.example.todoappexample.data.source.remote.TasksRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton


@Module(includes = [ApplicationModuleBinds::class])
object ApplicationModule {

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class TasksRemoteDataSource

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class TasksLocalDataSource

    @JvmStatic
    @Singleton
    @TasksRemoteDataSource
    @Provides
    fun providesTasksRemoteDataSource(): TasksDataSource {
        return TasksRemoteDataSource
    }

    @JvmStatic
    @Singleton
    @TasksLocalDataSource
    @Provides
    fun providesTasksLocalDataSource(
        dataBase: ToDoDatabase,
        ioDispatcher: CoroutineDispatcher
    ): TasksDataSource {
        return TasksLocalDataSource(dataBase.taskDao(),ioDispatcher)
    }

    @JvmStatic
    @Singleton
    @Provides
    fun providesDataBase(context: Context): ToDoDatabase {
        return Room.databaseBuilder(context, ToDoDatabase::class.java, "Tasks.db")
            .build()
    }

    @JvmStatic
    @Singleton
    @Provides
    fun providesIoDispatcher() = Dispatchers.IO
}

@Module
abstract class ApplicationModuleBinds {

    @Singleton
    @Binds
    abstract fun bindRepository(repo: DefaultTasksRepository): TasksRepository
}