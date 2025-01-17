package de.tuberlin.mcc.simra.app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;

@Database(entities = {DataLogEntry.class, MetaDataEntry.class, IncidentLogEntry.class}, version = 1)
public abstract class SimRaDB extends RoomDatabase {

    public abstract DataLogDao getDataLogDao();
    public abstract MetaDataDao getMetaDataDao();
    public abstract IncidentLogDao getIncidentLogDao();
    public abstract CombinedDao getCombinedDao();

    private static volatile SimRaDB INSTANCE;

    public static SimRaDB getDataBase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SimRaDB.class) {
                INSTANCE = Room
                        .databaseBuilder(context.getApplicationContext(), SimRaDB.class, "SimRaDB")
                        .allowMainThreadQueries()
                        .build();
            }
        }
        return INSTANCE;
    }
}
