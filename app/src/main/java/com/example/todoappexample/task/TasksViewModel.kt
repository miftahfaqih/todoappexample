package com.example.todoappexample.task

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.example.todoappexample.Event
import com.example.todoappexample.R
import com.example.todoappexample.data.Result
import com.example.todoappexample.data.Task
import com.example.todoappexample.data.source.TasksRepository
import com.example.todoappexample.util.wrapEspressoIdlingResource
import kotlinx.coroutines.launch
import javax.inject.Inject

class TasksViewModel @Inject constructor(
    private val tasksRepository: TasksRepository) : ViewModel()  {

    private val _items = MutableLiveData<List<Task>>().apply { value = emptyList() }
    val items: LiveData<List<Task>> = _items

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _currentFilteringLabel = MutableLiveData<Int>()
    val currentFilteringLabel: LiveData<Int> = _currentFilteringLabel

    private val _noTasksLabel = MutableLiveData<Int>()
    val noTasksLabel: LiveData<Int> = _noTasksLabel

    private val _noTasksIconRes = MutableLiveData<Int>()
    val noTaskIconRes: LiveData<Int> = _noTasksIconRes

    private val _tasksAddViewVisible = MutableLiveData<Boolean>()
    val tasksAddViewVisible = _tasksAddViewVisible

    private val _snackbarText = MutableLiveData<Event<Int>>()
    val snackbarText: LiveData<Event<Int>> = _snackbarText

    private var _currentFiltering = TasksFilterType.ALL_TASKS

    private val isDataLoadingError = MutableLiveData<Boolean>()

    private val _openTaskEvent = MutableLiveData<Event<String>>()
    val openTaskEvent: LiveData<Event<String>> = _openTaskEvent

    private val _newTaskEvent = MutableLiveData<Event<Unit>>()
    val newTaskEvent: LiveData<Event<Unit>> = _newTaskEvent

    val empty: LiveData<Boolean> = Transformations.map(_items){
        it.isEmpty()
    }

    init {
        setFiltering(TasksFilterType.ALL_TASKS)
        loadTasks(true)
    }

    fun setFiltering(requestType: TasksFilterType) {
        _currentFiltering = requestType

        when(requestType){
            TasksFilterType.ALL_TASKS -> {
                setFilter(
                    R.string.label_all, R.string.no_tasks_all,
                    R.drawable.logo_no_fill, true
                )
            }
            TasksFilterType.ACTIVE_TASKS -> {
                setFilter(
                    R.string.label_active, R.string.no_tasks_active,
                    R.drawable.ic_check_circle_96dp, false
                )
            }
            TasksFilterType.COMPLETED_TASKS -> {
                setFilter(
                    R.string.label_completed, R.string.no_tasks_completed,
                    R.drawable.ic_verified_user_96dp, false
                )
            }
        }
    }

    fun clearCompletedTasks(){
        viewModelScope.launch {
            tasksRepository.clearCompletedTasks()
            showSnackBarMessage(R.string.completed_tasks_cleared)
            loadTasks(false)
        }
    }

    fun completeTask(task: Task, completed: Boolean) = viewModelScope.launch {
        if(completed){
            tasksRepository.completeTask(task)
        }else{
            tasksRepository.activateTask(task)
        }
        loadTasks(false)
    }

    private fun setFilter(@StringRes filteringLabelString: Int, @StringRes noTasksLabelString: Int,
                          @DrawableRes noTasksIconDraw: Int, tasksAddVisible: Boolean) {
        _currentFilteringLabel.value = filteringLabelString
        _noTasksLabel.value = noTasksLabelString
        _noTasksIconRes.value = noTasksIconDraw
        tasksAddViewVisible.value = tasksAddVisible

    }

    fun loadTasks(forceUpdate: Boolean) {
        _dataLoading.value = true

        wrapEspressoIdlingResource {
            viewModelScope.launch {
                val tasksResult = tasksRepository.getTasks(forceUpdate)

                if(tasksResult is Result.Success){
                    val tasks = tasksResult.data

                    val tasksToShow = ArrayList<Task>()

                    for(task in tasks){
                        when(_currentFiltering){
                            TasksFilterType.ALL_TASKS -> tasksToShow.add(task)
                            TasksFilterType.ACTIVE_TASKS -> if(task.isActive){
                                tasksToShow.add(task)
                            }
                            TasksFilterType.COMPLETED_TASKS -> if(task.isCompleted){
                                tasksToShow.add(task)
                            }
                        }
                    }
                    isDataLoadingError.value = false
                    _items.value = ArrayList(tasksToShow)
                }else{
                    isDataLoadingError.value = true
                    _items.value = emptyList()
                    showSnackBarMessage(R.string.loading_tasks_error)
                }

                _dataLoading.value = false
            }
        }

    }

    fun showEditResultMessage(result: Int) {
        when (result) {
            EDIT_RESULT_OK -> showSnackBarMessage(R.string.successfully_saved_task_message)
            ADD_EDIT_RESULT_OK -> showSnackBarMessage(R.string.successfully_added_task_message)
            DELETE_RESULT_OK -> showSnackBarMessage(R.string.successfully_deleted_task_message)
        }
    }

    private fun showSnackBarMessage(message: Int) {
        _snackbarText.value = Event(message)
    }

    fun refresh(){
        loadTasks(true)
    }


    /**
     * Called by the Data Binding library and the FAB's click listener.
     */
    fun addNewTask() {
        _newTaskEvent.value = Event(Unit)
    }

    /**
     * Called by Data Binding.
     */
    fun openTask(taskId: String) {
        _openTaskEvent.value = Event(taskId)
    }


}