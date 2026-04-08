/*
 * Copyright (C) 2020 ChiefQuippy
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
package zadscripts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.gc;

/**
 *
 * @author ChiefQuippy
 */
public class ScriptsAutomation {

    private final String driver = "org.neo4j:neo4j-jdbc-driver:4.0.0";
    private final String protocol = DbConfig.get("neo4j.url") + "?database=" + DbConfig.get("neo4j.database");
    private Connection conn = null;
    ArrayList statements = new ArrayList(); // list of Statements, PreparedStatements
    PreparedStatement psInsert = null;
    PreparedStatement psUpdate = null;
    Statement s = null;
    Statement t = null;
    ResultSet rs = null;
    ScriptReturnBoolean srBol=null;
    WriteCSV wCsv=null;
    private String vertexDB=null;
    Producer<String, String> producer=null;
    ResultSetMap rSMap=null;
    
public ScriptsAutomation(ScriptReturnBoolean srBol, Producer producer, ResultSetMap rSMap) {
    this.srBol=srBol;
    this.producer=producer;
    this.rSMap=rSMap;
    startConn();
}    


public static void helloZeppelin() {
    System.out.print("Hello Zeppelin");
}


public void pollS12() throws JsonProcessingException {
 int pollcounter=0;
// this.startConn();
 System.out.println(rSMap.getValueS12("tConstraint")+" "+ rSMap.getValueS12("rangeLow") + " " + rSMap.getValueS12("rangeHigh") + " " + rSMap.getValueS12("index") + " " + rSMap.getValueS12("dimensionRead1") + " " + rSMap.getValueS12("dimensionRead2") + " " + rSMap.getValueS12("writeDimension"));
// System.out.println(new Integer((String)rSMap.getValueS12("tConstraint")));
// rSMap.printMap();
 boolean pollLoop = true;
    while (pollLoop==true)
    {
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted \n" +
        "UNWIND range(toInteger(?),toInteger(?)) AS n\n" +
        "WITH toString(n) AS iN, ? as iM\n" +
        "MATCH (cl:CollectedProductArgNode {Dimension:?,MaxN:iM,N:iN,tft:?}) -[:Arguments]->(aN:ArgumentsNode {ScalarProduct:?})\n" +
        "WITH {tft:toString(cl.tft)} as line \n"  +
        "RETURN line");
        statements.add(psInsert);
        Integer tConstraint = new Integer(rSMap.getValueS12("tConstraint").toString());
        psInsert.setString(1,(String) rSMap.getValueS12("rangeLow"));                       
        psInsert.setString(2,(String)rSMap.getValueS12("rangeHigh"));
        psInsert.setString(3,(String)rSMap.getValueS12("index"));
        psInsert.setString(4,(String)rSMap.getValueS12("writeDimension"));
        psInsert.setInt(5,tConstraint.intValue());
        psInsert.setString(6,(String)rSMap.getValueRS("scalarProduct"));
//        System.out.println("scalarProduct: " + (String)rSMap.getValueRS("scalarProduct"));
        ResultSet rs = psInsert.executeQuery();
        String jSMapString=null;
        conn.commit();   
        Boolean hasRS = TRUE; 
        while ( hasRS ) {
        if (rs.next()) {
        Map rsMap = new HashMap();
        rsMap=((Map)rs.getObject(1));
        jSMapString =  new ObjectMapper().writeValueAsString(rsMap);
        System.out.println( new ObjectMapper().writeValueAsString(rsMap));
        pollLoop=false;
        } else {  hasRS=FALSE;}; //System.out.println("pollcounter:" + pollcounter++ );
      }
     }
         catch (SQLException sqle)
        { printSQLException(sqle); }
        statements.remove(0);
    }    
//    try {
//        if (!conn.isClosed())  {     this.endConn(); }
//    } catch (SQLException ex) {
//        Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
//    }   
}


