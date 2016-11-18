package ict.ictbase.coprocessor.global;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Durability;

import java.io.IOException;

public class IndexObserverBaseline extends BasicIndexObserver {
    @Override
    public void prePut(final ObserverContext<RegionCoprocessorEnvironment> e, final Put put, final WALEdit edit, final Durability durability) throws IOException {
        super.prePut(e, put, edit, durability);
        System.out.println("**************global IndexObserverBaseline");
        dataTableWithIndexes.insertNewToIndexes(put);
        dataTableWithIndexes.readBaseAndDeleteOld(put);
    }
}
