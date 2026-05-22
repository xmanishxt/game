package com.example.data.repo

import com.example.data.db.ComboDao
import com.example.data.model.CustomCombo
import kotlinx.coroutines.flow.Flow

class ComboRepository(private val comboDao: ComboDao) {
    val allCombos: Flow<List<CustomCombo>> = comboDao.getAllCombos()

    fun getCombosForCharacter(characterId: String): Flow<List<CustomCombo>> {
        return comboDao.getCombosForCharacter(characterId)
    }

    suspend fun insertCombo(combo: CustomCombo): Long {
        return comboDao.insertCombo(combo)
    }

    suspend fun updateCombo(combo: CustomCombo) {
        comboDao.updateCombo(combo)
    }

    suspend fun deleteCombo(combo: CustomCombo) {
        comboDao.deleteCombo(combo)
    }

    suspend fun clearCustomCombos() {
        comboDao.deleteAllCustomCombos()
    }
}