public void pollS3t() throws JsonProcessingException {
    int pollcounter=0;
//    this.startConn();
//  rSMap.setKeyValueS3tMap("rangeLow",rangeLow);rSMap.setKeyValueS3tMap("rangeHigh",rangeHigh);rSMap.setKeyValueS3tMap("nMax",nMax);rSMap.setKeyValueS3tMap("dimension1",dimension1);rSMap.setKeyValueS3tMap("dimension2",dimension2);rSMap.setKeyValueS3tMap("dimension3",dimension3);rSMap.setKeyValueS3tMap("tConstraint",tConstraint);
    System.out.println("pollS3t"); 
 //   System.out.println(rSMap.getValueS3t("tConstraint")+" "+ rSMap.getValueS3t("rangeLow") + " " + rSMap.getValueS3t("rangeHigh") + " " + rSMap.getValueS3t("nMax") + " " + rSMap.getValueS3t("dimension1") + " " + rSMap.getValueS3t("dimension2") + " " + rSMap.getValueS3t("dimension3"));

//    rSMap.printMapt();
    boolean pollLoop = true;
    while (pollLoop==true)
    {
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted \n" +
//      "MATCH (i:IndexedBy {N:?,RowCounter:?,MaxN:?,Dimension:?} )-[eee:VertexIndexedBy {twoSeq:?, divisor:?}]->  (v:VertexNode {Scalar:?,Degree:toString(?)} )\n" +
        "MATCH (i:IndexedBy {N:?,RowCounter:?,MaxN:?,Dimension:?} )-[]->  (v:VertexNode {Scalar:?,Degree:toString(?)} )\n" +
        "WITH {Dimension:i.Dimension} as line \n"  +
        "RETURN line");
//        System.out.println("eTrm: " + (Long)rSMap.getValueRS3t("eTrm")+" "+(String) rSMap.getValueRS3t("iN")+" "+(String)rSMap.getValueRS3t("dimension")+" "+(String)rSMap.getValueRS("tScalar")+" "+(String)rSMap.getValueRS("tDegree"));
        Long tConstraint = (Long)rSMap.getValueRS3t("eTrm");
        statements.add(psInsert);
        psInsert.setString(1,(String) rSMap.getValueRS3t("iN"));                       
        psInsert.setLong(2,tConstraint);
        psInsert.setString(3,(String)rSMap.getValueRS3t("iM"));
        psInsert.setString(4,(String)rSMap.getValueRS3t("dimension"));
//        psInsert.setLong(5,tConstraint);
//        psInsert.setString(6,(String)rSMap.getValueRS("pTD"));
        psInsert.setString(5,(String)rSMap.getValueRS3t("tScalar"));
        psInsert.setString(6,(String)rSMap.getValueRS3t("tDegree"));
        ResultSet rs = psInsert.executeQuery();
        String jSMapString=null;
        conn.commit();   
        Boolean hasRS = TRUE; 
        while ( hasRS ) {
        if (rs.next()) {
        Map rsMap = new HashMap();
        rsMap=((Map)rs.getObject(1));
        jSMapString =  new ObjectMapper().writeValueAsString(rsMap);
//        System.out.println( new ObjectMapper().writeValueAsString(rsMap));
        pollLoop=false;
        } else {  hasRS=FALSE;}; //System.out.println("pollcounter:" + pollcounter++ );
      }
     }
         catch (SQLException sqle)
        { printSQLException(sqle); System.out.println("inside sqlerror catch");}
        statements.remove(0);
    }    
//    try {
//        if (!conn.isClosed())  {     this.endConn(); }
//    } catch (SQLException ex) {
//        Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
//    }   
}

public void pollS3a() throws JsonProcessingException {
    int pollcounter=0;
//    this.startConn();
    System.out.println("pollS3a"); 
//    rSMap.printMapa();
    boolean pollLoop = true;
    while (pollLoop==true)
    {
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted \n" +
        "MATCH  (i:IndexedByArgument {N:?,MaxN:?,Dimension:?} ) -[]-> (v:VertexNode {Scalar:?,Degree:toString(?)}) \n" +
        "WITH {Dimension:i.Dimension} as line \n"  +
        "RETURN line");
 //       System.out.println("eTrm: " + (Long)rSMap.getValueRS3a("eTrm")+" "+(String) rSMap.getValueRS3a("iN")+" "+(String)rSMap.getValueRS3a("dimension")+" "+(String)rSMap.getValueRS3a("tScalar")+" "+(String)rSMap.getValueRS3a("tDegree"));
        statements.add(psInsert);
        psInsert.setString(1,(String) rSMap.getValueRS3a("iN"));                       
        psInsert.setString(2,(String)rSMap.getValueRS3a("iM"));
        psInsert.setString(3,(String)rSMap.getValueRS3a("dimension"));
        psInsert.setString(4,(String)rSMap.getValueRS3a("tScalar"));
        psInsert.setString(5,(String)rSMap.getValueRS3a("tDegree"));
        ResultSet rs = psInsert.executeQuery();
        String jSMapString=null;
        conn.commit();   
        Boolean hasRS = TRUE; 
        while ( hasRS ) {
        if (rs.next()) {
        Map rsMap = new HashMap();
        rsMap=((Map)rs.getObject(1));
        jSMapString =  new ObjectMapper().writeValueAsString(rsMap);
//        System.out.println( new ObjectMapper().writeValueAsString(rsMap));
        pollLoop=false;
        } else {  hasRS=FALSE;};   //System.out.println("S3A pollcounter:" + pollcounter++ );
      }
     }
         catch (SQLException sqle)
        { printSQLException(sqle); System.out.println("inside sqlerror catch");}
        statements.remove(0);
    }    
