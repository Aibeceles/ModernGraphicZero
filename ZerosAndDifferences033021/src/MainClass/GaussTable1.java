/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainClass;

/**
 *
 * @author Aibes
 */

import config.DbConfig;
import LoopLists.GaussBean1;
import LoopLists.LoopList;
import PArrayReset.PolynomialArray;
import fractionintegerset.FractionIntegerDriver;
import fractionintegerset.FractionIntegerDriverIterate;
import fractionintegerset.muNumDen;
import mucorrolationthreaded.*;
import java.beans.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import static java.lang.System.gc;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.sql.ResultSetMetaData;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.io.LocalOutputFile;
import org.apache.parquet.conf.PlainParquetConfiguration;
import java.nio.file.Paths;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

/**
 *  
 * @author Aibes
 * Writes directly to ZADDB or to .csv
 */

public class GaussTable1 extends Thread {
    private Semaphore tlSem;  
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private String sampleProperty;
    private PropertyChangeSupport propertySupport;
    private String framework = "embedded";
//    private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
//    private String protocol = "jdbc:derby:";
 
//    private String driver = "com.mysql.jdbc.Driver";
//    private String protocol = "jdbc:mysql://localhost:3306/";
    
    private String driver = "org.neo4j:neo4j-jdbc-driver:3.3.0";
    private String protocol = DbConfig.get("neo4j.url");

    String csvFile = "zad032819.csv";
    FileWriter writer;
    Semaphore configSemaphore;
    
    private Connection conn = null;
    ArrayList statements = new ArrayList(); // list of Statements, PreparedStatements
    PreparedStatement psInsert = null;
    PreparedStatement psUpdate = null;
    Statement s = null;
    Statement t = null;
    ResultSet rs = null;    
    
    rowBean dBbean = new rowBean();   // database buffer is a queue of rowBeans
    List dbbVector = new ArrayList();    
    LoopsDriver mangee = null;
    LoopsDriverTable lMangee = null;    //  totally need an interface here; murderus run90 otherwise
    LoopsDriverTwoPManager tMangee=null;
    LoopsDriverTwoP ttMangee=null;
    FractionIntegerDriver fMangee = null;
    FractionIntegerDriverIterate fiMangee=null;
    BigDecimal zero=new BigDecimal(0);
    LoopList cypherList;
    public int dimension=2;
    private volatile boolean producerDone = false;
    private static final String PARQUET_STAGING_DIR =
        Paths.get(System.getProperty("user.dir"), "data", "parquet_batches").toAbsolutePath().toString();
    
    public void setDimension(int dimension) {
    this.dimension=dimension;
    System.out.println(".setDimension dimension: "+this.dimension + " "+dimension);
    }

    public void setProducerDone() {
        this.producerDone = true;
        synchronized (dbbVector) {
            dbbVector.notifyAll();
        }
    }
    
