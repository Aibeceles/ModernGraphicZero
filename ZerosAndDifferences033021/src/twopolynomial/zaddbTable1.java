package twopolynomial;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author Aibes
 */

//import LoopLists.GaussBean1;
//import LoopLists.LoopList;
//import fractionintegerset.FractionIntegerDriver;
//import fractionintegerset.FractionIntegerDriverIterate;
//import fractionintegerset.muNumDen;
//import mucorrolationthreaded.*;
import config.DbConfig;
import java.beans.*;
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
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import static twopolynomial.twoTableDBThread1.printSQLException;

/**
 *
 * @author Aibes
 */

public class zaddbTable1 extends Thread {
    private Semaphore tlSem;  
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private String sampleProperty;
    private PropertyChangeSupport propertySupport;
    private String framework = "embedded";
//    private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
//    private String protocol = "jdbc:derby:";
 
//    private String driver = "com.mysql.jdbc.Driver";
//    private String protocol = "jdbc:mysql://localhost:3306/";
    
    private String driver = "org.neo4j:neo4j-jdbc-driver:3.3.0";
    private String protocol = DbConfig.get("neo4j.url");

    
    
    private Connection conn = null;
    ArrayList statements = new ArrayList(); // list of Statements, PreparedStatements
    PreparedStatement psInsert = null;
    PreparedStatement psUpdate = null;
    Statement s = null;
    Statement t = null;
    ResultSet rs = null;    
    
    rowBean dBbean = new rowBean();   // database buffer is a queue of rowBeans
    vertexVector dbbVector = new vertexVector();    
    plvManager mangee = null;
//    FractionIntegerDriver fMangee = null;
//    FractionIntegerDriverIterate fiMangee=null;
    BigDecimal zero=new BigDecimal(0);
    vertexVector cypherList;
  
    private vertexVector tLL=null;
    private BigDecimal NN=null;
    private BigDecimal flatFileRowCounterr=null;
    private BigDecimal nMaxx=null;   //  setters, because of setfields, sould be private???
    private BigDecimal tSeqDB=null;
    private BigDecimal bTermDB=null;
    private BigDecimal targetEvaluateDB=null;
    private int constant;
    private int linear;
    private int square;
    private String vertexDB=null;
    private vertex vertexDBVertex=null;
    private String vertexDegreeDB=null;
    private String vertexScalarDB=null;
    
    
    
    
    
