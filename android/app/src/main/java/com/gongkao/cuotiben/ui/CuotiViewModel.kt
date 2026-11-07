package com.gongkao.cuotiben.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gongkao.cuotiben.data.AppDatabase
import com.gongkao.cuotiben.data.ErrorRecord
import com.gongkao.cuotiben.data.KnowledgePoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CuotiViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()

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

    /** 修改某一条错误记录的备注 */
    fun updateRecordNote(record: ErrorRecord, note: String) {
        viewModelScope.launch { dao.updateRecordNote(record.id, note.trim()) }
    }
}
