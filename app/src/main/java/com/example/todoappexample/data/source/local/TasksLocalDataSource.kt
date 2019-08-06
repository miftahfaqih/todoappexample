package com.example.todoappexample.data.source.local

import com.example.todoappexample.data.Result
import com.example.todoappexample.data.Task
import com.example.todoappexample.data.source.TasksDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

class TasksLocalDataSource internal constructor(
    private val tasksDao: TasksDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TasksDataSource{

    override suspend fun getTasks(): Result<List<Task>> = withContext(ioDispatcher) {
        return@withContext try {
            Result.Success(tasksDao.getTasks())
        }catch (e: Exception){
            Result.Error(e)
        }

    }

    override suspend fun getTask(taskId: String): Result<Task>  = withContext(ioDispatcher){
        try {
            val task = tasksDao.getTaskById(taskId)
            if(task!=null){
                return@withContext Result.Success(task)
            }else{
                return@withContext Result.Error(Exception("Task not found"))
            }
        }catch (e: Exception){
            return@withContext Result.Error(e)
        }
    }

    override suspend fun saveTask(task: Task) {
        tasksDao.insertTask(task)
    }

    override suspend fun completeTask(task: Task) {
        tasksDao.updateCompleted(task.id, true)
    }

    override suspend fun completeTask(taskId: String) {
        tasksDao.updateCompleted(taskId,true)
    }

    override suspend fun activateTask(task: Task) {
        tasksDao.updateCompleted(task.id, false)
    }

    override suspend fun activateTask(taskId: String) {
        tasksDao.updateCompleted(taskId,false)
    }

    override suspend fun clearCompletedTasks() {
        tasksDao.deleteCompletedTasks()
    }

    override suspend fun deleteAllTasks() {
        tasksDao.deleteTask()
    }

    override suspend fun deleteTask(taskId: String) {
        tasksDao.deleteTaskById(taskId)
    }
}