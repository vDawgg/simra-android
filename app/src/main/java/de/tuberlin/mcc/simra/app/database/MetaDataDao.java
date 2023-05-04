package de.tuberlin.mcc.simra.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import io.reactivex.Completable;

@Dao
public interface MetaDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void updateOrAddMetadataEntryForRide(MetaDataEntry entry);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void updateOrAddMetadataEntries(List<MetaDataEntry> entries);

    @Query("select * from metadata_table where rideId == :rideId")
    public MetaDataEntry getMetadataEntryForRide(Integer rideId);

    @Query("select * from metadata_table order by rideId desc")
    public MetaDataEntry[] getMetadataEntriesSortedByKey();

    @Query("select * from metadata_table order by lastModified desc")
    public MetaDataEntry[] getMetadataEntriesLastModified();

    @Query("delete from metadata_table where rideId == :rideId")
    public void deleteMetadataEntryForRide(Integer rideId);
}
