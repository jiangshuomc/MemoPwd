package com.example.memopwd.data.todo

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items WHERE memoId = :memoId ORDER BY sortOrder ASC, id ASC")
    fun getTodosByMemoId(memoId: Long): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE memoId = :memoId ORDER BY sortOrder ASC, id ASC")
    suspend fun getTodosOnce(memoId: Long): List<TodoItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<TodoItem>)

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Query("DELETE FROM todo_items WHERE memoId = :memoId")
    suspend fun deleteByMemoId(memoId: Long)
}
