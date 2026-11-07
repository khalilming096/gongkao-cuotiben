package com.gongkao.cuotiben.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gongkao.cuotiben.data.KnowledgePoint
import com.gongkao.cuotiben.model.Catalog
import com.gongkao.cuotiben.model.StudyModule
import com.gongkao.cuotiben.model.SubCategory
import com.gongkao.cuotiben.model.Topic

/**
 * 新增 / 编辑知识点：子类 + 名称 + 错误原因。
 * 子类、错因、示例文案都随题型变化。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PointEditorDialog(
    module: StudyModule,
    topic: Topic?,
    initialSubKey: String,
    existing: KnowledgePoint?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, reason: String, subKey: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var reason by remember { mutableStateOf(existing?.reason ?: "") }
    var subKey by remember {
        mutableStateOf(
            existing?.subKey?.takeIf { it.isNotEmpty() }
                ?: initialSubKey.takeIf { it.isNotEmpty() }
                ?: topic?.subs?.firstOrNull()?.key.orEmpty()
        )
    }

    val selectedSub: SubCategory? = topic?.subs?.firstOrNull { it.key == subKey }
    val targetLabel = if (topic != null) "${module.label} · ${topic.label}" else module.label
    val canSave = name.isNotBlank() && (topic == null || subKey.isNotEmpty())
    val (ownReasons, commonReasons) = Catalog.reasonsFor(module, topic?.key.orEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = if (existing == null) "新增知识点" else "编辑知识点",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (existing == null) "添加到「$targetLabel」" else "修改子类、名称或错误原因",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                if (topic != null) {
                    Text(
                        text = "子类",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        topic.subs.forEach { sub ->
                            KeyChip(
                                label = sub.label,
                                badge = sub.total.toString(),
                                selected = subKey == sub.key,
                                onClick = { subKey = sub.key }
                            )
                        }
                    }
                    val basis = when {
                        selectedSub == null -> ""
                        selectedSub.details.isNotEmpty() ->
                            "${selectedSub.total} 题 · 常见细分：" +
                                selectedSub.details.take(5).joinToString("、") { it.name }
                        selectedSub.merged > 0 ->
                            "${selectedSub.total} 题 · 含 ${selectedSub.merged} 个小类"
                        else -> "${selectedSub.total} 题"
                    }
                    Text(
                        text = basis,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    text = "知识点名称",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = Catalog.namePlaceholder(module, selectedSub),
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                )

                val details = selectedSub?.details.orEmpty().take(5)
                if (details.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        details.forEach { detail ->
                            OfferChip(text = detail.name, onClick = { name = detail.name })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "错误原因",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 120) reason = it },
                    minLines = 2,
                    maxLines = 4,
                    placeholder = {
                        Text("如：" + Catalog.hintFor(module, topic?.key.orEmpty()), fontSize = 14.sp)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ownReasons.forEach { item ->
                        ReasonChipButton(
                            text = item,
                            own = true,
                            onClick = { reason = appendReason(reason, item) }
                        )
                    }
                    commonReasons.forEach { item ->
                        ReasonChipButton(
                            text = item,
                            own = false,
                            onClick = { reason = appendReason(reason, item) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onConfirm(name, reason, subKey) }
            ) {
                Text("保存", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

private fun appendReason(current: String, item: String): String {
    val text = current.trim()
    return when {
        text.isEmpty() -> item
        text.contains(item) -> text
        else -> "$text，$item"
    }
}

/** 子类胶囊，右侧带题量 */
@Composable
private fun KeyChip(label: String, badge: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.padding(2.dp))
            Text(
                text = badge,
                fontSize = 10.5.sp,
                color = if (selected) Color.White.copy(alpha = 0.75f)
                else MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** 名称推荐胶囊 */
@Composable
private fun OfferChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/** 错因胶囊：专属用主色，通用用灰色 */
@Composable
private fun ReasonChipButton(text: String, own: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (own) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (own) FontWeight.Medium else FontWeight.Normal,
            color = if (own) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 给某一次错误记录补备注与题号 */
@Composable
fun RecordNoteDialog(
    order: Int,
    timeText: String,
    existingNote: String,
    existingQid: String,
    onDismiss: () -> Unit,
    onConfirm: (note: String, qid: String) -> Unit
) {
    var note by remember { mutableStateOf(existingNote) }
    var qid by remember { mutableStateOf(existingQid) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "第 $order 次错误",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "记录时间 $timeText",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "题库题号（选填）",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = qid,
                    onValueChange = { if (it.length <= 20) qid = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    placeholder = { Text("如：10204478", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                )
                Text(
                    text = "填了以后可以拿这串号去题库里翻原题",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "备注（这道题错在哪）",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 120) note = it },
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("如：选项里「不」字看漏了", fontSize = 13.5.sp) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note, qid) }) {
                Text("保存", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/** 删除单条错误记录的确认 */
@Composable
fun DeleteRecordDialog(
    order: Int,
    timeText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("删除第 $order 次记录", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(
                text = "这条记录的时间是 $timeText。删除后累计错误次数会减 1，其余记录的「第几次」会重新排列，此操作不可恢复。",
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/** 删除知识点确认 */
@Composable
fun DeletePointDialog(
    point: KnowledgePoint,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("删除知识点", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(
                text = "删除「${point.name}」后，它的 ${point.errorCount} 条累计记录也会一并清除，此操作不可恢复。",
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("使用说明", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InfoLine("① 五个模块固定；言语理解、判断推理、资料分析的题型与子类取自你的题库，共 14 个题型、78 个子类、18,011 题。")
                InfoLine("② 子类后面的数字是该子类的真实题量，题量大的更值得先补。")
                InfoLine("③ 新增时先选子类，名称可以从「常见细分」里点选；错因会按题型给出专属选项。")
                InfoLine("④ 清单左侧圆圈勾选一次＝又错了一次，误触可在提示条里撤销。")
                InfoLine("⑤ 详情页每条记录可填题库题号，方便回题库翻原题。")
                InfoLine("⑥ 政治理论、常识没有题库分类，直接用模块级错因记录。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun InfoLine(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
