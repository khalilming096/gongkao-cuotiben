package com.gongkao.cuotiben

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gongkao.cuotiben.data.ErrorRecord
import com.gongkao.cuotiben.data.KnowledgePoint
import com.gongkao.cuotiben.model.Catalog
import com.gongkao.cuotiben.model.StudyModule
import com.gongkao.cuotiben.model.SubGroup
import com.gongkao.cuotiben.ui.CuotiViewModel
import com.gongkao.cuotiben.ui.DeletePointDialog
import com.gongkao.cuotiben.ui.DeleteRecordDialog
import com.gongkao.cuotiben.ui.DetailScreen
import com.gongkao.cuotiben.ui.GroupScreen
import com.gongkao.cuotiben.ui.HomeScreen
import com.gongkao.cuotiben.ui.InfoDialog
import com.gongkao.cuotiben.ui.ModuleScreen
import com.gongkao.cuotiben.ui.PointEditorDialog
import com.gongkao.cuotiben.ui.RecordNoteDialog
import com.gongkao.cuotiben.ui.SubModuleScreen
import com.gongkao.cuotiben.ui.components.AppBar
import com.gongkao.cuotiben.ui.moduleSubtitle
import com.gongkao.cuotiben.ui.theme.CuotiTheme
import com.gongkao.cuotiben.util.TimeText
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CuotiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CuotiApp()
                }
            }
        }
    }
}

