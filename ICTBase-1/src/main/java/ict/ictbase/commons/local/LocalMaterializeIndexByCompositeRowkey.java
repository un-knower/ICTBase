package ict.ictbase.commons.local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.regionserver.Region;
import org.apache.hadoop.hbase.util.Bytes;

public class LocalMaterializeIndexByCompositeRowkey implements
		LocalMaterializeIndex {

	public List<String> getByIndexByRange(HTable indexTable, byte[] valueStart,
			byte[] valueStop, byte[] columnFamily, byte[] columnName)
			throws IOException {
		Scan scan = new Scan();
		// scan.addColumn(IndexStorageFormat.INDEXTABLE_COLUMNFAMILY,
		// IndexStorageFormat.INDEXTABLE_SPACEHOLDER);
		scan.setAttribute(IndexStorageFormat.SCAN_INDEX_FAMILIY, columnFamily);
		scan.setAttribute(IndexStorageFormat.SCAN_INDEX_QUALIFIER, columnName);
		scan.setAttribute(IndexStorageFormat.SCAN_START_VALUE, valueStart);
		scan.setAttribute(IndexStorageFormat.SCAN_STOP_VALUE, valueStop);

		String startValue = Bytes.toString(scan
				.getAttribute(IndexStorageFormat.SCAN_START_VALUE));
		String stopValue = Bytes.toString(scan
				.getAttribute(IndexStorageFormat.SCAN_STOP_VALUE));
		String a = null;
		System.out.println("*******" + startValue + "*******");
		System.out.println("*******" + stopValue + "*******");
		System.out.println("*******" + a + "*******");

		if (stopValue == null) { // point query
			System.out.println("&&&&&&&&&&&");
		} else {
			System.out.println("11111111111");
		}

		// ResultScanner is for client-side scanning.
		List<String> toRet = new ArrayList<String>();
		ResultScanner rs = indexTable.getScanner(scan);
		try {
			Result r;
			while ((r = rs.next()) != null) {
				for (Cell cell : r.listCells()) {
					System.out
							.println(String
									.format("come in row:%s,family:%s,qualifier:%s,value:%s,timestamp:%s",
											Bytes.toString(CellUtil
													.cloneRow(cell)),
											Bytes.toString(CellUtil
													.cloneFamily(cell)),
											Bytes.toString(CellUtil
													.cloneQualifier(cell)),
											Bytes.toString(CellUtil
													.cloneValue(cell)), cell
													.getTimestamp()));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		rs.close();
		return toRet;
	}

	public void putToIndex(Region region, String regionStartKey,
			byte[] columnFamily, byte[] columnName, byte[] dataValue,
			byte[] dataKey) throws IOException {
		String indexRowkey = regionStartKey + "#"
				+ Bytes.toString(columnFamily) + "#"
				+ Bytes.toString(columnName) + "#" + Bytes.toString(dataValue)
				+ "#" + Bytes.toString(dataKey);
//		System.out.println("********************indexRowkey:"+indexRowkey);
		Put put2Index = new Put(Bytes.toBytes(indexRowkey));
		put2Index.addColumn(IndexStorageFormat.INDEXTABLE_COLUMNFAMILY,
				IndexStorageFormat.INDEXTABLE_SPACEHOLDER,
				IndexStorageFormat.INDEXTABLE_SPACEHOLDER);
		put2Index.setAttribute("index_put", Bytes.toBytes("1"));//is index put ,do not print the log information
		region.put(put2Index);
//		System.out.println("******** in the putToIndex method in class LocalMaterializeIndexByCompositeRowkey");
	}

	public boolean deleteFromIndex( Region region,String regionStartKey,byte [] columnFamily ,byte [] columnName,byte []  dataValue,byte []  dataKey) throws IOException {
		String indexRowkey = regionStartKey+"#"+Bytes.toString(columnFamily)+"#"+Bytes.toString(columnName)+"#"
				+Bytes.toString(dataValue)+"#"+Bytes.toString(dataKey);
//		Get get = new Get(Bytes.toBytes(indexRowkey));
//	        
//	    Result r = region.get(get);
//	    
//	    if(r.isEmpty()){
//        	return false;
//        }else{
//        	Delete del = new Delete(Bytes.toBytes(indexRowkey));
//        	del.addColumn(IndexStorageFormat.INDEXTABLE_COLUMNFAMILY, IndexStorageFormat.INDEXTABLE_SPACEHOLDER);
//             //del.setTimestamp(timestamp);
//            region.delete(del);
//        }
//        return true;
        
        Delete del = new Delete(Bytes.toBytes(indexRowkey));
    	del.addColumn(IndexStorageFormat.INDEXTABLE_COLUMNFAMILY, IndexStorageFormat.INDEXTABLE_SPACEHOLDER);
        region.delete(del);
        return true;
	}
}

class IndexStorageFormat {
	static final public byte[] INDEXTABLE_COLUMNFAMILY = Bytes
			.toBytes("INDEX_CF"); // be consistent with column_family_name in
									// weblog_cf_country (in current preloaded
									// dataset)
	static final public byte[] INDEXTABLE_SPACEHOLDER = Bytes.toBytes("EMPTY");
	static final public String SCAN_INDEX_FAMILIY = "scan_index_family";
	static final public String SCAN_INDEX_QUALIFIER = "scan_index_qualifier";

	static final public String SCAN_START_VALUE = "scan_start_value";
	static final public String SCAN_STOP_VALUE = "scan_stop_value";
}