//    try {
//        if (!conn.isClosed())  {     this.endConn(); }
//    } catch (SQLException ex) {
//        Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
//    }   
}






public void s12Dc(String degreeConst, String rangeLow,String rangeHigh,String index,String dimensionRead1,String dimensionRead2, String writeDimension, int tConstraint) throws JsonProcessingException  {
//    this.startConn();
    System.out.println("S12 tConstraint: "+ "  " + tConstraint + " " + rangeLow + " "+rangeHigh+" "+index+" "+dimensionRead1+" "+dimensionRead2+" "+writeDimension +" "+ degreeConst);

    try
     {  
        s = conn.createStatement();
   //     conn.setAutoCommit(true);
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted \n" +
        "UNWIND range(toInteger(?),toInteger(?)) AS n\n" +
        "WITH toString(n) AS N, ? as nMax\n" +
        "MATCH (v:VertexNode {Degree:?})<-[vI:VertexIndexedBy {twoSeq:?}]-(i:IndexedBy {N:N,MaxN:nMax,Dimension:?})-[]->(e:Evaluate)\n" +
        "WITH i.N as iN, i.MaxN as iM,e,v,vI,i                        \n" +
        "OPTIONAL MATCH (vv:VertexNode)<-[vII:VertexIndexedBy]-(ii:IndexedByArgument)-[]->(ee:Evaluate {Value:toString(vI.twoSeq)})      \n" +
        "                    where ii.MaxN=iM  AND ii.Dimension=?\n" +
        "\n" +
        "WITH\n" +
        "vI.twoSeq AS vTwoSeq,\n" +
        "vI.divisor AS vDivisor,\n" +
        "vII.twoSeq AS vvTwoSeq,\n" +
        "ee.Value AS eeValue,\n" +
        "vII.divisor AS vvDivisor,\n" +
        "iN,\n" +
        "iM,\n" +
        "i.Dimension AS iDimension,\n" +
        "v.Scalar AS vScalar,\n" +
        "v.Degree AS vDegree,\n" +
        "vv.Scalar AS vvScalar,\n" +
        "vv.Degree AS vvDegree,i\n" +
        "                                    \n" +
        "WITH\n" +
        "\n" +
        "iN,iM,vTwoSeq as pD,vTwoSeq,vvDegree,vvScalar,vDivisor,vvDivisor,vvTwoSeq,  \n" +
        "CASE WHEN vvScalar IS null THEN 99 ELSE  toInteger(eeValue) END as p2Eval,\n" +
        "     toString( CASE WHEN toString(vDegree)='-1' THEN apoc.number.exact.mul(toString(vScalar),'2') ELSE toString(vScalar) END ) AS TTScalar,\n" +
        "toString( CASE WHEN toString(vvDegree)='-1' THEN apoc.number.exact.mul(toString(vvScalar),'2') ELSE toString(vvScalar) END) AS AAScalar,\n" +
        "toString( CASE WHEN toString(vDegree)='-1' THEN apoc.number.exact.add(vDegree,'1') ELSE apoc.number.exact.add(vDegree,'0') END ) AS TTDegree,\n" +
        "toString( CASE WHEN toString(vvDegree)='-1' THEN apoc.number.exact.add(vvDegree,'1') ELSE apoc.number.exact.add(vvDegree,'0')  END) AS AADegree\n" +
        "\n" +
        "WITH\n" +
        "{iM:iM,iN:iN,pD:pD,p2Eval:p2Eval,vTwoSeq:vTwoSeq,TTDegree:TTDegree,TTScalar:TTScalar,vDivisor:vDivisor,vvDivisor:vvDivisor,vvTwoSeq:vvTwoSeq,AADegree:AADegree,AAScalar:AAScalar,\n" +
        "degreeSum:CASE WHEN vvDegree IS Null THEN TTDegree ELSE apoc.number.exact.add(TTDegree,AADegree) END,\n" +
        "scalarProduct:CASE WHEN vvScalar IS Null THEN toString(TTScalar) ELSE apoc.number.exact.mul(TTScalar,AAScalar) END,\n" +
        "resultDivisor:CASE WHEN vvScalar IS Null THEN toString(vDivisor) ELSE apoc.number.exact.mul( toString(vDivisor),apoc.number.exact.mul(toString(vvDivisor),toString(pD))) END,\n" +
        "writeDimension:?} AS line\n" +
        "RETURN line");                
        statements.add(psInsert);
        psInsert.setString(1, rangeLow);
        psInsert.setString(2, rangeHigh);
        psInsert.setString(3, index);
        psInsert.setString(4,degreeConst);
        psInsert.setInt(5, tConstraint);
        psInsert.setString(6, dimensionRead1);
        psInsert.setString(7, dimensionRead2);
        psInsert.setString(8, writeDimension);
        ResultSet rs = psInsert.executeQuery();
        String jSMapString=null;
        conn.commit(); 
       Boolean hasRS = TRUE; 
 
       while ( hasRS ) {
       if (rs.next()) {  
        Map rsMap = new HashMap();
        rsMap.putAll((Map)rs.getObject(1));
        jSMapString =  new ObjectMapper().writeValueAsString(rsMap);
        System.out.println( new ObjectMapper().writeValueAsString(rsMap));
        rSMap.setKeyValueS12Map("tConstraint", tConstraint);rSMap.setKeyValueS12Map("rangeLow",rangeLow);rSMap.setKeyValueS12Map("rangeHigh",rangeHigh);rSMap.setKeyValueS12Map("index",index);rSMap.setKeyValueS12Map("dimensionRead1",dimensionRead1);rSMap.setKeyValueS12Map("dimensionRead2",dimensionRead2);rSMap.setKeyValueS12Map("writeDimension",writeDimension);rSMap.setKeyValueS12Map("degreeConst",degreeConst);
        rSMap.setMap((HashMap)rsMap);
        ProducerRecord record = new ProducerRecord("s12",null,jSMapString);
        try {
         RecordMetadata m =   (RecordMetadata) producer.send(record).get();
//        System.out.println("Message produced, offset: " + m.offset());
//        System.out.println("Message produced, partition : " + m.partition());
//        System.out.println("Message produced, topic: " + m.topic());
        } catch (InterruptedException ex) {
            Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
          System.out.println("InterruptedException"+ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
          System.out.println("ExecutionException"+ex);
        }
       
        } else {hasRS=FALSE;}
        }  // the while
   }
    catch (SQLException sqle)
    {
        printSQLException(sqle);
    } 
    
    statements.remove(0);
//    try {
//        if (!conn.isClosed())  {     this.endConn(); }
//    } catch (SQLException ex) {
//        Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
//    }
 gc();
}







