package com.example.todoappexample.di

import android.content.Context
import com.example.todoappexample.TodoApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules =[
        ApplicationModule::class,
        TasksModule::class,
        AndroidSupportInjectionModule::class
    ])

interface ApplicationComponent: AndroidInjector<TodoApplication> {
    @Component.Factory
    interface Factory{
        fun create(@BindsInstance applicationContext: Context) : ApplicationComponent
    }
}