    public synchronized void goCypher()  {
        try
        {     
            s = conn.createStatement();
            String d="d";
            statements.add(s);
            psInsert = conn.prepareStatement("Create (node:Node {vertex: ? } )");
            statements.add(psInsert);
            psInsert.setString(1, "seven");
            System.out.println(psInsert.execute());
            conn.commit();
           System.out.println("Committed the goCypher");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
           
    }
 
   public GaussTable1()  {
     //  this.dimension=dimension;
   }
 
    public GaussTable1(Semaphore tlSema,List dbVector,Semaphore configSemaphore) throws IOException{
       this.writer = new FileWriter(csvFile);
       dbbVector=dbVector;
       tlSem=tlSema;
  //     tMangee=mang;
       this.configSemaphore=configSemaphore;
    }   
   
   
   
  
    public GaussTable1(Semaphore tlSema,List dbVector,LoopsDriverTwoPManager mang,Semaphore configSemaphore) throws IOException{
       this.writer = new FileWriter(csvFile);
       dbbVector=dbVector;
       tlSem=tlSema;
       tMangee=mang;
       this.configSemaphore=configSemaphore;
    }
      
    public GaussTable1(Semaphore tlSema,List dbVector,LoopsDriverTwoP mang) throws IOException{
       this.writer = new FileWriter(csvFile);
       dbbVector=dbVector;
       tlSem=tlSema;
       ttMangee=mang;
    }
   
    public GaussTable1(Semaphore tlSema,List dbVector,LoopsDriverTable mang) throws IOException{
       this.writer = new FileWriter(csvFile);
       dbbVector=dbVector;
       tlSem=tlSema;
       lMangee=mang;
    }  
   
    public GaussTable1(Semaphore tlSema,List dbVector,LoopsDriver mang) throws IOException{
       this.writer = new FileWriter(csvFile);
       dbbVector=dbVector;
       tlSem=tlSema;
       mangee=mang;
    }   
  
    public GaussTable1(Semaphore tlSema,List dbVector,FractionIntegerDriver mang) throws IOException {
       this.writer = new FileWriter(csvFile);
       dbbVector=dbVector;
       tlSem=tlSema;
       fMangee=mang; 
    }   
    
    public GaussTable1(Semaphore tlSema,List dbVector,FractionIntegerDriverIterate mang) throws IOException {
        this.writer = new FileWriter(csvFile);
       dbbVector=dbVector;
       tlSem=tlSema;
       fiMangee=mang; 
    }   
   
/*  bString.size =2 constrains goFidCypher method call.
 *      
*/
     
private void runLoopsDriver() throws IOException {

    int batchSize = 3;
    int chainSize = 4;
    int batchNumber = batchSize * chainSize;
    List<GaussBean1> batch = new ArrayList<>();

    File dir = new File(PARQUET_STAGING_DIR);
    if (!dir.exists()) dir.mkdirs();
    System.out.println("Parquet staging dir: " + dir.getAbsolutePath());

    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
    String filePath = PARQUET_STAGING_DIR + File.separator + "batches_" + timestamp + "_" + UUID.randomUUID() + ".parquet";

    int fileBatch = 0;

    try (ParquetWriter<GenericRecord> writer = AvroParquetWriter
            .<GenericRecord>builder(new LocalOutputFile(Paths.get(filePath)))
            .withSchema(DNODE_BATCH_SCHEMA)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withConf(new PlainParquetConfiguration())
            .build()) {

        while (true) {
            try {
                synchronized (dbbVector) {
                    while (dbbVector.isEmpty() && !producerDone) {
                        dbbVector.wait(2000);
                    }
                    if (dbbVector.isEmpty() && producerDone) break;
                    if (dbbVector.size() < 5000) {
                        dbbVector.notifyAll();
                    }
                }

                if (!tlSem.tryAcquire(2, TimeUnit.SECONDS)) {
                    continue;
                }

                int available = Math.min(batchNumber, dbbVector.size());
                int count = (available / chainSize) * chainSize;
                if (count == 0 && available > 0) {
                    count = available;
                }
                for (int i = 0; i < count; i++) {
                    GaussBean1 gaussBean = (GaussBean1) dbbVector.get(0);
                    // System.out.println("GaussBean1:   " + gaussBean + " " + gaussBean.getfigPArray() + " " + gaussBean.getpArray() + " " + gaussBean.getLoopList().getVmResult() + " rootList: " + gaussBean.getLoopList().getrootList() + " pArrayIndexValue: " + gaussBean.getpArrayIndexValue() + " evaluateValue: " + gaussBean.getevaluateValue());
                    batch.add(gaussBean);
                    dbbVector.remove(0);
                }

                if (!batch.isEmpty()) {
                    LoopList[] loopLists = batch.stream().map(GaussBean1::getLoopList).toArray(LoopList[]::new);
                    String[] vmResults = batch.stream().map(GaussBean1::getLoopList).map(LoopList::getVmResult).toArray(String[]::new);
                    String[] pArrays = batch.stream().map(GaussBean1::getpArray).toArray(String[]::new);
                    int actualBatchSize = batch.size() / chainSize;
                    int remainder = batch.size() % chainSize;

                    for (int b = 0; b < actualBatchSize; b++) {
                        for (int c = 0; c < chainSize; c++) {
                            int idx = b * chainSize + c;
                            if (idx >= loopLists.length) break;

                            GenericRecord record = new GenericData.Record(DNODE_BATCH_SCHEMA);
                            record.put("vmResult", vmResults[idx]);
                            record.put("n", loopLists[idx].getNumerator());
                            record.put("d", loopLists[idx].getDenominator());
                            record.put("z", loopLists[idx].getTotalZero());
                            record.put("muList", loopLists[idx].getmuList());
                            record.put("rootList", loopLists[idx].getrootList());
                            record.put("wNum", loopLists[idx].getworkNum());
                            record.put("pArray", pArrays[idx]);
                            // System.out.println("DEBUG rootList[" + idx + "] wNum=" + loopLists[idx].getworkNum() + " rootList=" + loopLists[idx].getrootList());
                            record.put("batch_id", b);
                            record.put("chain_position", c);
                            record.put("file_batch", fileBatch);
                            writer.write(record);
                        }
                    }

                    if (remainder > 0) {
                        int baseIdx = actualBatchSize * chainSize;
                        for (int c = 0; c < remainder; c++) {
                            int idx = baseIdx + c;
                            GenericRecord record = new GenericData.Record(DNODE_BATCH_SCHEMA);
                            record.put("vmResult", vmResults[idx]);
                            record.put("n", loopLists[idx].getNumerator());
                            record.put("d", loopLists[idx].getDenominator());
                            record.put("z", loopLists[idx].getTotalZero());
                            record.put("muList", loopLists[idx].getmuList());
                            record.put("rootList", loopLists[idx].getrootList());
                            record.put("wNum", loopLists[idx].getworkNum());
                            record.put("pArray", pArrays[idx]);
                            //System.out.println("DEBUG rootList[" + idx + "] wNum=" + loopLists[idx].getworkNum() + " rootList=" + loopLists[idx].getrootList());
                            record.put("batch_id", actualBatchSize);
                            record.put("chain_position", c);
                            record.put("file_batch", fileBatch);
                            writer.write(record);
                        }
                    }

                    fileBatch++;
                    batch.clear();
                }

            } catch (InterruptedException ex) {
                Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    System.out.println("Wrote " + fileBatch + " batches to " + filePath);
}

private void ggoCCypherDebug(LoopList[] loopLists, String[] vmResults) {

    System.out.println("Executing goCCypher...");

    int[] numerators   = Arrays.stream(loopLists).mapToInt(LoopList::getNumerator).toArray();
    int[] denominators = Arrays.stream(loopLists).mapToInt(LoopList::getDenominator).toArray();
    int[] totalZeros   = Arrays.stream(loopLists).mapToInt(LoopList::getTotalZero).toArray();
    int[] wNumTags     = Arrays.stream(loopLists).mapToInt(LoopList::getworkNum).toArray();
    String[] muLists   = Arrays.stream(loopLists).map(LoopList::getmuList).toArray(String[]::new);
    String[] rootLists = Arrays.stream(loopLists).map(LoopList::getrootList).toArray(String[]::new);

    PreparedStatement psInsert = null;
    ResultSet rs = null;
    try {
        psInsert = conn.prepareStatement(
            "WITH ? AS vmResult, ? AS n, ? AS d, ? AS z, ? AS m, ? AS r, ? AS w\n" +
            "UNWIND RANGE(0, SIZE(vmResult) - 1) AS idx\n" +
            "WITH vmResult[idx] AS vmResult, n[idx] AS n, d[idx] AS d, z[idx] AS z, m[idx] AS m, r[idx] AS r, w[idx] AS w \n" +

            "OPTIONAL MATCH (dNode:Dnode {vmResult:vmResult})\n" +
            "WITH dNode, vmResult, n, d, z, m, r, w\n" +
            "CALL {\n" +
                "WITH dNode, vmResult, n, d, z, m, r, w\n" +
                "WITH dNode, vmResult, n, d, z, m, r, w WHERE dNode IS NULL\n" +
                "CREATE (node:Dnode {vmResult:vmResult})\n" +
                "SET\n" +
                "node.muList=m,\n" +
                "node.rootList=r,\n" +
                "node.totalZeros=z,\n" +
                "node.n=n,\n" +
                "node.d=d,\n" +
                "node.wNum=w,\n" +
                "node.new=TRUE\n" +
                "RETURN node, TRUE AS created\n" +
                "UNION ALL\n" +
                "WITH dNode, vmResult, n, d, z, m, r, w\n" +
                "WITH dNode, vmResult, n, d, z, m, r, w WHERE dNode IS NOT NULL\n" +
                "RETURN NULL AS node, FALSE AS created\n" +
            "}\n" +
            "WITH created, CASE WHEN dNode IS NULL THEN node ELSE dNode END AS dNode\n" +
            "WITH collect([dNode, created]) AS nodeList\n" +
            "UNWIND range(0,size(nodeList)-2) AS index\n" +
            "WITH nodeList, index, nodeList[index][0] AS source, nodeList[index+1][0] AS target WHERE nodeList[index][1]\n" +
            "MERGE (source)-[:zMap]->(target)\n" +
            // Return summary counters via Cypher's built-in summary
            "RETURN TRUE AS completed;"
        );

        Array vmResultsArray = conn.createArrayOf("VARCHAR", vmResults);
        Array muListsArray   = conn.createArrayOf("VARCHAR", muLists);
        Array rootListsArray = conn.createArrayOf("VARCHAR", rootLists);

        Long[] numeratorsArray  = Arrays.stream(numerators).mapToObj(i -> (long) i).toArray(Long[]::new);
        Long[] denominatorArray = Arrays.stream(denominators).mapToObj(i -> (long) i).toArray(Long[]::new);
        Long[] zerosArray       = Arrays.stream(totalZeros).mapToObj(i -> (long) i).toArray(Long[]::new);
        Long[] wNumArray        = Arrays.stream(wNumTags).mapToObj(i -> (long) i).toArray(Long[]::new);

        Array nArray = conn.createArrayOf("INTEGER", numeratorsArray);
        Array dArray = conn.createArrayOf("INTEGER", denominatorArray);
        Array zArray = conn.createArrayOf("INTEGER", zerosArray);
        Array wArray = conn.createArrayOf("INTEGER", wNumArray);

        psInsert.setArray(1, vmResultsArray);
        psInsert.setArray(2, nArray);
        psInsert.setArray(3, dArray);
        psInsert.setArray(4, zArray);
        psInsert.setArray(5, muListsArray);
        psInsert.setArray(6, rootListsArray);
        psInsert.setArray(7, wArray);

        // ── KEY CHANGE: use executeQuery() so we get the full response ──
        boolean hasResultSet = psInsert.execute();

        if (hasResultSet) {
            rs = psInsert.getResultSet();

            // Print every column of every row returned
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            System.out.println("=== Query Response ===");
            // Print column headers
            StringBuilder header = new StringBuilder();
            for (int c = 1; c <= colCount; c++) {
                header.append(String.format("%-20s", meta.getColumnName(c)));
            }
            System.out.println(header);
            System.out.println("-".repeat(colCount * 20));

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                StringBuilder row = new StringBuilder();
                for (int c = 1; c <= colCount; c++) {
                    Object val = rs.getObject(c);
                    row.append(String.format("%-20s", val != null ? val.toString() : "NULL"));
                }
                System.out.println(row);
            }
            System.out.println("=== Total rows returned: " + rowCount + " ===");

        } else {
            // No result set — fall back to update count
            int updateCount = psInsert.getUpdateCount();
            System.out.println("=== No ResultSet. Update count: " + updateCount + " ===");
        }

        // ── OPTIONAL: Neo4j / GQL drivers expose SummaryCounters ──
        // If your JDBC driver wraps a Neo4j driver, you can cast and inspect:
        //
        // if (psInsert instanceof Neo4jPreparedStatement neo4jPs) {
        //     ResultSummary summary = neo4jPs.getResultSummary();
        //     SummaryCounters counters = summary.counters();
        //     System.out.println("Nodes created  : " + counters.nodesCreated());
        //     System.out.println("Nodes deleted  : " + counters.nodesDeleted());
        //     System.out.println("Rels created   : " + counters.relationshipsCreated());
        //     System.out.println("Props set      : " + counters.propertiesSet());
        // }

        conn.commit();
        //System.out.println("COMMITTED");

    } catch (SQLException sqle) {
        printSQLException(sqle);
    } finally {
        try { if (rs       != null) rs.close();       } catch (SQLException ex) { Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex); }
        try { if (psInsert != null) psInsert.close();  } catch (SQLException ex) { Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex); }
    }
}





//private void ggoCCypher(String[] figPArrays, String[] pArrays, LoopList[] loopLists, int[] workNums, String[] vmResults, int[] resultListIds, String[] fPArrays, String[] muMaskLists) {

private void ggoCCypher(LoopList[] loopLists, String[] vmResults, int batchNumber, int chainSize) {

        System.out.println("Executing goCCypher...");
        int[] numerators = Arrays.stream(loopLists).mapToInt(LoopList::getNumerator).toArray();
        int[] denominators = Arrays.stream(loopLists).mapToInt(LoopList::getDenominator).toArray();
        int[] totalZeros = Arrays.stream(loopLists).mapToInt(LoopList::getTotalZero).toArray();
        int[] wNumTags = Arrays.stream(loopLists).mapToInt(LoopList::getworkNum).toArray();

        String[] muLists = Arrays.stream(loopLists).map(LoopList::getmuList).toArray(String[]::new);
        String[] rootLists = Arrays.stream(loopLists).map(LoopList::getrootList).toArray(String[]::new);
        
        PreparedStatement psInsert = null;
        try {
             psInsert = conn.prepareStatement(
                "CALL apoc.periodic.iterate(\n" +
                "// Single-row source — just passes the raw lists through\n" +
                "'RETURN $vmResult AS vmResult, $n AS n, $d AS d, $z AS z, $m AS m, $r AS r, $w AS w, $batchNumber AS batchNumber, $chainSize AS chainSize',\n" +
                "// Action query — owns the UNWIND and all processing\n" +
                "'\n" +
                "WITH vmResult, n, d, z, m, r, w, batchNumber, chainSize\n" +
                "UNWIND RANGE(0, batchNumber - 1) AS batch\n" +
                "UNWIND RANGE(0, chainSize - 1) AS idx\n" +
                "WITH vmResult[batch*chainSize+idx] AS vmResult, n[batch*chainSize+idx] AS n, d[batch*chainSize+idx] AS d, z[batch*chainSize+idx] AS z,\n" +
                "m[batch*chainSize+idx] AS m, r[batch*chainSize+idx] AS r, w[batch*chainSize+idx] AS w\n" +
                "OPTIONAL MATCH (dNode:Dnode {vmResult: vmResult})\n" +
                "CALL {\n" +
                "WITH dNode, vmResult, n, d, z, m, r, w\n" +
                "WITH dNode, vmResult, n, d, z, m, r, w WHERE dNode IS NULL\n" +
                "CREATE (node:Dnode {vmResult: vmResult})\n" +
                "SET\n" +
                "node.muList     = m,\n" +
                "node.rootList   = r,\n" +
                "node.totalZeros = z,\n" +
                "node.n          = n,\n" +
                "node.d          = d,\n" +
                "node.wNum       = w,\n" +
                "node.new        = TRUE\n" +
                "RETURN node, TRUE AS created\n" +
                "UNION ALL\n" +
                "WITH dNode, vmResult, n, d, z, m, r, w\n" +
                "WITH dNode, vmResult, n, d, z, m, r, w WHERE dNode IS NOT NULL\n" +
                "RETURN NULL AS node, FALSE AS created\n" +
                "}\n" +
                "WITH created, CASE WHEN dNode IS NULL THEN node ELSE dNode END AS dNode\n" +
                "WITH collect([dNode, created]) AS nodeList\n" +
                "UNWIND range(0, size(nodeList) - 2) AS index\n" +
                "WITH nodeList, index, nodeList[index][0] AS source, nodeList[index+1][0] AS target\n" +
                "WHERE nodeList[index][1]\n" +
                "MERGE (source)-[:zMap]->(target)\n" +
                "RETURN count(*) AS mergedRels\n" +
                "',\n" +
                "{batchSize: 1, parallel:  false, params: { vmResult: ?, n:?, d:?, z:?, m:?, r:?, w:?, batchNumber:?, chainSize:?}}\n" +
                " )\n" +
                "YIELD batches, total, errorMessages\n" +
                "RETURN batches, total, errorMessages;");  

       Array vmResultsArray = conn.createArrayOf("VARCHAR", vmResults);
       Array muListsArray = conn.createArrayOf("VARCHAR", muLists);
       Array rootListsArray = conn.createArrayOf("VARCHAR", rootLists); 

       Long[] numeratorsArray = Arrays.stream(numerators)
                               .mapToObj(i -> (long) i)  // Explicit conversion to Long
                               .toArray(Long[]::new);
       Array nArray = conn.createArrayOf("INTEGER", numeratorsArray);
       
       Long[] denominatorArray = Arrays.stream(denominators)
                               .mapToObj(i -> (long) i)  // Explicit conversion to Long
                               .toArray(Long[]::new);
       Array dArray = conn.createArrayOf("INTEGER", denominatorArray);

       Long[] zerosArray = Arrays.stream(totalZeros)
                               .mapToObj(i -> (long) i)  // Explicit conversion to Long
                               .toArray(Long[]::new);
       Array zArray = conn.createArrayOf("INTEGER", zerosArray);


       Long[] wNumArray = Arrays.stream(wNumTags)
                               .mapToObj(i -> (long) i)  // Explicit conversion to Long
                               .toArray(Long[]::new);
       Array wArray = conn.createArrayOf("INTEGER", wNumArray);




       
        psInsert.setArray(1, vmResultsArray);
        psInsert.setArray(2, nArray); 
        psInsert.setArray(3, dArray);        
        psInsert.setArray(4, zArray);
        psInsert.setArray(5, muListsArray);
        psInsert.setArray(6, rootListsArray);
        psInsert.setArray(7, wArray);  

        psInsert.setInt(8, batchNumber);
        psInsert.setInt(9, chainSize);  

       psInsert.executeUpdate();
       conn.commit();
       //System.out.println("    COMMITTED");
       } catch (SQLException sqle) {
       printSQLException(sqle);
       } finally {
       try {
           if (psInsert != null) {
               psInsert.close();
           }
        } catch (SQLException ex) {
           Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex);
       }
   
    }

}

private static final Schema DNODE_BATCH_SCHEMA = SchemaBuilder.record("DnodeBatch")
    .namespace("MainClass")
    .fields()
    .requiredString("vmResult")
    .requiredInt("n")
    .requiredInt("d")
    .requiredInt("z")
    .requiredString("muList")
    .requiredString("rootList")
    .requiredInt("wNum")
    .requiredString("pArray")
    .requiredInt("batch_id")
    .requiredInt("chain_position")
    .requiredInt("file_batch")
    .endRecord();



//  shut off for batching 02260225
private void rrunLoopsDriver() throws IOException {
 

// uppercaseNames = ["ALICE", "BOB", "CHARLIE"]



//          while ((mangee.isAlive()) || (dbbVector.size()>0)) {       
             int x=0;
//             while ( (tMangee.isAlive())   || (dbbVector.size()>0)) {
             while ((dbbVector.size()>0)) {
             if (dbbVector.size()<5000) synchronized (dbbVector) {{dbbVector.notify();}}
             x++;
             if (x %1000 ==0 ) {gc(); }
             try {
                tlSem.acquire();
             System.out.println("while looppey " + " "+dbbVector.size());
                //              System.out.println("                       gauss6able      ");
                if (dbbVector.size()>0) {
                try {
  //                System.out.println("GaussBean1:   " + ((GaussBean1)dbbVector.get(0)).toString());
                  System.out.println("GaussBean1:   " + ((GaussBean1)dbbVector.get(0)).toString()+ " "+((GaussBean1)dbbVector.get(0)).getfigPArray()+" "+((GaussBean1)dbbVector.get(0)).getpArray()+" "+((GaussBean1)dbbVector.get(0)).getLoopList().getVmResult()+    "pArrayIndexValue: "+((GaussBean1)dbbVector.get(0)).getpArrayIndexValue()+    "evaluateValue: "+((GaussBean1)dbbVector.get(0)).getevaluateValue());    
   //             go(((GaussBean1)dbbVector.get(0)).getpArray(),((GaussBean1)dbbVector.get(0)).getLoopList(),((GaussBean1)dbbVector.get(0)).getWNum(),((GaussBean1)dbbVector.get(0)).getVmResult(),((GaussBean1)dbbVector.get(0)).getresultlistid());
                if (((GaussBean1)dbbVector.get(0)).getLoopList().getworkNum()==0) {cypherList=((GaussBean1)dbbVector.get(0)).getLoopList(); }
                goCCypher(((GaussBean1)dbbVector.get(0)).getfigPArray(),((GaussBean1)dbbVector.get(0)).getpArray(),((GaussBean1)dbbVector.get(0)).getLoopList(),((GaussBean1)dbbVector.get(0)).getLoopList().getworkNum(),((GaussBean1)dbbVector.get(0)).getLoopList().getVmResult(),((GaussBean1)dbbVector.get(0)).getresultlistid(),((GaussBean1)dbbVector.get(0)).getfPArray(),((GaussBean1)dbbVector.get(0)).getMuMaskList()); 

                dbbVector.remove(0);
                } catch (NullPointerException e) {
                System.out.println("                                                                                   dbVector empty"+ e.toString());
                dbbVector.remove(0);
                }
                }
                else {System.out.println("if dbbVector.size()>0 else println   " );}
//                dbbVector.removeElementAt(0);
            } catch (InterruptedException ex) {
                Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("twotabledbthread dbVectorSize"+ dbbVector.size());
           }
           System.out.println("exit rrunLoopsDriver()"); 
}    
    
   

    public void run(){
     long threadId = Thread.currentThread().getId();
      System.out.println("                                                                          GaussTable1"+"threadId: "+threadId);
      startConn();
      System.out.println("                                                                                         It is inside GaussCorrolation? ");
      try {
          synchronized (dbbVector) {
              while (dbbVector.isEmpty() && !producerDone) {
                  dbbVector.wait(2000);
              }
          }
          System.out.println("Consumer ready — items available or producer done");
      } catch (InterruptedException ex) {
          Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex);
      }

       try {
          runLoopsDriver();
          //rrunLoopsDriver();
          } catch (IOException ex) {
              Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex);
          }

