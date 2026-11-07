package com.gongkao.cuotiben.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.gongkao.cuotiben.model.StudyModule
import com.gongkao.cuotiben.model.SubGroup
import com.gongkao.cuotiben.ui.components.SectionLabel

/**
 * 子模块列表：言语理解、判断推理两个模块的第二层。
 * 子模块按题型切分，进入后再按子类分组看知识点。
 */
@Composable
fun GroupScreen(
    module: StudyModule,
    groups: List<SubGroup>,
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    onOpenGroup: (SubGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionLabel(
                text = "选择子模块",
                endText = "共 ${groups.size} 个 · 按题型划分"
            )
        }
        items(groups, key = { it.key }) { group ->
            val groupPoints = points.filter { it.moduleKey == module.key && it.groupKey == group.key }
            val errorCount = errorCountOfGroup(points, records, module.key, group.key)
            GroupCard(
                module = module,
                group = group,
                pointCount = groupPoints.size,
                errorCount = errorCount,
                onClick = { onOpenGroup(group) }
            )
        }
    }
}

@Composable
private fun GroupCard(
    module: StudyModule,
    group: SubGroup,
    pointCount: Int,
    errorCount: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 15.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(module.softColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.label.take(1),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = module.mainColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${group.categories.size} 个子类",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = pointCount.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " 个知识点",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
                Text(
                    text = "累计 $errorCount 次错误",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFC8CCD6),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

/** 某个子模块下的累计错误次数 */
fun errorCountOfGroup(
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    moduleKey: String,
    groupKey: String
): Int {
    val ids = points
        .filter { it.moduleKey == moduleKey && it.groupKey == groupKey }
        .map { it.id }
        .toSet()
    return records.count { it.pointId in ids }
}
