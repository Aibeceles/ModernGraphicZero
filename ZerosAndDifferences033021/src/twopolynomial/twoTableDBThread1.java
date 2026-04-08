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
public class twoTableDBThread1 extends Thread {
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
    BigDecimal thisNmax = new BigDecimal(0);
    
    twoTableDBThread1(exportDB eeDB,Semaphore tlSema,vertexVector dbVector,plvManager mang){
       dbbVector=dbVector;
       tlSem=tlSema;
       mangee=mang;
    }   
    
        public void run(){
          System.out.println("twotabledbthread1 started "+mangee.isAlive()+dbbVector.size() );   
         startConn();
         System.out.println("                   twotabledbthread1 startedededed "+mangee.isAlive()+dbbVector.size() ); 
 //        while ((mangee.isAlive()) || (dbbVector.size()>0)) {       
 //            System.out.println("twotabledbthread1 while loop" + mangee.isAlive());
 //            try {
 //               tlSem.acquire();
//                System.out.println("twotabledbthread1 run tl" + ((rowBean)dbbVector.elementAt(0)).getTL().toString());
  //              tlToTable(((rowBean)dbbVector.elementAt(0)).getTL(),((rowBean)dbbVector.elementAt(0)).getNN(),((rowBean)dbbVector.elementAt(0)).getFlatFileRowCounter(),((rowBean)dbbVector.elementAt(0)).getNMax());
  //              thisNmax = ((rowBean)dbbVector.elementAt(0)).getNMax();
  //              dbbVector.removeElementAt(0);
  //          } catch (InterruptedException ex) {
  //              Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
  //          }
  //          System.out.println("twotabledbthread1 dbVectorSize"+ dbbVector.size());
  //         }
 //      System.out.println("twotabledbthread1 nMax"+((rowBean)dbbVector.elementAt(0)).getNMax() );   
 //    goDrawProduct(thisNmax);   //  select powers of two for integer table 
 ///    System.out.println("goDrawIntervals................................................." );
  //   goDrawCosets();  // get values for each integer in myfile2
  //   goDrawIntervals(); // make table of intervals from cosets
  //   goDrawMoreQuad();   //  arithmetic of compresult table
 //    goDrawMoreQuadCosets();
     goDrawMoreIntervals();
     System.out.println("twotabledbthread1 ended");
 //    System.out.println("twotabledbthread1 while loop" + mangee.isAlive());
    }   
    
/**
 * resultSet from compresults table containing terms of each integer's quadratic is generated.
 * and passed to goResults1
 * @param j 
*/    
        
        
    public synchronized void goGetPolyCosets(int j)
    {
 //     create table compresults(num numeric(30,0), argument numeric(30,0), scalar numeric(30,0), degree numeric(30,0), rowcounter numeric(30,0), nmax numeric(30,0),resultcounter numeric(30,0), result numeric(30,0))");  
     BigDecimal n = new BigDecimal(j);
     BigDecimal nMax = new BigDecimal(0);
     BigDecimal increment = new BigDecimal(1);
     BigDecimal resultsInteger = new BigDecimal(j);
     try
     {  
        s = conn.createStatement();
        statements.add(s);
            System.out.println("goGetPoly");
            psInsert = conn.prepareStatement("select * from compresults where num=?");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {
            BigDecimal num = rs.getBigDecimal(1);    
            BigDecimal argument = rs.getBigDecimal(2); 
            BigDecimal scalar = rs.getBigDecimal(3); 
            BigDecimal degree = rs.getBigDecimal(4); 
            BigDecimal rowcounter = rs.getBigDecimal(5); 
            BigDecimal nmax = rs.getBigDecimal(6); 
            BigDecimal inc = rs.getBigDecimal(7); 
            BigDecimal result = rs.getBigDecimal(8); 
            System.out.println(num);
            System.out.println(argument);
            System.out.println(scalar);
            System.out.println(degree); 
            System.out.println(rowcounter);
            System.out.println(nmax);
            System.out.println(inc);
            System.out.println(result);
           
      //      goCompIntervals(resultsInteger,argument,scalar,degree,rowcounter,nmax,inc,result);  // new method here
            goResults1(n, argument, scalar, degree, rowcounter, nmax);
           }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
      //  System.out.println(k);   
    }   
         
   /** The quadratic, for each bit in target integer, has result extended and is written to
     * compresults table.
     * 
     * @param resultsInteger
     * @param argument
     * @param scalar
     * @param degree
     * @param rowcounter
     * @param nMax
     * @param inc
     * @param results 
     */
    
    public synchronized void goCompResults(BigDecimal resultsInteger , BigDecimal argument   , BigDecimal scalar , BigDecimal degree , BigDecimal rowcounter , BigDecimal nMax,BigDecimal inc, BigDecimal results){
    BigDecimal counter= new BigDecimal(1);
    BigDecimal increment = new BigDecimal(1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal result = new BigDecimal(0);
    BigDecimal square = new BigDecimal(0);
    result=scalar; 
     
    try
        {     
            if (degree.compareTo(zero)==0) { result=scalar; }
            if (degree.compareTo(one)==0)  { result=scalar.multiply(nMax); }
            if (degree.compareTo(two)==0) 
            {
                square=zero;
                square=nMax.pow(2);
                result=scalar.multiply(square);
            } 
            s = conn.createStatement();
            statements.add(s);
            psInsert = conn.prepareStatement("insert into compresults values(?, ?, ?, ?, ?, ?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, resultsInteger);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, rowcounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.setBigDecimal(7, counter);
            psInsert.setBigDecimal(8, result); 
            psInsert.executeUpdate();
            conn.commit();
         }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
  }
    
    
    /**
     * method for reading integer quadratics and writing interval table
     * note j, range of integers hard coded !!!  set in goDrawProduct
     */
    
    
    
    public synchronized void goDrawIntervals() {
   
    int binInteger = 4;   
    System.out.println("inside godDrawIntrvals results");
    for (int j=binInteger; j< 50; j++) {
        goIntervalResults1(j);    
       }    
    }
   
     
    /** iterate ech integer in morequad cosets. Pass to gomoreintervlresults1
    *
    */
    
    public synchronized void goDrawMoreIntervals() {
        //  method for reading integer quadratics and writing interval table
        //  note j, range of integers hard coded !!!  set in goDrawProduct 
  
         int binInteger = 4;   
    System.out.println("inside godDrawIntrvals results");
    for (int j=binInteger; j< 175; j+=2) {
        goMoreIntervalResults1(j);    
       }    
    }
    
    
    
    
    public synchronized void goDrawMoreQuad() {
        //  method for reading integer quadratics and writing interval table
        //  note j, range of integers hard coded !!!  set in goDrawProduct 
    int binInteger = 4;   
 Runtime r = Runtime.getRuntime();
    System.out.println("inside godDraworeQuad results");
    for (int j=binInteger; j< 175; j+=2) {
        r.gc();
        goMoreQuad(j);    
       }    
    }
        
    
    /**
     * Iterate each integer in compresults table.
     * ToDo: first integer and last integer needs gui assignment.
     * A range of parameters for each quadratic form of each integer is
     * subsequently generated.
     *
     */
    
    
    public synchronized void goDrawCosets() {
        //  method for reading integer quadratics and writing interval table
        //  note j, range of integers hard coded !!!  set in goDrawProduct 
    int binInteger = 4;   
    System.out.println("inside godDrawIntrvals results");
    for (int j=binInteger; j< 150; j+=2) {
        goGetPolyCosets(j);    
       }    
    }    
     
    
    public synchronized void goDrawMoreQuadCosets() {
        //  method for reading integer quadratics and writing interval table
        //  note j, range of integers hard coded !!!  set in goDrawProduct 
    Runtime r = Runtime.getRuntime();
    int binInteger = 4;   
    System.out.println("inside godDrawMoreQuadCosets");
    for (int j=binInteger; j< 175; j+=2) {
        r.gc();
        goMoreQuadCosets(j);    
       }    
    }
    
    public synchronized void goMoreQuadCosets(int j)

    {   
    BigDecimal increment = new BigDecimal(j);     //   increment was previously set to 40
    BigDecimal increment1 = new BigDecimal(1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal resultTally=new BigDecimal(0);
    BigDecimal intervalResult = new BigDecimal(0);
    BigDecimal num = new BigDecimal(j);
    BigDecimal numm = new BigDecimal(0);    
    BigDecimal argument = new BigDecimal(2); 
    BigDecimal scalar = new BigDecimal(3); 
    BigDecimal degree = new BigDecimal(4); 
    BigDecimal rowcounter = new BigDecimal(5); 
    BigDecimal nmax = new BigDecimal(6); 
    BigDecimal inc = new BigDecimal(7); 
    BigDecimal results = new BigDecimal(8);
    String key = new String();
    String key1= new String("ba");
    String keyBlank = new String("");
    for (int k=j; k<175; k+=2) {
        try
        {     
   //       resultTally=zero; 
            s = conn.createStatement();
            statements.add(s);
            System.out.println("goMoreQuadCosets num, increment " + num + " " + increment);
            key=keyBlank;
            key=key.concat(num.toString());
            key=key.concat(increment.toString());
            System.out.println("key " + key);
            //           key="num.toString()+increment.toString()";
            psInsert = conn.prepareStatement("select * from morequad where interval=?");
            statements.add(psInsert);
            psInsert.setString(1, key);
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {      // note resulttally is accumulated
            numm = rs.getBigDecimal(1);    
            argument = rs.getBigDecimal(2); 
            scalar = rs.getBigDecimal(3); 
            degree = rs.getBigDecimal(4); 
            rowcounter = rs.getBigDecimal(5); 
            nmax = rs.getBigDecimal(6); 
            inc = rs.getBigDecimal(7); 
            results = rs.getBigDecimal(8);
            goMoreQuadResults1(numm,argument,scalar,degree,rowcounter,nmax,key);           
            }
            rs.close();
            increment=increment.add(one);
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
      }    
      statements.clear();
    }  
   
    
    public synchronized void goMoreQuadResults1(BigDecimal n, BigDecimal argument, BigDecimal scalar, BigDecimal degree, BigDecimal flatFileRowCounter, BigDecimal nMax, String key)
    {    //  probably not best way but goResults has loop for generating cosets.
    BigDecimal counter= new BigDecimal(1);
    BigDecimal increment = new BigDecimal(10);     //   increment was previously set to 40
    BigDecimal minusOne = new BigDecimal(-1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal result = new BigDecimal(0);
    BigDecimal square = new BigDecimal(0);
    BigDecimal interval = new BigDecimal(0);
    BigDecimal lowInterval = new BigDecimal(0);
    BigDecimal intervalResult = new BigDecimal(0);
    int i = nMax.intValue()+1;    
   //i=i+14;
    //  this loop it for use in coset generation not siimple twoPoly calculation
   for (int j=10; j<i; j++) {
        try
        {     
            if (degree.compareTo(minusOne)==0) { result=scalar; }
            if (degree.compareTo(zero)==0) { result=scalar; result=result.divide(two);  }
            if (degree.compareTo(one)==0)  { result=scalar.multiply(increment); result=result.divide(two); }
            if (degree.compareTo(two)==0) 
            {
                square=zero;
                square=increment.pow(2);
            //    square=counter.pow(2);
                result=scalar.multiply(square);
                result=result.divide(two);
            }
            s = conn.createStatement();
            statements.add(s);
  //          System.out.println("Cosets!!!!!!!!!!:result:" + result + "   "+ increment+" "+degree);
            psInsert = conn.prepareStatement("insert into morequadcosets values(?, ?, ?, ?, ?, ?, ?, ?,?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, flatFileRowCounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.setBigDecimal(7, increment);
            psInsert.setBigDecimal(8, result); 
            psInsert.setString(9, key);
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
     statements.clear();    
    }   
    
    
    
    
    
    
    
    
    
    
    
    
    public synchronized void buildPoly()
    {  //  the querry paramaters are hard coded for now.     note:  not used method!!!!
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
//            System.out.println("NEW Error Code Here!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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
       BigDecimal minusDegree = new BigDecimal(-1);
       Integer test;
       BigDecimal thisArgument = new BigDecimal(0);       // tl argument
       BigDecimal pLVRargument = new BigDecimal(0);                           
       BigDecimal thisScalar = new BigDecimal(0);         //  tl scalar
       vertexTable dbu = new vertexTable();
       for (int j=0; j< tL.size(); j++) { 
       thisArgument = ((vertex)tL.elementAt(j)).getArgument();
       thisScalar = ((vertex)tL.elementAt(j)).getScalar();   
       degree=((vertex)tL.elementAt(j)).getDegree();    
       goResults(N, thisArgument, thisScalar, degree, flatFileRowCounter, nMax);
    }
     goResults(N, thisArgument, N, minusDegree, flatFileRowCounter, nMax);  //  dummy degree to load the constant scalar 
   } 
        
    
 
     public void startConn()  {
       
     System.out.println("SimpleApp starting in twotabeleeelelelel1 " + framework + " mode");
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

            System.out.println("Connected to  database " + dbName);
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
            System.out.println("NEW Error Code Here!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            psInsert = conn.prepareStatement("insert into vertex values(?, ?, ?, ?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, flatFileRowCounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.executeUpdate();
  //          System.out.println("Inserted");
            conn.commit();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
           
    }

    
    public synchronized void goIntervals(BigDecimal n, BigDecimal argument, BigDecimal scalar, BigDecimal degree, BigDecimal flatFileRowCounter, BigDecimal nMax, BigDecimal interval,BigDecimal results)
    {    //  probably not best way but goResults has loop for generating cosets.
    BigDecimal counter= new BigDecimal(1);
    BigDecimal increment = new BigDecimal(1);     //   increment was previously set to 40
    BigDecimal minusOne = new BigDecimal(-1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal result = new BigDecimal(0);
    BigDecimal square = new BigDecimal(0);
  //  int i = nMax.intValue()+1;    
   //i=i+14;
    //  this loop it for use in coset generation not siimple twoPoly calculation
  // for (int j=-20; j<i; j++) {
        result=results;
        try
        {     
               
            s = conn.createStatement();
            statements.add(s);
            System.out.println("intervals!!!!!!!!!!:result:" + result + "   "+ increment+" "+degree);
            psInsert = conn.prepareStatement("insert into intervals values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, flatFileRowCounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.setBigDecimal(7, increment);
            psInsert.setBigDecimal(8, result);
            psInsert.setBigDecimal(9, interval);
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
  //  }    
    }
 
  /** fed ech integer from go draw intervals.
     * include increment for placement on morequadintervals table
     * @param j 
     */ 
    
    public synchronized void goIntervalResults1(int j)

    {   
    BigDecimal increment = new BigDecimal(1);     //   increment was previously set to 40
    BigDecimal increment1 = new BigDecimal(1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal resultTally=new BigDecimal(0);
    BigDecimal intervalResult = new BigDecimal(0);
    BigDecimal num = new BigDecimal(j);
    BigDecimal numm = new BigDecimal(0);    
    BigDecimal argument = new BigDecimal(2); 
    BigDecimal scalar = new BigDecimal(3); 
    BigDecimal degree = new BigDecimal(4); 
    BigDecimal rowcounter = new BigDecimal(5); 
    BigDecimal nmax = new BigDecimal(6); 
    BigDecimal inc = new BigDecimal(7); 
    BigDecimal results = new BigDecimal(8);
    for (int k=1; k<20; k++) {
        try
        {     
            resultTally=zero; 
            s = conn.createStatement();
            statements.add(s);
            System.out.println("goGetPoly num, increment " + num + " " + increment);
            psInsert = conn.prepareStatement("select * from cosets where num=? and resultcounter=?");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, num);
            psInsert.setBigDecimal(2, increment);
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {      // note resulttally is accumulated
            numm = rs.getBigDecimal(1);argument = rs.getBigDecimal(2);scalar = rs.getBigDecimal(3);degree = rs.getBigDecimal(4); 
            rowcounter = rs.getBigDecimal(5);nmax = rs.getBigDecimal(6);inc = rs.getBigDecimal(7);results = rs.getBigDecimal(8);
            resultTally=resultTally.add(results);
           }
            increment1=increment.add(one); 
            for (int l=k+1; l<20; l++) {           
           intervalResult=resultTally.subtract(goIntervalResults2(num,l));
           goCompIntervals(numm,argument,scalar,degree,increment1,nmax,inc,intervalResult);
           increment1=increment1.add(one); 
           }  
            increment=increment.add(one);
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
      }    
    }
   
   
    public synchronized void goSelectMoreInterval()

    {   // j is passed in as prefix 
    BigDecimal increment = new BigDecimal(1);     //   increment was previously set to 40
    BigDecimal increment1 = new BigDecimal(1);
    BigDecimal mIncrement = new BigDecimal(15);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal resultTally=new BigDecimal(0);
    BigDecimal intervalResult = new BigDecimal(0);
    BigDecimal num = new BigDecimal(0);
    BigDecimal numm = new BigDecimal(0);
    BigDecimal numC = new BigDecimal(1);
    BigDecimal argument = new BigDecimal(2); 
    BigDecimal scalar = new BigDecimal(3); 
    BigDecimal degree = new BigDecimal(4); 
    BigDecimal rowcounter = new BigDecimal(5); 
    BigDecimal nmax = new BigDecimal(6); 
    BigDecimal inc = new BigDecimal(7); 
    BigDecimal results = new BigDecimal(8);
    String key = new String();
    String key1= new String("ba");
    String keyBlank = new String("");
    BigDecimal ten = new BigDecimal(25);
    Runtime r = Runtime.getRuntime();
    BigDecimal a = new BigDecimal(37);
    BigDecimal b = new BigDecimal(34);
    BigDecimal c = new BigDecimal(32);
    BigDecimal d = new BigDecimal(30);
    BigDecimal e = new BigDecimal(29);
    BigDecimal f = new BigDecimal(20);
    r.gc();
    for (int k=1; k<175; k++) {
        try
        {     
            s = conn.createStatement();
            statements.add(s);
            System.out.println("goGetPoly num, increment " + " " + increment);
            key=keyBlank;
            key=key.concat(numC.toString());
            mIncrement=ten;
            psInsert = conn.prepareStatement("select * from  morequadintervals where prefix=?");
            statements.add(psInsert);
            psInsert.setString(1, key);            
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {      // make sure mIncrement=inc
            numm = rs.getBigDecimal(1);argument = rs.getBigDecimal(2);scalar = rs.getBigDecimal(3);degree = rs.getBigDecimal(4); 
            rowcounter = rs.getBigDecimal(5);nmax = rs.getBigDecimal(6);inc = rs.getBigDecimal(7);results = rs.getBigDecimal(8);
            }
            rs.close();
            increment1=increment.add(one);
            increment1=mIncrement;
            increment1=increment1.add(one);
       //  goMoreCompIntervals(numm,argument,scalar,degree,increment1,nmax,inc,intervalResult,key,num,numC);
            increment=increment.add(one);
            mIncrement=mIncrement.add(one);
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        numC=numC.add(one);
      }    
    }
      
       
       
       
       
     /** pass results of quadratic parameteriztion x1,x2(from cosets table
     * 
     * @param j 
     */  
       
       
       
       
    public synchronized void goMoreIntervalResults1(int j)

    {   // j=1 to 50
    BigDecimal increment = new BigDecimal(1);     //   increment was previously set to 40
    BigDecimal increment1 = new BigDecimal(1);
    BigDecimal mIncrement = new BigDecimal(15);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal resultTally=new BigDecimal(0);
    BigDecimal intervalResult = new BigDecimal(0);
    BigDecimal num = new BigDecimal(j);
    BigDecimal numm = new BigDecimal(0);
    BigDecimal numC = new BigDecimal(j);
    BigDecimal argument = new BigDecimal(2); 
    BigDecimal scalar = new BigDecimal(3); 
    BigDecimal degree = new BigDecimal(4); 
    BigDecimal rowcounter = new BigDecimal(5); 
    BigDecimal nmax = new BigDecimal(6); 
    BigDecimal inc = new BigDecimal(7); 
    BigDecimal results = new BigDecimal(8);
    String key = new String();
    String key1= new String("ba");
    String keyBlank = new String("");
    BigDecimal ten = new BigDecimal(25);
    Runtime r = Runtime.getRuntime();
    r.gc();
    for (int k=j; k<175; k++) {
        try
        {     
          
            resultTally=zero;
            s = conn.createStatement();
            statements.add(s);
            System.out.println("goGetPoly num, increment " + num + " " + increment);
            key=keyBlank;
            key=key.concat(num.toString());
            key=key.concat(numC.toString());
            mIncrement=ten;
     for (int m=25; m<30; m++) {
            psInsert = conn.prepareStatement("select * from morequadcosets where interval=? and resultcounter=?");
            statements.add(psInsert);
            psInsert.setString(1, key);            
            psInsert.setBigDecimal(2, mIncrement);      
            resultTally=zero;
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {      // make sure mIncrement=inc
            numm = rs.getBigDecimal(1);argument = rs.getBigDecimal(2);scalar = rs.getBigDecimal(3);degree = rs.getBigDecimal(4); 
            rowcounter = rs.getBigDecimal(5);nmax = rs.getBigDecimal(6);inc = rs.getBigDecimal(7);results = rs.getBigDecimal(8);
            resultTally=resultTally.add(results);
           }
            rs.close();
            increment1=increment.add(one);
            increment1=mIncrement;
            increment1=increment1.add(one);
           for (int l=m+1; l<30; l++) {           
           intervalResult=resultTally.subtract(goMoreIntervalResults2(key,l));
           goMoreCompIntervals(numm,argument,scalar,degree,increment1,nmax,inc,intervalResult,key,num,numC,mIncrement);
           increment1=increment1.add(one); 
           }  
            increment=increment.add(one);
            mIncrement=mIncrement.add(one);
    } 
    }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
        numC=numC.add(one);
      }    
    }
     
       
       
       
       
       
    public synchronized BigDecimal goMoreIntervalResults2(String key, int incr)

    {   
    BigDecimal increment = new BigDecimal(incr);     //   increment was previously set to 40
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal resultTally=new BigDecimal(0);
    BigDecimal numm = new BigDecimal(0);    
    BigDecimal argument = new BigDecimal(2); 
    BigDecimal scalar = new BigDecimal(3); 
    BigDecimal degree = new BigDecimal(4); 
    BigDecimal rowcounter = new BigDecimal(5); 
    BigDecimal nmax = new BigDecimal(6); 
    BigDecimal inc = new BigDecimal(7); 
    BigDecimal results = new BigDecimal(8);
    try
     {     
         resultTally=zero; 
         s = conn.createStatement();
         statements.add(s);
//         System.out.println("gointervalresults2 num, increment " + num + " " + increment);
//         psInsert = conn.prepareStatement("select * from morequadcosets where num=? and resultcounter=?");
         psInsert = conn.prepareStatement("select * from morequadcosets where interval=? and resultcounter=?");
         statements.add(psInsert);
         psInsert.setString(1, key);
         psInsert.setBigDecimal(2, increment);
         ResultSet rs = psInsert.executeQuery();
  //       System.out.println("goIntervalResults2  " + num + increment);
         while ( rs.next() ) {      // note resulttally is accumulated
            numm = rs.getBigDecimal(1);    
            argument = rs.getBigDecimal(2); 
            scalar = rs.getBigDecimal(3); 
            degree = rs.getBigDecimal(4); 
            rowcounter = rs.getBigDecimal(5); 
            nmax = rs.getBigDecimal(6); 
            inc = rs.getBigDecimal(7); 
            results = rs.getBigDecimal(8);
            resultTally=resultTally.add(results);

    //  the following logic goes in next nested method which selects each  
    //  second momber of set product.(first is passed as argument. 
           }
         rs.close();
         }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        return(resultTally);  
    }
    
    public synchronized BigDecimal goIntervalResults2(BigDecimal num, int incr)

    {   
    BigDecimal increment = new BigDecimal(incr);     //   increment was previously set to 40
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal resultTally=new BigDecimal(0);
    BigDecimal numm = new BigDecimal(0);    
    BigDecimal argument = new BigDecimal(2); 
    BigDecimal scalar = new BigDecimal(3); 
    BigDecimal degree = new BigDecimal(4); 
    BigDecimal rowcounter = new BigDecimal(5); 
    BigDecimal nmax = new BigDecimal(6); 
    BigDecimal inc = new BigDecimal(7); 
    BigDecimal results = new BigDecimal(8);
    try
     {     
         resultTally=zero; 
         s = conn.createStatement();
         statements.add(s);
         System.out.println("gointervalresults2 num, increment " + num + " " + increment);
         psInsert = conn.prepareStatement("select * from cosets where num=? and resultcounter=?");
         statements.add(psInsert);
         psInsert.setBigDecimal(1, num);
         psInsert.setBigDecimal(2, increment);
         ResultSet rs = psInsert.executeQuery();
  //       System.out.println("goIntervalResults2  " + num + increment);
         while ( rs.next() ) {      // note resulttally is accumulated
            numm = rs.getBigDecimal(1);    
            argument = rs.getBigDecimal(2); 
            scalar = rs.getBigDecimal(3); 
            degree = rs.getBigDecimal(4); 
            rowcounter = rs.getBigDecimal(5); 
            nmax = rs.getBigDecimal(6); 
            inc = rs.getBigDecimal(7); 
            results = rs.getBigDecimal(8);
            resultTally=resultTally.add(results);

    //  the following logic goes in next nested method which selects each  
    //  second momber of set product.(first is passed as argument. 
           }          
         }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        return(resultTally);  
    }
      
    
    public synchronized void goMoreQuad(int j)

    {   
    BigDecimal increment = new BigDecimal(j);     //   increment was previously set to 40
    BigDecimal increment1 = new BigDecimal(1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal resultTally=new BigDecimal(0);
    BigDecimal intervalResult = new BigDecimal(0);
    BigDecimal num = new BigDecimal(j);
    BigDecimal numm = new BigDecimal(0);    
    BigDecimal argument = new BigDecimal(2); 
    BigDecimal scalar = new BigDecimal(3); 
    BigDecimal degree = new BigDecimal(4); 
    BigDecimal rowcounter = new BigDecimal(5); 
    BigDecimal nmax = new BigDecimal(6); 
    BigDecimal inc = new BigDecimal(7); 
    BigDecimal results = new BigDecimal(8);
    String key = new String();
    String key1= new String("ba");
    String keyBlank = new String("");
    Runtime r = Runtime.getRuntime();

    for (int k=j; k<175; k+=2) {
        try
        {   
            r.gc();
   //       resultTally=zero; 
            s = conn.createStatement();
            statements.add(s);
            System.out.println("goGetPoly num, increment " + num + " " + increment);
            key=keyBlank;
            key=key.concat(num.toString());
            key=key.concat(increment.toString());
            System.out.println("key " + key);
            //           key="num.toString()+increment.toString()";
            psInsert = conn.prepareStatement("select * from compresults where num=? or num=?");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, num);
            psInsert.setBigDecimal(2, increment);
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {      // note resulttally is accumulated
            numm = rs.getBigDecimal(1);    
            argument = rs.getBigDecimal(2); 
            scalar = rs.getBigDecimal(3); 
            degree = rs.getBigDecimal(4); 
            rowcounter = rs.getBigDecimal(5); 
            nmax = rs.getBigDecimal(6); 
            inc = rs.getBigDecimal(7); 
            results = rs.getBigDecimal(8);
            goCompMoreQuad(numm,argument,scalar,degree,increment,nmax,inc,results,key);
            //          resultTally=resultTally.add(results);
           }
            rs.close();
//            increment1=increment.add(one); 
//            for (int l=k+1; l<20; l++) {           
//           intervalResult=resultTally.subtract(goMoreQuad1(num,l));
//           goCompMoreQuad(numm,argument,scalar,degree,increment1,nmax,inc,intervalResult);
//           increment1=increment1.add(one); 
//           }  
            increment=increment.add(one);
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
      }    
    }

    public synchronized void goCompMoreQuad(BigDecimal n, BigDecimal argument, BigDecimal scalar, BigDecimal degree, BigDecimal flatFileRowCounter, BigDecimal nMax,BigDecimal inc,BigDecimal results,String key)
    {
    BigDecimal nMaxCopy = new BigDecimal(0);
    BigDecimal counter= new BigDecimal(1);
    BigDecimal decrement = new BigDecimal(-1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal result = new BigDecimal(0);
    BigDecimal square = new BigDecimal(0);
    BigDecimal minusOne = new BigDecimal(-1);
    vertexVector resultList = new vertexVector();
    int i = nMax.intValue();    
    nMaxCopy=nMax;
  
 //       System.out.println("RESULTLIST   ..................");
     
        try
        {     // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
            if (degree.compareTo(minusOne)==0) { }
            if (degree.compareTo(zero)==0) { results=results.divide(two); }
            if (degree.compareTo(one)==0)  { results=results.divide(two); }
            if (degree.compareTo(two)==0)  { results=results.divide(two); }
           s = conn.createStatement();
            statements.add(s);
  //          System.out.println("NEW Error Code Here!!!!!!and here");
            psInsert = conn.prepareStatement("insert into morequad values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, flatFileRowCounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.setBigDecimal(7, inc);
            psInsert.setBigDecimal(8, results);
            psInsert.setString(9, key);
            psInsert.executeUpdate();
  //          System.out.println("Inserted");
            conn.commit();
  //          System.out.println("Committed the transaction");
           statements.clear();
          
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
 }
       
       
       
       
       
       
       
    
    public synchronized BigDecimal goMoreQuad1(BigDecimal num, int incr)

    {   
    BigDecimal increment = new BigDecimal(incr);     //   increment was previously set to 40
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal resultTally=new BigDecimal(0);
    BigDecimal numm = new BigDecimal(0);    
    BigDecimal argument = new BigDecimal(2); 
    BigDecimal scalar = new BigDecimal(3); 
    BigDecimal degree = new BigDecimal(4); 
    BigDecimal rowcounter = new BigDecimal(5); 
    BigDecimal nmax = new BigDecimal(6); 
    BigDecimal inc = new BigDecimal(7); 
    BigDecimal results = new BigDecimal(8);
    try
     {     
         resultTally=zero; 
         s = conn.createStatement();
         statements.add(s);
         System.out.println("gointervalresults2 num, increment " + num + " " + increment);
         psInsert = conn.prepareStatement("select * from cosets where num=? and resultcounter=?");
         statements.add(psInsert);
         psInsert.setBigDecimal(1, num);
         psInsert.setBigDecimal(2, increment);
         ResultSet rs = psInsert.executeQuery();
  //       System.out.println("goIntervalResults2  " + num + increment);
         while ( rs.next() ) {      // note resulttally is accumulated
            numm = rs.getBigDecimal(1);    
            argument = rs.getBigDecimal(2); 
            scalar = rs.getBigDecimal(3); 
            degree = rs.getBigDecimal(4); 
            rowcounter = rs.getBigDecimal(5); 
            nmax = rs.getBigDecimal(6); 
            inc = rs.getBigDecimal(7); 
            results = rs.getBigDecimal(8);
            resultTally=resultTally.add(results);

    //  the following logic goes in next nested method which selects each  
    //  second momber of set product.(first is passed as argument. 
           }          
         }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        return(resultTally);  
    }
      
    
    /**
     * Starting from native evaluate, each integer's quadratic is parameterized 
     * over a range of values.
     * cosets table contains results.
     * increment is an index for cosets table
     * @param n
     * @param argument
     * @param scalar
     * @param degree
     * @param flatFileRowCounter
     * @param nMax 
     */
    
    
    
    public synchronized void goResults1(BigDecimal n, BigDecimal argument, BigDecimal scalar, BigDecimal degree, BigDecimal flatFileRowCounter, BigDecimal nMax)
    {   
    BigDecimal increment = new BigDecimal(1); 
    BigDecimal minusOne = new BigDecimal(-1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal result = new BigDecimal(0);
    BigDecimal square = new BigDecimal(0);
    int i = nMax.intValue()+1;    
   //i=i+14;
    //  this loop it for use in coset generation not siimple twoPoly calculation
   for (int j=1; j<i; j++) {
        try
        {     
            if (degree.compareTo(minusOne)==0) { result=scalar; }
            if (degree.compareTo(zero)==0) { result=scalar; result=result.divide(two);  }
            if (degree.compareTo(one)==0)  { result=scalar.multiply(increment); result=result.divide(two); }
            if (degree.compareTo(two)==0) 
            {
                square=zero;
                square=increment.pow(2);
                result=scalar.multiply(square);
                result=result.divide(two);
            }
            s = conn.createStatement();
            statements.add(s);
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
            conn.commit();
            increment=increment.add(one);
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
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
    result=scalar; 
    int i = nMax.intValue();    
    
 //   the result is not div by two
        try
        {     // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
            if (degree.compareTo(zero)==0) { result=scalar; }
            if (degree.compareTo(one)==0)  { result=scalar.multiply(nMax); }
            if (degree.compareTo(two)==0) 
            {
                square=zero;
                square=nMax.pow(2);
            //    square=counter.pow(2);
                result=scalar.multiply(square);
            } 
            s = conn.createStatement();
            statements.add(s);
  //          System.out.println("NEW Error Code Here!!!!!!and here");
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
  //          System.out.println("Inserted");
            conn.commit();
  //          System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
   //   }    
    }
    
    
    
     public synchronized void goCompIntervals(BigDecimal n, BigDecimal argument, BigDecimal scalar, BigDecimal degree, BigDecimal flatFileRowCounter, BigDecimal nMax,BigDecimal inc,BigDecimal results)
    {
    BigDecimal nMaxCopy = new BigDecimal(0);
    BigDecimal counter= new BigDecimal(1);
    BigDecimal decrement = new BigDecimal(-1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal result = new BigDecimal(0);
    BigDecimal square = new BigDecimal(0);
    vertexVector resultList = new vertexVector();
    int i = nMax.intValue();    
    nMaxCopy=nMax;
  
        System.out.println("RESULTLIST   ..................");
     
        try
        {     // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
          
            s = conn.createStatement();
            statements.add(s);
  //          System.out.println("NEW Error Code Here!!!!!!and here");
            psInsert = conn.prepareStatement("insert into interresults values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, flatFileRowCounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.setBigDecimal(7, inc);
            psInsert.setBigDecimal(8, results);
            psInsert.setBigDecimal(9, two);
            psInsert.executeUpdate();
  //          System.out.println("Inserted");
            conn.commit();
  //          System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
 }
    

 /**
      *  interval result arithmetic insert into morequadintervals
      * @param n
      * @param argument
      * @param scalar
      * @param degree
      * @param flatFileRowCounter
      * @param nMax
      * @param inc
      * @param results
      * @param key
      * @param prefix
      * @param suffix 
      */    
     
     
     
     public synchronized void goMoreCompIntervals(BigDecimal n, BigDecimal argument, BigDecimal scalar, BigDecimal degree, BigDecimal flatFileRowCounter, BigDecimal nMax,BigDecimal inc,BigDecimal results,String key,BigDecimal prefix,BigDecimal suffix,BigDecimal increment)
   {
    BigDecimal nMaxCopy = new BigDecimal(0);
    BigDecimal counter= new BigDecimal(1);
    BigDecimal decrement = new BigDecimal(-1);
    BigDecimal zero = new BigDecimal(0);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal result = new BigDecimal(0);
    BigDecimal square = new BigDecimal(0);
    vertexVector resultList = new vertexVector();
    int i = nMax.intValue();    
    nMaxCopy=nMax;
    System.out.println("RESULTLIST   ..................");
     
        try
        {     // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
          
            s = conn.createStatement();
            statements.add(s);
           System.out.println(n + " " + argument + " " + scalar + " " + degree + " " + flatFileRowCounter);
           System.out.println(nMax + " " + inc + " " + results + " " + two + " " + key);
            psInsert = conn.prepareStatement("insert into morequadintervals values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statements.add(psInsert);                       
//goMoreCompIntervals(numm,argument,scalar,degree,increment1,nmax,inc,intervalResult,key,num,numC);
//goMoreCompIntervals(n,argument,scalar,degree,flatFileRowCounter,nMax,inc,results,key,prefix,suffix)

            psInsert.setBigDecimal(1, n);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, flatFileRowCounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.setBigDecimal(7, inc);
            psInsert.setBigDecimal(8, results);
            psInsert.setBigDecimal(9, increment);
            psInsert.setString(10,key);
            psInsert.setBigDecimal(11, prefix);
            psInsert.setBigDecimal(12, suffix);
            psInsert.executeUpdate();
  //  goMoreCompIntervals(numm,argument,scalar,degree,increment1,nmax,inc,intervalResult,key);
  //                       n,argument,scalar,degree,flatFileRowCounter,nMax,inc,results,key
            
            conn.commit();
            statements.clear();
  //          System.out.println("j1Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
 }

/**
 * Significant bit k of target integer j is on results table.  Look it up.  
 * Pass resultSet to goCompResults.
 * @param k
 * @param j 
 */

    public synchronized void goGetPoly(int k, int j)
    {
   
     BigDecimal n = new BigDecimal(k);
     BigDecimal resultsInteger = new BigDecimal(j);
     try
     {  
        s = conn.createStatement();
        statements.add(s);
            System.out.println("goGetPoly");
            psInsert = conn.prepareStatement("select * from results where num=?");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, n);
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {
            BigDecimal num = rs.getBigDecimal(1);    
            BigDecimal argument = rs.getBigDecimal(2); 
            BigDecimal scalar = rs.getBigDecimal(3); 
            BigDecimal degree = rs.getBigDecimal(4); 
            BigDecimal rowcounter = rs.getBigDecimal(5); 
            BigDecimal nmax = rs.getBigDecimal(6); 
            BigDecimal inc = rs.getBigDecimal(7); 
            BigDecimal result = rs.getBigDecimal(8); 
            goCompResults(resultsInteger,argument,scalar,degree,rowcounter,nmax,inc,result);
            }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        System.out.println(k);   
    }        
    
   /**
      * For each integer 4-175 identify and pass to goGetPoly 
      * each significant binary bit,respective integer. 
      * 
      * @param nMax 
      */  
    
    public synchronized void goDrawProduct(BigDecimal nMax)
    {
    // note two polynomial starts is draw at n=4  FIGURED HOW TO GT N=3 N=2, N=1 URRNTLY CONSTANTS
    int binInteger = 4;   
    int auxCounter;
    String binString;
    int i = nMax.intValue();    
     System.out.println("inside go results");
    for (int j=binInteger; j< 175; j+=2) {
       binString=Integer.toBinaryString(j);
       System.out.println(j + "   " + binString.toString() + " " + binString.length()); 
       auxCounter=binString.length();
       for (int k=0; k<binString.length();k++) {
    //   auxCounter=binString.length();
       if (binString.charAt(auxCounter-1)=='1')
        {
           goGetPoly(k+1,j);    
        }
        auxCounter=auxCounter-1;
       }
   }    
    
    
 //   
 //   try
 //       {     // s.execute("create table results(num numeric(20,0), argument numeric(20,0), scalar numeric(20,0), degree numeric(20,0), rowcounter numeric(20,0), nmax numeric(20,0))"); 
 //           if (degree.compareTo(zero)==0) { result=scalar; }
 //           if (degree.compareTo(one)==0)  { result=scalar.multiply(nMax); }
 //           if (degree.compareTo(two)==0) 
 //           {
 //               square=zero;
 //               square=nMax.pow(2);
 //           //    square=counter.pow(2);
 //               result=scalar.multiply(square);
 //           } 
 //           s = conn.createStatement();
 //           statements.add(s);
  //          System.out.println("NEW Error Code Here!!!!!!and here");
 //           psInsert = conn.prepareStatement("insert into results values(?, ?, ?, ?, ?, ?, ?, ?)");
 //           statements.add(psInsert);
 //           psInsert.setBigDecimal(1, n);
 //           psInsert.setBigDecimal(2, argument);
 //           psInsert.setBigDecimal(3, scalar);
 //           psInsert.setBigDecimal(4, degree);
 //           psInsert.setBigDecimal(5, flatFileRowCounter);
 //           psInsert.setBigDecimal(6, nMax);
 //           psInsert.setBigDecimal(7, counter);
 //           psInsert.setBigDecimal(8, result); 
 //           
 //           psInsert.executeUpdate();
  //          System.out.println("Inserted");
 //           conn.commit();
  //          System.out.println("Committed the transaction");
 //       }
 //        catch (SQLException sqle)
 //       {
 //           printSQLException(sqle);
 //       } 
   //   }    
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

    
    
    
    
    
    
    

