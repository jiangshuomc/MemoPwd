package com.example.memopwd.data.password

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.memopwd.data.AppDatabase
import com.example.memopwd.security.CryptoManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PasswordRepository(
        AppDatabase.getInstance(application),
        CryptoManager(application)
    )

    private val keyword = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<String?>(null)

    fun setKeyword(value: String) { keyword.value = value }
    fun setCategory(value: String?) { categoryFilter.value = value }

    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val passwords: StateFlow<List<PasswordEntry>> = keyword
        .debounce(250)
        .flatMapLatest { kw ->
            if (kw.isBlank()) repository.getAllPasswords() else repository.searchPasswords(kw)
        }
        .combine(categoryFilter) { list, cat ->
            if (cat.isNullOrBlank() || cat == "全部") list else list.filter { it.category == cat }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(entryId: Long, title: String, category: String, plain: PasswordPlain, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.savePassword(entryId, title, category, plain)
            onDone()
        }
    }

    fun delete(entry: PasswordEntry) = viewModelScope.launch {
        repository.deletePassword(entry)
    }

    fun reveal(entry: PasswordEntry): PasswordPlain = repository.reveal(entry)

    /** 复制密码到剪贴板，30 秒后自动清空，避免长期残留在剪贴板里 */
    fun copySensitive(value: String) {
        val app = getApplication<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("password", value))
        viewModelScope.launch {
            kotlinx.coroutines.delay(30_000)
            val current = clipboard.primaryClip
            if (current != null && current.itemCount > 0 &&
                current.getItemAt(0).text?.toString() == value
            ) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
}
