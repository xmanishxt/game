package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CustomCombo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CustomCombo::class], version = 1, exportSchema = false)
abstract class ComboDatabase : RoomDatabase() {
    abstract fun comboDao(): ComboDao

    companion object {
        @Volatile
        private var INSTANCE: ComboDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ComboDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ComboDatabase::class.java,
                    "combo_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Use CoroutineScope to launch a background thread insert
                        scope.launch(Dispatchers.IO) {
                            val dao = getDatabase(context, scope).comboDao()
                            // Pre-populate default combos for each playable combatant
                            
                            // Ignis (Pyromancer - Red Flame Trails)
                            dao.insertCombo(CustomCombo(characterId = "ignis", name = "Solar Eruption", sequence = "L,L,H", damage = 28, specialColor = "#FF4500", isDefault = true))
                            dao.insertCombo(CustomCombo(characterId = "ignis", name = "Firestorm Pulse", sequence = "L,H,S", damage = 36, specialColor = "#FF8C00", isDefault = true))
                            
                            // Volt (Lightning Assassin - Cyan/Yellow Sparks)
                            dao.insertCombo(CustomCombo(characterId = "volt", name = "Spark Dash", sequence = "L,L,L", damage = 18, specialColor = "#00E5FF", isDefault = true))
                            dao.insertCombo(CustomCombo(characterId = "volt", name = "Thunder Burst", sequence = "L,H,H", damage = 32, specialColor = "#FFEB3B", isDefault = true))
                            
                            // Terra (Stone Vanguard - Ground Quakes & Shielding)
                            dao.insertCombo(CustomCombo(characterId = "terra", name = "Tectonic Slam", sequence = "H,H,H", damage = 30, specialColor = "#8B4513", isDefault = true))
                            dao.insertCombo(CustomCombo(characterId = "terra", name = "Aegis Shield", sequence = "L,L,S", damage = 24, specialColor = "#4CAF50", isDefault = true))
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