   public synchronized void goCypher()  {
        try
        {     
            s = conn.createStatement();
            String d="d";
            statements.add(s);
            psInsert = conn.prepareStatement("Create (node:Node {vertex: ? } )");
            statements.add(psInsert);
            psInsert.setString(1, "seven");
            System.out.println(psInsert.execute());
            conn.commit();
           System.out.println("Committed the goCypher");
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
           
    }
    
    public zaddbTable1(Semaphore tlSema,vertexVector dbVector,plvManager mang){
       dbbVector=dbVector;
       tlSem=tlSema;
       mangee=mang;
   } 

   
   
//   public zaddbTable1(Semaphore tlSema,List dbVector,LoopsDriver mang){
//       dbbVector=dbVector;
//       tlSem=tlSema;
//       mangee=mang;
//      }   
  
//    public zaddbTable1(Semaphore tlSema,List dbVector,FractionIntegerDriver mang) {
//       dbbVector=dbVector;
//       tlSem=tlSema;
//       fMangee=mang; 
//      }   
    
//      public zaddbTable1(Semaphore tlSema,List dbVector,FractionIntegerDriverIterate mang) {
//       dbbVector=dbVector;
//       tlSem=tlSema;
//       fiMangee=mang; 
//      }   
    
private void runFIDi() {     //fraction integer driver iterate
  //   while ((fiMangee.isAlive()) || (dbbVector.size()>0)) {       
  //          //            System.out.println("                 runFID while loop " + fiMangee.isAlive() + " "+dbbVector.size());
  //           try {
  //              tlSem.acquire();
  //              System.out.println("                       runFid      ");
  //              if (dbbVector.size()>0) {
  //              try {
  //                 System.out.println("MuNumDen:   "  +((BigDecimal)((muNumDen)dbbVector.get(0)).getMu()).toString()+"  "+ ((BigDecimal)((muNumDen)dbbVector.get(0)).getnumerator()).toString()+"   "+((BigDecimal)((muNumDen)dbbVector.get(0)).getDenominator()).toString()+"   "+((String)((muNumDen)dbbVector.get(0)).getBString()));     
  //                 goFidCypher(((BigDecimal)((muNumDen)dbbVector.get(0)).getMu()).toString(),((BigDecimal)((muNumDen)dbbVector.get(0)).getnumerator()).toString(),((BigDecimal)((muNumDen)dbbVector.get(0)).getDenominator()).toString(),((String)((muNumDen)dbbVector.get(0)).getBString()));
  //                 dbbVector.remove(0);
  //              } catch (NullPointerException e) {
  //              System.out.println("dbVector empty"+ e.toString());
  //              dbbVector.remove(0);
  //              }
  //              }
  //              else {System.out.println("if dbbVector.size()>0 else println   " );}
  //          } catch (InterruptedException ex) {
  //              Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
  //          }
  //          //           System.out.println("twotabledbthread dbVectorSize"+ dbbVector.size());
  //         }
        
}
    
    
    
    
private void runFID() {
   //  while ((fMangee.isAlive()) || (dbbVector.size()>0)) {       
   //          System.out.println("                 runFID while loop " + fMangee.isAlive() + " "+dbbVector.size());
   //          try {
   //             tlSem.acquire();
   //             System.out.println("                       runFid      ");
   //             if (dbbVector.size()>0) {
   //             try {
   //                System.out.println("MuNumDen:   "  +((BigDecimal)((muNumDen)dbbVector.get(0)).getMu()).toString()+"  "+ ((BigDecimal)((muNumDen)dbbVector.get(0)).getnumerator()).toString()+"   "+((BigDecimal)((muNumDen)dbbVector.get(0)).getDenominator()).toString());     
   //                goFidCypher(((BigDecimal)((muNumDen)dbbVector.get(0)).getMu()).toString(),((BigDecimal)((muNumDen)dbbVector.get(0)).getnumerator()).toString(),((BigDecimal)((muNumDen)dbbVector.get(0)).getDenominator()).toString(),((String)((muNumDen)dbbVector.get(0)).getBString()));
   //                //               go(((GaussBean1)dbbVector.get(0)).getpArray(),((GaussBean1)dbbVector.get(0)).getLoopList(),((GaussBean1)dbbVector.get(0)).getWNum(),((GaussBean1)dbbVector.get(0)).getVmResult(),((GaussBean1)dbbVector.get(0)).getresultlistid());
   //             dbbVector.remove(0);
   //             } catch (NullPointerException e) {
   //             System.out.println("                                                                                   dbVector empty"+ e.toString());
   //             dbbVector.remove(0);
   //             }
   //             }
   //             else {System.out.println("if dbbVector.size()>0 else println   " );}
   //          //                dbbVector.removeElementAt(0);
   //         } catch (InterruptedException ex) {
   //             Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
   //         }
   //              //           System.out.println("twotabledbthread dbVectorSize"+ dbbVector.size());
   //        }
        
}






private void runLoopsDriver() {
 
}    
    
private void runTwoPolyDriver() {
    
    
    System.out.println("zaddbTable1.runLoopsDriver.dbbVector.size(): "+dbbVector.size());
    System.out.println("zaddbTable1.runLoopsDriver.t1.getPermits: "+tlSem.availablePermits());
    while ((mangee.isAlive()) || (dbbVector.size()>0)) {       
 //            System.out.println("                                                GaussCorrolation while loop " + mangee.isAlive() + " "+dbbVector.size());
             try {
                tlSem.acquire();
  //              System.out.println("                       gauss6able      ");
                if (dbbVector.size()>0) {
                try {
                 //System.out.println("rowBean:   " + ((rowBean)dbbVector.get(0)).toString());     
                  this.NN=((rowBean)dbbVector.get(0)).getNN();
                  this.flatFileRowCounterr=((rowBean)dbbVector.get(0)).getflatFileRowCounterr();
                  this.nMaxx=((rowBean)dbbVector.get(0)).getNMax();
                  this.tSeqDB=((rowBean)dbbVector.get(0)).gettSeqDB();
                  this.bTermDB=((rowBean)dbbVector.get(0)).getbTermDB();
 //                 this.vertexDB=((rowBean)dbbVector.get(0)).getvertex().toString();
                  this.vertexDBVertex=((rowBean)dbbVector.get(0)).getvertex();
                  this.vertexDegreeDB=this.vertexDBVertex.getDegree().toString();
                  this.vertexScalarDB=this.vertexDBVertex.getScalar().toString();
                  this.vertexDB=this.vertexDBVertex.toString();
      System.out.println(".NN,  .flatFileRowCounterr,  .nMaxx,  .tSeqDB, .bTermDB, .vertexDB  : "+ this.NN.toString() + " "+this.flatFileRowCounterr.toString() + " "+this.nMaxx.toString() + " "+this.tSeqDB.toString() + " "+this.bTermDB.toString() + "               "+this.vertexDB.toString());  
      
      

                  
                 mergeIndexNode();       //works 9/19
                  matchIndexedBy();       
                  mergetSeqNode();                
                  matchTwoSeqFactor();    
                  mergeVertex();          
                  matchVertex();              
  
                  createTwoSeqValue();
                  matchTwoFactor();

                  createIndexedByEvaluate();
                  matchIndexedByEvaluate();

                  createVertexIndexedBy();
                  matchVertexIndexedBy();

                  dbbVector.remove(0);
                } catch (NullPointerException e) {
                System.out.println("                                                                                   dbVector empty"+ e.toString());
                dbbVector.remove(0);
                }
                }
                else {System.out.println("if dbbVector.size()>0 else println   " );}
//                dbbVector.removeElementAt(0);
            } catch (InterruptedException ex) {
                Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
            }
 //           System.out.println("twotabledbthread dbVectorSize"+ dbbVector.size());
           }
                    //  arithmeticCypher();
    
}    

private void arithmeticCypher()  {
    
     //     String n;
  //   String rowCounter;
  //     String maxN;
//     BigDecimal n = new BigDecimal(0);
//     BigDecimal resultsInteger = new BigDecimal(0);
     try
     {  
        s = conn.createStatement();
        statements.add(s);
            System.out.println(".matchArithmeticCypher");
            psInsert = conn.prepareStatement("match (v:VertexNode)<-[]-(i:IndexedBy)-[]->(e:Evaluate),(t:TwoSeqFactor)<-[]-(i) where i.N=? or i.N=?\n" +
"WITH {TwoSeq:COLLECT({TwoSeq:t.twoSeq}),Vertex:v.Vertex} as factors,v,i,e\n" +
"With Collect(e.Value) as Values,factors,v,i,e\n" +
"With collect(v.Vertex) as vV,factors,v,i,Values,e\n" +
"WITH collect(factors.TwoSeq) as fact,v,i.N as N,vV,Values,e\n" +
"Unwind RANGE(0,SIZE(fact)-1) as ii\n" +
"Unwind fact[ii] as ts\n" +
"With collect(ts.TwoSeq) as cts,v,N,vV,Values,e\n" +
"With REDUCE(tSum=0,ff IN cts | tSum +ff) As TotalTseq,v,N,vV,e,Values\n" +
"With v,N,vV,e,Values, Case\n" +
"when e.Value=\"4\" Then 0-TotalTseq Else TotalTseq End as TotalTseq\n" +
"With [v.Vertex,TotalTseq,N] as output,v,N,TotalTseq,vV //,e\n" +
"With {vVertex:v.Vertex,TotalT:TotalTseq,N:N} as outputMap,output,vV//,e\n" +
"with collect(output) as outputlist,outputMap,output,vV//,e\n" +
"with DISTINCT vV as vVV, outputlist,outputMap,output//,e\n" +
"with collect(outputMap.TotalT) as opMTT,vVV//,e\n" +
"With REDUCE(tSum=0,ff IN opMTT | tSum +ff) As TotalTseqq,opMTT,vVV//,e\n" +
"With Collect(TotalTseqq) as result,vVV    // but e down here dosnt collapse map.\n" +
"//Unwind RANGE(0,SIZE(output)-1) as iii\n" +
"//Unwind output[iii] as ts\n" +
"Return {Multiple:result,Vertex:vVV} // ,e.Value\n" +
"//Return opMTT,vVV\n" +
"//Return outputMap");
            statements.add(psInsert);
            psInsert.setString(1,"3" );
            psInsert.setString(2, "2");
 //           psInsert.setString(3, "4");
            ResultSet rs = psInsert.executeQuery();
            ResultSetMetaData rsmd =rs.getMetaData();
            System.out.println(rsmd.getColumnCount() + " " +rsmd.getColumnName(1));//+ " " +rsmd.getColumnName(2));
            //        ((Map)rs.getObject(1)).toString();
            Map rsMap = new HashMap();
            Set keySett= new HashSet();
            ArrayList keyList = new ArrayList();
            while ( rs.next() ) {
//            String n = rs.getString(1);    
//            String rowCounter = rs.getString(2); 
//            String maxN = rs.getString(3);
            rsMap=((Map)rs.getObject(1));
            keySett=((Map)rs.getObject(1)).keySet();
            System.out.println("(Map)rs.toString: " + ((Map)rs.getObject(1)).keySet());
            keySett=((Map)rs.getObject(1)).keySet();
            Iterator iterator=keySett.iterator();
            while(iterator.hasNext())  {
                String element = (String) iterator.next();
                System.out.println("KeySet: "+ element   );
                System.out.println("(Map)rs.toString: " + rsMap.get(element));
            }

            
                  this.NN=((rowBean)dbbVector.get(0)).getNN();
                  this.flatFileRowCounterr=((rowBean)dbbVector.get(0)).getflatFileRowCounterr();
                  this.nMaxx=((rowBean)dbbVector.get(0)).getNMax();
                  this.tSeqDB=((rowBean)dbbVector.get(0)).gettSeqDB();
                  this.bTermDB=((rowBean)dbbVector.get(0)).getbTermDB();
                  this.vertexDB=((rowBean)dbbVector.get(0)).getvertex().toString();

            
            
            

            System.out.println("KeySet: "+ keySett.toString()   );
            //System.out.println("(KeySet: " + keySett.);
            //       System.out.println("(Map)rs.toString: " + ((Map)rs.getObject(1)).);
            //       ((HashMap)rs.getObject(1))
            }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        System.out.println(); 
    
    
}










    
    public void run(){
      startConn();
      System.out.println("                                                                                         It is inside GaussCorrolation? ");
      try {
        Thread.sleep(7000);
      System.out.println("                                     Slept? ");
      } catch (InterruptedException ex) {
        Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
      }
       if (mangee!=null) {
        runTwoPolyDriver();          
       } else {
       //  if (fMangee!=null  )  runFID(); else {runFIDi();}
       }
       System.out.println("twotabledbthread ended");
       endConn();      //  maybe endconn is throwing excejptions  
       } 






    
    
    
    
    
//        public void run(){
//         startConn();
//         System.out.println("                                                                                         It is inside GaussCorrolation? ");
//goCypher();
////buildPoly();
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex);
//       }
//          while ((mangee.isAlive()) || (dbbVector.size()>0)) {       
////             System.out.println("                                                GaussCorrolation while loop " + mangee.isAlive() + " "+dbbVector.size());
//             try {
//                tlSem.acquire();
//
////                System.out.println( ((GaussBean1)dbbVector.get(0)).getLoopList()+" "+((GaussBean1)dbbVector.get(0)).getVmResult()+" "+((GaussBean1)dbbVector.get(0)).getpArray());                                                    // go(double y1,double y2,double y3,BigDecimal trZer,BigDecimal trTr,BigDecimal root1,BigDecimal root2,BigDecimal root3)               
//
////                System.out.println("                       gauss6able      ");
//               if (dbbVector.size()>0) {
// try {
// //                  System.out.println("GaussBean1:   " + ((GaussBean1)dbbVector.get(0)).toString());     
// //                System.out.println("pArray:   " + ((GaussBean1)dbbVector.get(0)).getpArray());               
// //                System.out.println("LoopList:   " + ((GaussBean1)dbbVector.get(0)).getLoopList());
// //                System.out.println("wNum:   " + ((GaussBean1)dbbVector.get(0)).getWNum());
// //                System.out.println("VmResult:   " + ((GaussBean1)dbbVector.get(0)).getVmResult());
// //                System.out.println("resultListId:   " + ((GaussBean1)dbbVector.get(0)).getresultlistid());
// //               ((GaussBean1)dbbVector.get(0)).getLoopList().getworkNum();((GaussBean1)dbbVector.get(0)).getLoopList().getVmResult();
//        if (((GaussBean1)dbbVector.get(0)).getLoopList().getworkNum()==0) {cypherList=((GaussBean1)dbbVector.get(0)).getLoopList(); }
//         goCCypher(((GaussBean1)dbbVector.get(0)).getfigPArray(),((GaussBean1)dbbVector.get(0)).getpArray(),((GaussBean1)dbbVector.get(0)).getLoopList(),((GaussBean1)dbbVector.get(0)).getLoopList().getworkNum(),((GaussBean1)dbbVector.get(0)).getLoopList().getVmResult(),((GaussBean1)dbbVector.get(0)).getresultlistid());
//
////        go(((GaussBean1)dbbVector.get(0)).getfigPArray(),((GaussBean1)dbbVector.get(0)).getpArray(),((GaussBean1)dbbVector.get(0)).getLoopList(),((GaussBean1)dbbVector.get(0)).getLoopList().getworkNum(),((GaussBean1)dbbVector.get(0)).getLoopList().getVmResult(),((GaussBean1)dbbVector.get(0)).getresultlistid());
////                tlToTable(((rowBean)dbbVector.elementAt(0)).getTL(),((rowBean)dbbVector.elementAt(0)).getNN(),((rowBean)dbbVector.elementAt(0)).getFlatFileRowCounter(),((rowBean)dbbVector.elementAt(0)).getNMax());
//                dbbVector.remove(0);
// } catch (NullPointerException e) {
//            System.out.println("                                                                                   dbVector empty"+ e.toString());
//            dbbVector.remove(0);
//            
//        }
// 
// 
// }
//                else {System.out.println("if dbbVector.size()>0 else println   " );}
////                dbbVector.removeElementAt(0);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
//            }
// //           System.out.println("twotabledbthread dbVectorSize"+ dbbVector.size());
//           }
//           System.out.println("twotabledbthread ended");
// //          System.out.println("TwoTableDBThread while loop" + mangee.isAlive());
//          //  new method for polynomials 
//        //   buildPoly();
//      endConn();      //  maybe endconn is throwing excejptions  
////    startConn();
////    buildPoly();
////    endConn();
//      //     eDB.go();
//       }   
    
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
            psInsert = conn.prepareStatement("MATCH (n:Node) RETURN n");
            statements.add(psInsert);
 

            ResultSet rs = psInsert.executeQuery();
            
            while ( rs.next() ) {
            String num = rs.getString(1);    
      //      Double argument = rs.getDouble(2);
      //      BigDecimal scalar = rs.getBigDecimal(3); 
      //      BigDecimal degree = rs.getBigDecimal(4); 
     //       BigDecimal rowcounter = rs.getBigDecimal(5); 
 
            System.out.println(num);
      //      System.out.println(argument);
     //       System.out.println(scalar);
      //      System.out.println(degree); 
     //       System.out.println(rowcounter);
            
            }
          //  System.out.println("Inserted");
          //  conn.commit();
          //  System.out.println("Committed the transaction");
  //       s.execute("CALL SYSCS_UTIL.SYSCS_EXPORT_TABLE (null,'rootlist','rootlist.txt',null,null,null)");
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
           Properties props = new Properties();
           String dbName = DbConfig.get("neo4j.database");

             conn = DriverManager.getConnection(protocol, DbConfig.get("neo4j.user"), DbConfig.get("neo4j.password"));
             System.out.println(conn.getMetaData().getURL());
     
     
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

//  Sub methods implementing cypher querries:
// 
  

private void mergeVmResult(String vmResult) {
  
        try {
            s = conn.createStatement();
            statements.add(s);
            psInsert = conn.prepareStatement("MERGE (d:Dnode {vmResult:?}) ");
            statements.add(psInsert);
            psInsert.setString(1, vmResult);
 //           psInsert.setString(2, pArray);
            psInsert.executeUpdate();
            conn.commit();
        } catch (SQLException sqle) { printSQLException(sqle); }
//        try {rs.close(); } catch (SQLException ex) {
//            Logger.getLogger(GaussTable1.class.getName()).log(Level.SEVERE, null, ex);
//        }
}

    public synchronized void matchIndexedBy()
    {
  //     String n;
  //   String rowCounter;
  //     String maxN;
//     BigDecimal n = new BigDecimal(0);
//     BigDecimal resultsInteger = new BigDecimal(0);
     try
     {  
        s = conn.createStatement();
        statements.add(s);
            System.out.println(".matchIndexedBy");
            psInsert = conn.prepareStatement("match (i:IndexedBy {N:?, RowCounter:?,MaxN:? }) RETURN i.N, i.RowCounter, i.MaxN");
            statements.add(psInsert);
            psInsert.setString(1, this.NN.toString());
            psInsert.setString(2, this.flatFileRowCounterr.toString());
            psInsert.setString(3, this.nMaxx.toString());
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {
            String n = rs.getString(1);    
            String rowCounter = rs.getString(2); 
            String maxN = rs.getString(3); 
            System.out.println(".matchIndexedBy n,rowCounter,maxN: " + n  + " "+ rowCounter + " " + maxN);
            }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        System.out.println();   
    }

private synchronized void matchVertexIndexedBy()
    {
     try
     {  
        s = conn.createStatement();
        statements.add(s);             
            System.out.println(".matchVertex");  //  
            psInsert = conn.prepareStatement("MATCH  p= (i:IndexedBy {N:? , RowCounter:?, MaxN:?}  )-[]-> (v:VertexNode {Vertex:?}) RETURN NODES(p)");
            statements.add(psInsert);
            psInsert.setString(1, this.NN.toString());
            psInsert.setString(2, this.flatFileRowCounterr.toString());
            psInsert.setString(3, this.nMaxx.toString());
            psInsert.setString(4, this.vertexDB.toString());
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {
            Object vertex = rs.getObject(1);  

            System.out.println(".matchVertexIndexedBy: " + vertex.toString());
            }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        System.out.println();   
    }
    
    
    
    
private synchronized void matchIndexedByEvaluate()
    {
     try
     {  
        s = conn.createStatement();
        statements.add(s);             
            System.out.println(".matchVertex");  //  
            psInsert = conn.prepareStatement("MATCH  p=(i:IndexedBy {N:?, RowCounter:?,MaxN:? })-[]->(e:Evaluate {Value:?}) Return nodes(p) ");
            statements.add(psInsert);
            psInsert.setString(1, this.NN.toString());
            psInsert.setString(2, this.flatFileRowCounterr.toString());
            psInsert.setString(3, this.nMaxx.toString());
            psInsert.setString(4, targetEvaluateDB.toString());
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {
            Object vertex = rs.getObject(1);  

            System.out.println(".matchIndexEvaluate: " + vertex.toString());
            }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        System.out.println();   
    }
    
    
    
private synchronized void matchTwoFactor()
    {
     try
     {  
        s = conn.createStatement();
        statements.add(s);                       
            System.out.println(".matchVertex");  
            psInsert = conn.prepareStatement("MATCH  p=(v:VertexNode {Vertex:?})-[]->(s:TwoSeqFactor {twoSeq:?}) Return nodes(p) ");
            statements.add(psInsert);
            psInsert.setString(1, this.vertexDB.toString());
            psInsert.setString(2, this.tSeqDB.toString());
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {
            Object vertex = rs.getObject(1);  

            System.out.println(".matchTwoFactor String: " + vertex.toString());
            }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        System.out.println();   
    }
    
    
    
    private synchronized void matchVertex()
    {
     try
     {  
        s = conn.createStatement();
        statements.add(s);
            System.out.println(".matchVertex");
            psInsert = conn.prepareStatement("match (v:VertexNode {Vertex:?}) RETURN v.Vertex");
            statements.add(psInsert);
            psInsert.setString(1, this.vertexDB);
            ResultSet rs = psInsert.executeQuery();
            while ( rs.next() ) {
            String vertex = rs.getString(1);    
            System.out.println(".matchVertex Vertex: " + vertex);
            }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        System.out.println();   
    }
    


public synchronized void matchTwoSeqFactor()
    {
     try
     {  
        s = conn.createStatement();
        statements.add(s);
            System.out.println(".matchTwoSeqFactor");
            psInsert = conn.prepareStatement("match (t:TwoSeqFactor {twoSeq:?}) RETURN t.twoSeq");
            statements.add(psInsert);
            psInsert.setString(1, this.tSeqDB.toString());
            ResultSet rs = psInsert.executeQuery();
            
            while ( rs.next() ) {
            String twoSeq = rs.getString(1);    
            System.out.println(".matchTwoSeqFactor twoSeq: " + twoSeq);
            }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
        System.out.println();   
    }




private void mergeVertex() {
 try {
            s = conn.createStatement();
            statements.add(s);
//            psInsert = conn.prepareStatement("MERGE (v:VertexNode {Vertex:?} )");
            psInsert = conn.prepareStatement("MERGE (v:VertexNode {Vertex:?,Scalar:?,Degree:?} )");
            statements.add(psInsert);
            psInsert.setString(1,this.vertexDB);
            psInsert.setString(2,this.vertexScalarDB);
            psInsert.setString(3,this.vertexDegreeDB);
            psInsert.executeUpdate();
            conn.commit();   
      }  catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
      }   
    
}


private void mergetSeqNode() {
    long tSeqDBlong = tSeqDB.longValue();
      try {
            s = conn.createStatement();
            statements.add(s);
            psInsert = conn.prepareStatement("MERGE (d:TwoSeqFactor {twoSeq:?} )");
            statements.add(psInsert);
            psInsert.setLong(1,tSeqDBlong);
            psInsert.executeUpdate();
            conn.commit();   
      }  catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
      }     
}

private void mergeIndexNode()   {    //  could be an overloaded creatWnumRcounter 
      try {
            s = conn.createStatement();
            statements.add(s);
            psInsert = conn.prepareStatement("MERGE (d:IndexedBy {N:?, RowCounter:?,MaxN:?} )");
            statements.add(psInsert);
            psInsert.setString(1,NN.toString());
            psInsert.setString(2, flatFileRowCounterr.toString());
            psInsert.setString(3,nMaxx.toString());
            psInsert.executeUpdate();
            conn.commit();   
      }  catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
      }  
    
}

private void createWnumRcounter(int wNum,int rCounter,String pArray) {
      try {
            s = conn.createStatement();
            statements.add(s);
            psInsert = conn.prepareStatement("CREATE (d:CreatedBy {resultId:?, wNum:?,pArray:?} )");
            statements.add(psInsert);
            psInsert.setInt(1, rCounter);
            psInsert.setInt(2, wNum);
            psInsert.setString(3,pArray);
            psInsert.executeUpdate();
            conn.commit();   
      }  catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
      }
}

private void createKnownByRelation(String vmResult,int wNum, int rCounter)   {
       try {   
            s = conn.createStatement();
            statements.add(s);
            psInsert = conn.prepareStatement(" MATCH (d:Dnode), (k:CreatedBy) WHERE d.vmResult=? AND k.resultId=? AND k.wNum=? WITH d,k MERGE  (d)-[ee:CreatedBye]->(k)");
            statements.add(psInsert);
            psInsert.setString(1, vmResult);
            psInsert.setInt(2, rCounter);
            psInsert.setInt(3, wNum);
            psInsert.executeUpdate();
            conn.commit();    
        }  catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
        }
}

private void createEvaluates(vertexVector loopList,String vmResult) {
    BigDecimal bD= new BigDecimal(0);
    Double bDD;
    System.out.println("     !!!! createEvaluates      !!!!!");
    try {
            int lSize= loopList.size();
            bD=(BigDecimal)loopList.get(0);  //  just need a bigdecimal definedas zero
            bDD=bD.doubleValue();            //  maybe these two lines are unnecessary

            for (Integer x=0;x<lSize;x++){
            System.out.print(" "+((BigDecimal)cypherList.get(x)).toString());
            if (((BigDecimal)cypherList.get(x)).compareTo(zero)==0) { 
            String X=x.toString();
            X=X.substring(0, 1);
            System.out.print (" X:" +x+ ": ");
            System.out.print(bDD);
            s = conn.createStatement();
            statements.add(s);
            psInsert = conn.prepareStatement(" MATCH (e:Evaluate), (d:Dnode) WHERE e.Value = ? AND d.vmResult=? WITH d,e MERGE  (d)-[ee:Envalues]->(e)");
            statements.add(psInsert);
            psInsert.setString(1, X);
            psInsert.setString(2, vmResult);
            psInsert.executeUpdate();
            conn.commit();
            }
          }
        } catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
    System.out.println();
} 

private void createIndexedByEvaluate() {

    //   Need to compute the target evaluate for cypher clause.
    BigDecimal targetEvaluate = new BigDecimal(1);
    BigDecimal dCounter = new BigDecimal(0);
    BigDecimal two = new BigDecimal(2);
    BigDecimal one = new BigDecimal(1);
    this.targetEvaluateDB=one;
   while (dCounter.compareTo(this.NN) == -1)  {
     targetEvaluate=targetEvaluate.multiply(two);
     dCounter=dCounter.add(one);
   }
   this.targetEvaluateDB=targetEvaluate;
   System.out.println(".createTwoPowerEvaluate: "+targetEvaluate.toString());
    try {   
            s = conn.createStatement();
            statements.add(s);               
            psInsert = conn.prepareStatement("MATCH (i:IndexedBy {N:?, RowCounter:?,MaxN:? }),(e:Evaluate {Value:?}) WITH i,e MERGE (i) -[:IndexedByEvaluate]->(e)");
            statements.add(psInsert);
            psInsert.setString(1, this.NN.toString());
            psInsert.setString(2, this.flatFileRowCounterr.toString());
            psInsert.setString(3, this.nMaxx.toString());
            psInsert.setString(4, targetEvaluate.toString());
            psInsert.executeUpdate();
            conn.commit();    
        }  catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
        }   
    
}

private void createVertexIndexedBy() {

    if (this.bTermDB.compareTo(zero)==1) {
    try {   
            s = conn.createStatement();       
            statements.add(s);                 
            psInsert = conn.prepareStatement(" MATCH (i:IndexedBy {N:?,  RowCounter:?, MaxN:?}), (v:VertexNode {Vertex:?}) WITH i,v MERGE  (i)-[ee:VertexIndexedBy]->(v)");
            statements.add(psInsert);
            psInsert.setString(1, this.NN.toString());
            psInsert.setString(2, this.flatFileRowCounterr.toString());
            psInsert.setString(3, this.nMaxx.toString());
            psInsert.setString(4, this.vertexDB.toString());
            psInsert.executeUpdate();
            conn.commit();  
            
        }  catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}


private void createTwoSeqValue() {
    long tSeqDBlong = tSeqDB.longValue();
    if (this.bTermDB.compareTo(zero)==1) {
    try {   
            s = conn.createStatement();
            statements.add(s);                           
            psInsert = conn.prepareStatement(" MATCH (i:IndexedBy {N:?,  RowCounter:?, MaxN:?}), (s:TwoSeqFactor {twoSeq:?}) WITH i,s MERGE  (i)-[ee:TwoFactor]->(s)");
            statements.add(psInsert);
            psInsert.setString(1, this.NN.toString());
            psInsert.setString(2, this.flatFileRowCounterr.toString());
            psInsert.setString(3, this.nMaxx.toString());
            psInsert.setLong(4, tSeqDBlong);
            psInsert.executeUpdate();
            conn.commit();    
        }  catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}

private void createZMap(String vmResult,int rCounter,int wNum)   {
        try {
            s = conn.createStatement();
            statements.add(s);
// System.out.println("gausstable1.createZmap() wNm,vmResult,rCounter "+" "+wNum+" "+vmResult+" "+rCounter); 
//         psInsert = conn.prepareStatement(" MATCH (d:Dnode), (c:CreatedBy) WHERE d.vmResult=? AND c.resultID=?  AND c.wNum=?  WITH d,e CREATE  (d)-[ee:zMap]->(e)");
            psInsert = conn.prepareStatement("match  (d:Dnode)-[:CreatedBye]->(c:CreatedBy {resultId:? , wNum:?}) , (dd:Dnode)-[:CreatedBye]->(cc:CreatedBy {resultId:? , wNum:?}) MERGE (d)-[:zMap]->(dd) ");
            statements.add(psInsert);
            psInsert.setInt(1, rCounter);
            psInsert.setInt(2, wNum);
            psInsert.setInt(3, rCounter);
            psInsert.setInt(4, wNum-1);
            psInsert.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
        }
   
}

public synchronized void goCCypher(String figPArray,String pArray,vertexVector loopList,int wNum,String vmResult,int rCounter)  {
    BigDecimal bD= new BigDecimal(0);
    Double bDD;
    int index;
    System.out.println("goCCypher() figPArray,pArray,wNm,vmResult,rCounter "+figPArray+" "+pArray+" "+wNum+" "+vmResult+" "+rCounter);
    System.out.println("goCCypher() "+loopList.toString());
    mergeVmResult(vmResult);
            createWnumRcounter(wNum,rCounter,pArray);
            createKnownByRelation(vmResult,wNum,rCounter);

            if (wNum==0) {
               createEvaluates(loopList,vmResult);
                
            }
            if (wNum>0)
            {
              createZMap(vmResult,rCounter,wNum);
            }
    }

private synchronized void mergeEvaluate(String mU, String numerator, String denominator,String nBinary){
 
        try {
            s = conn.createStatement();
            statements.add(s);
            psInsert = conn.prepareStatement("MERGE (e:Evaluate {Value:?}) SET e.n=?,e.d=?,e.nBinary=?");
  //          psInsert = conn.prepareStatement("create (e:Evaluate {Value:?,n:?,d:?,nBinary:?})");
            statements.add(psInsert);
            psInsert.setString(1, mU);
            psInsert.setString(2, numerator);
            psInsert.setString(3, denominator);
            psInsert.setString(4, nBinary);
            psInsert.executeUpdate();
            conn.commit();   
      }  catch (SQLException ex) {
            Logger.getLogger(zaddbTable1.class.getName()).log(Level.SEVERE, null, ex);
      } 
}   



public synchronized void goFidCypher(String mU, String numerator, String denominator,String nBinary)  {

   mergeEvaluate( mU, numerator,denominator,nBinary);
   
}




public synchronized void go(String figPArray,String pArray,vertexVector loopList,int wNum,String vmResult,int rCounter)  {
    BigDecimal bD= new BigDecimal(0);
    Double bDD;
    
    int index;
    System.out.println("gausstable1.go() figPArray,pArray,wNm,vmResult,rCounter "+figPArray+" "+pArray+" "+wNum+" "+vmResult+" "+rCounter);
        try
        {     
            s = conn.createStatement();
            statements.add(s);
            System.out.println("gaussiterationtable!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+ bD.toString());
            System.out.println(pArray.toString());
            psInsert = conn.prepareStatement("CREATE (d:Dnode {Polynomial:? , vmResult:? , resultID:?, wNum:?} )");
            statements.add(psInsert);
            System.out.println("gaussiterationtable!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+ bD.toString());
            psInsert.setString(1, figPArray);
            psInsert.setString(2, vmResult);
            psInsert.setInt(3, rCounter);
            psInsert.setInt(4, wNum);
            psInsert.executeUpdate();
            conn.commit();
            if (wNum==0) {
            int lSize= loopList.size();
            System.out.println("gaussiterationtable    wNum=1!!!!!   lSize: "+ lSize + " " );
            bD=(BigDecimal)loopList.get(0);
            bDD=bD.doubleValue(); 
            for (int x=0;x<lSize;x++){
            System.out.println("gaussiterationtable.go() loop cypherList.get(x): "+ ((BigDecimal)cypherList.get(x)) );    
            if (((BigDecimal)cypherList.get(x)).compareTo(zero)==0) {    
            s = conn.createStatement();
            statements.add(s);
            System.out.println("gaussiterationtable!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+ bD.toString());
            System.out.println(pArray.toString());
            psInsert = conn.prepareStatement(" MATCH (e:Evaluate), (d:Dnode) WHERE e.Value = ? AND d.Polynomial=? AND d.vmResult=? AND d.resultID=? WITH d,e MERGE  (d)-[ee:Envalues]->(e)");
                                     // psInsert = conn.prepareStatement("CREATE (d:Dnode {Polynomial:? vmResult:? , resultID:?} ) RETURN d");
            statements.add(psInsert);
            System.out.println("gaussiterationtable!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+ bD.toString());
            psInsert.setInt(1, x);
            psInsert.setString(2, figPArray);
            psInsert.setString(3, vmResult);
            psInsert.setInt(4, rCounter);
            psInsert.executeUpdate();
            conn.commit();
            }
            }
            }
            if (wNum>0)
            {
            s = conn.createStatement();
            statements.add(s);
            System.out.println("gaussiterationtable!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+ bD.toString());
            System.out.println(pArray.toString());
            psInsert = conn.prepareStatement(" MATCH (e:Dnode), (d:Dnode) WHERE e.Polynomial=? AND e.wNum=? AND e.resultID=?  AND d.Polynomial=? AND d.wNum=? AND d.resultID=? WITH d,e MERGE  (d)-[ee:zMap]->(e)");
       // psInsert = conn.prepareStatement("CREATE (d:Dnode {Polynomial:? vmResult:? , resultID:?} ) RETURN d");
            statements.add(psInsert);
            System.out.println("gaussiterationtable!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+ bD.toString());
            psInsert.setString(1, figPArray);
            psInsert.setInt(2, wNum);
            psInsert.setInt(3, rCounter);
            psInsert.setString(4, figPArray);
            psInsert.setInt(5, wNum-1);
            psInsert.setInt(6, rCounter);
            psInsert.executeUpdate();
            conn.commit();
            }
        }
         catch (SQLException sqle)
        {
            printSQLException(sqle);
        } 
//    }
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




