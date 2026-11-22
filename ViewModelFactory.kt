package com.example.memopwd.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.memopwd.data.AppDatabase
import com.example.memopwd.data.memo.MemoRepository
import com.example.memopwd.data.memo.MemoViewModel

/**
 * 手动装配依赖的 ViewModel 工厂。
 * 项目不引入 Hilt，避免再叠加一个注解处理器，降低编译失败概率。
 */
object ViewModelFactory {

    fun create(application: Application): ViewModelProvider.Factory {
        val memoRepository = MemoRepository(AppDatabase.getInstance(application))

        return viewModelFactory {
            initializer {
                MemoViewModel(memoRepository)
            }
        }
    }
}
