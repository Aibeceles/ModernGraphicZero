/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package twopolynomial;
import config.DbConfig;
import java.beans.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aibes
 */
public class twtableDBThreadCoset extends Thread {
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
    exportDB eDB = new exportDB();
    rowBean dBbean = new rowBean();   // database buffer is a queue of rowBeans
    vertexVector dbbVector = new vertexVector();    
    plvManager mangee = null;
   
    twtableDBThreadCoset(Semaphore cstSem,vertexVector cstVector,plvManager mang){
       dbbVector=cstVector;
       tlSem=cstSem;
       mangee=mang;
    }   
    
        public void run(){
         startConn();
   System.out.println("                  cosets!!!!!!!!!!!!!!!!!!!!!");
         while ((mangee.isAlive()) || (dbbVector.size()>0)) {       
         System.out.println("twotabledbthread     cosets!!!!!! while loop" + mangee.isAlive());
             try {
                tlSem.acquire();   
                tlToTable(((rowBean)dbbVector.elementAt(0)).getTL(),((rowBean)dbbVector.elementAt(0)).getNN(),((rowBean)dbbVector.elementAt(0)).getFlatFileRowCounter(),((rowBean)dbbVector.elementAt(0)).getNMax());
                dbbVector.removeElementAt(0);
            } catch (InterruptedException ex) {
                Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
            }

           }

       }   
    
    public synchronized void buildPoly()
    {  //  the querry paramaters are hard coded for now. 
       // the query partamaters are hard codend for now.  see plvManager currently 4,10-22
        BigDecimal n = new BigDecimal(9);
        BigDecimal nMax = new BigDecimal(0);
        BigDecimal increment = new BigDecimal(1);
        int j = 10;
         for (j=10; j<23; j++)
         nMax=nMax.add(increment);
        {
         for (int k=5; k<j; k++)
        {
         n=n.add(increment);       
        
        try
        { // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
            s = conn.createStatement();
            statements.add(s);
            System.out.println("NEW Error Code Here!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            psInsert = conn.prepareStatement("select * from vertex where nmax=? and num=?");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, nMax);

            ResultSet rs = psInsert.executeQuery();
            
            while ( rs.next() ) {
            BigDecimal num = rs.getBigDecimal(1);    
            BigDecimal argument = rs.getBigDecimal(2); 
            BigDecimal scalar = rs.getBigDecimal(3); 
            BigDecimal degree = rs.getBigDecimal(4); 
            BigDecimal rowcounter = rs.getBigDecimal(5); 
            BigDecimal nmax = rs.getBigDecimal(6); 
            System.out.println(num);
            System.out.println(argument);
            System.out.println(scalar);
            System.out.println(degree); 
            System.out.println(rowcounter);
            System.out.println(nmax);
            }
          //  System.out.println("Inserted");
          //  conn.commit();
          //  System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
    }
}
    }
        
        
    public void tlToTable(vertexVector tL,BigDecimal N, BigDecimal flatFileRowCounter, BigDecimal nMax){
       vertex nextVertex = null;   //  what does this do
       BigDecimal pLVRdegree= new BigDecimal(0);   //  span tl,  accumulate tl from row to row                                                
       BigDecimal pLVRscalar= new BigDecimal(0);                                
       BigDecimal degree= new BigDecimal(1);              // tl degree  
       Integer test;
       BigDecimal thisArgument = new BigDecimal(0);       // tl argument
       BigDecimal pLVRargument = new BigDecimal(0);                           
       BigDecimal thisScalar = new BigDecimal(0);         //  tl scalar
       vertexTable dbu = new vertexTable();

    for (int j=0; j< tL.size(); j++) { 
     thisArgument = ((vertex)tL.elementAt(j)).getArgument();
     thisScalar = ((vertex)tL.elementAt(j)).getScalar();   
     degree=((vertex)tL.elementAt(j)).getDegree();    
//     System.out.println("twotabledbthread1 tl t1ToTable"+ tL.toString());
     goResults(N, thisArgument, thisScalar, degree, flatFileRowCounter, nMax);
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
            String dbName = DbConfig.get("derby.database.two");
            conn = DriverManager.getConnection(protocol + dbName
                    + ";", props);

            System.out.println("Connected to  database twotabledb1thread   cosets!!!!!!!! " );
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


            if (framework.equals("embedded"))
            {
                try
                {
                    // the shutdown=true attribute shuts down Derby
                    DriverManager.getConnection("jdbc:derby:;shutdown=true");

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

    public synchronized void go(BigDecimal n, BigDecimal argument, BigDecimal scalar, BigDecimal degree, BigDecimal flatFileRowCounter, BigDecimal nMax)
    {
        try
        {     // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
            s = conn.createStatement();
            statements.add(s);
     //       System.out.println("twotabledbthread!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            psInsert = conn.prepareStatement("insert into vertex values(?, ?, ?, ?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, flatFileRowCounter);
            psInsert.setBigDecimal(6, nMax);
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
    {    //  probably not best way but goResults has loop for generating cosets.
    BigDecimal counter= new BigDecimal(1);
    BigDecimal increment = new BigDecimal(1);     //   increment was previously set to 40
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal result = new BigDecimal(0);
    BigDecimal square = new BigDecimal(0);
    int i = nMax.intValue()+1;    
   i=i+14;
    //  this loop it for use in coset generation not siimple twoPoly calculation
   for (int j=-20; j<i; j++) {
        try
        {     
            if (degree.compareTo(zero)==0) { result=scalar; }
            if (degree.compareTo(one)==0)  { result=scalar.multiply(increment); }
            if (degree.compareTo(two)==0) 
            {
                square=zero;
                square=increment.pow(2);
            //    square=counter.pow(2);
                result=scalar.multiply(square);
            }
            
            s = conn.createStatement();
            statements.add(s);
           System.out.println("Cosets!!!!!!!!!!:result:" + result + "   "+ increment+" "+degree);
            psInsert = conn.prepareStatement("insert into cosets values(?, ?, ?, ?, ?, ?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, flatFileRowCounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.setBigDecimal(7, increment);
            psInsert.setBigDecimal(8, result); 
            psInsert.executeUpdate();
  //          System.out.println("Inserted");
            conn.commit();
  //          System.out.println("Committed the transaction");
            increment=increment.add(one);
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
