package com.gongkao.cuotiben.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gongkao.cuotiben.data.ErrorRecord
import com.gongkao.cuotiben.data.KnowledgePoint
import com.gongkao.cuotiben.model.SubCategory
import com.gongkao.cuotiben.model.SubGroup
import com.gongkao.cuotiben.ui.components.EmptyState
import com.gongkao.cuotiben.ui.components.PointRow
import com.gongkao.cuotiben.util.TimeText

/**
 * 子模块内的知识点清单：按子类分组展示，顶部可按子类筛选。
 *
 * 分组标题同时展示该子类的判定依据，方便归类和复盘时对照。
 */
@Composable
fun SubModuleScreen(
    group: SubGroup,
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    onCheck: (KnowledgePoint) -> Unit,
    onOpen: (KnowledgePoint) -> Unit,
    onAdd: () -> Unit
) {
    var filter by rememberSaveable(group.key) { mutableStateOf("") }

    if (points.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            EmptyState(
                title = "${group.label}还没有知识点",
                description = "${group.dimension}。\n新增时选好子类，清单就会自动分组。",
                actionText = "新增第一个知识点",
                onAction = onAdd
            )
        }
        return
    }

    // 有知识点的子类才出现在列表里；空子类通过筛选条仍然可见
    val usedCategories = group.categories.filter { cat -> points.any { it.subKey == cat.key } }
    val uncategorized = points.filter { p -> group.categories.none { it.key == p.subKey } }

    val shown = if (filter.isEmpty()) usedCategories else usedCategories.filter { it.key == filter }
    val showUncategorized = filter.isEmpty() && uncategorized.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DimensionBar(group.dimension)
        }

        item {
            FilterBar(
                categories = group.categories,
                selected = filter,
                countOf = { cat -> points.count { it.subKey == cat.key } },
                total = points.size,
                onSelect = { filter = it }
            )
        }

        if (shown.isEmpty() && !showUncategorized) {
            item {
                Text(
                    text = "这个子类下还没有知识点。点右下角新增，会自动归到当前选中的子类。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 24.dp)
                )
            }
        }

        shown.forEach { category ->
            val catPoints = points.filter { it.subKey == category.key }
            item(key = "h-${category.key}") {
                CategoryHeader(
                    title = category.label,
                    count = catPoints.size,
                    basis = category.basis
                )
            }
            items(catPoints, key = { it.id }) { point ->
                PointItem(point, records, onCheck, onOpen)
            }
        }

        if (showUncategorized) {
            item(key = "h-none") {
                CategoryHeader(
                    title = "未分类",
                    count = uncategorized.size,
                    basis = "早期录入或编辑后未指定子类，点进详情可重新归类"
                )
            }
            items(uncategorized, key = { it.id }) { point ->
                PointItem(point, records, onCheck, onOpen)
            }
        }
    }
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

/** 细分维度提示条 */
@Composable
private fun DimensionBar(dimension: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "细分维度",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = dimension,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** 子类筛选条：全部 + 各子类，后面带条数 */
@Composable
private fun FilterBar(
    categories: List<SubCategory>,
    selected: String,
    countOf: (SubCategory) -> Int,
    total: Int,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Chip(
            label = "全部",
            count = total,
            selected = selected.isEmpty(),
            onClick = { onSelect("") }
        )
        categories.forEach { category ->
            Chip(
                label = category.label,
                count = countOf(category),
                selected = selected == category.key,
                onClick = { onSelect(if (selected == category.key) "" else category.key) }
            )
        }
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
            .clip(RoundedCornerShape(9.dp))
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
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) androidx.compose.ui.graphics.Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = count.toString(),
                fontSize = 11.5.sp,
                color = if (selected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f)
                else MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** 子类分组标题：名称 + 条数 + 该子类的判定依据 */
@Composable
private fun CategoryHeader(title: String, count: Int, basis: String) {
    Column(modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 10.dp, bottom = 1.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$count 个",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = basis,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