       System.out.println("gaussTable1 ended");
       endConn();      //  maybe endconn is throwing excejptions  
//       configSemaphore.release();
    } 


        
    public void tlToTable(BigDecimal trZero, BigDecimal trTriangle, BigDecimal muDecimal, BigDecimal rootOne, BigDecimal rootTwo,BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal determinat,BigDecimal numerator,BigDecimal denominator){

   
   //      go(trZero,trTriangle,muDecimal,rootOne,rootTwo,a,b,c,determinat,numerator,denominator);

       
       
      } 
        
    
 
     public void startConn()  {
     loadDriver();
      try {
           Properties props = new Properties();
           String dbName = DbConfig.get("neo4j.database");
           String protocol = DbConfig.get("neo4j.url") + "?database=" + dbName;
           
           conn = DriverManager.getConnection(protocol, DbConfig.get("neo4j.user"), DbConfig.get("neo4j.password"));
           System.out.println(conn.getMetaData().getURL());
           System.out.println("Connected to  database ZAD" );
            // We want to control transactions manually. Autocommit is on by
            // default in JDBC.
            conn.setAutoCommit(false);
           }

        catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 

   }

  public void endConn()  {
            {
                try
                {
                    conn.close();
                }
                catch (SQLException se)
                {
                    if (( (se.getErrorCode() == 50000)
                            && ("XJ015".equals(se.getSQLState()) ))) {
                        // we got the expected exception
                        System.out.println("Derby shut down normally");
                        // Nte that for single database shutdown, the expected
                        // SQL state is "08006", and the error code is 45000.
                    } else {
                        // if the error code or SQLState is different, we have
                        // an unexpected exception (shutdown failed)
                        System.err.println("Derby did not shut down normally");
                        printSQLException(se);
                    }
                }
            
//  need to close statements
            int i = 0;
            while (!statements.isEmpty()) {
                // PreparedStatement extend Statement
                Statement st = (Statement)statements.remove(i);
                try {
                    if (st != null) {
                        st.close();
                        st = null;
                    }
                } catch (SQLException sqle) {
                    printSQLException(sqle);
                }

            }
//  neede to close connection
            //Connection
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException sqle) {
                printSQLException(sqle);
            }
          //  there is no reading so result set is irrelevant.
          // release all open resources to avoid unnecessary memory usage
            // ResultSet
            try {
                if (rs != null) {
                    rs.close();
                    rs = null;
                }
            } catch (SQLException sqle) {
                printSQLException(sqle);
            }
        }
}

