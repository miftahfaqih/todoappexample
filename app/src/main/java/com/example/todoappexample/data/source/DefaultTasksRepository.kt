package com.example.todoappexample.data.source

import com.example.todoappexample.data.Result
import com.example.todoappexample.data.Task
import com.example.todoappexample.di.ApplicationModule
import javax.inject.Inject
import com.example.todoappexample.di.ApplicationModule.TasksRemoteDataSource
import com.example.todoappexample.di.ApplicationModule.TasksLocalDataSource
import com.example.todoappexample.util.EspressoIdlingResource
import com.example.todoappexample.util.wrapEspressoIdlingResource
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Error
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 * To simplify the sample, this repository only uses the local data source only if the remote
 * data source fails. Remote is the source of truth.
 */
class DefaultTasksRepository @Inject constructor(
    @TasksRemoteDataSource private val tasksRemoteDataSource: TasksDataSource,
    @TasksLocalDataSource private val tasksLocalDataSource: TasksDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : TasksRepository {

    private var cachedTasks: ConcurrentMap<String, Task>? = null

    override suspend fun getTasks(forceUpdate: Boolean): Result<List<Task>> {
        wrapEspressoIdlingResource {

            return withContext(ioDispatcher){
                if(!forceUpdate){
                    cachedTasks?.let {
                        return@withContext Result.Success(it.values.sortedBy { it.id })
                    }
                }
                val newTasks = fetchTasksFromRemoteOrLocal(forceUpdate)

                (newTasks as? Result.Success)?.let { refreshCache(it.data) }

                cachedTasks?.values?.let { tasks ->
                    return@withContext Result.Success(tasks.sortedBy { it.id })
                }

                (newTasks as? Result.Success)?.let {
                    if(it.data.isEmpty()){
                        return@withContext Result.Success(it.data)
                    }
                }

                return@withContext Result.Error(Exception("Illegal State"))
            }
        }
    }

    private suspend fun fetchTasksFromRemoteOrLocal(forceUpdate: Boolean): Result<List<Task>> {
        val remoteTasks = tasksRemoteDataSource.getTasks()
        when(remoteTasks){
            is Result.Error -> Timber.w("Remote data source fetch failed")
            is Result.Success -> {
                refreshLocalDataSource(remoteTasks.data)
                return remoteTasks
            }
            else ->throw IllegalStateException()
        }

        if(forceUpdate){
            return Result.Error(Exception("Can't force refresh: Remote data source is unavailable"))
        }

        val localTasks = tasksLocalDataSource.getTasks()
        if(localTasks is Result.Success) return localTasks
        return Result.Error(Exception("Error fetching from remote and local"))

    }

    override suspend fun getTask(taskId: String, forceUpdate: Boolean): Result<Task> {
        wrapEspressoIdlingResource {
            return withContext(ioDispatcher){
                if(forceUpdate){
                    getTaskWithId(taskId)?.let{
                        EspressoIdlingResource.decrement()
                        return@withContext Result.Success(it)
                    }
                }

                val newTask = fetchTaskFromRemoteOrLocal(taskId,forceUpdate)

                (newTask as? Result.Success)?.let { cacheTask(it.data) }

                return@withContext newTask
            }
        }
    }

    private suspend fun fetchTaskFromRemoteOrLocal(taskId: String, forceUpdate: Boolean): Result<Task> {
        val remoteTask = tasksRemoteDataSource.getTask(taskId)
        when(remoteTask){
            is Result.Error -> Timber.w("Remote data source fetch failed")
            is Result.Success -> {
                refreshLocalDataSource(remoteTask.data)
                return remoteTask
            }
            else -> throw IllegalStateException()
        }

        if(forceUpdate){
            return Result.Error(Exception("Refresh failed"))
        }

        val localTasks = tasksLocalDataSource.getTask(taskId)
        if(localTasks is Result.Success) return localTasks
        return Result.Error(Exception("Error fetching from remote or local"))
    }

    override suspend fun saveTask(task: Task) {
        cacheAndPerform(task){
            coroutineScope {
                launch { tasksRemoteDataSource.saveTask(task) }
                launch { tasksLocalDataSource.saveTask(task) }
            }
        }
    }

    override suspend fun completeTask(task: Task) {
        cacheAndPerform(task){
            it.isCompleted = true
            coroutineScope {
                launch { tasksRemoteDataSource.completeTask(task) }
                launch { tasksLocalDataSource.completeTask(task) }
            }
        }
    }

    override suspend fun completeTask(taskId: String) {
        withContext(ioDispatcher){
            getTaskWithId(taskId)?.let {
                completeTask(taskId)
            }
        }
    }

    override suspend fun activateTask(task: Task) {
        cacheAndPerform(task){
            it.isCompleted = false
            coroutineScope {
                launch { tasksRemoteDataSource.activateTask(task) }
                launch { tasksLocalDataSource.activateTask(task) }
            }
        }
    }

    override suspend fun activateTask(taskId: String) {
        withContext(ioDispatcher){
            getTaskWithId(taskId)?.let {
                activateTask(taskId)
            }
        }
    }

    override suspend fun clearCompletedTasks() {
        coroutineScope{
            launch { tasksRemoteDataSource.clearCompletedTasks() }
            launch { tasksLocalDataSource.clearCompletedTasks() }
        }
        withContext(ioDispatcher){
            cachedTasks?.entries?.removeAll{ it.value.isCompleted}
        }
    }

    override suspend fun deleteAllTasks() {
        coroutineScope {
            launch { tasksRemoteDataSource.deleteAllTasks() }
            launch { tasksLocalDataSource.deleteAllTasks() }
        }

        cachedTasks?.clear()
    }

    override suspend fun deleteTask(taskId: String) {
        coroutineScope {
            launch { tasksRemoteDataSource.deleteTask(taskId) }
            launch { tasksLocalDataSource.deleteTask(taskId) }
        }

        cachedTasks?.remove(taskId)
    }

    private fun getTaskWithId(id: String) = cachedTasks?.get(id)

    private fun refreshCache(tasks: List<Task>) {
        cachedTasks?.clear()
        tasks.sortedBy { it.id }.forEach {
            cacheAndPerform(it) {}
        }
    }

    private suspend fun refreshLocalDataSource(tasks: List<Task>) {
        tasksLocalDataSource.deleteAllTasks()
        for(task in tasks){
            tasksLocalDataSource.saveTask(task)
        }
    }

    private suspend fun refreshLocalDataSource(task: Task){
        tasksLocalDataSource.saveTask(task)
    }

    private inline fun cacheAndPerform(task: Task, perform: (Task) -> Unit) {
        val cachedTask = cacheTask(task)
        perform(cachedTask)
    }

    private fun cacheTask(task: Task): Task {
        val cachedTask = Task(task.title, task.description, task.isCompleted, task.id)
        // Create if it doesn't exist.
        if (cachedTasks == null) {
            cachedTasks = ConcurrentHashMap()
        }
        cachedTasks?.put(cachedTask.id, cachedTask)
        return cachedTask
    }

}