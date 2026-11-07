package com.gongkao.cuotiben.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gongkao.cuotiben.data.ErrorRecord
import com.gongkao.cuotiben.data.KnowledgePoint
import com.gongkao.cuotiben.model.StudyModule
import com.gongkao.cuotiben.ui.components.EmptyState
import com.gongkao.cuotiben.ui.components.PointRow
import com.gongkao.cuotiben.util.TimeText

/**
 * 知识点清单（扁平版）：用于没有子模块的三个模块——政治理论、常识、资料分析。
 */
@Composable
fun ModuleScreen(
    module: StudyModule,
    points: List<KnowledgePoint>,
    records: List<ErrorRecord>,
    onCheck: (KnowledgePoint) -> Unit,
    onOpen: (KnowledgePoint) -> Unit,
    onAdd: () -> Unit
) {
    if (points.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            EmptyState(
                title = "还没有知识点",
                description = "把「${module.label}」的易错考点加进来，勾选一次就记一次错，\n累计次数会告诉你哪里最该补。",
                actionText = "新增第一个知识点",
                onAction = onAdd
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(points, key = { it.id }) { point ->
            val pointRecords = records.filter { it.pointId == point.id }
            PointRow(
                point = point,
                recordCount = pointRecords.size,
                latestText = pointRecords.maxOfOrNull { it.timestamp }?.let { TimeText.format(it) },
                onCheck = { onCheck(point) },
                onOpen = { onOpen(point) }
            )
        }
    }
}

/** 模块页副标题：知识点数量与累计错误次数 */
fun moduleSubtitle(points: List<KnowledgePoint>, records: List<ErrorRecord>, module: StudyModule): String {
    val modulePoints = points.count { it.moduleKey == module.key }
    val errors = errorCountOf(points, records, module.key)
    return "$modulePoints 个知识点 · 累计 $errors 次"
}