public void s12Initial(String rangeLow,String rangeHigh,String index,String dimensionRead1,String dimensionRead2, String writeDimension, String tConstraint) throws JsonProcessingException  {
//    this.startConn();
    System.out.println("S12Inital "+rangeLow+" "+rangeHigh+" "+index+" "+dimensionRead1+" "+dimensionRead2+" "+writeDimension+" "+tConstraint);
  

    try
     {  
        s = conn.createStatement();
   //     conn.setAutoCommit(true);
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted \n" +
        "UNWIND range(toInteger(?),toInteger(?)) AS n\n" +
        "WITH toString(n) AS N, ? as nMax\n" +
        "MATCH (v:VertexNode)<-[]-(i:IndexedBy)-[]->(e:Evaluate),(t:TwoSeqFactor)<-[]-(i) where i.N=N AND i.MaxN=nMax AND i.Dimension=? AND t.twoSeq=? \n" +
        "WITH i.N as iN, i.MaxN as iM,e,v,t,i                       \n" +
        "OPTIONAL MATCH (vv:VertexNode)<-[]-(ii:IndexedBy)-[]->(ee:Evaluate {Value:toString(t.twoSeq)}),(tt:TwoSeqFactor)<-[]-(ii) where ii.MaxN=iM  AND ii.Dimension=?\n" +
        "WITH\n" +
        "'1'  AS pDivisor, \n" +
        "'2' AS pDimension,\n" +
        "'2' AS vDivisor,\n" +
        "tt.twoSeq AS vvTwoSeq,\n" +
        "ee.Value AS eeValue,\n" +
        "'2'  AS vvDivisor,\n" +
        " i,      //iN,\n" +
        "'4' AS iDimension,\n" +
        "       // iM,\n" +
        "t.twoSeq AS vTwoSeq,\n" +
        "v.Degree AS vDegree,\n" +
        "vv.Degree AS vvDegree,\n" +
        "v.Scalar AS vScalar,\n" +
        "vv.Scalar AS vvScalar,  \n" +
        "toString( CASE WHEN toString(i.RowCounter)='2' THEN apoc.number.exact.mul(toString(v.Scalar),'2') ELSE toString(v.Scalar) END ) AS TTScalarRowCounter\n" +
        "WITH\n" +
        "i,pDivisor as pD,vTwoSeq,vvDegree,vvScalar,vDivisor,vvDivisor,vvTwoSeq,       //  N,iM    *****    vTwoSeq as pD\n" +
        "CASE WHEN vvScalar IS null THEN 99 ELSE  toInteger(eeValue) END as p2Eval,\n" +
        "     toString( CASE WHEN toString(vDegree)='-1' THEN apoc.number.exact.mul(toString(vScalar),'2') ELSE toString(TTScalarRowCounter) END ) AS TTScalar,\n" +
        "     toString( CASE WHEN toString(vvDegree)='-1' THEN apoc.number.exact.mul(toString(vvScalar),'2') ELSE apoc.number.exact.mul(toString(vvScalar),vvTwoSeq) END) AS AAScalar,     \n" +
        "     toString( CASE WHEN toString(vDegree)='-1' THEN apoc.number.exact.add(vDegree,'1') ELSE apoc.number.exact.add(vDegree,'0') END ) AS TTDegree,\n" +
        "     toString( CASE WHEN toString(vvDegree)='-1' THEN apoc.number.exact.add(vvDegree,'1') ELSE apoc.number.exact.add(vvDegree,'0')  END) AS AADegree\n" +
        "\n" +
        "With {iN:i.N,iM:i.MaxN,pD:pD,vTwoSeq:vTwoSeq,vDivisor:vDivisor,vvDivisor:vvDivisor,vvTwoSeq:CASE WHEN vvTwoSeq IS NULL THEN \"0\" ELSE vvTwoSeq END,p2Eval:p2Eval,\n" +
        "       TTScalar:TTScalar,AAScalar:CASE WHEN AAScalar IS NULL THEN \"0\" ELSE AAScalar END,TTDegree:TTDegree,AADegree:CASE WHEN AADegree IS NULL THEN \"0\" ELSE AADegree END,\n" +
        "       degreeSum:CASE WHEN vvDegree IS Null THEN TTDegree ELSE apoc.number.exact.add(TTDegree,AADegree) END,\n" +
        "       scalarProduct:CASE WHEN vvScalar IS Null THEN toString(TTScalar) ELSE apoc.number.exact.mul(TTScalar,AAScalar) END,\n" +
        "       resultDivisor:CASE WHEN vvScalar IS Null THEN toString(vDivisor) ELSE apoc.number.exact.mul( toString(vDivisor),apoc.number.exact.mul(toString(vvDivisor),toString(pD))) END,\n" +
        "       writeDimension:?} as lines\n" +
        "RETURN lines");
        statements.add(psInsert);  //can have a psinsert method to hanle this.  It would though require objject array.
        psInsert.setString(1, rangeLow);
        psInsert.setString(2, rangeHigh);
        psInsert.setString(3, index);
        psInsert.setString(4, dimensionRead1);
        psInsert.setString(5, tConstraint);
        psInsert.setString(6, dimensionRead2);
        psInsert.setString(7, writeDimension);
        psInsert.executeUpdate();
        ResultSet rs = psInsert.executeQuery();
        conn.commit();
        String jSMapString=null;
        Boolean hasRS = TRUE;
       
       while ( hasRS ) {
       if (rs.next()) {
           Map rsMap = new HashMap();
           rsMap.putAll((Map)rs.getObject(1));
           jSMapString =  new ObjectMapper().writeValueAsString(rsMap);
           System.out.println( jSMapString);
           rSMap.setKeyValueS12Map("tConstraint", tConstraint);rSMap.setKeyValueS12Map("rangeLow",rangeLow);rSMap.setKeyValueS12Map("rangeHigh",rangeHigh);rSMap.setKeyValueS12Map("index",index);rSMap.setKeyValueS12Map("dimensionRead1",dimensionRead1);rSMap.setKeyValueS12Map("dimensionRead2",dimensionRead2);rSMap.setKeyValueS12Map("writeDimension",writeDimension);
           rSMap.setMap((HashMap)rsMap);
           ProducerRecord record = new ProducerRecord("s12",null,jSMapString);
        try {
        RecordMetadata m =(RecordMetadata) producer.send(record).get();
//        System.out.println("Message produced, offset: " + m.offset());
//        System.out.println("Message produced, partition : " + m.partition());
//        System.out.println("Message produced, topic: " + m.topic());
        } catch (InterruptedException ex) {
          Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
          System.out.println("InterruptedException"+ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
          System.out.println("ExecutionException"+ex);
        }
        
        } else {hasRS=FALSE;}
       } 
    
             
       
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
            srBol.setBooleanFALSE();
            System.out.println("The suspected error.");
        }       
     
       statements.remove(0);
//        try {
//            if (!conn.isClosed())  {     this.endConn(); }
//        } catch (SQLException ex) {
//            Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
//        }
 gc(); 
}

