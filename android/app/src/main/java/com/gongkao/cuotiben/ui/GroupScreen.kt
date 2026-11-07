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
import com.gongkao.cuotiben.model.Catalog
import com.gongkao.cuotiben.model.StudyModule
import com.gongkao.cuotiben.model.Topic
import com.gongkao.cuotiben.ui.components.EmptyState
import com.gongkao.cuotiben.ui.components.PointRow
import com.gongkao.cuotiben.ui.components.SectionLabel

/**
 * 题型列表：言语理解、判断推理、资料分析三个模块的第二层，
 * 题型与子类来自题库分类表。
 */
@Composable
fun GroupScreen(
    module: StudyModule,
    topics: List<Topic>,
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    onOpenTopic: (Topic) -> Unit,
    onOpenLoose: () -> Unit
) {
    val looseCount = points.count { it.moduleKey == module.key && it.groupKey.isEmpty() }
    val catalogTotal = Catalog.module(module.key)?.total ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionLabel(text = "选择题型", endText = "题库共 $catalogTotal 题")
        }
        items(topics, key = { it.key }) { topic ->
            val topicPoints = points.filter { it.moduleKey == module.key && it.groupKey == topic.key }
            val errorCount = errorCountOfTopic(points, records, module.key, topic.key)
            TopicCard(
                module = module,
                topic = topic,
                pointCount = topicPoints.size,
                errorCount = errorCount,
                onClick = { onOpenTopic(topic) }
            )
        }
        if (looseCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenLoose)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 15.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFFEDEFF4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "未",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF8E95A3)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "未归类",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "早期数据或题库里找不到的分类",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$looseCount 个知识点",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点进去可重新归类",
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
        }
    }
}

@Composable
private fun TopicCard(
    module: StudyModule,
    topic: Topic,
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
                    text = topic.label.take(1),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = module.mainColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${topic.subs.size} 个子类 · ${topic.total} 题",
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

/** 某个题型下的累计错误次数 */
fun errorCountOfTopic(
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    moduleKey: String,
    topicKey: String
): Int {
    val ids = points
        .filter { it.moduleKey == moduleKey && it.groupKey == topicKey }
        .map { it.id }
        .toSet()
    return records.count { it.pointId in ids }
}

/** 未归类：早期数据或题库里找不到的分类，点进详情可重新归类 */
@Composable
fun LooseScreen(
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    onCheck: (KnowledgePoint) -> Unit,
    onOpen: (KnowledgePoint) -> Unit
) {
    if (points.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            EmptyState(
                title = "没有未归类知识点",
                description = "所有知识点都已经对上题库分类了。"
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "这些知识点没有对应到题库分类，点开详情可以重新选题型与子类。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
        items(points, key = { it.id }) { point ->
            val pointRecords = records.filter { it.pointId == point.id }
            PointRow(
                point = point,
                recordCount = pointRecords.size,
                latestText = pointRecords.maxOfOrNull { it.timestamp }
                    ?.let { com.gongkao.cuotiben.util.TimeText.format(it) },
                onCheck = { onCheck(point) },
                onOpen = { onOpen(point) }
            )
        }
    }
}
