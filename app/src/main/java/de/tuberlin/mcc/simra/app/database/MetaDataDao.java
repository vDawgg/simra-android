package de.tuberlin.mcc.simra.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import io.reactivex.Completable;

@Dao
public interface MetaDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void updateOrAddMetadataEntryForRide(MetaDataEntry entry);

    @Query("select * from metadata_table where rideId == :rideId")
    public MetaDataEntry getMetadataEntryForRide(Integer rideId);

    @Query("select * from metadata_table")
    public MetaDataEntry[] getMetaDataEntries();

    //TODO: Check if this is actually the right order
    @Query("select * from metadata_table order by rideId desc")
    public MetaDataEntry[] getMetadataEntriesSortedByKey();

    @Query("delete from metadata_table where rideId == :rideId")
    public void deleteMetadataEntryForRide(Integer rideId);
}