//  Sub methods implementing cypher querries:
// 
public ResultSet muQuerry(int denominator) { 
    Integer denInt = new Integer(denominator);
    String denString = denInt.toString();
    startConn();
    ResultSet rs=null;
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
        "MATCH (e:MuNumber {d:?})\n" +
        "RETURN e.nBinary,e.n,e.d LIMIT 1"); 
        statements.add(psInsert);
        psInsert.setString(1,denString);

        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
         endConn();
        return(rs);
} 

public ResultSet configureQuery() { 

    startConn();
    ResultSet rs=null;
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
        "MATCH (e:Configure)\n" +
        "RETURN e.setProductRange,e.setProductRAngeIncrement,e.maxSetProductRange,e.dimension,e.maxFigPScalar,e.integerRange"); 
        statements.add(psInsert);
//        psInsert.setString(1,denString);

        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
         endConn();
        return(rs);
}

public ResultSet configureQuery1() { 

    startConn();
    ResultSet rs=null;
    try
     {  
        s = conn.createStatement();
        statements.add(s);

        psInsert = conn.prepareStatement(
            "CYPHER runtime=interpreted\n" +
            "MATCH (lock:ConfigLock {name:'configure-queue'})\n" +
            "SET lock.token = randomUUID()\n" +
            "WITH lock\n" +
            "MATCH (e:Configure)\n" +
            "WITH e ORDER BY id(e) LIMIT 1\n" +
            "WITH e, e.setProductRange AS setProductRange, " +
            "e.setProductRAngeIncrement AS setProductRAngeIncrement, " +
            "e.maxSetProductRange AS maxSetProductRange, " +
            "e.dimension AS dimension, " +
            "e.maxFigPScalar AS maxFigPScalar, " +
            "e.integerRange AS integerRange, " +
            "e.pArray AS pArray\n" +
            "DELETE e\n" +
            "RETURN setProductRange, setProductRAngeIncrement, maxSetProductRange, " +
            "dimension, maxFigPScalar, integerRange, pArray"
        );
 
        statements.add(psInsert);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
         endConn();
        return(rs);
}

public ResultSet muQuerry() { 
//    Integer denInt = new Integer(denominator);
//    String denString = denInt.toString();
    startConn();
    ResultSet rs=null;
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
        "MATCH (e:MuNumber)\n" +
        "RETURN e.nBinary,e.n,e.d LIMIT 1"); 
        statements.add(psInsert);
//        psInsert.setString(1,denString);

        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
         endConn();
        return(rs);
}

