package com.gongkao.cuotiben.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [KnowledgePoint::class, ErrorRecord::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): CuotiDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** v1 → v2：知识点增加子模块 / 子类两个字段，老数据归为空（仍挂在模块下） */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE knowledge_points ADD COLUMN groupKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE knowledge_points ADD COLUMN subKey TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v2 → v3：错误记录增加备注字段 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE error_records ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v3 → v4：错误记录增加题库题号 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE error_records ADD COLUMN qid TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cuotiben.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
