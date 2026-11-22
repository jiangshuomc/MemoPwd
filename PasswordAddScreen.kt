package com.example.memopwd.ui.password

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.memopwd.data.password.PasswordPlain
import com.example.memopwd.data.password.PasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordAddScreen(
    entryId: Long?,
    viewModel: PasswordViewModel,
    initialTitle: String = "",
    initialCategory: String = "默认",
    initialPlain: PasswordPlain? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()

    var title by remember { mutableStateOf(initialTitle) }
    var category by remember { mutableStateOf(initialCategory) }
    var username by remember { mutableStateOf(initialPlain?.username ?: "") }
    var password by remember { mutableStateOf(initialPlain?.password ?: "") }
    var note by remember { mutableStateOf(initialPlain?.note ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank() && (username.isNotBlank() || password.isNotBlank())

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) "新增账号" else "编辑账号") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.save(
                                entryId = entryId ?: 0L,
                                title = title.trim(),
                                category = category.trim().ifBlank { "默认" },
                                plain = PasswordPlain(
                                    username = username.trim(),
                                    password = password,
                                    note = note.trim()
                                ),
                                onDone = onBack
                            )
                        },
                        enabled = canSave
                    ) {
                        Icon(Icons.Default.Check, "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("名称") },
                placeholder = { Text("如：GitHub、招商银行") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("账号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (showPassword) "隐藏密码" else "显示密码"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                shape = RoundedCornerShape(12.dp)
            )

            CategoryField(
                current = category,
                suggestions = categories,
                onPick = { category = it }
            )

            Text(
                "保存后内容由系统密钥库加密存储，只有生物识别解锁后才能查看。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(
    current: String,
    suggestions: List<String>,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = onPick,
            label = { Text("分类") },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        if (suggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                suggestions.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            onPick(cat)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