public void muDelete(String nBinary) {
    startConn();
    PreparedStatement psInsert = null; 
    try {
         psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
         "MATCH (e:MuNumber {nBinary:?})\n" +
         "DELETE e"); 
         psInsert.setString(1, nBinary);
         psInsert.executeUpdate();
         conn.commit();
        } catch (SQLException sqle) { printSQLException(sqle); }
//        try {rs.close(); } catch (SQLException ex) {
//            Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex);
//        }
        try {
            psInsert.close();
        } catch (SQLException ex) {
            Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex);
        }
         endConn();
}  
  
private void wZeroPattern (String vmResult,PolynomialArray fPArray,int n,int d, int totalZero, ArrayList muList, ArrayList rootList,int wNum,int rCounter,String pArray,int translateIndex,int pArrayIndexValue,
                          String muMaskList)  {
    int determined=0;
//    System.out.println("wPatternZZ this.dimension: " + this.dimension);
    if (dimension-wNum==totalZero)  {determined=1;} 
//    System.out.println("wPatternZZ  dimension,wnum,totalZero,determined: "+dimension+" "+wNum+" "+" "+totalZero+" "+determined);
    PreparedStatement psInsert = null;
    try {
       psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
            "MERGE (d:Dnode {vmResult:?,n:?,d:?,totalZero:?,muList:?,rootList:?,determined:?})\n" +
            "MERGE (c:CreatedBy {resultId:?, wNum:?,pArray:?,skipList:?} )\n" +
            "MERGE (d)-[ee:CreatedBye]->(c) ");
            psInsert.setString(1, vmResult);
            psInsert.setInt(2, n);
            psInsert.setInt(3, d);            
            psInsert.setInt(4, totalZero);
            psInsert.setString(5, muList.toString());
            psInsert.setString(6, rootList.toString());
            psInsert.setInt(7, determined);
            psInsert.setInt(8, rCounter);
            psInsert.setInt(9, wNum);
            psInsert.setString(10,pArray);
            psInsert.setString(11,muMaskList);
            psInsert.executeUpdate();
            conn.commit();            
    } catch (SQLException sqle) { printSQLException(sqle); }  
