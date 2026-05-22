package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CustomCombo
import kotlinx.coroutines.flow.Flow

@Dao
interface ComboDao {
    @Query("SELECT * FROM custom_combos ORDER BY id ASC")
    fun getAllCombos(): Flow<List<CustomCombo>>

    @Query("SELECT * FROM custom_combos WHERE characterId = :characterId ORDER BY id ASC")
    fun getCombosForCharacter(characterId: String): Flow<List<CustomCombo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombo(combo: CustomCombo): Long

    @Update
    suspend fun updateCombo(combo: CustomCombo)

    @Delete
    suspend fun deleteCombo(combo: CustomCombo)

    @Query("DELETE FROM custom_combos WHERE isDefault = 0")
    suspend fun deleteAllCustomCombos()
}
