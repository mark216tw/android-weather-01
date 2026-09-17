package com.simpleweather.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites", primaryKeys = ["id"])
data class FavoriteEntity(
    val id: String,
    val name: String,
    val adminArea: String?,
    val country: String?,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    val addedAt: Long,
)

@Entity(tableName = "weather_cache", primaryKeys = ["cacheKey"])
data class WeatherCacheEntity(
    val cacheKey: String,
    val payload: String,
    val fetchedAt: Long,
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun contains(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE cacheKey = :key")
    suspend fun get(key: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE cacheKey != 'current_location' AND fetchedAt < :before AND cacheKey NOT IN (:favoriteKeys)")
    suspend fun deleteOld(before: Long, favoriteKeys: List<String>)
}

@Database(
    entities = [FavoriteEntity::class, WeatherCacheEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun cacheDao(): WeatherCacheDao
}