public void s3TEc(String rangeLow, String rangeHigh, String nMax, String dimension1, String dimension2, String dimension3, int tConstraint) throws JsonProcessingException  {
//    this.startConn();
    System.out.println();
    System.out.println("s3T" + rangeLow+" "+rangeHigh+" "+nMax+" "+dimension1+" "+dimension2+" "+dimension3+" "+tConstraint); 
    System.out.println();
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted \n" +
        "UNWIND range(toInteger(?),toInteger(?)) AS n\n" +
        "WITH toString(n) AS iN, ? as iM\n" +
        "MATCH (cl:CollectedProductArgNode {Dimension:?,MaxN:iM,N:iN,tft:?})-[:Arguments]->(aN:ArgumentsNode )\n" +
        "WITH max(aN.ResultDivisor) AS maxDivisor,iN,iM\n" +
        "MATCH (cl:CollectedProductArgNode {Dimension:?,MaxN:iM,N:iN,tft:?})-[:Arguments]->(aN:ArgumentsNode )\n" +
        "//RETURN aN.ScalarProduct,aN.DegreeSum,aN.ResultDivisor,cl.tft,cl.Divisor,iN,iM\n" +
        "WITH cl.tft AS clTft, cl.Divisor AS clDivisor, COLLECT({ResultScalar:aN.ScalarProduct, ResultDegree:aN.DegreeSum, ResultDivisor:aN.ResultDivisor}) AS ds, iN, iM, maxDivisor\n" +
        "WITH {DS:ds, ET:clTft, Divisor:clDivisor} AS resultProduct,iN,iM,maxDivisor\n" +
        "WITH COLLECT(resultProduct) as rPC,resultProduct.ET as eT,iN,iM,resultProduct.Divisor as rpD1,maxDivisor\n" +
        "UNWIND RANGE(0,SIZE(rPC)-1) as iI\n" +
        "WITH rPC[iI].DS as rPCDS,eT,iN,iM,rpD1,maxDivisor\n" +
        "UNWIND rPCDS as rPCDSU\n" +
        "WITH COLLECT({RScalar:rPCDSU.ResultScalar}) AS rScalarList, rPCDSU.ResultDegree AS rDegree, rPCDSU.ResultDivisor AS rDivisor2, eT, rpD1, iN, iM, maxDivisor\n" +
        "WITH {RScalarList:rScalarList,RDegree:rDegree,RDivisor:rDivisor2,ET:eT,Divisor:rpD1} as resultMap,iN,iM,maxDivisor\n" +
        "WITH COLLECT(resultMap) as resultMapC,resultMap.RDivisor as rDivisor,resultMap.ET as eTrm,resultMap.RDegree as rDRM,iN,iM,resultMap.Divisor as rpD2,maxDivisor\n" +
        "UNWIND RANGE(0,SIZE(resultMapC)-1) as iII\n" +
        "WITH resultMapC[iII].RScalarList as rSL,rDivisor,rDRM,iN,iM,rpD2,maxDivisor,eTrm\n" +
        "UNWIND RANGE(0,SIZE(rSL)-1) as iIII\n" +
        "UNWIND rSL[iIII] as rSLI\n" +
        "WITH COLLECT(apoc.number.exact.mul(rSLI.RScalar,apoc.number.exact.div(maxDivisor,rDivisor))) as cts,rDRM,iN,iM,rpD2,maxDivisor,eTrm \n" +
        "WITH REDUCE(sum='0', x in cts | apoc.number.exact.add(sum,toString(x))) as acc,rDRM,iN,iM,rpD2,maxDivisor,eTrm\n" +
        "WITH COLLECT({Degree:rDRM,Scalar:acc}) AS dsList, maxDivisor, iM, iN, eTrm\n" +
        "WITH {DSList:dsList,DivideBy:maxDivisor} AS pTerms,iM,iN,eTrm \n" +
        "WITH COLLECT(pTerms) as pTermss,iM,iN,eTrm\n" +
        "UNWIND RANGE(0,SIZE(pTermss)-1) as pT\n" +
        "WITH pTermss[pT].DSList as dSList,iN,iM,pTermss[pT].DivideBy AS pTD,eTrm\n" +
        "UNWIND dSList as dSListU\n" +
        "WITH dSListU.Degree AS tDegree,dSListU.Scalar AS tScalar,iN,iM,pTD,eTrm \n" +
        "WITH {dimension:?,tDegree:tDegree,tScalar:tScalar,iN:iN,iM:iM,pTD:pTD,eTrm:eTrm} AS line\n" +
        "RETURN line");
        statements.add(psInsert);
        psInsert.setString(1,rangeLow);
        psInsert.setString(2,rangeHigh);
        psInsert.setString(3,nMax);
        psInsert.setString(4,dimension1);
        psInsert.setInt(5, tConstraint);
