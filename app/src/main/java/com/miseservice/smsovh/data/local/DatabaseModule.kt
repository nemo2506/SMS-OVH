package com.miseservice.smsovh.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_settings ADD COLUMN restPort INTEGER NOT NULL DEFAULT 8080")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_settings ADD COLUMN ovhAppKey TEXT")
        db.execSQL("ALTER TABLE app_settings ADD COLUMN ovhAppSecret TEXT")
        db.execSQL("ALTER TABLE app_settings ADD COLUMN ovhConsumerKey TEXT")
        db.execSQL("ALTER TABLE app_settings ADD COLUMN ovhServiceName TEXT")
        db.execSQL("ALTER TABLE app_settings ADD COLUMN ovhEndpoint TEXT")
        db.execSQL("ALTER TABLE app_settings ADD COLUMN ovhCountryPrefix TEXT")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideLogDao(db: AppDatabase): LogDao = db.logDao()
}
