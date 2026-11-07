package com.gongkao.cuotiben.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 知识点：挂在某个模块（或 模块/子模块/子类）下，记录名称、错误原因与累计错误次数。
 * errorCount 是累计值，每次勾选 +1，并与 [ErrorRecord] 一一对应。
 *
 * 层级：模块 moduleKey（必填）→ 子模块 groupKey（可选）→ 子类 subKey（可选）。
 * 政治理论、常识、资料分析没有子模块，groupKey / subKey 留空。
 */
@Entity(tableName = "knowledge_points")
data class KnowledgePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val moduleKey: String,
    val groupKey: String = "",
    val subKey: String = "",
    val name: String,
    val reason: String,
    val errorCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 单次错误记录：勾选一次生成一条，用于「查看记录」。
 * note 是事后补写的备注（这道题错在哪），可在列表里单独编辑。
 */
@Entity(
    tableName = "error_records",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgePoint::class,
            parentColumns = ["id"],
            childColumns = ["pointId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pointId")]
)
data class ErrorRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pointId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    /** 题库题号，关联到「错题复盘」里的原题，可为空 */
    val qid: String = ""
)
