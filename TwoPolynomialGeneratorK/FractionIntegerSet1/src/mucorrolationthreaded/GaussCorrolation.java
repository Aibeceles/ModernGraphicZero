/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mucorrolationthreaded;

import SqlErata.GaussCorrSql;
import SqlErata.GCSqltrZerotrTriang;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author Aibes
 */
public class GaussCorrolation extends Thread {

ExecutorService executorService1 = Executors.newFixedThreadPool(10);    
CompletionService<String> service = new ExecutorCompletionService<String>(executorService1);    
Semaphore gaSemap = new Semaphore(1);
List gaussList = new ArrayList();
private BigDecimal trZero = new BigDecimal(1);
 private BigDecimal trTriang = new BigDecimal(2);
 private BigDecimal root1 = new BigDecimal(19);
 private BigDecimal root2 = new BigDecimal(19);
 private BigDecimal root3 = new BigDecimal(19);
 private BigDecimal one = new BigDecimal(19);
  private BigDecimal onee = new BigDecimal(1);
  private BigDecimal twoo = new BigDecimal(2);
  private BigDecimal zero = new BigDecimal(0);
  GaussCorrSql gCSql = new GaussCorrSql();
  GCSqltrZerotrTriang gCtztt =new GCSqltrZerotrTriang();
  
  
public GaussCorrolation(Semaphore t1Sem, List gaussLi) {
gaSemap=t1Sem;
gaussList=gaussLi;
}    
    
public void run()  {
   gCSql.startConn();
   gCtztt.startConn();
  
   
   
    for (int a=2; a<=4; a++)            
    {
      trZero=onee;
      for (int b=1; b<=a; b++)
      {       
        ResultSet rs1=gCtztt.buildPoly(trZero, trTriang);
            
          try {
              //     for (int r1=1; r1<=131; r1++)
              while(rs1.next())
              {
                  BigDecimal rootone = rs1.getBigDecimal(4);
                  ResultSet rs2=gCtztt.buildPoly(trZero, trTriang);
                  while(rs2.next())
                //  for (int r2=1; r2<=131; r2++)
                  {
                      BigDecimal roottwo = rs2.getBigDecimal(4);
                       ResultSet rs3=gCtztt.buildPoly(trZero, trTriang);
                       while(rs3.next())
                    //  for (int r3=1; r3<=130; r3++)
                      {
                          BigDecimal rootthree = rs3.getBigDecimal(4); 
              //            System.out.println("GaussCorrolation rs1,2,3 parameters"+rs1.);
            if ((rootthree.compareTo(roottwo)!=0) && (rootthree.compareTo(rootone)!=0)&& (rootone.compareTo(roottwo)!=0)) {
                          System.out.println("GaussCorrolation worker parameters"+" trZero: "+trZero.toString()+" trTriang: "+trTriang.toString()+" root1: "+rootone.toString()+" root2: "+roottwo.toString()+" root3: "+rootthree.toString());
                          Callable worker = new twoTableDBThread_1(gaussList,gaSemap,trZero,trTriang,rootone,roottwo,rootthree,gCSql);
                          service.submit(worker);
                        //  root3=root3.add(onee);
            }
                       }
                      //root2=root2.add(onee);
                  }
                  //root1=root1.add(onee);
              } } catch (SQLException ex) {
              Logger.getLogger(GaussCorrolation.class.getName()).log(Level.SEVERE, null, ex);
          }
       trZero=trZero.add(onee);
      }
     trTriang=trTriang.add(onee);
    }
    
   executorService1.shutdown();
   System.out.println("GaussCorrolation ended");

  try {
  while (!executorService1.isTerminated()) {
    final Future<String> future = service.take();
    System.out.println(future.get());
  }
} catch (ExecutionException | InterruptedException ex) { } 


  
}


}
