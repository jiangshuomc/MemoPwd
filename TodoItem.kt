package com.example.memopwd.data.todo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_items",
    indices = [Index(value = ["memoId"])]
)
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoId: Long,
    val text: String,
    val isDone: Boolean = false,
    val sortOrder: Int = 0
)
