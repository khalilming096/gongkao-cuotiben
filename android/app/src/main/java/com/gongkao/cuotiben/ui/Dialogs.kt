package com.gongkao.cuotiben.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gongkao.cuotiben.data.KnowledgePoint
import com.gongkao.cuotiben.model.QUICK_REASONS
import com.gongkao.cuotiben.model.SubGroup

/**
 * 新增 / 编辑知识点：子类（有子模块时）+ 名称 + 错误原因。
 * 编辑时改动子类会把这条知识点在清单里换组，累计次数与历史记录不变。
 */
@Composable
fun PointEditorDialog(
    moduleLabel: String,
    group: SubGroup?,
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
                ?: group?.categories?.firstOrNull()?.key.orEmpty()
        )
    }

    val targetLabel = if (group != null) "${moduleLabel} · ${group.label}" else moduleLabel
    val canSave = name.isNotBlank() && (group == null || subKey.isNotEmpty())

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

                if (group != null) {
                    Text(
                        text = "子类",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        group.categories.forEach { category ->
                            SubChip(
                                label = category.label,
                                selected = subKey == category.key,
                                onClick = { subKey = category.key }
                            )
                        }
                    }
                    Text(
                        text = "判定依据：" + (group.categories.firstOrNull { it.key == subKey }?.basis ?: ""),
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
                    placeholder = { Text("如：成语褒贬误用", fontSize = 14.sp) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                )

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
                    placeholder = { Text("如：把贬义词当成中性词用", fontSize = 14.sp) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    QUICK_REASONS.forEach { quick ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    reason = if (reason.isBlank()) quick
                                    else if (reason.contains(quick)) reason
                                    else "$reason，$quick"
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = quick,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

/** 子类选择小胶囊 */
@Composable
private fun SubChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 删除确认：会连带清除该知识点的全部错误记录 */
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
                InfoLine("① 主界面固定五个模块；言语理解、判断推理下面还有一层子模块。")
                InfoLine("② 子模块按题型划分，子类按解法差异划分，分组标题上写着该子类的判定依据。")
                InfoLine("③ 底部「新增知识点」先选子类再填名称与错误原因，清单会自动按子类分组。")
                InfoLine("④ 清单左侧圆圈勾选一次＝又错了一次；点错了可在下方提示条里点「撤销」。")
                InfoLine("⑤ 详情页每条记录右侧「⋯」可以补备注，也可以单独删掉这一次。")
                InfoLine("⑥ 政治理论、常识、资料分析考点线性、题量集中，不设子模块，直接在模块下记知识点。")
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

/** 给某一次错误记录补备注 */
@Composable
fun RecordNoteDialog(
    order: Int,
    timeText: String,
    existingNote: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf(existingNote) }

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
                    text = "记录时间 $timeText · 可补写这道题错在哪",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 120) note = it },
                minLines = 3,
                maxLines = 5,
                placeholder = {
                    Text("如：2023 联考 A 卷 第 12 题，选项里「不」字看漏了", fontSize = 13.5.sp)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }) {
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
