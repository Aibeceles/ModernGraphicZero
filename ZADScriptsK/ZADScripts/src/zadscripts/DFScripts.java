/*
 * Copyright (C) 2020 Aibes
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

//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.gc;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import static zadscripts.ScriptsAutomation.printSQLException;

/**
 * Utility Class contains Cyper query methods for use in Spark notebook scripts.
 * @author Aibes
 * 
 */
public class DFScripts {

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
 
    
 
    
    
public DFScripts() {
    startConn();

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
            System.out.println("Loaded the appropriate driver and rev.");
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

/**
 *    CollectedProductArgNode pattern queried by MaxN,N,Dimension,tConstraint(RowCounter).
 *    Terms of target P returned as ResultSet.
 */
     
    

public ResultSet s3T(String rangeLow, String rangeHigh, String nMax, String dimension1, String dimension2, String dimension3, int tConstraint)  {
//    this.startConn();
//    System.out.println();
//    System.out.println("s3T" + rangeLow+" "+rangeHigh+" "+nMax+" "+dimension1+" "+dimension2+" "+dimension3+" "+tConstraint); 
//    System.out.println();
    ResultSet rs=null;
    List valueList=new ArrayList(); 
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
        "RETURN ? AS dimension, dSListU.Degree AS tDegree,dSListU.Scalar AS tScalar,iN,iM,pTD,toString(eTrm) as eTrm "); 
        statements.add(psInsert);
        psInsert.setString(1,rangeLow);
        psInsert.setString(2,rangeHigh);
        psInsert.setString(3,nMax);
        psInsert.setString(4,dimension1);
        psInsert.setInt(5, tConstraint);
        psInsert.setString(6,dimension1);
        psInsert.setInt(7, tConstraint);
        psInsert.setString(8,dimension3);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
//        System.out.println("RS: "+rs.toString());
        return(rs);
}


/**
 * IndexBy node pattern queried.  Returned ResultSet contains terms of P(MaxN,N)
 * 
 * @param rangeLow
 * @param rangeHigh
 * @param nMax
 * @param dimension1
 * @return 
 */

public ResultSet s3A(String rangeLow, String rangeHigh, String nMax, String dimension1)  {
//    this.startConn();
    System.out.println();
//    System.out.println("s3A" + rangeLow+" "+rangeHigh+" "+nMax+" "+dimension1+" "+dimension2+" "+dimension3+" "); 
    System.out.println();
    ResultSet rs=null;
    List valueList=new ArrayList(); 
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
        "UNWIND range(toInteger(?),toInteger(?)) AS n\n" +
        "WITH toString(n) AS iN, ? as iM\n" +
        "MATCH (v:VertexNode)<-[vI:VertexIndexedBy]-(i:IndexedBy {N:iN,MaxN:iM,Dimension:?})-[]->(e:Evaluate)\n" +
        "WITH iN, iM, max(vI.divisor) as maxDivisor\n" +
        "MATCH (v:VertexNode)<-[vI:VertexIndexedBy]-(i:IndexedBy {N:iN,MaxN:iM,Dimension:?})-[]->(e:Evaluate)\n" +
        "// return v.Scalar,v.Degree,vI.divisor,vI.twoSeq\n" +
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
        "WITH dSListU.Degree AS aDegree,dSListU.Scalar AS aScalar,iN,iM,pTD\n" +
        "RETURN aDegree,aScalar,pTD,iM,iN"); 
        statements.add(psInsert);
        psInsert.setString(1,rangeLow);
        psInsert.setString(2,rangeHigh);
        psInsert.setString(3,nMax);
        psInsert.setString(4,dimension1);
        psInsert.setString(5, dimension1);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
        return(rs);
}

/**
 * 
 * @param rangeLow
 * @param rangeHigh
 * @param nMax
 * @param dimension1
 * @param rCounter
 * @return 
 */


public ResultSet s3ARC(String rangeLow, String rangeHigh, String nMax, String dimension1, int rCounter)  {
//    this.startConn();
    System.out.println();
//    System.out.println("s3A" + rangeLow+" "+rangeHigh+" "+nMax+" "+dimension1+" "+dimension2+" "+dimension3+" "); 
    System.out.println();
    ResultSet rs=null;
    List valueList=new ArrayList(); 
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
        "UNWIND range(toInteger(?),toInteger(?)) AS n\n" +
        "WITH toString(n) AS iN, ? as iM\n" +
        "MATCH (v:VertexNode)<-[vI:VertexIndexedBy]-(i:IndexedBy {N:iN,MaxN:iM,Dimension:?,RowCounter:?})-[]->(e:Evaluate)\n" +
        "WITH iN, iM, max(vI.divisor) as maxDivisor\n" +
        "MATCH (v:VertexNode)<-[vI:VertexIndexedBy]-(i:IndexedBy {N:iN,MaxN:iM,Dimension:?,RowCounter:?})-[]->(e:Evaluate)\n" +
        "// return v.Scalar,v.Degree,vI.divisor,vI.twoSeq\n" +
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
        "WITH dSListU.Degree AS aDegree,dSListU.Scalar AS aScalar,iN,iM,pTD\n" +
        "RETURN aDegree,aScalar,pTD,iM,iN"); 
        statements.add(psInsert);
        psInsert.setString(1,rangeLow);
        psInsert.setString(2,rangeHigh);
        psInsert.setString(3,nMax);
        psInsert.setString(4,dimension1);
        psInsert.setInt(5,rCounter);
        psInsert.setString(6, dimension1);
        psInsert.setInt(7,rCounter);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
        return(rs);
}


