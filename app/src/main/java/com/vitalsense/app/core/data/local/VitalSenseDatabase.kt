package com.vitalsense.app.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vitalsense.app.core.data.local.dao.VitalSenseDao
import com.vitalsense.app.core.data.local.entity.*
import com.vitalsense.app.core.data.local.typeconverters.Converters

@Database(
    entities = [
        VillageEntity::class,
        PatientEntity::class,
        AshaWorkerEntity::class,
        DoctorEntity::class,
        ConditionRecordEntity::class,
        PrescriptionEntity::class,
        AppointmentEntity::class,
        BroadcastNoticeEntity::class,
        DispensaryEntity::class,
        GovernmentSchemeEntity::class,
        OutboxEntity::class,
        DoctorDaySlotEntity::class,
        QueueEntryEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VitalSenseDatabase : RoomDatabase() {

    abstract fun vitalSenseDao(): VitalSenseDao

    companion object {
        @Volatile
        private var INSTANCE: VitalSenseDatabase? = null

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createQueueTables(db)
            }
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createQueueTables(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createQueueTables(db)
            }
        }

        private fun createQueueTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `doctor_day_slots` (
                    `id` TEXT NOT NULL,
                    `doctorId` TEXT NOT NULL,
                    `dateFormatted` TEXT NOT NULL,
                    `startTime` TEXT NOT NULL,
                    `endTime` TEXT NOT NULL,
                    `capacity` INTEGER NOT NULL,
                    `isWalkInOpen` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `queue_entries` (
                    `id` TEXT NOT NULL,
                    `doctorId` TEXT NOT NULL,
                    `doctorName` TEXT NOT NULL,
                    `dateFormatted` TEXT NOT NULL,
                    `tokenNumber` INTEGER NOT NULL,
                    `provisionalToken` INTEGER NOT NULL,
                    `appointmentId` TEXT,
                    `patientId` TEXT NOT NULL,
                    `patientName` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `priorityFlag` INTEGER NOT NULL,
                    `checkedInAt` INTEGER NOT NULL,
                    `calledAt` INTEGER,
                    `consultationStartedAt` INTEGER,
                    `completedAt` INTEGER,
                    `outcomeNotes` TEXT,
                    `isPendingSync` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }

        fun getDatabase(context: Context): VitalSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VitalSenseDatabase::class.java,
                    "vitalsense_database"
                )
                    .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

