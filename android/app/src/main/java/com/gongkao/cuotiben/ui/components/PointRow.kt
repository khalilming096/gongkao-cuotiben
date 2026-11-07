package com.gongkao.cuotiben.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.gongkao.cuotiben.data.KnowledgePoint

/**
 * 知识点卡片：左侧勾选圈、中间名称与错因、右侧累计次数。
 * 模块页与子模块页共用同一种卡片，保证层级不同但条目手感一致。
 */
@Composable
fun PointRow(
    point: KnowledgePoint,
    recordCount: Int,
    latestText: String?,
    onCheck: () -> Unit,
    onOpen: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(start = 11.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
        ) {
            CheckCircle(active = point.errorCount > 0, onClick = onCheck)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = point.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = point.errorCount.toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " 次",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    ReasonChip()
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = point.reason.ifBlank { "未填写具体原因" },
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp)
                        .height(0.8.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "查看 $recordCount 条记录 ›",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = latestText?.let { "截止 $it" } ?: "未记录",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 「错因」小标签，统一采用暖色底，和主色区分开 */
@Composable
fun ReasonChip(text: String = "错因") {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFFBF3E2))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            color = Color(0xFFB4791C)
        )
    }
}

/**
 * 勾选圆圈：点一次代表「又错了一次」，
 * 累计次数 > 0 时高亮，便于一眼看出已记录过的知识点。
 */
@Composable
fun CheckCircle(active: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.8.dp,
                color = if (active) accent else Color(0xFFDCDFE7),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "记一次错",
            tint = if (active) accent else Color(0xFFC2C7D2),
            modifier = Modifier.size(18.dp)
        )
    }
}
