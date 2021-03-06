package ict.ictbase.commons.global;
import ict.ictbase.util.HIndexConstantsAndUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;

public class GlobalHTableWithIndexesDriver extends HTable {
    protected static int errorIndex = 0;
    protected Map<String, HTable> indexTables = null;
    protected GlobalMaterializeIndex policyToMaterializeIndex = null;

    @SuppressWarnings("deprecation")
	public GlobalHTableWithIndexesDriver(Configuration conf, byte[] tableName) throws IOException {
        super(conf, tableName);
        HTableDescriptor dataTableDesc = null; 
        try {
            dataTableDesc = getTableDescriptor();
            //enable autoflush
            setAutoFlush(true);
        } catch (IOException e1) {
            throw new RuntimeException("TTERROR_" + (errorIndex++) + ": " + e1.getMessage());
        }

        policyToMaterializeIndex = new GlobalMaterializeIndexByCompositeRowkey(); //TOREMOVE
        initIndexTables(dataTableDesc, conf);
    }
    @SuppressWarnings("deprecation")
    public void initIndexTables(HTableDescriptor dataTableDesc, Configuration conf) {
        //initialize index table
        indexTables = new HashMap<String, HTable>();
        //scan through all indexed columns
        for (int indexNumber = 1; ; indexNumber++){
            String indexedColumn = dataTableDesc.getValue(HIndexConstantsAndUtils.INDEX_INDICATOR + indexNumber);
            if(indexedColumn == null){
                //no (further) index column, at current index
                break;
            } else {
                String[] names = indexedColumn.split("\\|");
                String indexedColumnFamilyName = names[0];
                String indexedColumnName = names[1]; 
                String indexTableName = dataTableDesc.getNameAsString() + "_" + indexedColumnFamilyName + "_" + indexedColumnName;
                try {
					HTable indexTable = new HTable(conf, indexTableName);
                    indexTable.setAutoFlush(true);
                    indexTables.put(indexTableName, indexTable);
                } catch (IOException e1) {
                    throw new RuntimeException("TTERROR_" + (errorIndex++) + ": " + e1.getMessage());
                }
            }
        }
    }

    public HTable getIndexTable(byte[] columnFamily, byte[] columnName) {
        String dataTableName =  Bytes.toString(this.getTableName());
        String indexTableName = dataTableName + "_" + Bytes.toString(columnFamily) + "_" + Bytes.toString(columnName);
        HTable indexTable = indexTables.get(indexTableName);
        if(indexTable == null){
            throw new RuntimeException("TTERROR_" + (errorIndex ++) + ": Unable to find index table with name:" + indexTableName + "!");
        }
        return indexTable;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if(indexTables != null) {
            for(Map.Entry<String, HTable> entry : indexTables.entrySet()){
                entry.getValue().close();
            }
        }
    }

    /**
	@para, valueStop is exclusive!
	@return, <dataValue in range, list of dataKeys>
    */
    protected List<String> internalGetByIndexByRange(byte[] columnFamily, byte[] columnName, byte[] valueStart, byte[] valueStop) throws IOException {
    	HTable indexTable = getIndexTable(columnFamily, columnName);
        return policyToMaterializeIndex.getByIndexByRange(indexTable, valueStart, valueStop);
    }

    public void putToIndex(byte[] columnFamily, byte[] columnName, byte[] dataValue, byte[] dataKey) throws IOException {
        HTable indexTable = getIndexTable(columnFamily, columnName);
        policyToMaterializeIndex.putToIndex(indexTable, dataValue, dataKey);
    }

    public boolean deleteFromIndex(byte[] columnFamily, byte[] columnName, byte[] dataValue, byte[] dataKey) throws IOException {
//    	System.out.println("****************: global "+Bytes.toString(columnFamily)+"\t"+Bytes.toString(columnName)+"\t"+
//    			Bytes.toString(dataValue)+"\t"+Bytes.toString(dataKey));
        HTable indexTable = getIndexTable(columnFamily, columnName);
       boolean isDeleted  =  policyToMaterializeIndex.deleteFromIndex(indexTable, dataValue, dataKey);
       return isDeleted;
    }
    public void deleteFromBaseTable (byte [] dataKey,long ts) throws IOException{
    	Delete delete = new Delete(dataKey,ts);
    	this.delete(delete);
    }
    
}

