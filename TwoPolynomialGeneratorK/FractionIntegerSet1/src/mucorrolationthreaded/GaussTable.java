/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mucorrolationthreaded;

/**
 *
 * @author Aibes
 */

import java.beans.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import config.DbConfig;

/**
 *
 * @author Aibes
 */

public class GaussTable extends Thread {
    private Semaphore tlSem;  
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
    
    rowBean dBbean = new rowBean();   // database buffer is a queue of rowBeans
    List dbbVector = new ArrayList();    
    GaussCorrolation mangee = null;
   
    public GaussTable(Semaphore tlSema,List dbVector,GaussCorrolation mang){
       dbbVector=dbVector;
       tlSem=tlSema;
       mangee=mang;
    }   
    
        public void run(){
         startConn();
 
          System.out.println("is it inside GaussCorrolation? ");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            Logger.getLogger(GaussTable.class.getName()).log(Level.SEVERE, null, ex);
       }
          while ((mangee.isAlive()) || (dbbVector.size()>0)) {       
             System.out.println("GaussCorrolation while loop" + mangee.isAlive());
             try {
                tlSem.acquire();
                System.out.println( ((GaussBean)dbbVector.get(0)).getY1()+" "+((GaussBean)dbbVector.get(0)).getY2()+" "+((GaussBean)dbbVector.get(0)).getY3()+" " +((GaussBean)dbbVector.get(0)).getTrZero()+" "+((GaussBean)dbbVector.get(0)).getTrTr()+" "+((GaussBean)dbbVector.get(0)).getRoot1()+" "+((GaussBean)dbbVector.get(0)).getRoot2()+" "+((GaussBean)dbbVector.get(0)).getRoot3());                                                    // go(double y1,double y2,double y3,BigDecimal trZer,BigDecimal trTr,BigDecimal root1,BigDecimal root2,BigDecimal root3)               
 System.out.println("                       gauss6able      ");
                go(((GaussBean)dbbVector.get(0)).getY1(),((GaussBean)dbbVector.get(0)).getY2(),((GaussBean)dbbVector.get(0)).getY3(),((GaussBean)dbbVector.get(0)).getTrZero(),((GaussBean)dbbVector.get(0)).getTrTr(),((GaussBean)dbbVector.get(0)).getRoot1(),((GaussBean)dbbVector.get(0)).getRoot2(),((GaussBean)dbbVector.get(0)).getRoot3());
//                tlToTable(((rowBean)dbbVector.elementAt(0)).getTL(),((rowBean)dbbVector.elementAt(0)).getNN(),((rowBean)dbbVector.elementAt(0)).getFlatFileRowCounter(),((rowBean)dbbVector.elementAt(0)).getNMax());
                dbbVector.remove(0);
//                dbbVector.removeElementAt(0);
            } catch (InterruptedException ex) {
                Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
            }
 //           System.out.println("twotabledbthread dbVectorSize"+ dbbVector.size());
           }
           System.out.println("twotabledbthread ended");
 //          System.out.println("TwoTableDBThread while loop" + mangee.isAlive());
          //  new method for polynomials 
        //   buildPoly();
      endConn();      //  maybe endconn is throwing excejptions  
//    startConn();
//    buildPoly();
//    endConn();
      //     eDB.go();
       }   
    
    public synchronized void buildPoly()
    {  //  the querry paramaters are hard coded for now. 
       // the query partamaters are hard codend for now.  see plvManager currently 4,10-22
        BigDecimal n = new BigDecimal(9);
        BigDecimal nMax = new BigDecimal(0);
        BigDecimal increment = new BigDecimal(1);
        
         System.out.println("inside buildpoly");
        try
        { // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
            s = conn.createStatement();
            statements.add(s);
//            System.out.println("NEW Error Code Here!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            psInsert = conn.prepareStatement("select * from roots");
            statements.add(psInsert);
 

            ResultSet rs = psInsert.executeQuery();
            
            while ( rs.next() ) {
            BigDecimal num = rs.getBigDecimal(1);    
            BigDecimal argument = rs.getBigDecimal(2); 
            BigDecimal scalar = rs.getBigDecimal(3); 
            BigDecimal degree = rs.getBigDecimal(4); 
            BigDecimal rowcounter = rs.getBigDecimal(5); 
 
            System.out.println(num);
            System.out.println(argument);
            System.out.println(scalar);
            System.out.println(degree); 
            System.out.println(rowcounter);
            
            }
          //  System.out.println("Inserted");
          //  conn.commit();
          //  System.out.println("Committed the transaction");
         s.execute("CALL SYSCS_UTIL.SYSCS_EXPORT_TABLE (null,'rootlist','rootlist.txt',null,null,null)");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 

    }
        
        
        
        
        
        
   public void tlToTable(BigDecimal trZero, BigDecimal trTriangle, BigDecimal muDecimal, BigDecimal rootOne, BigDecimal rootTwo,BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal determinat,BigDecimal numerator,BigDecimal denominator){

   
   //      go(trZero,trTriangle,muDecimal,rootOne,rootTwo,a,b,c,determinat,numerator,denominator);

       
       
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
            printSQLException(sqle);
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

public synchronized void go(double y1,double y2,double y3,BigDecimal trZer,BigDecimal trTr,BigDecimal root1,BigDecimal root2,BigDecimal root3)

 //   public synchronized void go(BigDecimal trZero, BigDecimal trTriangle, BigDecimal muDecimal, BigDecimal rootOne, BigDecimal rootTwo,BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal determinat,BigDecimal numerator,BigDecimal denominator)
    {
        try
        {     // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
            s = conn.createStatement();
            statements.add(s);
            System.out.println("gausstable!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            psInsert = conn.prepareStatement("insert into gausstable values(?, ?, ?, ?, ?,?,?,?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, trTr);
            psInsert.setBigDecimal(2, trZer);
            psInsert.setDouble(3, y1);
            psInsert.setDouble(4, y2);
            psInsert.setDouble(5, y3);
            psInsert.setBigDecimal(6, root1);
            psInsert.setBigDecimal(7, root2);
            psInsert.setBigDecimal(8, root3);
            psInsert.executeUpdate();
            System.out.println("Inserted gausstable thread");
            conn.commit();
  //          System.out.println("Committed the transaction twotabledbthread");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
           
    }

    public synchronized void goResults(BigDecimal n, BigDecimal argument, BigDecimal scalar, BigDecimal degree, BigDecimal flatFileRowCounter, BigDecimal nMax)
    {
    BigDecimal counter= new BigDecimal(1);
    BigDecimal increment = new BigDecimal(1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal result = new BigDecimal(0);
    BigDecimal square = new BigDecimal(0);
    int i = nMax.intValue();    
    
    for (int j=0; j<i; j++) {
        try
        {     // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
            if (degree.compareTo(zero)==0) { result=scalar; }
            if (degree.compareTo(one)==0)  { result=scalar.multiply(counter); }
            if (degree.compareTo(two)==0) 
            {
                square=zero;
                square=counter.pow(2);
                result=scalar.multiply(square);
            } 
            s = conn.createStatement();
            statements.add(s);
            System.out.println("NEW Error Code Here!!!!!!and here");
            psInsert = conn.prepareStatement("insert into results values(?, ?, ?, ?, ?, ?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, flatFileRowCounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.setBigDecimal(7, counter);
            psInsert.setBigDecimal(8, result); 
            
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
      }    
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
            //e.printStackTrace(System.err);
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
    
    public String getSampleProperty() {
        return sampleProperty;
    }
    
    public void setSampleProperty(String value) {
        String oldValue = sampleProperty;
        sampleProperty = value;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }







}




