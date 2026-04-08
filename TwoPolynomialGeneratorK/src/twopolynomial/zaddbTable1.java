package twopolynomial;

/*
 * Copyright (C) 2019 Aibes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Write TwoPolynomial objects to file.
 * @author Aibes
 */

public class zaddbTable1 extends Thread {
    private Semaphore tlSem;  
    private String csvFile;             
    FileWriter writer;
    rowBean dBbean = new rowBean();   
    vertexVector dbbVector = new vertexVector();    
    plvManager mangee = null;
    BigDecimal zero=new BigDecimal(0);
    vertexVector cypherList;
    private BigDecimal NN=null;
    private BigDecimal flatFileRowCounterr=null;
    private BigDecimal nMaxx=null;   
    private BigDecimal tSeqDB=null;
    private BigDecimal bTermDB=null;
    private BigDecimal targetEvaluateDB=null;
    private String vertexDB=null;
    private vertex vertexDBVertex=null;
    private String vertexDegreeDB=null;
    private String vertexScalarDB=null;
    
    final Properties kafkaProps = new Properties();
 //   final Producer<String, String> producer;
    
    public zaddbTable1(Semaphore tlSema,vertexVector dbVector,plvManager mang,String passedCsvFile) throws IOException{
       this.kafkaProps.put("bootstrap.servers", "localhost:9092");
       this.kafkaProps.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
       this.kafkaProps.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
       dbbVector=dbVector;
       tlSem=tlSema;
       mangee=mang;
       csvFile=passedCsvFile;
       this.writer = new FileWriter(csvFile,true);
       
    } 

    
 /*      Refactor for map structure to be streamed to kafka.
  *      not to need new map each while mangee.isAlive.
  *      put will overwrite the key.  
  *      So one map before key.   Note writer is still opend and closed.
 */   
    
private void runTwoPolyDriver() throws IOException {

    BigDecimal targetEvaluate = new BigDecimal(1);
    BigDecimal dCounter = new BigDecimal(0);
    BigDecimal two = new BigDecimal(2);
    BigDecimal one = new BigDecimal(1);
    BigDecimal isSameNN = new BigDecimal(0);
    final Producer producer = new KafkaProducer(kafkaProps);
    List valueList=new ArrayList();
    valueList.add("NN");valueList.add("flatFileRowCounterr");valueList.add("nMaxx");valueList.add("tSeqDB");
    valueList.add("bTermDB");valueList.add("vertexDBVertex");valueList.add("vertexDegreeDB");
    valueList.add("vertexScalarDB");valueList.add("vertexDB");valueList.add("targetEvaluate");
    writeLine(writer, valueList,';',' '); 
//    valueList.clear();    //  only one valueList.  the array is otherwise a HashSet
    Map mMap = new HashMap();
    String jSMapString=null;
    int vLIndex=0;
    System.out.println("Passed csv File: " + csvFile);
    while ((mangee.isAlive()) || (dbbVector.size()>0)) {       
    try {
    tlSem.acquire();
    if (dbbVector.size()>0) {
    try {
    this.NN=((rowBean)dbbVector.get(0)).getNN();
    this.flatFileRowCounterr=((rowBean)dbbVector.get(0)).getflatFileRowCounterr();
    this.nMaxx=((rowBean)dbbVector.get(0)).getNMax();
    this.tSeqDB=((rowBean)dbbVector.get(0)).gettSeqDB();
    this.bTermDB=((rowBean)dbbVector.get(0)).getbTermDB();
    this.vertexDBVertex=((rowBean)dbbVector.get(0)).getvertex();
    this.vertexDegreeDB=this.vertexDBVertex.getDegree().toString();
    this.vertexScalarDB=this.vertexDBVertex.getScalar().toString();
    this.vertexDB=this.vertexDBVertex.toString();
    if (isSameNN.compareTo(this.NN) != 0 ) {
    targetEvaluate=one;dCounter=zero;
    while (dCounter.compareTo(this.NN) == -1)  {
     targetEvaluate=targetEvaluate.multiply(two);
     dCounter=dCounter.add(one);
     isSameNN=this.NN;
    }
    System.out.println(".NN,  .nMaxx, : "+ this.NN.toString() + " "+this.nMaxx.toString());
    }                 
    mMap.put(valueList.get(vLIndex),this.NN.toString());vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));
    mMap.put(valueList.get(vLIndex),this.flatFileRowCounterr.toString());vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));
    mMap.put(valueList.get(vLIndex),this.nMaxx.toString());vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));
    mMap.put(valueList.get(vLIndex),this.tSeqDB.toString());vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));
    mMap.put(valueList.get(vLIndex),this.bTermDB.toString());vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));    
    mMap.put(valueList.get(vLIndex),this.vertexDBVertex.toString());vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));
    mMap.put(valueList.get(vLIndex),this.vertexDegreeDB);vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));
    mMap.put(valueList.get(vLIndex),this.vertexScalarDB);vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));
    mMap.put(valueList.get(vLIndex),this.vertexDB);vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));
    mMap.put(valueList.get(vLIndex),targetEvaluate.toString());vLIndex++;
    System.out.println(valueList.get(vLIndex-1)+" "+mMap.get(valueList.get(vLIndex-1)));
    vLIndex=0;
    
        jSMapString =  new ObjectMapper().writeValueAsString(mMap);
         System.out.println( new ObjectMapper().writeValueAsString(mMap));
        ProducerRecord record = new ProducerRecord("twoPoly",null,jSMapString);
//        try {
         RecordMetadata m;
        try {
            System.out.println("Before it didn't happen");
            m = (RecordMetadata) producer.send(record).get();
            System.out.println("Message produced, offset: " + m.offset());
            System.out.println("Message produced, partition : " + m.partition());
            System.out.println("Message produced, topic: " + m.topic());
            
        } catch (InterruptedException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
           System.out.println("somethind did happen first catch ");
        } catch (ExecutionException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
           System.out.println("somethind did happen second catch ");
        }
        finally {
            System.out.println("somethind did happen finally");
            producer.flush();
                   }
//    writeLine(writer, valueList,';',' ');       
//    valueList.clear();
    dbbVector.remove(0);
    } catch (NullPointerException e) {
    System.out.println("                                                                                   dbVector empty"+ e.toString());
    dbbVector.remove(0);
    }
    }
    else {System.out.println("   " );}
    } catch (InterruptedException ex) {
    Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
    }
//    if (dbbVector.size()%100==0) {writer.flush();}
    }
    writer.close();
   // producer.close();
}    

  
    public void run(){

      try {
        Thread.sleep(3000);
      } catch (InterruptedException ex) {
        Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
      }
       if (mangee!=null) {
          try {          
              runTwoPolyDriver();
          } catch (IOException ex) {
              Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
          }
       } else {
       }
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
        sb.append("\n");
        w.append(sb.toString());
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
            e = e.getNextException();
        }
    }

 
    
 }




