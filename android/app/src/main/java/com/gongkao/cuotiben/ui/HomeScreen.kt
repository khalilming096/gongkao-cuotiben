package com.gongkao.cuotiben.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gongkao.cuotiben.data.ErrorRecord
import com.gongkao.cuotiben.data.KnowledgePoint
import com.gongkao.cuotiben.model.Catalog
import com.gongkao.cuotiben.model.StudyModule
import com.gongkao.cuotiben.ui.components.SectionLabel
import com.gongkao.cuotiben.util.TimeText

@Composable
fun HomeScreen(
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    onOpenModule: (StudyModule) -> Unit
) {
    val modulesCovered = StudyModule.values().count { module -> points.any { it.moduleKey == module.key } }
    val latest = records.maxOfOrNull { it.timestamp }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OverviewCard(
                totalErrors = records.size,
                totalPoints = points.size,
                modulesCovered = modulesCovered,
                latestText = latest?.let { TimeText.format(it) } ?: "—"
            )
        }
        item {
            SectionLabel(text = "选择模块", endText = "共 5 个模块")
        }
        items(StudyModule.values().toList()) { module ->
            ModuleCard(
                module = module,
                pointCount = points.count { it.moduleKey == module.key },
                errorCount = errorCountOf(points, records, module.key),
                subGroupCount = Catalog.groupsOf(module).size,
                onClick = { onOpenModule(module) }
            )
        }
    }
}

@Composable
private fun OverviewCard(
    totalErrors: Int,
    totalPoints: Int,
    modulesCovered: Int,
    latestText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF20263A), Color(0xFF2C3550))
                )
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column {
            Text(
                text = "累计错误记录",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.62f)
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = totalErrors.toString(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "次",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 5.dp, bottom = 5.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StatBlock("知识点", "$totalPoints 个")
                StatBlock("覆盖模块", "$modulesCovered / 5")
                StatBlock("最近记录", latestText)
            }
        }
    }
}

@Composable
private fun RowScope.StatBlock(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ModuleCard(
    module: StudyModule,
    pointCount: Int,
    errorCount: Int,
    subGroupCount: Int,
    onClick: () -> Unit
) {
    val hasSubGroups = subGroupCount > 0
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
                    text = module.short,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = module.mainColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = module.subtitle,
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
                        text = if (hasSubGroups) subGroupCount.toString() else pointCount.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasSubGroups) " 个子模块" else " 个知识点",
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

/** 某模块下的累计错误次数（按记录条数统计，与知识点次数之和一致） */
fun errorCountOf(
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    moduleKey: String
): Int {
    val ids = points.filter { it.moduleKey == moduleKey }.map { it.id }.toSet()
    return records.count { it.pointId in ids }
}
