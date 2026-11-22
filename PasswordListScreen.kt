package com.example.memopwd.ui.password

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.memopwd.data.password.PasswordEntry
import com.example.memopwd.data.password.PasswordPlain
import com.example.memopwd.data.password.PasswordViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordListScreen(
    viewModel: PasswordViewModel,
    onAdd: () -> Unit,
    onEdit: (PasswordEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val passwords by viewModel.passwords.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var keyword by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var copiedHint by remember { mutableStateOf(false) }

    LaunchedEffect(copiedHint) {
        if (copiedHint) {
            delay(1500)
            copiedHint = false
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = keyword,
                onValueChange = {
                    keyword = it
                    viewModel.setKeyword(it)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("搜索账号") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == "全部",
                        onClick = {
                            selectedCategory = "全部"
                            viewModel.setCategory(null)
                        },
                        label = { Text("全部") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            viewModel.setCategory(cat)
                        },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (passwords.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "还没有保存的账号，点右下角 + 新增",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(passwords, key = { it.id }) { entry ->
                    PasswordCard(
                        entry = entry,
                        viewModel = viewModel,
                        onEdit = { onEdit(entry) },
                        onCopied = { copiedHint = true }
                    )
                }
            }
        }

        if (copiedHint) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            ) { Text("已复制，30 秒后自动清空剪贴板") }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, "新增账号", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun PasswordCard(
    entry: PasswordEntry,
    viewModel: PasswordViewModel,
    onEdit: () -> Unit,
    onCopied: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }

    // 显示 30 秒后自动重新遮住
    LaunchedEffect(revealed) {
        if (revealed) {
            delay(30_000)
            revealed = false
        }
    }

    // 只在 revealed 为 true 时解密，收起即丢弃明文引用
    val plain: PasswordPlain? = remember(revealed, entry.id, entry.cipherText) {
        if (revealed) viewModel.reveal(entry) else null
    }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        if (revealed) "隐藏" else "查看",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                entry.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (plain != null) {
                Spacer(Modifier.height(10.dp))
                MaskedRow(label = "账号", value = plain.username) {
                    viewModel.copySensitive(plain.username)
                    onCopied()
                }
                MaskedRow(label = "密码", value = plain.password, masked = true) {
                    viewModel.copySensitive(plain.password)
                    onCopied()
                }
                if (plain.note.isNotBlank()) {
                    MaskedRow(label = "备注", value = plain.note) {
                        viewModel.copySensitive(plain.note)
                        onCopied()
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Text(
                    "••••••••  ••••••••",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MaskedRow(
    label: String,
    value: String,
    masked: Boolean = false,
    onCopy: () -> Unit
) {
    if (value.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(
            "$label：",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (masked) "•".repeat(value.length.coerceIn(6, 12)) else value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.ContentCopy, "复制$label",
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