//    statements.remove(0);
    try {
        psInsert.close();
    } catch (SQLException ex) {
        Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex);
    }
}

private void wPattern (String vmResult,PolynomialArray fPArray,int n,int d, int totalZero, ArrayList muList, ArrayList rootList,int wNum,int rCounter,String pArray,int translateIndex,int pArrayIndexValue,
                          String muMaskList)  {
    int determined=0;
//   System.out.println("wPatternZZ this.dimension: " + this.dimension);
    if (dimension-wNum==totalZero)  {determined=1;} 
//    System.out.println("wPatternZZ  dimension,wnum,totalZero,determined: "+dimension+" "+wNum+" "+" "+totalZero+" "+determined); 
    PreparedStatement psInsert = null;
    try {
       psInsert = conn.prepareStatement(" CYPHER runtime=interpreted\n" +
            "MERGE (d:Dnode {vmResult:?,n:?,d:?,totalZero:?,muList:?,rootList:?,determined:?})\n" +
            "MERGE (c:CreatedBy {resultId:?, wNum:?,pArray:?,skipList:?} )\n" +
            "MERGE  (d)-[ee:CreatedBye]->(c)\n" +
            "WITH d,c\n" +               
            "MATCH  (dd:Dnode)-[:CreatedBye]->(cc:CreatedBy {resultId:? , wNum:?})\n" +
            "WITH d,dd\n" +   
            "MERGE (d)-[:zMap]->(dd)");
            psInsert.setString(1, vmResult);
            psInsert.setInt(2, n);
            psInsert.setInt(3, d);            
            psInsert.setInt(4, totalZero);
            psInsert.setString(5, muList.toString());
            psInsert.setString(6, rootList.toString());
            psInsert.setInt(7, determined);            
            psInsert.setInt(8, rCounter);
            psInsert.setInt(9, wNum);
            psInsert.setString(10,pArray);
            psInsert.setString(11,muMaskList);
            psInsert.setInt(12, rCounter);
            psInsert.setInt(13, wNum-1);
            psInsert.executeUpdate();
            conn.commit();            
    } catch (SQLException sqle) { printSQLException(sqle); }  
    System.out.println("Exited wPattern");
//    statements.remove(0);
    try {
        psInsert.close();
    } catch (SQLException ex) {
        Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex);
    }
}

