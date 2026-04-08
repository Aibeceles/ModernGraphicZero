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
import java.io.IOException;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.gc;
import java.math.BigDecimal;
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
 *
 * @author Aibes
 */
public class DFScripts_1 {

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
 
    
 
    
    
public DFScripts_1() {
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


    

public ResultSet s3T(String rangeLow, String rangeHigh, String nMax, String dimension1, String dimension2, String dimension3, int tConstraint)  {
//    this.startConn();
    System.out.println();
    System.out.println("s3T" + rangeLow+" "+rangeHigh+" "+nMax+" "+dimension1+" "+dimension2+" "+dimension3+" "+tConstraint); 
    System.out.println();
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
//        "WITH {dimension:?,tDegree:tDegree,tScalar:tScalar,iN:iN,iM:iM,pTD:pTD,eTrm:eTrm} AS line\n" +
//        "RETURN line");
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
        rs = psInsert.executeQuery();
        conn.commit();
        while ( rs.next() ) {
           for (int j=1; j< 7; j++) { 
            try {
                 valueList.add(rs.getString(j)); 
 //                System.out.println("rs(j): " + rs.getString(j).toString());  
               }
            catch(NullPointerException npe) {
            System.out.println("ResultSet Null");
 //           valueList.add("Null");
            }   
            }
 //           System.out.println(valueList.toString());
            try {
                //wCsv.writeLine(valueList,';',' ');
                // System.out.println(valueList.toString());
                wCsv.writeLine(valueList,';','"');
                valueList.clear();
            } catch (IOException ex) {
                Logger.getLogger(ScriptsAutomation.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("caught an error.");
            }
            }




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

/*   It is a fact, sPara should be properly initialized.  Not!! in DFScripts_1.main.
  *
  */
  
  
public static void main(String[] args) throws Exception
{
        ScriptParameters sPara = new ScriptParameters(args);

        

}
    
}
