package com.example.todoappexample.data.source.local

import androidx.room.*
import com.example.todoappexample.data.Task


@Dao
interface TasksDao {

    /**
     * Select all task from task table
     *
     * @return all task
     */
    @Query("SELECT * FROM Tasks")
    suspend fun getTasks(): List<Task>

    /**
     *Select a task by id
     *
     * @param taskId the task id
     * @return the task with taskId
     */
    @Query("SELECT * FROM Tasks WHERE entryId = :taskId")
    suspend fun getTaskById(taskId: String): Task?

    /**
     *Insert a task in the database. if the task already exists, replace it.
     *
     * @param task the task to be inserted
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    /**
     *Update a task
     *
     * @param task to be updated
     * @return the number of task updated. this should always be 1
     */
    @Update
    suspend fun updateTask(task: Task): Int

    /**
     * update the complete status of the task
     *
     * @param taskId id of the task
     * @param completed status to be updated
     */
    @Query("UPDATE tasks SET completed = :completed WHERE entryId= :taskId")
    suspend fun updateCompleted(taskId: String, completed: Boolean)

    /**
     * delete task by id
     *
     * @return the number of task deleted. This should always be 1
     */
    @Query("DELETE FROM tasks WHERE entryId = :taskId")
    suspend fun deleteTaskById(taskId: String)

    /**
     * delete all task
     */
    @Query("DELETE FROM tasks")
    suspend fun deleteTask()

    /**
     * delete all complete tasks from the table
     *
     * @return the number of tasks deleted
     */
    @Query("DELETE FROM tasks WHERE completed = 1")
    suspend fun deleteCompletedTasks(): Int

}