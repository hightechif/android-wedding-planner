package app.weddingplanner.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        BudgetItemEntity::class,
        BudgetSummaryEntity::class,
        FinalVendorEntity::class,
        WeddingEventEntity::class,
        AppUserEntity::class,
        RowIdentityEntity::class,
        SyncRunEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class WeddingDatabase : RoomDatabase() {
    abstract fun dao(): WeddingDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WeddingDatabase =
        Room.databaseBuilder(context, WeddingDatabase::class.java, "wedding.db")
            // Only a cache of the sheet so far (no pending local edits until M2): rebuilding is safe.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideDao(db: WeddingDatabase): WeddingDao = db.dao()
}
