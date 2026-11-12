package com.gongkao.cuotiben.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gongkao.cuotiben.data.ErrorRecord
import com.gongkao.cuotiben.data.KnowledgePoint
import com.gongkao.cuotiben.model.SubCategory
import com.gongkao.cuotiben.model.Topic
import com.gongkao.cuotiben.ui.components.BankNote
import com.gongkao.cuotiben.ui.components.EmptyState
import com.gongkao.cuotiben.ui.components.formatCount
import com.gongkao.cuotiben.ui.components.PointRow
import com.gongkao.cuotiben.util.TimeText

/**
 * 题型内的知识点清单：按子类分组，顶部可筛选。
 * 分组标题带该子类的题库题量和常见细分。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubModuleScreen(
    topic: Topic,
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    filter: String,
    onFilterChange: (String) -> Unit,
    onCheck: (KnowledgePoint) -> Unit,
    onOpen: (KnowledgePoint) -> Unit,
    onAdd: () -> Unit
) {
    if (points.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DimensionBar()
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Chip(label = "全部", count = 0, selected = true, onClick = { onFilterChange("") })
                topic.subs.forEach { sub ->
                    Chip(
                        label = sub.label,
                        count = 0,
                        selected = false,
                        onClick = { onFilterChange(sub.key) }
                    )
                }
            }
            EmptyState(
                title = "${topic.label}还没有知识点",
                description = "新增时选好子类，清单会自动按子类分组。",
                actionText = "新增第一个知识点",
                onAction = onAdd
            )
        }
        return
    }

    val usedSubs = topic.subs.filter { sub -> points.any { it.subKey == sub.key } }
    val uncategorized = points.filter { p -> topic.subs.none { it.key == p.subKey } }
    val shown = if (filter.isEmpty()) usedSubs else usedSubs.filter { it.key == filter }
    val showUncat = filter.isEmpty() && uncategorized.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { DimensionBar() }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Chip(
                    label = "全部",
                    count = points.size,
                    selected = filter.isEmpty(),
                    onClick = { onFilterChange("") }
                )
                topic.subs.forEach { sub ->
                    Chip(
                        label = sub.label,
                        count = points.count { it.subKey == sub.key },
                        selected = filter == sub.key,
                        onClick = { onFilterChange(if (filter == sub.key) "" else sub.key) }
                    )
                }
            }
        }

        if (shown.isEmpty() && !showUncat) {
            item {
                Text(
                    text = "这个子类下还没有知识点。点下方新增，会自动归到当前选中的子类。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 24.dp)
                )
            }
        }

        shown.forEach { sub ->
            val subPoints = points.filter { it.subKey == sub.key }
            item(key = "h-${sub.key}") {
                SubHeader(
                    title = sub.label,
                    total = sub.total,
                    count = subPoints.size,
                    hint = subHint(sub)
                )
            }
            items(subPoints, key = { it.id }) { point ->
                PointItem(point, records, onCheck, onOpen)
            }
        }

        if (showUncat) {
            item(key = "h-none") {
                SubHeader(
                    title = "未分类",
                    total = 0,
                    count = uncategorized.size,
                    hint = "子类已不在题库中，点进详情可重新归类"
                )
            }
            items(uncategorized, key = { it.id }) { point ->
                PointItem(point, records, onCheck, onOpen)
            }
        }

        item {
            BankNote(text = topic.label + " 题库 " + formatCount(topic.total) + " 题")
        }
    }
}

private fun subHint(sub: SubCategory): String {
    val details = sub.details
    if (details.isNotEmpty()) {
        return details.take(4).joinToString(" · ") { it.name }
    }
    return if (sub.merged > 0) "含 ${sub.merged} 个小类" else ""
}

@Composable
private fun PointItem(
    point: KnowledgePoint,
    records: List<ErrorRecord>,
    onCheck: (KnowledgePoint) -> Unit,
    onOpen: (KnowledgePoint) -> Unit
) {
    val pointRecords = records.filter { it.pointId == point.id }
    PointRow(
        point = point,
        recordCount = pointRecords.size,
        latestText = pointRecords.maxOfOrNull { it.timestamp }?.let { TimeText.format(it) },
        onCheck = { onCheck(point) },
        onOpen = { onOpen(point) }
    )
}

@Composable
private fun DimensionBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "子类",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "点子类可只看这一类；新增时选好子类，会自动归到当前子类",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun Chip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (count > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    color = if (selected) Color.White.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** 子类分组标题：名称、题库题量、我的知识点数、常见细分 */
@Composable
private fun SubHeader(title: String, total: Int, count: Int, hint: String) {
    Column(modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 10.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (total > 0) "$count 个知识点" else "$count 个",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (hint.isNotEmpty()) {
            Text(
                text = hint,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
