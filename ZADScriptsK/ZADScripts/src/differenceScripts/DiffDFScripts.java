/*
 * Copyright (C) 2021 Aibes
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
package differenceScripts;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.Producer;
import zadscripts.ResultSetMap;
import zadscripts.ScriptReturnBoolean;
import static zadscripts.ScriptsAutomation.printSQLException;
import zadscripts.WriteCSV;

/**
 * DifferenceGraph utility queries.  
 * Firstly facilitating Data Exploration.
 * @author Aibes
 */
public class DiffDFScripts {
    
    private final String driver = "org.neo4j:neo4j-jdbc-driver:4.0.0";
    private final String protocol = zadscripts.DbConfig.get("neo4j.url") + "?database=" + zadscripts.DbConfig.get("neo4j.database");
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
    
public DiffDFScripts() {
    startConn();

}    
    
public void startConn()  {

     loadDriver();
      try {
           Properties props = new Properties();
           conn = DriverManager.getConnection(protocol, zadscripts.DbConfig.get("neo4j.user"), zadscripts.DbConfig.get("neo4j.password"));
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

  
/*  Querry graphDB for Dnodes at wNum parameter
*   Note this query is to be run agains ZAD graph output. 
*/  
 
public ResultSet wNumQX(int wNum)  {
   
    ResultSet rs=null;
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("MATCH (n:Dnode) - [] -> (c:CreatedBy {wNum:?})\n" +
                                         "WITH DISTINCT n,toString(c.wNum) as wNum\n" +
                                         "RETURN toString(n.d) as denominator, toString(n.n) as numerator, wNum, toString(n.determined) as determined, n.muList as muList"); 
        statements.add(psInsert);
        psInsert.setInt(1,wNum);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {  printSQLException(sqle); }
        statements.remove(0);
    return(rs);
}

public ResultSet wNumQXDetermined(int wNum)  {
   
    ResultSet rs=null;
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("MATCH (n:Dnode {determined:1}  ) - [] -> (c:CreatedBy {wNum:?})\n" +
                                         "WITH DISTINCT n,toString(c.wNum) as wNum\n" +
                                         "RETURN toString(n.d) as denominator, toString(n.n) as numerator, wNum, toString(n.determined) as determined, n.muList as muList"); 
        statements.add(psInsert);
        psInsert.setInt(1,wNum);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {  printSQLException(sqle); }
        statements.remove(0);
    return(rs);
}







public ResultSet wNumQ(int wNum)  {
   
    ResultSet rs=null;
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("MATCH (n:Dnode) - [] -> (c:CreatedBy {wNum:?})\n" +
                            "WITH DISTINCT n,toString(c.wNum) as wNum\n" +
                            "RETURN toString(n.d) as denominator, toString(n.n) as numerator, wNum, toString(n.determined) as determined"); 
        statements.add(psInsert);
        psInsert.setInt(1,wNum);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {  printSQLException(sqle); }
        statements.remove(0);
    return(rs);
}

public ResultSet wNumQQ(int wNum)  {
   
    ResultSet rs=null;
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
                                         "MATCH (n:Dnode) - [] -> (c:CreatedBy {wNum:?})\n" +
                                         "WITH DISTINCT n\n" +
                                         "RETURN toString(n.d) as denominator, toString(n.n) as numerator, toString(n.determined) as determined"); 
        statements.add(psInsert);
        psInsert.setInt(1,wNum);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {  printSQLException(sqle); }
        statements.remove(0);
    return(rs);
}


public ResultSet wNumDenQ(int wNum, String d)  {
   
    ResultSet rs=null;
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
                                         "MATCH (m:MuNumber {d:?})\n" +
                                         "WITH m.d as mDen\n" +
                                         "MATCH (n:Dnode {d:toInteger(mDen)}) - [] -> (c:CreatedBy {wNum:?})   \n" +
                                         "WITH DISTINCT n\n" +
                                         "RETURN toString(n.d) as denominator, toString(n.n) as numerator, ?, toString(n.determined) as determined"); 
        statements.add(psInsert);
        psInsert.setString(1, d);
        psInsert.setInt(2,wNum);
        psInsert.setInt(3,wNum);
        rs = psInsert.executeQuery();
        conn.commit();
     }
         catch (SQLException sqle)
        {  printSQLException(sqle); }
        statements.remove(0);
    return(rs);
}




