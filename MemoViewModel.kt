package com.example.memopwd.data.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memopwd.data.todo.TodoItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MemoViewModel(private val repository: MemoRepository) : ViewModel() {

    private val keyword = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<String?>(null)

    fun setKeyword(value: String) { keyword.value = value }
    fun setCategory(value: String?) { categoryFilter.value = value }

    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memos: StateFlow<List<Memo>> = keyword
        .debounce(250)
        .flatMapLatest { kw ->
            if (kw.isBlank()) repository.getAllMemos() else repository.searchMemos(kw)
        }
        .combine(categoryFilter) { list, cat ->
            if (cat.isNullOrBlank() || cat == "全部") list else list.filter { it.category == cat }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveMemo(memo: Memo, todos: List<TodoItem>, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveMemo(memo, todos)
            onDone(id)
        }
    }

    fun deleteMemo(memo: Memo) = viewModelScope.launch {
        repository.deleteMemo(memo)
    }

    fun toggleTodo(todo: TodoItem) = viewModelScope.launch {
        repository.toggleTodo(todo)
    }

    fun observeTodos(memoId: Long): Flow<List<TodoItem>> = repository.observeTodos(memoId)

    suspend fun loadMemo(memoId: Long): Memo? = repository.getMemoById(memoId)

    suspend fun loadTodos(memoId: Long): List<TodoItem> = repository.getTodos(memoId)
}
