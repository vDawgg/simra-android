package de.tuberlin.mcc.simra.app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.tuberlin.mcc.simra.app.entities.DataLogEntry;

//TODO: Change exportSchema later on when migrating to the db
@Database(entities = {DataLogEntry.class}, version = 1, exportSchema = false)
public abstract class SimRaDB extends RoomDatabase {

    public abstract DataLogDao getDataLogDao();

    private static volatile SimRaDB INSTANCE;

    // Is it smart to set the number of threads to a fixed amount and what is this used for anyways?
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static SimRaDB getDataBase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SimRaDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room
                            .databaseBuilder(context.getApplicationContext(), SimRaDB.class, "SimRaDB")
                            //TODO: Potentially change this if performance isnt good.
                            // ->This can potentially lock up the main thread and using asynchronous
                            // operations is advised.
                            // ->Need to look into properly doing that while making sure that
                            // the operations finish in time
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
