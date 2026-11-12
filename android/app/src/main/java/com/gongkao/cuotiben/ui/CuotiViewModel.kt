package com.gongkao.cuotiben.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gongkao.cuotiben.data.AppDatabase
import com.gongkao.cuotiben.data.ErrorRecord
import com.gongkao.cuotiben.data.KnowledgePoint
import com.gongkao.cuotiben.data.SeedImporter
import com.gongkao.cuotiben.model.Catalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CuotiViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()

    init {
        Catalog.load(app)
        viewModelScope.launch {
            // 首次启动导入内置错题数据（assets/seed-backup.json），已有数据则跳过
            SeedImporter.importIfNeeded(app, AppDatabase.get(app))
            migrateLegacyCategories()
        }
    }

    /**
     * v2.1 的知识点用的是手写英文子类 key，如果在新题库分类里找不到，
     * 按映射表换过去；映射不到的归为「未归类」，不丢数据。
     */
    private suspend fun migrateLegacyCategories() {
        dao.allPoints().forEach { p ->
            if (p.groupKey.isEmpty()) return@forEach
            if (Catalog.subOf(p.moduleKey, p.groupKey, p.subKey) != null) return@forEach
            val hit = Catalog.legacyMapping(p.moduleKey, p.groupKey, p.subKey)
            dao.updatePoint(
                if (hit != null) p.copy(groupKey = hit.first, subKey = hit.second)
                else p.copy(groupKey = "", subKey = "")
            )
        }
    }

    val points: StateFlow<List<KnowledgePoint>> = dao.observePoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val records: StateFlow<List<ErrorRecord>> = dao.observeRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 新增知识点，回调返回新 id，便于「撤销」 */
    fun addPoint(
        moduleKey: String,
        groupKey: String,
        subKey: String,
        name: String,
        reason: String,
        onCreated: (Long) -> Unit = {}
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            val id = dao.insertPoint(
                KnowledgePoint(
                    moduleKey = moduleKey,
                    groupKey = groupKey,
                    subKey = subKey,
                    name = trimmedName,
                    reason = reason.trim()
                )
            )
            onCreated(id)
        }
    }

    fun updatePoint(
        point: KnowledgePoint,
        name: String,
        reason: String,
        subKey: String = point.subKey
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            dao.updatePoint(
                point.copy(name = trimmedName, reason = reason.trim(), subKey = subKey)
            )
        }
    }

    fun deletePoint(point: KnowledgePoint) {
        viewModelScope.launch { dao.deletePoint(point.id) }
    }

    fun deletePointById(id: Long) {
        viewModelScope.launch { dao.deletePoint(id) }
    }

    /** 勾选一次：累计次数 +1，并新增一条错误记录 */
    fun recordError(point: KnowledgePoint) {
        viewModelScope.launch { dao.recordOneError(point.id) }
    }

    /** 撤销刚勾选的那一次 */
    fun undoLatestRecord(pointId: Long) {
        viewModelScope.launch { dao.undoLatestRecord(pointId) }
    }

    /** 删除指定的某一条错误记录 */
    fun deleteRecord(record: ErrorRecord) {
        viewModelScope.launch { dao.removeRecord(record) }
    }

    /** 修改某一条错误记录的备注与题号 */
    fun updateRecordContent(record: ErrorRecord, note: String, qid: String) {
        viewModelScope.launch {
            dao.updateRecordContent(record.id, note.trim(), qid.trim())
        }
    }
}
