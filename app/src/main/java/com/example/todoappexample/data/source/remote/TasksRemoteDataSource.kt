package com.example.todoappexample.data.source.remote

import com.example.todoappexample.data.Result
import com.example.todoappexample.data.Result.Success
import com.example.todoappexample.data.Task
import com.example.todoappexample.data.source.TasksDataSource
import kotlinx.coroutines.delay
import java.lang.Exception

object TasksRemoteDataSource : TasksDataSource {

    private const val SERVICE_LATENCY_IN_MILLIS = 2000L

    private var TASK_SERVICE_DATA = LinkedHashMap<String, Task>(2)


    init {
        addTask("Build tower in Pisa", "Ground looks good, no foundation work required.")
        addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!")
    }

    private fun addTask(title: String, description: String){
        val newTask = Task(title,description)
        TASK_SERVICE_DATA.put(newTask.id,newTask)
    }

    override suspend fun getTasks(): Result<List<Task>> {
        val task = TASK_SERVICE_DATA.values.toList()
        delay(SERVICE_LATENCY_IN_MILLIS)
        return Success(task)
    }

    override suspend fun getTask(taskId: String): Result<Task> {
        delay(SERVICE_LATENCY_IN_MILLIS)
        TASK_SERVICE_DATA[taskId]?.let {
            return Success(it)
        }
        return Result.Error(Exception("Task not found"))
    }

    override suspend fun saveTask(task: Task) {
        TASK_SERVICE_DATA.put(task.id,task)
    }

    override suspend fun completeTask(task: Task) {
        val completedTask = Task(task.title, task.description, true, task.id)
        TASK_SERVICE_DATA.put(task.id,completedTask)
    }

    override suspend fun completeTask(taskId: String) {

    }

    override suspend fun activateTask(task: Task) {
        val activeTask = Task(task.title, task.description,false,task.id)
        TASK_SERVICE_DATA.put(task.id,activeTask)
    }

    override suspend fun activateTask(taskId: String) {

    }

    override suspend fun clearCompletedTasks() {
        TASK_SERVICE_DATA = TASK_SERVICE_DATA.filterValues {
            !it.isCompleted
        } as LinkedHashMap<String, Task>
    }

    override suspend fun deleteAllTasks() {
        TASK_SERVICE_DATA.clear()
    }

    override suspend fun deleteTask(taskId: String) {
        TASK_SERVICE_DATA.remove(taskId)
    }
}