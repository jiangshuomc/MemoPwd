package com.example.memopwd.data.memo

import com.example.memopwd.data.AppDatabase
import com.example.memopwd.data.todo.TodoItem
import kotlinx.coroutines.flow.Flow

class MemoRepository(database: AppDatabase) {

    private val memoDao = database.memoDao()
    private val todoDao = database.todoDao()

    fun getAllMemos(): Flow<List<Memo>> = memoDao.getAllMemos()
    fun searchMemos(keyword: String): Flow<List<Memo>> = memoDao.searchMemos(keyword)
    fun getAllCategories(): Flow<List<String>> = memoDao.getAllCategories()
    suspend fun getMemoById(id: Long): Memo? = memoDao.getMemoById(id)
    fun observeTodos(memoId: Long): Flow<List<TodoItem>> = todoDao.getTodosByMemoId(memoId)
    suspend fun getTodos(memoId: Long): List<TodoItem> = todoDao.getTodosOnce(memoId)

    suspend fun saveMemo(memo: Memo, todos: List<TodoItem>): Long {
        val memoId = if (memo.id == 0L) memoDao.insertMemo(memo) else {
            memoDao.updateMemo(memo.copy(updatedAt = System.currentTimeMillis()))
            memo.id
        }
        todoDao.deleteByMemoId(memoId)
        todos.forEachIndexed { index, item ->
            todoDao.insertTodo(item.copy(memoId = memoId, sortOrder = index))
        }
        return memoId
    }

    suspend fun deleteMemo(memo: Memo) {
        todoDao.deleteByMemoId(memo.id)
        memoDao.deleteMemo(memo)
    }

    suspend fun toggleTodo(todo: TodoItem) {
        todoDao.updateTodo(todo.copy(isDone = !todo.isDone))
    }
}