public synchronized void goCCypher(String figPArray,String pArray,LoopList loopList,int wNum,String vmResult,int rCounter, PolynomialArray fPArray,String muMaskList)  {
    BigDecimal bD= new BigDecimal(0);
    Double bDD;
    int index;
    System.out.println("goCCypher(),wNm" +wNum);
//    System.out.println("goCCypher() "+loopList.toString());
        if (wNum==0) {
            wZeroPattern(vmResult,fPArray,loopList.getNumerator(),loopList.getDenominator(),loopList.getTotalZero(),loopList.getmuListt(),loopList.getrootListt(),wNum,rCounter,pArray,0,0,muMaskList);
       }
        if (wNum>0)
        {
            wPattern(vmResult,fPArray,loopList.getNumerator(),loopList.getDenominator(),loopList.getTotalZero(),loopList.getmuListt(),loopList.getrootListt(),wNum,rCounter,pArray,0,0,muMaskList);
        }
    System.out.println("exit goCCypher()");
}
    
    private static String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }

    public static void writeLine(Writer w, List<String> values, char separators, char customQuote) throws IOException {

        boolean first = true;
        //default customQuote is empty
        if (separators == ' ') {
            separators = ',';
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separators);
            }
            if (customQuote == ' ') {
                sb.append(followCVSformat(value));
            } else {
                sb.append(customQuote).append(followCVSformat(value)).append(customQuote);
            }

            first = false;
        }
 //       System.out.println("GaussTable1.writeLine sb length: "+sb.length());
        sb.append("\n");
        w.append(sb.toString());
 //       System.out.println("GaussTable1.writeLine sb length: "+sb.length());
   
    }