public ResultSet s12Initial(String rangeLow,String rangeHigh,String index,String dimensionRead1,String dimensionRead2, String writeDimension, String tConstraint)  {
//    this.startConn();
//    System.out.println();
//    System.out.println("s3A" + rangeLow+" "+rangeHigh+" "+nMax+" "+dimension1+" "+dimension2+" "+dimension3+" "); 
//    System.out.println();
    ResultSet rs=null;
    List valueList=new ArrayList(); 
    try
     {  
        s = conn.createStatement();
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
        "RETURN i.N AS in,i.MaxN AS iM,pD,vTwoSeq,vDivisor,vvDivisor,CASE WHEN vvTwoSeq IS NULL THEN \"0\" ELSE vvTwoSeq END,p2Eval,\n" +
        "       TTScalar,CASE WHEN AAScalar IS NULL THEN \"0\" ELSE AAScalar END,TTDegree,CASE WHEN AADegree IS NULL THEN \"0\" ELSE AADegree END,\n" +
        "       CASE WHEN vvDegree IS Null THEN TTDegree ELSE apoc.number.exact.add(TTDegree,AADegree) END,\n" +
        "       CASE WHEN vvScalar IS Null THEN toString(TTScalar) ELSE apoc.number.exact.mul(TTScalar,AAScalar) END,\n" +
        "       CASE WHEN vvScalar IS Null THEN toString(vDivisor) ELSE apoc.number.exact.mul( toString(vDivisor),apoc.number.exact.mul(toString(vvDivisor),toString(pD))) END,\n" +
        "       ?");
//        "RETURN lines"); 
        statements.add(psInsert);
        psInsert.setString(1, rangeLow);
        psInsert.setString(2, rangeHigh);
        psInsert.setString(3, index);
        psInsert.setString(4, dimensionRead1);
        psInsert.setString(5, tConstraint);
        psInsert.setString(6, dimensionRead2);
        psInsert.setString(7, writeDimension);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
        return(rs);
}

public ResultSet s12OQ(String rangeLow,String rangeHigh,String index,String dimensionRead1,String dimensionRead2, String writeDimension, String tConstraint, String tLowerS,String tIndex,String tSeq)  {
//    this.startConn();
//    System.out.println();
//    System.out.println("s3A" + rangeLow+" "+rangeHigh+" "+nMax+" "+dimension1+" "+dimension2+" "+dimension3+" "); 
//    System.out.println();
    ResultSet rs=null;
    List valueList=new ArrayList(); 
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted \n" +
"        UNWIND range(toInteger(?),toInteger(?)) AS n\n" +
"        WITH toString(n) AS N, ? as nMax\n" +
"        MATCH (v:VertexNode)<-[]-(i:IndexedBy)-[]->(e:Evaluate),(t:TwoSeqFactor)<-[]-(i) where i.N=N AND i.MaxN=nMax AND i.Dimension=? AND t.twoSeq=? \n" +
"        WITH i.N as iN, i.MaxN as iM,e,v,t,i                       \n" +
"        OPTIONAL MATCH (vv:VertexNode)<-[]-(ii:IndexedBy)-[]->(ee:Evaluate),(tt:TwoSeqFactor)<-[]-(ii) where ii.N=? AND ii.MaxN=? AND ii.Dimension=? AND tt.twoSeq=?\n" +
"        WITH\n" +
"        '1'  AS pDivisor, \n" +
"        '2' AS pDimension,\n" +
"        '2' AS vDivisor,\n" +
"        tt.twoSeq AS vvTwoSeq,\n" +
"        ee.Value AS eeValue,\n" +
"        '2'  AS vvDivisor,\n" +
"         i,      //iN,\n" +
"        '4' AS iDimension,\n" +
"               // iM,\n" +
"        t.twoSeq AS vTwoSeq,\n" +
"        v.Degree AS vDegree,\n" +
"        vv.Degree AS vvDegree,\n" +
"        v.Scalar AS vScalar,\n" +
"        vv.Scalar AS vvScalar,  \n" +
"        toString( CASE WHEN toString(i.RowCounter)='2' THEN apoc.number.exact.mul(toString(v.Scalar),'2') ELSE toString(v.Scalar) END ) AS TTScalarRowCounter\n" +
"        WITH\n" +
"        i,pDivisor as pD,vTwoSeq,vvDegree,vvScalar,vDivisor,vvDivisor,vvTwoSeq,    \n" +
"        CASE WHEN vvScalar IS null THEN 99 ELSE  toInteger(eeValue) END as p2Eval,\n" +
"             toString( CASE WHEN toString(vDegree)='-1' THEN apoc.number.exact.mul(toString(vScalar),'2') ELSE toString(TTScalarRowCounter) END ) AS TTScalar,\n" +
"             toString( CASE WHEN toString(vvDegree)='-1' THEN apoc.number.exact.mul(toString(vvScalar),'2') ELSE apoc.number.exact.mul(toString(vvScalar),vvTwoSeq) END) AS AAScalar,     \n" +
"             toString( CASE WHEN toString(vDegree)='-1' THEN apoc.number.exact.add(vDegree,'1') ELSE apoc.number.exact.add(vDegree,'0') END ) AS TTDegree,\n" +
"             toString( CASE WHEN toString(vvDegree)='-1' THEN apoc.number.exact.add(vvDegree,'1') ELSE apoc.number.exact.add(vvDegree,'0')  END) AS AADegree \n" +
        "RETURN i.N AS in,i.MaxN AS iM,pD,vTwoSeq,vDivisor,vvDivisor,CASE WHEN vvTwoSeq IS NULL THEN \"0\" ELSE vvTwoSeq END,p2Eval,\n" +
        "       TTScalar,CASE WHEN AAScalar IS NULL THEN \"0\" ELSE AAScalar END,TTDegree,CASE WHEN AADegree IS NULL THEN \"0\" ELSE AADegree END,\n" +
        "       CASE WHEN vvDegree IS Null THEN TTDegree ELSE apoc.number.exact.add(TTDegree,AADegree) END,\n" +
        "       CASE WHEN vvScalar IS Null THEN toString(TTScalar) ELSE apoc.number.exact.mul(TTScalar,AAScalar) END,\n" +
        "       CASE WHEN vvScalar IS Null THEN toString(vDivisor) ELSE apoc.number.exact.mul( toString(vDivisor),apoc.number.exact.mul(toString(vvDivisor),toString(pD))) END,\n" +
        "       ?");
