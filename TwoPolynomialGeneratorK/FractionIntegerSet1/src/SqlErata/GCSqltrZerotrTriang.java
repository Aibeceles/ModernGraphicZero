/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SqlErata;

import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import static mucorrolationthreaded.twoTableDBThread_1.printSQLException;
import mugauss.GaussMain;
import config.DbConfig;

/**
 *
 * @author HP_Administrator
 */
public class GCSqltrZerotrTriang {
 
 public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private String sampleProperty;
    private PropertyChangeSupport propertySupport;
    private String framework = "embedded";
    private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    private String protocol = DbConfig.get("derby.url");
    private Connection conn = null;
    ArrayList statements = new ArrayList(); // list of Statements, PreparedStatements
    PreparedStatement psInsert = null;
    PreparedStatement psUpdate = null;
    Statement s = null;
    Statement t = null;
    ResultSet rs = null;      
    
   public GCSqltrZerotrTriang() 
    {
    }
    
        private void loadDriver() {
        try {
            Class.forName(driver).newInstance();
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
    
    
    
     public void startConn()  {
         System.out.println("Connected to  database twotabledb1thread " );
     System.out.println("SimpleApp starting in " + framework + " mode");
     loadDriver();
      try {
           Properties props = new Properties(); // connection properties
            // providing a user name and password is optional in the embedded
            // and derbyclient frameworks
            props.put("user", DbConfig.get("derby.user"));
            props.put("password", DbConfig.get("derby.password"));
            String dbName = DbConfig.get("derby.database");
            conn = DriverManager.getConnection(protocol + dbName
                    + ";", props);
            System.out.println("Connected to  database twotabledb1thread " );
            // We want to control transactions manually. Autocommit is on by
            // default in JDBC.
            conn.setAutoCommit(false);
           }

        catch (SQLException sqle)
        {
          System.out.println("trubble with twotabledbthread_1 .startcon " );
            printSQLException(sqle);
        } 

   }    
    
    
public  ResultSet buildPoly(BigDecimal trZero,BigDecimal trTriang)
    { 
        BigDecimal rootSquared = new BigDecimal(1);


        System.out.println("inside      GaussCorrSql()     buildpoly");
  synchronized(this) {
          try
        { // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
            s = conn.createStatement();
            statements.add(s);
//            System.out.println("NEW Error Code Here!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            psInsert = conn.prepareStatement("select * from roots where (trzero=? and trtriangle=?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, trZero);
            psInsert.setBigDecimal(2, trTriang);
            ResultSet rs = psInsert.executeQuery();
        //     System.out.println("                RS.ToString"+rs.toString());
            System.out.println(" GCsqltrztrt ");
            return(rs);
            
    //        while ( rs.next() ) {
    //        BigDecimal trzero = rs.getBigDecimal(1);    
    //        BigDecimal trtriangle = rs.getBigDecimal(2); 
    //        BigDecimal mudecimal = rs.getBigDecimal(3); 
     //       BigDecimal rootone = rs.getBigDecimal(4);
    //        results=true;
    //        System.out.println(mudecimal + " i index: " + i);
    //        rootSquared=rootone.pow(2);
     //       A[i][1]=rootSquared.doubleValue();
     //       A[i][2]=rootone.doubleValue();
     //       A[i][3]=one;
     //       A[i][4]=mudecimal.doubleValue();
     //       i++;
     //       }
            
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
  return(rs); 
  }
          
    }  
    
 public void endConn()  {


 //           if (framework.equals("embedded"))
            {
                try
                {
                    // the shutdown=true attribute shuts down Derby
           //         DriverManager.getConnection("jdbc:derby:;shutdown=true");
                    conn.close();
                    // To shut down a specific database only, but keep the
                    // engine running (for example for connecting to other
                    // databases), specify a database in the connection URL:
                    //DriverManager.getConnection("jdbc:derby:" + dbName + ";shutdown=true");
                }
                catch (SQLException se)
                {
            System.out.println("trubble with twotabledbthread_1 .endcon 1" );
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
                    System.out.println("trubble with twotabledbthread_1 .endcon 2" );
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
                System.out.println("trubble with twotabledbthread_1 .endcon 3" );
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
    System.out.println("trubble with twotabledbthread_1 .endcon 4" );
                printSQLException(sqle);
            }
        }
}   
    
    
    
    
    
}
