/*  test the project update
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
import java.util.Stack;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *                        implementing the arithmetic table that is not yet created
 * @author Aibes
 */
public class fakeLP extends Thread {

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
    Stack lpStack = new Stack();
    
    
    fakeLP(){
    }    


    public void deleteKey(BigDecimal lastEtest, String lastEName) {
       System.out.println("DELETEKEY " + lastEtest);  
         try
        {  
            s = conn.createStatement();

            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("delete from morequadintervalscopy where name=? and result=?");
            statements.add(psInsert);
            psInsert.setString(1, lastEName);
            psInsert.setBigDecimal(2, lastEtest);
            psInsert.executeUpdate();
            System.out.println("DELETED");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
     }
    

  public void writeSolutionArgs(String lastEName, BigDecimal numm, BigDecimal argument, BigDecimal scalar, BigDecimal degree, BigDecimal rowCounter, BigDecimal nMax, BigDecimal inc, BigDecimal results, BigDecimal sCounter){   
//s.execute("create table morequadcosets(num numeric(30,0), argument numeric(30,0), scalar numeric(30,0), degree numeric(30,0),"
//        + " rowcounter numeric(30,0), nmax numeric(30,0),resultcounter numeric(30,0), result numeric(30,0),interval char(30))");    

      try
        {     
            s = conn.createStatement();
            statements.add(s);
            psInsert = conn.prepareStatement("insert into solutiontable values(?, ?, ?, ?, ?, ?, ?, ?,?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, sCounter);
            psInsert.setBigDecimal(2, argument);
            psInsert.setBigDecimal(3, scalar);
            psInsert.setBigDecimal(4, degree);
            psInsert.setBigDecimal(5, rowCounter);
            psInsert.setBigDecimal(6, nMax);
            psInsert.setBigDecimal(7, inc);
            psInsert.setBigDecimal(8, results); 
            psInsert.setString(9,lastEName);
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
    
    
public void getSolutionArgs(String lastEName, BigDecimal sCounter)   {

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
            s = conn.createStatement();
            statements.add(s);
            System.out.println("getSolutionArgs " );
            psInsert = conn.prepareStatement("select * from morequadcosets where interval=?");
            statements.add(psInsert);
            psInsert.setString(1, lastEName);            
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {      // make sure mIncrement=inc
            numm = rs.getBigDecimal(1);
            argument = rs.getBigDecimal(2);
            scalar = rs.getBigDecimal(3);
            degree = rs.getBigDecimal(4); 
            rowcounter = rs.getBigDecimal(5);
            nmax = rs.getBigDecimal(6);
            inc = rs.getBigDecimal(7);
            results = rs.getBigDecimal(8);
            writeSolutionArgs(lastEName, numm, argument, scalar, degree, rowcounter, nmax, inc, results,sCounter);
            }
            rs.close();
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }    
}    
    
 public void writeSolution1(Stack lpStack, solutionCounter sCounter) {
 BigDecimal zero=new BigDecimal(0); 
 BigDecimal lastEtest= new BigDecimal(0);
 String lastEName;
 String blank=new String("");
 for(int i=0;i<lpStack.size();i++)
 {
    lastEtest=(BigDecimal)((lpStObj)lpStack.get(i)).value;
    lastEName=(String)((lpStObj)lpStack.get(i)).name;
    if ( lastEtest.compareTo(zero)==1) {
//    s.execute("create table floatsolution(result numeric(30,0),solutionnumber numeric(30,0),name char(30),prefix char(30),suffix char(30))");
    getSolutionArgs(lastEName,sCounter.getCurrent());  
        try
        {  
            s = conn.createStatement();
            statements.add(s);
            System.out.println("solutionTable insert");
            psInsert = conn.prepareStatement("insert into floatsolution values(?, ?, ?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, lastEtest);
            psInsert.setBigDecimal(2, sCounter.getCurrent());
            psInsert.setString(3, lastEName);
            psInsert.setString(4, blank);
            psInsert.setString(5, blank);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
          
        
        
    }

}     
sCounter.increment();       
       
       
}
    
    
    public void writeSolution(Stack lpStack,solutionCounter sCounter) {
    
    BigDecimal zero=new BigDecimal(0);    
    BigDecimal lastEtest= new BigDecimal(0);
    String lastEName;
    System.out.println("WRITESOLUTION ");   
     
  //    int k = lpStack.size();
    writeSolution1(lpStack,sCounter);
    int k = lpStack.size()+1;    
    for (int j=1; j<k; j++) {
    lastEtest=(BigDecimal)((lpStObj)lpStack.lastElement()).value;
    lastEName=(String)((lpStObj)lpStack.lastElement()).name;
   //     lastEtest=(BigDecimal)lpStack.lastElement();
     if ( lastEtest.compareTo(zero)==1) {
      deleteKey(lastEtest,lastEName);
      j=k;
     }
       lpStack.pop();
     System.out.println("popping lpStack " + lpStack.toString() + "  j " + j);
    }
    System.out.println("popping lpStack " + lpStack.toString());
    }
    
    
    public void popping(Stack lpStack, BigDecimal popNumber){
//    int k = lpStack.size();
    int k = lpStack.size()+1;    
    System.out.println("popping lpStack  popNumber" + popNumber.intValue() + "  lpStack size" + lpStack.size());
    for (int j=popNumber.intValue(); j<k; j++) {
        lpStack.pop();
     System.out.println("popping lpStack " + lpStack.toString() + "  j " + j);
    }
    
    System.out.println("popping lpStack " + lpStack.toString());
    }
    
    public BigDecimal startLP(Stack lpStack,BigDecimal rCounter,BigDecimal cCounter,BigDecimal pFix) {  
                                
    BigDecimal aSize = new BigDecimal(5);
    BigDecimal one = new BigDecimal(1);
    BigDecimal mOne = new BigDecimal(-1);
    BigDecimal rID = new BigDecimal(0);
    BigDecimal cID = new BigDecimal(0);
    BigDecimal value = new BigDecimal(-1);
    try
    {  
        s = conn.createStatement();
        statements.add(s);
        System.out.println("startLP " + rCounter + " " + cCounter);
        psInsert = conn.prepareStatement("select * from algebraTable where rid=? and cid=?",ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
        statements.add(psInsert);
        psInsert.setBigDecimal(1, rCounter);
        psInsert.setBigDecimal(2, cCounter);
        ResultSet rs = psInsert.executeQuery();
        if (!rs.next()) {return(mOne);}
        rs.beforeFirst();
        while ( rs.next() ) {
        rID = rs.getBigDecimal(1);cID = rs.getBigDecimal(2);value = rs.getBigDecimal(3); 
        }
       }    
       catch (SQLException sqle)
       {
         printSQLException(sqle);
       }
       return(value);
}    
    
    
    
    public void search(Stack lpStack, BigDecimal value, BigDecimal rCounter, BigDecimal cCounter, BigDecimal pFix, currentColumn maxRow, solutionCounter sCounter)  {

    String blank= new String("");
    BigDecimal one = new BigDecimal(1);
    BigDecimal zero= new BigDecimal(0);
    BigDecimal mOne= new BigDecimal(-1);
    BigDecimal mTwo= new BigDecimal(-2);
    BigDecimal mFour=new BigDecimal(-4);
    BigDecimal mFive=new BigDecimal(-5);
    BigDecimal mSix=new BigDecimal(-6);
    BigDecimal columnTest=new BigDecimal(0);
    System.out.println("new search call value+++ " + value);
    
    try
    {  
        s = conn.createStatement();
        statements.add(s);
        System.out.println("search pFix (prefix)" + pFix);
        if (pFix.compareTo(zero)==0) {   System.out.println("pFix=0 condition");
        psInsert = conn.prepareStatement("select * from morequadintervalscopy where result=?",ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
        psInsert.setBigDecimal(1, value);
        } else
        {  System.out.println("zzzzzz");
        psInsert = conn.prepareStatement("select * from morequadintervalscopy where result=? and prefix=?",ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);psInsert.setBigDecimal(1, value);  psInsert.setBigDecimal(2, pFix);  }
        statements.add(psInsert);
        ResultSet rs = psInsert.executeQuery();
        if (!rs.next()) { lpStObj rec=new lpStObj(mOne,blank); lpStack.push(rec); System.out.println("search push mOne");   }
        rs.beforeFirst();
        while ( rs.next() ) {
        BigDecimal results = rs.getBigDecimal(8);BigDecimal key = rs.getBigDecimal(10);
        BigDecimal prefix = rs.getBigDecimal(11);BigDecimal suffix = rs.getBigDecimal(12); 
        lpStObj rec=new lpStObj(results,key.toString());  lpStack.push(rec);   //  after pop this is no longer synchronized with column counter.
        System.out.println("lpStack" + lpStack.toString() + " prefix " + prefix+ " RESLTS " + results + " Key: " + key); System.out.println("Just before search call     rCounter " + rCounter + "cCounter " + cCounter + "maxRow " + maxRow.getCurrent() + " Results:" + results);
        search(lpStack,startLP(lpStack,rCounter,cCounter.add(one),pFix),rCounter,cCounter.add(one),prefix,maxRow,sCounter);
        System.out.println("Search fallThrough " + rCounter + "  " + cCounter + " maxRow " + maxRow.getCurrent()+ " prefix " + prefix); System.out.println("Search fall through lpStack" + lpStack.toString()+ " Results:" + results);
        columnTest=cCounter.add(one);
        System.out.println("CSearch1 condition olumnTest " + columnTest + " maxRow  " + maxRow.getCurrent() + " cCounter " + cCounter);
        if ( (maxRow.getCurrent().compareTo(cCounter.add(one))==0)) {   
        search1(lpStack,startLP(lpStack,cCounter.add(one),one,pFix),cCounter,one,suffix,sCounter);
        System.out.println("Search1 return     rCounter " + rCounter + "cCounter " + cCounter + " maxRow  " + maxRow.getCurrent()+ " prefix " + prefix); System.out.println("lpStack" + lpStack.toString()+ " Results:" + results );
        }
        if (rs.isLast()) {  System.out.println("                                         rs.isLast     " + lpStack.toString());  
        popping(lpStack,cCounter.add(mOne));         //lpStack.pop(); 
        System.out.println("rs.isLast" );
        } else { 
        if (lpStack.size()>0) { 
        if  (((BigDecimal)((lpStObj)lpStack.lastElement()).value).compareTo(mSix)==0) {
         rs.afterLast();
         }
        if  (((BigDecimal)((lpStObj)lpStack.lastElement()).value).compareTo(mFour)==0)
         {
         System.out.println("RS Row number." + rs.getRow());
         rs.afterLast();
         lpStObj recc=new lpStObj(mFive,blank);
         lpStack.push(recc);
         maxRow.decrement();
         System.out.println("Search resultSet clear."); System.out.println(" maxRow  " + maxRow.getCurrent() + " cCounter " + cCounter);
        }
        if  (((BigDecimal)((lpStObj)lpStack.lastElement()).value).compareTo(mTwo)==0)
        {    
           popping(lpStack,cCounter);
           System.out.println("POPPING return     rCounter " + rCounter + "cCounter " + cCounter + " maxRow  " + maxRow.getCurrent()+ " prefix " + prefix);
           maxRow.reset();
         } } } } }
       catch (SQLException sqle) { printSQLException(sqle);  }          
     } 
    
   public void search1(Stack lpStack, BigDecimal value, BigDecimal rCounter, BigDecimal cCounter, BigDecimal pFix,solutionCounter sCounter)  {
                    //  search1(lpStack,startLP(lpStack,cCounter.add(one),one,pFix),cCounter.add(one),one,suffix);
    BigDecimal one = new BigDecimal(1);
    BigDecimal two = new BigDecimal(2);
    BigDecimal zero= new BigDecimal(0);
    BigDecimal mOne= new BigDecimal(-1);
    BigDecimal mTwo= new BigDecimal(-2);
    BigDecimal mFour=new BigDecimal(-4);
    BigDecimal mSix=new BigDecimal(-6);
    BigDecimal columnTest=new BigDecimal(1);
    BigDecimal maxColumn= new BigDecimal(5);
    String blank = "";
 
    try
    {  
        s = conn.createStatement();
        statements.add(s);
        System.out.println("search1 " + pFix  + " " + rCounter + "  " + cCounter );
        if (pFix.compareTo(zero)==0) {
         System.out.println("bbbb");
        psInsert = conn.prepareStatement("select * from morequadintervalscopy where result=?",ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
        psInsert.setBigDecimal(1, value);
        } else
        {  System.out.println("vvvvv");
           psInsert = conn.prepareStatement("select * from morequadintervalscopy where result=? and prefix=?",ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);    
           psInsert.setBigDecimal(1, value);  psInsert.setBigDecimal(2, pFix);  }
        statements.add(psInsert);
        ResultSet rs = psInsert.executeQuery();
        if ((!rs.next()) && (value.compareTo(mOne)==1)) { lpStObj rec=new lpStObj(mTwo,blank); lpStack.push(rec);  rs.beforeFirst();} else
        { rs.beforeFirst(); if (!rs.next()) 
        { 
            lpStObj rec=new lpStObj(mOne,blank);
            lpStack.push(rec);  
            System.out.println("Search1 lpStack no rs.next push mOne" + lpStack.toString());
            columnTest=cCounter.add(one);
            if (  columnTest.compareTo(maxColumn.add(one))==-1) { 
                System.out.println("search1 columnTest compare");
                search1(lpStack,startLP(lpStack,rCounter,cCounter.add(one),pFix),rCounter,cCounter.add(one),pFix,sCounter);
                } else {
                lpStObj recc=new lpStObj(mFour,blank);
                lpStack.push(recc);
                System.out.println("Search1 lpStack push mFour" + lpStack.toString());
            }
        }   
        }    
        rs.beforeFirst();       
        while ( rs.next() ) {               //    use the index to select just the values from the rs i want
        BigDecimal results = rs.getBigDecimal(8);BigDecimal key = rs.getBigDecimal(10);
        BigDecimal prefix = rs.getBigDecimal(11);BigDecimal suffix = rs.getBigDecimal(12); 
        lpStObj rec=new lpStObj(results,key.toString());
        lpStack.push(rec);
        System.out.println("Search1 lpStack rs.next" + lpStack.toString());
        columnTest=cCounter.add(one);
        if (  columnTest.compareTo(maxColumn)==-1) { 
        System.out.println("search1 columnTest compare");    
        search1(lpStack,startLP(lpStack,rCounter,cCounter.add(one),pFix),rCounter,cCounter.add(one),prefix,sCounter);
        }
        if  (((BigDecimal)((lpStObj)lpStack.lastElement()).value).compareTo(mSix)==0) {
         rs.afterLast();
        }
        }
       }
       catch (SQLException sqle) { printSQLException(sqle);  }          
    System.out.println("search1 exit..." + rCounter + "  " + cCounter);
    if (  (rCounter.compareTo(two)==0) &&  (((BigDecimal)((lpStObj)lpStack.lastElement()).value).compareTo(mFour)==0) ) { 
    writeSolution(lpStack,sCounter);
    lpStObj rec=new lpStObj(mSix,blank);
    lpStack.push(rec);
    }    
   } 
    
   /**
    * LPThread can be instanciated iterativly for different algebraTable values.
    * the semaphore would communicate agreement with rest of row thread.and,,,
    * 
    */
   
    
    
    public void run(){
         BigDecimal zero = new BigDecimal(0);
         BigDecimal one = new BigDecimal(1);
         BigDecimal rCounter = new BigDecimal(1);
         BigDecimal cCounter = new BigDecimal(1);
         BigDecimal pFix = new BigDecimal(0);
         BigDecimal maxRow= new BigDecimal(5);
         BigDecimal mNintyNine = new BigDecimal(-99);
         currentColumn cColumn= new currentColumn();
         solutionCounter sCounter= new solutionCounter();
         cColumn.setCurrent(maxRow);
         startConn();
         newCopy(); // clear morequadintervalscopy,floatSolution table
         newmorequadintervalsCopy();
         //        setAlgebraTable();     only use when making a particular arethmetic group 
         lpStack.push(mNintyNine);
         while ( ((BigDecimal)lpStack.lastElement()).compareTo(mNintyNine)==0    )  {
         lpStack.clear();
         rCounter=one;cCounter=one;pFix=zero;cColumn.setCurrent(maxRow);
         startLP(lpStack,rCounter,cCounter,pFix);
         search(lpStack,startLP(lpStack,rCounter,cCounter,pFix),rCounter,cCounter,pFix,cColumn,sCounter);      
         System.out.println("run lpStack " + lpStack.toString());
         if  (((BigDecimal)((lpStObj)lpStack.firstElement()).value).compareTo(zero)==1)  {
         lpStack.clear();
         lpStack.push(mNintyNine);
         }    
             
         } 

         
         //   search(Stack lpStack, BigDecimal value, BigDecimal rCounter, BigDecimal cCounter, BigDecimal pFix)
       }
    
    public void newCopy()  {
        try
        {  
            s = conn.createStatement();
            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("delete from morequadintervalscopy");
            statements.add(psInsert);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
         try
        {  
            s = conn.createStatement();
            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("delete from floatsolution");
            statements.add(psInsert);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }   
            try
        {  
            s = conn.createStatement();
            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("delete from solutiontable");
            statements.add(psInsert);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }    
        
    }
    
 public void newmorequadintervalsCopy()  {
  int updateQuery = 0;
     try
        {  
            s = conn.createStatement();
    //        statements.add(s);
            System.out.println("!!!!!!");
            String stringQuery = "INSERT INTO morequadintervalscopy SELECT * FROM morequadintervals ";
                                            //   morequadintervalscopy
            //          psInsert = conn.prepareStatement("select into morequadintervalscopy from morequadintervals ");
  //          statements.add(psInsert);
  //          psInsert.executeUpdate();
            updateQuery = s.executeUpdate(stringQuery);
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }
    }
    
    
    
    
    
    public void setAlgebraTable() {
     BigDecimal rID = new BigDecimal(0);
     BigDecimal cID = new BigDecimal(0);
     BigDecimal value = new BigDecimal(0);
     
     try
        {  
            s = conn.createStatement();
            rID = new BigDecimal(1);
            cID = new BigDecimal(1);
            value = new BigDecimal(37);
            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("insert into algebraTable values(?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, rID);
            psInsert.setBigDecimal(2, cID);
            psInsert.setBigDecimal(3, value);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }     
     
      try
        {  
         //   s = conn.createStatement();
            rID = new BigDecimal(1);
            cID = new BigDecimal(2);
            value = new BigDecimal(34);
            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("insert into algebraTable values(?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, rID);
            psInsert.setBigDecimal(2, cID);
            psInsert.setBigDecimal(3, value);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }   
     
          try
        {  
         //   s = conn.createStatement();
            rID = new BigDecimal(1);
            cID = new BigDecimal(3);
            value = new BigDecimal(32);
            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("insert into algebraTable values(?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, rID);
            psInsert.setBigDecimal(2, cID);
            psInsert.setBigDecimal(3, value);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        
            try
        {  
         //   s = conn.createStatement();
            rID = new BigDecimal(1);
            cID = new BigDecimal(4);
            value = new BigDecimal(30);
            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("insert into algebraTable values(?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, rID);
            psInsert.setBigDecimal(2, cID);
            psInsert.setBigDecimal(3, value);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
     
        try
        {  
         //   s = conn.createStatement();
            rID = new BigDecimal(2);
            cID = new BigDecimal(2);
            value = new BigDecimal(29);
            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("insert into algebraTable values(?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, rID);
            psInsert.setBigDecimal(2, cID);
            psInsert.setBigDecimal(3, value);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        }         
      
            try
        {  
         //   s = conn.createStatement();
            rID = new BigDecimal(2);
            cID = new BigDecimal(3);
            value = new BigDecimal(20);
            statements.add(s);
            System.out.println("!!!!!!");
            psInsert = conn.prepareStatement("insert into algebraTable values(?, ?, ?)");
            statements.add(psInsert);
            psInsert.setBigDecimal(1, rID);
            psInsert.setBigDecimal(2, cID);
            psInsert.setBigDecimal(3, value);
            psInsert.executeUpdate();
            System.out.println("Inserted");
            conn.commit();
            statements.clear();
            System.out.println("Committed the transaction");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
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
