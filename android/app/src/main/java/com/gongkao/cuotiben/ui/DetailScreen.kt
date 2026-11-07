package com.gongkao.cuotiben.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gongkao.cuotiben.data.ErrorRecord
import com.gongkao.cuotiben.data.KnowledgePoint
import com.gongkao.cuotiben.ui.components.SectionLabel
import com.gongkao.cuotiben.util.TimeText

@Composable
fun DetailScreen(
    point: KnowledgePoint,
    records: List<ErrorRecord>,
    breadcrumb: String,
    onCheck: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditRecordNote: (ErrorRecord, Int) -> Unit,
    onDeleteRecord: (ErrorRecord, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            HeroCard(
                point = point,
                breadcrumb = breadcrumb,
                recordCount = records.size,
                onCheck = onCheck,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }

        item {
            SectionLabel(
                text = "错误记录 · 由近到远",
                endText = records.lastOrNull()?.let { "首次 " + TimeText.format(it.timestamp) }
            )
        }

        if (records.isEmpty()) {
            item {
                Text(
                    text = "还没有错误记录，返回清单勾选一次即可生成第一条。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 20.dp)
                )
            }
        } else {
            itemsIndexed(records, key = { _, record -> record.id }) { index, record ->
                RecordRow(
                    order = records.size - index,
                    record = record,
                    onEditNote = { onEditRecordNote(record, records.size - index) },
                    onDelete = { onDeleteRecord(record, records.size - index) }
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    point: KnowledgePoint,
    breadcrumb: String,
    recordCount: Int,
    onCheck: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = breadcrumb,
                fontSize = 11.5.sp,
                letterSpacing = 0.3.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = point.name,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFFBF3E2))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(text = "错误原因", fontSize = 11.sp, color = Color(0xFFB4791C))
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    text = point.reason.ifBlank { "未填写具体原因" },
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                StatColumn(
                    value = point.errorCount.toString(),
                    label = "累计错误次数",
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatColumn(
                    value = recordCount.toString(),
                    label = "错误记录条数",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                ActionButton(
                    text = "再记一次错",
                    filled = true,
                    modifier = Modifier.weight(1f),
                    onClick = onCheck
                )
                ActionButton(
                    text = "编辑",
                    filled = false,
                    modifier = Modifier.weight(1f),
                    onClick = onEdit
                )
                ActionButton(
                    text = "删除",
                    filled = false,
                    danger = true,
                    modifier = Modifier.weight(1f),
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val dangerColor = MaterialTheme.colorScheme.error
    val container = when {
        filled -> MaterialTheme.colorScheme.primary
        danger -> Color(0xFFFDF7F7)
        else -> Color(0xFFFBFBFD)
    }
    val contentColor = when {
        filled -> Color.White
        danger -> dangerColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = when {
        filled -> null
        danger -> BorderStroke(1.dp, Color(0xFFF2DEDE))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }

    Surface(
        shape = RoundedCornerShape(11.dp),
        color = container,
        contentColor = contentColor,
        border = border,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 单条错误记录：左侧是「第几次」，右侧是时间与更多菜单。
 * 点「⋯」可以给这条记录补备注，或单独删除这一次。
 */
@Composable
private fun RecordRow(
    order: Int,
    record: ErrorRecord,
    onEditNote: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = order.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.width(10.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(start = 13.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "第 $order 次错误",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (record.note.isNotBlank()) {
                        Text(
                            text = record.note,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Text(
                    text = TimeText.format(record.timestamp),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "这条记录的操作",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("添加/修改备注", fontSize = 14.sp) },
                            onClick = {
                                menuOpen = false
                                onEditNote()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "删除这一次",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
