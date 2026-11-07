package com.gongkao.cuotiben.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CuotiDao {

    @Query("SELECT * FROM knowledge_points ORDER BY createdAt DESC")
    abstract fun observePoints(): Flow<List<KnowledgePoint>>

    @Query("SELECT * FROM error_records ORDER BY timestamp DESC")
    abstract fun observeRecords(): Flow<List<ErrorRecord>>

    @Insert
    abstract suspend fun insertPoint(point: KnowledgePoint): Long

    @Update
    abstract suspend fun updatePoint(point: KnowledgePoint)

    @Query("DELETE FROM knowledge_points WHERE id = :id")
    abstract suspend fun deletePoint(id: Long)

    @Insert
    abstract suspend fun insertRecord(record: ErrorRecord): Long

    @Query("DELETE FROM error_records WHERE id = :id")
    abstract suspend fun deleteRecord(id: Long)

    @Query("UPDATE error_records SET note = :note, qid = :qid WHERE id = :id")
    abstract suspend fun updateRecordContent(id: Long, note: String, qid: String)

    @Query("SELECT * FROM knowledge_points")
    abstract suspend fun allPoints(): List<KnowledgePoint>

    @Query("SELECT * FROM error_records WHERE pointId = :pointId ORDER BY timestamp DESC, id DESC LIMIT 1")
    abstract suspend fun latestRecord(pointId: Long): ErrorRecord?

    @Query("UPDATE knowledge_points SET errorCount = errorCount + 1 WHERE id = :id")
    abstract suspend fun increaseCount(id: Long)

    /** 减少累计次数，用 CASE 兜底防止出现负数 */
    @Query("UPDATE knowledge_points SET errorCount = CASE WHEN errorCount > 0 THEN errorCount - 1 ELSE 0 END WHERE id = :id")
    abstract suspend fun decreaseCount(id: Long)

    /**
     * 勾选一次：写入一条错误记录，同时把该知识点的累计次数 +1。
     * 两步在同一事务内完成，保证次数与记录条数始终一致。
     */
    @Transaction
    open suspend fun recordOneError(pointId: Long): Long {
        val id = insertRecord(ErrorRecord(pointId = pointId))
        increaseCount(pointId)
        return id
    }

    /** 撤销最近一次勾选：删掉最新那条记录并把次数减回去 */
    @Transaction
    open suspend fun undoLatestRecord(pointId: Long) {
        val latest = latestRecord(pointId) ?: return
        deleteRecord(latest.id)
        decreaseCount(pointId)
    }

    /** 删除指定的一条错误记录，并把次数减回去 */
    @Transaction
    open suspend fun removeRecord(record: ErrorRecord) {
        deleteRecord(record.id)
        decreaseCount(record.pointId)
    }
}