//        "RETURN lines"); public ResultSet s12OQ(rangeLow,rangeHigh,index,dimensionRead1,dimensionRead2,writeDimension,tConstraint,tLowerS,tIndex,tSeq)
        statements.add(psInsert);
        psInsert.setString(1, rangeLow);
        psInsert.setString(2, rangeHigh);
        psInsert.setString(3, index);
        psInsert.setString(4, dimensionRead1);
        psInsert.setString(5, tConstraint);
        psInsert.setString(6, tLowerS);
        psInsert.setString(7, tIndex);
        psInsert.setString(8, dimensionRead2);        
        psInsert.setString(9, tSeq);
        psInsert.setString(10, writeDimension);
        rs = psInsert.executeQuery();
        System.out.println(rs.toString());
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
        return(rs);
}

/*  Querry for returning a single quadratic
* 
*/


public ResultSet s12QuadQ(String rangeLow,String rangeHigh,String index)  {
//    this.startConn();
//    System.out.println();
//    System.out.println("s3A" + rangeLow+" "+rangeHigh+" "+nMax+" "+dimension1+" "+dimension2+" "+dimension3+" "); 
//    System.out.println();
    ResultSet rs=null;
    List valueList=new ArrayList(); 
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted \n" +
"        UNWIND range(toInteger(?),toInteger(?)) AS n\n" +
"        WITH toString(n) AS N, ? as nMax\n" +
"        MATCH (v:VertexNode)<-[]-(i:IndexedBy)-[]->(e:Evaluate),(t:TwoSeqFactor)<-[]-(i) where i.N=N AND i.MaxN=nMax AND i.Dimension='2'  // AND t.twoSeq=2 \n" +
"        WITH i.N as iN, i.MaxN as iM,e,v,t,i,                       \n" +
"        '1'  AS pDivisor, \n" +
"        '2' AS pDimension,\n" +
"        '2' AS tDivisor,\n" +
"        t.twoSeq AS rowScalar,\n" +
"        v.Degree AS vDegree,\n" +
"        v.Scalar AS vScalar\n" +
"        //i.RowCounter as rowCounter\n" +
"        Return  iN,iM,rowScalar,tDivisor,\n" +
"        toString( CASE WHEN toString(vDegree)='-1' THEN apoc.number.exact.mul(toString(vScalar),'2') ELSE toString(v.Scalar) END ) AS TTScalar,\n" +
"        toString( CASE WHEN toString(vDegree)='-1' THEN apoc.number.exact.add(vDegree,'1') ELSE apoc.number.exact.add(vDegree,\"0\") END ) AS TTDegree");
        statements.add(psInsert);
        psInsert.setString(1, rangeLow);
        psInsert.setString(2, rangeHigh);
        psInsert.setString(3, index);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        statements.remove(0);
        return(rs);
}









  public void endConn()  {

            {
                try
                {
                    System.out.print("conn.close()");
                    conn.close();
                    System.out.print("conn.close()");
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