//        psInsert.setString(6,dConstraint);
        psInsert.setString(6,dimension1);
        psInsert.setInt(7, tConstraint);
//        psInsert.setString(9,dConstraint);        
        psInsert.setString(8,dimension3);
        ResultSet rs = psInsert.executeQuery();
        String jSMapString=null;
        conn.commit();
       Boolean hasRS = TRUE;
       while ( hasRS ) {
       if (rs.next()) {        
       Map rsMap = new HashMap();
        rsMap.putAll((Map)rs.getObject(1));
        jSMapString =  new ObjectMapper().writeValueAsString(rsMap);
        System.out.println( jSMapString);
        rSMap.setKeyValueS3tMap("rangeLow",rangeLow);rSMap.setKeyValueS3tMap("rangeHigh",rangeHigh);rSMap.setKeyValueS3tMap("nMax",nMax);rSMap.setKeyValueS3tMap("dimension1",dimension1);rSMap.setKeyValueS3tMap("dimension2",dimension2);rSMap.setKeyValueS3tMap("dimension3",dimension3);rSMap.setKeyValueS3tMap("tConstraint",tConstraint);
        rSMap.setMap3t((HashMap)rsMap);
        ProducerRecord record = new ProducerRecord("s3t",null,jSMapString);        
       try {
         RecordMetadata m =   (RecordMetadata) producer.send(record).get();
//        System.out.println("Message produced, offset: " + m.offset());
//        System.out.println("Message produced, partition : " + m.partition());
//        System.out.println("Message produced, topic: " + m.topic());
        } catch (InterruptedException ex) {
            Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
          System.out.println("InterruptedException"+ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
          System.out.println("ExecutionException"+ex);
        }
       
       }  else { hasRS=FALSE;} 

       }
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }

        statements.remove(0);
