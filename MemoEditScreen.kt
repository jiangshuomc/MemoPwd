package com.example.memopwd.ui.memo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.memopwd.data.memo.Memo
import com.example.memopwd.data.todo.TodoItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoEditScreen(
    memoId: Long?,
    viewModel: MemoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories by viewModel.categories.collectAsState()

    var loaded by remember { mutableStateOf(memoId == null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("默认") }
    var isPinned by remember { mutableStateOf(false) }
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var todos by remember { mutableStateOf<List<TodoItem>>(emptyList()) }
    var todoDraft by remember { mutableStateOf("") }

    // 编辑已有备忘录时，从数据库回填
    LaunchedEffect(memoId) {
        if (memoId != null) {
            val memo = viewModel.loadMemo(memoId)
            if (memo != null) {
                title = memo.title
                content = memo.content
                category = memo.category
                isPinned = memo.isPinned
                imageUris = memo.imageUris
                todos = viewModel.loadTodos(memoId)
            }
            loaded = true
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // 持久化读取权限，重启后仍能显示
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // 部分 provider 不支持持久化，忽略即可
                }
            }
            imageUris = imageUris + uris.map { it.toString() }
        }
    }

    if (!loaded) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (memoId == null) "新建备忘录" else "编辑备忘录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            "置顶",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        val memo = Memo(
                            id = memoId ?: 0,
                            title = title.trim(),
                            content = content.trim(),
                            imageUris = imageUris,
                            category = category,
                            isPinned = isPinned
                        )
                        viewModel.saveMemo(memo, todos) { onBack() }
                    }) {
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("正文") },
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).defaultMinSize(minHeight = 120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // 分类标签
            CategoryEditor(
                current = category,
                suggestions = categories,
                onPick = { category = it }
            )

            // 图片区
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(imageUris, key = { it }) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { imageUris = imageUris - uri },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Close, "移除",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    FilledTonalIconButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(84.dp)
                    ) {
                        Icon(Icons.Outlined.Image, "添加图片")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 待办清单
            Text("待办", style = MaterialTheme.typography.labelLarge)
            todos.forEach { todo ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = todo.isDone,
                        onCheckedChange = {
                            todos = todos.map { t ->
                                if (t.id == todo.id) t.copy(isDone = it) else t
                            }
                        }
                    )
                    Text(
                        todo.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (todo.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { todos = todos.filter { it.id != todo.id } }) {
                        Icon(Icons.Default.DeleteOutline, "删除待办", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = todoDraft,
                    onValueChange = { todoDraft = it },
                    placeholder = { Text("新增待办事项") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = {
                        if (todoDraft.isNotBlank()) {
                            todos = todos + TodoItem(
                                memoId = memoId ?: 0,
                                text = todoDraft.trim(),
                                sortOrder = todos.size
                            )
                            todoDraft = ""
                        }
                    },
                    enabled = todoDraft.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, "添加待办")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CategoryEditor(
    current: String,
    suggestions: List<String>,
    onPick: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(current) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        AssistChip(
            onClick = { editing = !editing },
            label = { Text(current) },
            leadingIcon = { Icon(Icons.Filled.Label, null, modifier = Modifier.size(14.dp)) }
        )
        if (editing) {
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("新分类名") },
                trailingIcon = {
                    IconButton(onClick = {
                        if (draft.isNotBlank()) {
                            onPick(draft.trim())
                            editing = false
                        }
                    }) {
                        Icon(Icons.Default.Check, "确定", modifier = Modifier.size(18.dp))
                    }
                }
            )
        } else if (suggestions.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(suggestions.take(5)) { cat ->
                    FilterChip(
                        selected = cat == current,
                        onClick = { onPick(cat) },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}
