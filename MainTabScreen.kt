package com.example.memopwd.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.memopwd.data.memo.MemoViewModel
import com.example.memopwd.data.password.PasswordViewModel
import com.example.memopwd.data.password.PasswordPlain
import com.example.memopwd.security.BiometricAuthManager
import com.example.memopwd.security.CryptoManager
import com.example.memopwd.ui.ViewModelFactory
import com.example.memopwd.ui.memo.MemoEditScreen
import com.example.memopwd.ui.memo.MemoListScreen
import com.example.memopwd.ui.password.PasswordAddScreen
import com.example.memopwd.ui.password.PasswordListScreen
import kotlinx.coroutines.launch

@Composable
fun MainTabScreen(
    biometricAuthManager: BiometricAuthManager,
    cryptoManager: CryptoManager,
    onLockLost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val memoViewModel: MemoViewModel = viewModel(
        factory = ViewModelFactory.create(application)
    )
    val passwordViewModel: PasswordViewModel = viewModel()

    var selectedTab by remember { mutableStateOf(0) }
    // 密码区是否已解锁；切走即重新上锁
    var unlocked by remember { mutableStateOf(false) }
    var unverified by remember { mutableStateOf(false) }

    // 备忘录编辑页：null 表示列表，Long? 用 -1L 代表新建
    var editingMemoId by remember { mutableStateOf<Long?>(null) }
    var creatingMemo by remember { mutableStateOf(false) }

    // 密码表单页
    var passwordFormOpen by remember { mutableStateOf(false) }
    var editingEntryId by remember { mutableStateOf<Long?>(null) }
    var editingInitial by remember { mutableStateOf<Triple<String, String, PasswordPlain>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun showError(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            creatingMemo || editingMemoId != null -> {
                MemoEditScreen(
                    memoId = editingMemoId,
                    viewModel = memoViewModel,
                    onBack = {
                        creatingMemo = false
                        editingMemoId = null
                    }
                )
            }

            passwordFormOpen -> {
                PasswordAddScreen(
                    entryId = editingEntryId,
                    viewModel = passwordViewModel,
                    initialTitle = editingInitial?.first ?: "",
                    initialCategory = editingInitial?.second ?: "默认",
                    initialPlain = editingInitial?.third,
                    onBack = {
                        passwordFormOpen = false
                        editingEntryId = null
                        editingInitial = null
                    }
                )
            }

            else -> {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Filled.EditNote, null) },
                                label = { Text("备忘录") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = {
                                    requestUnlock(
                                        unlocked = unlocked,
                                        authManager = biometricAuthManager,
                                        cryptoManager = cryptoManager,
                                        onReady = {
                                            unlocked = true
                                            unverified = false
                                            selectedTab = 1
                                        },
                                        onError = { showError(it) },
                                        onLockLost = onLockLost
                                    )
                                },
                                icon = { Icon(Icons.Filled.Lock, null) },
                                label = { Text("密码本") }
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        when (selectedTab) {
                            0 -> MemoListScreen(
                                viewModel = memoViewModel,
                                onEdit = { id ->
                                    if (id == null) creatingMemo = true else editingMemoId = id
                                }
                            )
                            1 -> if (unlocked) {
                                PasswordListScreen(
                                    viewModel = passwordViewModel,
                                    onAdd = {
                                        editingEntryId = null
                                        editingInitial = null
                                        passwordFormOpen = true
                                    },
                                    onEdit = { entry ->
                                        editingEntryId = entry.id
                                        editingInitial = Triple(
                                            entry.title,
                                            entry.category,
                                            passwordViewModel.reveal(entry)
                                        )
                                        passwordFormOpen = true
                                    }
                                )
                            } else {
                                LockedPlaceholder(onTap = {
                                    requestUnlock(
                                        unlocked = false,
                                        authManager = biometricAuthManager,
                                        cryptoManager = cryptoManager,
                                        onReady = {
                                            unlocked = true
                                            unverified = false
                                        },
                                        onError = { showError(it) },
                                        onLockLost = onLockLost
                                    )
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun requestUnlock(
    unlocked: Boolean,
    authManager: BiometricAuthManager,
    cryptoManager: CryptoManager,
    onReady: () -> Unit,
    onError: (String) -> Unit,
    onLockLost: () -> Unit
) {
    if (unlocked && cryptoManager.isKeyReady()) {
        onReady()
        return
    }
    authManager.authenticate(
        onSuccess = {
            if (cryptoManager.isKeyReady()) {
                onReady()
            } else {
                onError("密钥不可用，可能已被系统重置")
                onLockLost()
            }
        },
        onError = onError
    )
}

@Composable
private fun LockedPlaceholder(onTap: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Button(onClick = onTap) {
            Text("点击解锁密码本")
        }
    }
}