/*
*   https://stackoverflow.com/questions/29674354/java-arraylistinteger-to-arrayliststring-vice-versa
*/

public ArrayList<Integer> arrayStringToIntegerArrayList(String arrayString){
    String removedBrackets = arrayString.substring(1, arrayString.length() - 1);
    String[] individualNumbers = removedBrackets.split(",");
    ArrayList<Integer> integerArrayList = new ArrayList<>();
    for(String numberString : individualNumbers){
        integerArrayList.add(Integer.parseInt(numberString.trim()));
    }
    return integerArrayList;
}

 
    
    private void loadDriver() {
        try {
    
            Class.forName("org.neo4j.jdbc.Driver").newInstance();
          //  Class.forName(driver).newInstance();
            System.out.println("Loaded the appropriate driver");
        } catch (ClassNotFoundException cnfe) {
            System.err.println("\nUnable to load the JDBC driver " + driver);
            System.err.println("Please check your CLASSPATH.");
            cnfe.printStackTrace(System.err);
        } catch (InstantiationException ie) {
            System.err.println(
                        "\nUnable to instantiate the JDBC driver " + driver);
            ie.printStackTrace(System.err);
        } catch (IllegalAccessException iae) {
            System.err.println(
                        "\nNot allowed to access the JDBC driver " + driver);
            iae.printStackTrace(System.err);
        }
    }

    private void reportFailure(String message) {
        System.err.println("\nData verification failed:");
        System.err.println('\t' + message);
    }

    public static void printSQLException(SQLException e)
    {
        // Unwraps the entire exception chain to unveil the real cause of the
        // Exception.
        while (e != null)
        {
            System.err.println("\n----- SQLException -----");
            System.err.println("  SQL State:  " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  Message:    " + e.getMessage());
            // for stack traces, refer to derby.log or uncomment this:
            e.printStackTrace(System.err);
            e = e.getNextException();
        }
    }


    private void parseArguments(String[] args)
    {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("derbyclient"))
            {
                framework = "derbyclient";
                driver = "org.apache.derby.jdbc.ClientDriver";
                protocol = "jdbc:derby://localhost:1527/";
            }
        }
    }   

}