public ResultSet allMuQ()  {
    
    ResultSet rs=null;
 
    try
     {  
        s = conn.createStatement();
        statements.add(s);
        psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
                                         "MATCH (n:MuNumber )\n" +
                                         "RETURN toString(n.Value) as value,toString(n.size) as size,toString(n.d) as d,toString(n.n) as n,n.nBinary as nBinary"); 
        statements.add(psInsert);
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


public ResultSet muQuerryDB() { 
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
        "RETURN e.nBinary"); 
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

private List muQuerry()  {    
    ResultSet rs;
    List valueList=new ArrayList();   
    rs=muQuerryDB();
    Boolean hasRS = TRUE;
    System.out.println("ValueList: " + valueList.toString());
    try {
        if ( hasRS ) {
        if (rs.next())  {
            
                try {
                    valueList.add(rs.getString(1));
                    //                System.out.println("rs(j): " + rs.getString(j).toString());
                }
                catch(NullPointerException npe) {
                    System.out.println("ResultSet Null");
                }
            
             System.out.println(valueList.toString());
             hasRS=FALSE;
        } else { hasRS=FALSE; } 
        }
    } catch (SQLException ex) {
        Logger.getLogger(DiffDFScripts.class.getName()).log(Level.SEVERE, null, ex);
    }
    //   System.out.println("MuQuerry hasRS: "+hasRS);
    return(valueList);
}

public ResultSet wNumSkipQ(int wNum)  {
   
    ResultSet rs=null;
    try
    {  
       s = conn.createStatement();
       statements.add(s);
       psInsert = conn.prepareStatement("CYPHER runtime=interpreted\n" +
                                        "MATCH (n:Dnode) - [] -> (c:CreatedBy {wNum:?})\n" +
                                        "WITH DISTINCT n,toString(c.wNum) AS wNum,c.skipList AS skipPattern\n" +
                                        "RETURN toString(n.d) AS denominator, toString(n.n) AS numerator, wNum, toString(n.determined) AS determined, skipPattern"); 
       statements.add(psInsert);
       psInsert.setInt(1,wNum);
       rs = psInsert.executeQuery();
       conn.commit();
     }
     catch (SQLException sqle)
        { printSQLException(sqle); }
        statements.remove(0);
    return(rs);
}




public ArrayList<Integer> arrayStringToIntegerArrayList(String arrayString){
    String removedBrackets = arrayString.substring(1, arrayString.length() - 1);
    String[] individualNumbers = removedBrackets.split(",");
    ArrayList<Integer> integerArrayList = new ArrayList<>();
    for(String numberString : individualNumbers){
        integerArrayList.add(Integer.parseInt(numberString.trim()));
    }
    Collections.reverse(integerArrayList);
    return integerArrayList;
}

private List muListBoolean(ArrayList theList, int dimension)  {
List muListBoolean = new ArrayList(); 
int currentTheListValue;
int mLBIndex;

for (int x=0; x<dimension; x++)  {
   muListBoolean.add(false); 
}
//System.out.println("muListBoolean, theList: "+theList.toString());
for (int x=0; x<theList.size(); x++)  {
////    System.out.println(x);
    currentTheListValue=(int)theList.get(x);
    mLBIndex=currentTheListValue-1+x;
    muListBoolean.set(mLBIndex, true);
}
return(muListBoolean);    
}


/*  https://www.geeksforgeeks.org/program-calculate-value-ncr/
*
*/

public static int nCr(int n, int r)
{
    return fact(n) / (fact(r) *
                  fact(n - r));
}

/*  https://www.geeksforgeeks.org/program-calculate-value-ncr/
*
*/

public static int fact(int n)
{
    int res = 1;
    for (int i = 2; i <= n; i++)
        res = res * i;
    return res;
}

//  End of Class    

}
