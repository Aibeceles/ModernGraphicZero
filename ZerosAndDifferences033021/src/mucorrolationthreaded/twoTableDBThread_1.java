/*  Opportunity for injection commented withithin relevant methods.
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mucorrolationthreaded;

import config.DbConfig;
import SqlErata.GaussCorrSql;
import mugauss.*;
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
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aibes
 */

public class twoTableDBThread_1 implements Callable<String> {
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
    MuCorrolation1 mangee = null;
    GaussMain gMain = new GaussMain();
 private BigDecimal trZero = new BigDecimal(1);
 private BigDecimal trTriang = new BigDecimal(4);
 private BigDecimal root1 = new BigDecimal(1);
 private BigDecimal root2 = new BigDecimal(2);
 private BigDecimal root3 = new BigDecimal(3);
 GaussCorrSql gCSql = new GaussCorrSql();   
    
 //    twoTableDBThread_1(Semaphore tlSema,List dbVector,MuCorrolation1 mang){
    twoTableDBThread_1(List dbVector, Semaphore t1Sema,BigDecimal trZero1,BigDecimal trTriang1,BigDecimal root11,BigDecimal root22,BigDecimal root33,GaussCorrSql gCSql){

// bean injection opportunity
 
    dbbVector=dbVector;
    tlSem=t1Sema;
//    mangee=mang;
    trZero=trZero1;
    trTriang=trTriang1;
    root1=root11;
    root2=root22;
    root3=root33;
    this.gCSql=gCSql;
    }   
    
        public String call(){
     //    startConn();

         
    buildPoly(trZero,trTriang,root1,root2,root3);
  //  endConn();
      //     eDB.go();
        return("worker twoTableDBThread_1 return");
        }   
    
    public void buildPoly(BigDecimal trZero,BigDecimal trTriang,BigDecimal root1,BigDecimal root2,BigDecimal root3)
    { 
        BigDecimal rootSquared = new BigDecimal(1);

        // opportunity for injection
        GaussMain gMain = new GaussMain();
        double[][] A = new double[4][5];
        double one=1;
        int i=1;
        boolean results = false;
        System.out.println("inside buildpoly");
  
          try
        { // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 

            ResultSet rs = gCSql.buildPoly(trZero, trTriang, root1, root2, root3);
  System.out.println(trZero +" " + trTriang+" " +root1+" " +root2+" " +root3);
    //        System.out.println(rs.next()+ " rs.next() " + i);
            while ( rs.next() ) {
            BigDecimal trzero = rs.getBigDecimal(1);    
            BigDecimal trtriangle = rs.getBigDecimal(2); 
            BigDecimal mudecimal = rs.getBigDecimal(3); 
            BigDecimal rootone = rs.getBigDecimal(4);
      System.out.println("rootone "+rootone.toString()+" ");
            results=true;
            System.out.println(mudecimal + " i index: " + i);
            rootSquared=rootone.pow(2);
            A[i][1]=rootSquared.doubleValue();
            A[i][2]=rootone.doubleValue();
            A[i][3]=one;
            A[i][4]=mudecimal.doubleValue();
            i++;
            }
            
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
         System.out.println("resultset");
         if (results)  {
                System.out.println("PrintMatrix");
             gMain.printMatrix(A);
             gMain.gauss(dbbVector,tlSem,A, 3, 4,trZero,trTriang,root1,root2,root3);    //a , 4,5  
         } 
            
 //        gMain.gauss(A, 4, 5); 
    }
        
        
        
        
        
        
   public void tlToTable(BigDecimal trZero, BigDecimal trTriangle, BigDecimal muDecimal, BigDecimal rootOne, BigDecimal rootTwo,BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal determinat,BigDecimal numerator,BigDecimal denominator){

   
         go(trZero,trTriangle,muDecimal,rootOne,rootTwo,a,b,c,determinat,numerator,denominator);

       
       
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
            String dbName = DbConfig.get("derby.database"); // the name of the database
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

    public synchronized void go(BigDecimal trZero, BigDecimal trTriangle, BigDecimal muDecimal, BigDecimal rootOne, BigDecimal rootTwo,BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal determinat,BigDecimal numerator,BigDecimal denominator)
    {
        try
        {     // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
            s = conn.createStatement();
            statements.add(s);
     //       System.out.println("twotabledbthread!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            psInsert = conn.prepareStatement("insert into roots values(?, ?, ?, ?, ?,?,?,?,?,?,?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, trZero);
            psInsert.setBigDecimal(2, trTriangle);
            psInsert.setBigDecimal(3, muDecimal);
            psInsert.setBigDecimal(4, rootOne);
            psInsert.setBigDecimal(5, rootTwo);
            psInsert.setBigDecimal(6, a);
            psInsert.setBigDecimal(7, b);
            psInsert.setBigDecimal(8, c);
            psInsert.setBigDecimal(9, determinat);
            psInsert.setBigDecimal(10, numerator);
            psInsert.setBigDecimal(11, denominator);
            psInsert.executeUpdate();
   //         System.out.println("Inserted twotabledbthread");
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
