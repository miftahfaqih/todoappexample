package com.example.todoappexample.di

import androidx.lifecycle.ViewModel
import com.example.todoappexample.task.TasksFragment
import com.example.todoappexample.task.TasksViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class TasksModule {

    @ContributesAndroidInjector(modules = [
        ViewModelBuilder::class])
    internal abstract fun tasksFragment(): TasksFragment

    @Binds
    @IntoMap
    @ViewModelKey(TasksViewModel::class)
    abstract fun bindViewModel(viewModel: TasksViewModel): ViewModel
}