//    try {
//        if (!conn.isClosed())  {     this.endConn(); }
//    } catch (SQLException ex) {
//        Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
//    }
 gc();
}

public void s3ADc(String rangeLow, String rangeHigh, String nMax, String dimension1, String dimension2, String dimension3, int tConstraint, String dConstraint) throws JsonProcessingException  {
//    this.startConn();
    System.out.println("s3A  "+ rangeLow+" "+rangeHigh+" "+nMax+" "+dimension1+" "+dimension2+" "+dimension3+" "+tConstraint+" "+dConstraint);
    rSMap.setKeyValueS3aMap("rangeLow",rangeLow);rSMap.setKeyValueS3aMap("rangeHigh",rangeHigh);rSMap.setKeyValueS3aMap("nMax",nMax);
    rSMap.setKeyValueS3aMap("dimension1",dimension1);rSMap.setKeyValueS3aMap("dimension2",dimension2);rSMap.setKeyValueS3aMap("dimension3",dimension3);rSMap.setKeyValueS3aMap("dConstraint",dConstraint);

    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted \n" +
        "UNWIND range(toInteger(?),toInteger(?)) AS n\n" +
        "WITH toString(n) AS iN, ? as iM\n" +
        "MATCH (v:VertexNode {Degree:?})<-[vI:VertexIndexedBy]-(i:IndexedBy {N:iN,MaxN:iM,Dimension:?})\n" +
        "WITH iN, iM, max(vI.divisor) as maxDivisor\n" +
        "MATCH (v:VertexNode {Degree:?})<-[vI:VertexIndexedBy]-(i:IndexedBy {N:iN,MaxN:iM,Dimension:?})\n" +
        "WITH vI.twoSeq AS viTwoSeq, COLLECT({ResultScalar:v.Scalar, ResultDegree:v.Degree, ResultDivisor:vI.divisor}) AS ds, iN, iM, maxDivisor\n" +
        "WITH {DS:ds, Divisor:viTwoSeq} AS resultProduct,iN,iM,maxDivisor\n" +
        "\n" +
        "WITH COLLECT(resultProduct) as rPC,iN,iM,resultProduct.Divisor as rpD1,maxDivisor\n" +
        "UNWIND RANGE(0,SIZE(rPC)-1) as iI\n" +
        "WITH rPC[iI].DS as rPCDS,iN,iM,rpD1,maxDivisor\n" +
        "UNWIND rPCDS as rPCDSU\n" +
        "WITH COLLECT({RScalar:rPCDSU.ResultScalar}) AS rScalarList, rPCDSU.ResultDegree AS rDegree, rPCDSU.ResultDivisor AS rDivisor2, rpD1, iN, iM, maxDivisor\n" +
        "WITH {RScalarList:rScalarList,RDegree:rDegree,RDivisor:rDivisor2,Divisor:rpD1} as resultMap,iN,iM,maxDivisor\n" +
        "WITH COLLECT(resultMap) as resultMapC,resultMap.RDivisor as rDivisor,resultMap.RDegree as rDRM,iN,iM,resultMap.Divisor as rpD2,maxDivisor\n" +
        "UNWIND RANGE(0,SIZE(resultMapC)-1) as iII\n" +
        "WITH resultMapC[iII].RScalarList as rSL,rDivisor,rDRM,iN,iM,rpD2,maxDivisor\n" +
        "UNWIND RANGE(0,SIZE(rSL)-1) as iIII\n" +
        "UNWIND rSL[iIII] as rSLI\n" +
        "\n" +
        "//RETURN rSLI.RScalar,rDivisor,rDRM,iN,iM,maxDivisor\n" +
        "WITH COLLECT(apoc.number.exact.mul(rSLI.RScalar,apoc.number.exact.div(maxDivisor,rDivisor))) as cts,rDRM,iN,iM,rpD2,maxDivisor\n" +
        "WITH REDUCE(sum='0', x in cts | apoc.number.exact.add(sum,toString(x))) as acc,rDRM,iN,iM,rpD2,maxDivisor\n" +
        "WITH COLLECT(acc) as accc,rDRM,iN,iM,maxDivisor\n" +
        "WITH REDUCE(sum='0', x in accc | apoc.number.exact.add(sum,toString(x))) as acc,rDRM,iN,iM,maxDivisor\n" +
        "WITH COLLECT({Degree:rDRM,Scalar:acc}) AS dsList, maxDivisor, iM, iN\n" +
        "WITH {DSList:dsList,DivideBy:maxDivisor} AS pTerms,iM,iN \n" +
        "WITH COLLECT(pTerms) as pTermss,iM,iN\n" +
        "UNWIND RANGE(0,SIZE(pTermss)-1) as pT\n" +
        "WITH pTermss[pT].DSList as dSList,iN,iM,pTermss[pT].DivideBy AS pTD\n" +
        "UNWIND dSList as dSListU\n" +
        "WITH dSListU.Degree AS aDegree,dSListU.Scalar AS aScalar,iN,iM,pTD \n " +
        "WITH {dimension:?,tDegree:aDegree,tScalar:aScalar,pTD:pTD,iM:iM,iN:iN} AS line\n" +
        "RETURN line");
        statements.add(psInsert);
        psInsert.setString(1,rangeLow);
        psInsert.setString(2,rangeHigh);
        psInsert.setString(3,nMax);
        psInsert.setString(4,dConstraint);
        psInsert.setString(5,dimension1);
        psInsert.setString(6,dConstraint);
        psInsert.setString(7,dimension1);        
        psInsert.setString(8,dimension3);
        ResultSet rs = psInsert.executeQuery();
        String jSMapString=null;
        conn.commit();
       Boolean hasRS = TRUE;
       Boolean atLeastOneRS = FALSE;
       while ( hasRS ) {
       if (rs.next()) {    
            atLeastOneRS = TRUE;
            Map rsMap = new HashMap();
            rsMap.putAll((Map)rs.getObject(1));
            jSMapString =  new ObjectMapper().writeValueAsString(rsMap);
            System.out.println("someting to print: " +jSMapString);
            rSMap.setMap3a((HashMap)rsMap);
            ProducerRecord record = new ProducerRecord("s3a",null,jSMapString);
        try {
        RecordMetadata m =   (RecordMetadata) producer.send(record).get();
//        System.out.println("Message produced, offset: " + m.offset());
//        System.out.println("Message produced, partition : " + m.partition());
//        System.out.println("Message produced, topic: " + m.topic());
        } catch (InterruptedException ex) {
            Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
          System.out.println("InterruptedException"+ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
          System.out.println("ExecutionException"+ex);
        }
         
       } else { hasRS=FALSE; if (!atLeastOneRS) {srBol.setBooleanFALSE();} } 
       }
    }
    catch (SQLException sqle)
    {
            printSQLException(sqle);
    }       
    
    statements.remove(0);
//    try {
//        if (!conn.isClosed())  {     this.endConn(); }
//    } catch (SQLException ex) {
//        Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
//    }
 gc();
}

     public void startConn()  {

     loadDriver();
      try {
           Properties props = new Properties();
           conn = DriverManager.getConnection(protocol, DbConfig.get("neo4j.user"), DbConfig.get("neo4j.password"));
           System.out.println(conn.getMetaData().getURL());
           conn.setAutoCommit(false);
          }
        catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 

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
            //e.printStackTrace(System.err);
            e = e.getNextException();
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
                        System.out.println("neo4j shut down normally");
                    } else {
                        System.err.println("neo4j did not shut down normally");
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
    
    
    
    
    
    
}
