package ict.ictbase.client;

import ict.ictbase.commons.global.GlobalHTableGetByIndex;
import ict.ictbase.util.HIndexConstantsAndUtils;

import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

public class Demo {

    public static void initTables(Configuration conf, String testTableName, String columnFamily, String indexedColumnName) throws Exception{
        Connection con = ConnectionFactory.createConnection(conf);
    	Admin admin = con.getAdmin();
    	TableName tn = TableName.valueOf(testTableName);
        if (!admin.isTableAvailable(tn)){
            HIndexConstantsAndUtils.createAndConfigBaseTable(conf, Bytes.toBytes(testTableName), Bytes.toBytes(columnFamily), new String[]{indexedColumnName});
        }
        
        if(admin.isTableDisabled(tn)){
        	admin.enableTable(tn);
        }

        byte[] indexTableName = HIndexConstantsAndUtils.generateIndexTableName(Bytes.toBytes(testTableName), Bytes.toBytes(columnFamily), Bytes.toBytes(indexedColumnName)/*TODO column family in index table*/);
        TableName indexTN=TableName.valueOf(indexTableName);
        if (!admin.isTableAvailable(indexTN)){
            HIndexConstantsAndUtils.createAndConfigIndexTable(conf, indexTableName, Bytes.toBytes(columnFamily));
        }
        if(admin.isTableDisabled(indexTN)){
        	admin.enableTable(indexTN);
        }
    }

    public static void initCoProcessors(Configuration conf, String coprocessorJarLoc, GlobalHTableGetByIndex htable) throws Exception {
       int coprocessorIndex = 1;
       HIndexConstantsAndUtils.updateCoprocessor(conf, htable.getTableName(), coprocessorIndex++, true, coprocessorJarLoc, "ict.ictbase.coprocessor.IndexObserverwReadRepair");
//       HIndexConstantsAndUtils.updateCoprocessor(conf, htable.getTableName(), coprocessorIndex++, true, coprocessorJarLoc, "ict.ictbase.coprocessor.PhysicalDeletionInCompaction");
       htable.configPolicy(GlobalHTableGetByIndex.PLY_READCHECK);
    }

    public static void main(String[] args) throws Exception{
       Configuration conf = HBaseConfiguration.create();
       String testTableName = "testtable";
       String columnFamily = "cf";
       String indexedColumnName = "country";

//       if(args.length <= 0){
//           System.err.println("format: java -cp <classpath> tthbase.client.Demo <where coproc.jar is>");
//           System.err.println("example: java -cp build/jar/libDeli-client.jar:conf:lib/hbase-binding-0.1.4.jar tthbase.client.Demo  /root/app/deli/build/jar/libDeli-coproc.jar ");
//           return;
//       }
//       String locCoproc = args[0];
//       String coprocessorJarLoc = "file:" + locCoproc;
       
       String coprocessorJarLoc="hdfs://data8:9000/jar/ICTBase-0.0.1-SNAPSHOT.jar";

       initTables(conf, testTableName, columnFamily, indexedColumnName);
       GlobalHTableGetByIndex htable = new GlobalHTableGetByIndex(conf, Bytes.toBytes(testTableName));
       initCoProcessors(conf, coprocessorJarLoc, htable);

       //put value1
       Put p = new Put(Bytes.toBytes("key1"));
       p.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(indexedColumnName), 101, Bytes.toBytes("v1"));
       htable.put(p);

       //getByIndex
       htable.configPolicy(GlobalHTableGetByIndex.PLY_FASTREAD);
    }
}