@Composable
fun CuotiApp(vm: CuotiViewModel = viewModel()) {
    val points by vm.points.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()

    // 路由：moduleKey 为空即在主界面；groupKey 为空即在模块层或知识点清单
    var moduleKey by rememberSaveable { mutableStateOf("") }
    var groupKey by rememberSaveable { mutableStateOf("") }
    var detailId by rememberSaveable { mutableStateOf(-1L) }

    var editorOpen by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<KnowledgePoint?>(null) }
    var editorModuleKey by remember { mutableStateOf(StudyModule.DATA.key) }
    var editorGroupKey by remember { mutableStateOf("") }
    var editorSubKey by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<KnowledgePoint?>(null) }
    var noteTarget by remember { mutableStateOf<Pair<ErrorRecord, Int>?>(null) }
    var recordDeleteTarget by remember { mutableStateOf<Pair<ErrorRecord, Int>?>(null) }
    var infoOpen by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val routeModule: StudyModule? = StudyModule.byKey(moduleKey)
    val groups: List<SubGroup> = routeModule?.let { Catalog.groupsOf(it) } ?: emptyList()
    val routeGroup: SubGroup? = groups.firstOrNull { it.key == groupKey }
    val currentPoint: KnowledgePoint? = points.firstOrNull { it.id == detailId }

    // 界面文案与层级状态在渲染前一次算好
    val onGroupList = routeModule != null && groups.isNotEmpty() && routeGroup == null
    val barTitle = when {
        detailId > 0 -> "错误记录"
        routeGroup != null -> routeGroup.label
        routeModule != null -> routeModule.label
        else -> "公考错题本"
    }
    val barSubtitle = when {
        detailId > 0 && currentPoint != null && routeModule != null ->
            Catalog.breadcrumb(routeModule, currentPoint.groupKey, currentPoint.subKey)
        routeGroup != null -> {
            val n = points.count { it.moduleKey == moduleKey && it.groupKey == groupKey }
            "${routeGroup.categories.size} 个子类 · $n 个知识点"
        }
        onGroupList ->
            "${groups.size} 个子模块 · ${points.count { it.moduleKey == moduleKey }} 个知识点"
        routeModule != null -> moduleSubtitle(points, records, routeModule)
        else -> null
    }
    val canGoBack = detailId > 0 || routeModule != null

    fun goBack() {
        when {
            detailId > 0 -> detailId = -1L
            routeGroup != null -> groupKey = ""
            else -> moduleKey = ""
        }
    }

    BackHandler(enabled = canGoBack) { goBack() }

    fun notify(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    // 勾选一次即记录，同时给出「撤销」，误触可一键回退
    val onCheck: (KnowledgePoint) -> Unit = { point ->
        vm.recordError(point)
        val nth = point.errorCount + 1
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "已记录「${point.name}」第 $nth 次",
                actionLabel = "撤销",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                vm.undoLatestRecord(point.id)
                snackbarHostState.showSnackbar(
                    message = "已撤销这次记录",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val showAddBar = routeModule != null && detailId <= 0 && !onGroupList

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            AppBar(
                title = barTitle,
                subtitle = barSubtitle,
                onBack = if (canGoBack) ({ goBack() }) else null,
                onInfo = { infoOpen = true }
            )

            when {
                detailId > 0 && currentPoint != null && routeModule != null -> {
                    val pointRecords = records
                        .filter { it.pointId == currentPoint.id }
                        .sortedByDescending { it.timestamp }
                    DetailScreen(
                        point = currentPoint,
                        records = pointRecords,
                        breadcrumb = Catalog.breadcrumb(
                            routeModule,
                            currentPoint.groupKey,
                            currentPoint.subKey
                        ),
                        onCheck = { onCheck(currentPoint) },
                        onEdit = {
                            editorTarget = currentPoint
                            editorModuleKey = currentPoint.moduleKey
                            editorGroupKey = currentPoint.groupKey
                            editorSubKey = currentPoint.subKey
                            editorOpen = true
                        },
                        onDelete = { deleteTarget = currentPoint },
                        onEditRecordNote = { record, order -> noteTarget = record to order },
                        onDeleteRecord = { record, order -> recordDeleteTarget = record to order }
                    )
                }

                routeGroup != null && routeModule != null -> {
                    val group = routeGroup
                    val groupPoints = points.filter {
                        it.moduleKey == moduleKey && it.groupKey == group.key
                    }
                    SubModuleScreen(
                        group = group,
                        points = groupPoints,
                        records = records,
                        onCheck = onCheck,
                        onOpen = { point -> detailId = point.id },
                        onAdd = {
                            editorTarget = null
                            editorModuleKey = moduleKey
                            editorGroupKey = group.key
                            editorSubKey = ""
                            editorOpen = true
                        }
                    )
                }

                onGroupList && routeModule != null -> {
                    GroupScreen(
                        module = routeModule,
                        groups = groups,
                        points = points,
                        records = records,
                        onOpenGroup = { group ->
                            groupKey = group.key
                            detailId = -1L
                        }
                    )
                }

                routeModule != null -> {
                    val module = routeModule
                    val modulePoints = points.filter { it.moduleKey == module.key }
                    ModuleScreen(
                        module = module,
                        points = modulePoints,
                        records = records,
                        onCheck = onCheck,
                        onOpen = { point -> detailId = point.id },
                        onAdd = {
                            editorTarget = null
                            editorModuleKey = module.key
                            editorGroupKey = ""
                            editorSubKey = ""
                            editorOpen = true
                        }
                    )
                }

                else -> HomeScreen(
                    points = points,
                    records = records,
                    onOpenModule = { module ->
                        moduleKey = module.key
                        groupKey = ""
                        detailId = -1L
                    }
                )
            }
        }

        // 底部固定操作条：全宽、居中、常驻可见，比右下角悬浮按钮更好点也更好找
        if (showAddBar && routeModule != null) {
            val barModule = routeModule
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clickable {
                            editorTarget = null
                            editorModuleKey = barModule.key
                            editorGroupKey = routeGroup?.key.orEmpty()
                            editorSubKey = ""
                            editorOpen = true
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "新增知识点",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showAddBar) 84.dp else 16.dp)
        )
    }

    if (editorOpen) {
        PointEditorDialog(
            moduleLabel = StudyModule.of(editorModuleKey).label,
            group = Catalog.groupOf(StudyModule.of(editorModuleKey), editorGroupKey),
            initialSubKey = editorSubKey,
            existing = editorTarget,
            onDismiss = { editorOpen = false },
            onConfirm = { name, reason, subKey ->
                val editing = editorTarget
                if (editing != null) {
                    vm.updatePoint(editing, name, reason, subKey)
                    notify("已更新「${name.trim()}」")
                } else {
                    vm.addPoint(editorModuleKey, editorGroupKey, subKey, name, reason) { newId ->
                        scope.launch {
                            val where = Catalog.groupOf(StudyModule.of(editorModuleKey), editorGroupKey)
                            val label = where?.label ?: StudyModule.of(editorModuleKey).label
                            val result = snackbarHostState.showSnackbar(
                                message = "已添加到「$label」",
                                actionLabel = "撤销",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                vm.deletePointById(newId)
                            }
                        }
                    }
                }
                editorOpen = false
            }
        )
    }

    val deleting = deleteTarget
    if (deleting != null) {
        DeletePointDialog(
            point = deleting,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deletePoint(deleting)
                if (detailId == deleting.id) detailId = -1L
                deleteTarget = null
                notify("已删除「${deleting.name}」")
            }
        )
    }

    val noting = noteTarget
    if (noting != null) {
        RecordNoteDialog(
            order = noting.second,
            timeText = TimeText.format(noting.first.timestamp),
            existingNote = noting.first.note,
            onDismiss = { noteTarget = null },
            onConfirm = { note ->
                vm.updateRecordNote(noting.first, note)
                noteTarget = null
                notify(if (note.isBlank()) "已清空备注" else "备注已保存")
            }
        )
    }

    val deletingRecord = recordDeleteTarget
    if (deletingRecord != null) {
        DeleteRecordDialog(
            order = deletingRecord.second,
            timeText = TimeText.format(deletingRecord.first.timestamp),
            onDismiss = { recordDeleteTarget = null },
            onConfirm = {
                vm.deleteRecord(deletingRecord.first)
                recordDeleteTarget = null
                notify("已删除第 ${deletingRecord.second} 次记录")
            }
        )
    }

    if (infoOpen) {
        InfoDialog(onDismiss = { infoOpen = false })
    }
}
