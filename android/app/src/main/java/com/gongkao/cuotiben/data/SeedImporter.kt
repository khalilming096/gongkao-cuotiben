package com.gongkao.cuotiben.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 首次启动把 assets/seed-backup.json 导进数据库。
 *
 * 数据来自 tools/sync_fenbi.py 生成的备份（粉笔采集的错题），结构与网页版导出备份一致：
 * { app, version, points:[{id,mod,g,s,name,reason,count,created}], records:[{id,pid,ts,n,q}] }
 *
 * 规则：
 * - 只在数据库为空时导一次，用 SharedPreferences 记录，避免重复写入；
 * - 备份里的 id 与自增主键无关，用 idMap 把记录挂到新知识点上；
 * - 全程在 IO 线程，任何异常都不影响 App 启动（静默失败，只打日志）。
 */
object SeedImporter {

    private const val PREFS = "seed_prefs"
    private const val KEY = "fenbi_seed_v1"
    private const val ASSET = "seed-backup.json"

    suspend fun importIfNeeded(context: Context, db: AppDatabase) {
        withContext(Dispatchers.IO) {
            val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (sp.getBoolean(KEY, false)) return@withContext

            val dao = db.dao()
            // 已经有数据就不覆盖，只标记一下
            if (dao.allPoints().isNotEmpty()) {
                sp.edit().putBoolean(KEY, true).apply()
                return@withContext
            }

            val text = runCatching {
                context.assets.open(ASSET).bufferedReader().use { it.readText() }
            }.getOrNull() ?: return@withContext

            runCatching { import(db, text) }
                .onSuccess { sp.edit().putBoolean(KEY, true).apply() }
                .onFailure { Log.w("SeedImporter", "导入种子数据失败", it) }
        }
    }

    private suspend fun import(db: AppDatabase, text: String) {
        val dao = db.dao()
        val root = JSONObject(text)
        val points = root.optJSONArray("points") ?: return
        val records = root.optJSONArray("records") ?: return

        val idMap = HashMap<Long, Long>()
        db.withTransaction {
            for (i in 0 until points.length()) {
                val o = points.getJSONObject(i)
                val oldId = o.optLong("id", i.toLong() + 1)
                val newId = dao.insertPoint(
                    KnowledgePoint(
                        moduleKey = o.optString("mod"),
                        groupKey = o.optString("g"),
                        subKey = o.optString("s"),
                        name = o.optString("name"),
                        reason = o.optString("reason"),
                        errorCount = o.optInt("count", 0),
                        createdAt = o.optLong("created", System.currentTimeMillis())
                    )
                )
                idMap[oldId] = newId
            }

            for (i in 0 until records.length()) {
                val o = records.getJSONObject(i)
                val pointId = idMap[o.optLong("pid")]
                if (pointId != null) {
                    dao.insertRecord(
                        ErrorRecord(
                            pointId = pointId,
                            timestamp = o.optLong("ts", System.currentTimeMillis()),
                            note = o.optString("n"),
                            qid = o.optString("q")
                        )
                    )
                }
            }
        }
    }
}
