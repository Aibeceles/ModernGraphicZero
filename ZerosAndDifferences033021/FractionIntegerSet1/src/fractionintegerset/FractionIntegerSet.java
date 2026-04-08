/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package fractionintegerset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import mucorrolationthreaded.GaussCorrolation;
import mucorrolationthreaded.GaussTable;
import mucorrolationthreaded.MuCorrolation1;
import mucorrolationthreaded.MuCorrolation11Iterated;
import SqlErata.newDB;
import mucorrolationthreaded.twoTableDBThread;
//import javax.enterprise.concurrent
//import javax.enterprise.concurrent.ManagedFutureTask;


/**
 *
 * @author Aibes
 */
public class FractionIntegerSet {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
 
    
    
        
    public static void main(String[] args) throws Exception {
 
   // ProductBean productBean = new ProductBean();
   // productBean.addPropertyChangeListener(new ProductPropertyChangeListener());

     newDB nDB = new newDB();    
     Semaphore t1Sema = new Semaphore(0);
     List rootBuffer = new ArrayList();

     Semaphore t2Sema = new Semaphore(1);
     List gaussBuffer = new ArrayList(); 
      
     Semaphore muSem = new Semaphore(0);
     List muList = new ArrayList();   
   
 //  nDB.go(args);  
    
   FractionIntegerDriver fig= new FractionIntegerDriver(muList,muSem);          
   FractionIntegerDriverIterate figI= new FractionIntegerDriverIterate(muList,muSem);     
  
   MuCorrolation1 muCorr = new MuCorrolation1(t1Sema,rootBuffer,fig,muList,muSem);
   MuCorrolation11Iterated muCorrI = new MuCorrolation11Iterated(t1Sema,rootBuffer,figI,muList,muSem);
   
   twoTableDBThread tTDBT = new twoTableDBThread(t1Sema,rootBuffer,muCorrI);

   GaussCorrolation gCorr = new GaussCorrolation(t2Sema,gaussBuffer);
   GaussTable gTable = new GaussTable(t2Sema,gaussBuffer,gCorr);

   fig.start();
 // figI.start();     
// GaussTable(Semaphore tlSema,List dbVector,MuCorrolation1 mang)        
//     twoTableDBThread_1 tTdbt1 = new twoTableDBThread_1();
  //  nDB.go(args);   
 //  gCorr.start();
//   gTable.start();
//     tTdbt1.start();
//     muCorr.start();
//    muCorrI.start();     
 //   tTDBT.start();
    
   try{
      fig.join();
    } 
    catch (InterruptedException e){
       System.out.println("FractionIntegerDriver threadInterrupted");
    }
   try{
      figI.join();
    } 
    catch (InterruptedException e){
       System.out.println("FractionIntegerDriver threadInterrupted");
    }     
     
     
     try{
      gCorr.join();     
      System.out.println("Data offeringgg: " + gaussBuffer.toString());
     } catch (InterruptedException e){
       System.out.println("gCorr threadInterrupted");
     }     
     try{
       gTable.join();
     } catch (InterruptedException e){
       System.out.println("gtable threadInterrupted");
    }
     
     
 //    System.out.println("Data offering: " + gaussBuffer.toString());
 //    for (int i=0;i<gaussBuffer.size();i++) {
 //     System.out.println(((GaussBean)gaussBuffer.get(i)).getX1() + " " + ((GaussBean)gaussBuffer.get(i)).getY1() + " " + ((GaussBean)gaussBuffer.get(i)).getX2() + " " + ((GaussBean)gaussBuffer.get(i)).getY2() + " " + ((GaussBean)gaussBuffer.get(i)).getX3() + " " + ((GaussBean)gaussBuffer.get(i)).getY3()+ " " + ((GaussBean)gaussBuffer.get(i)).getTrTr()+ " " + ((GaussBean)gaussBuffer.get(i)).getTrZero()+ " " + ((GaussBean)gaussBuffer.get(i)).getRoot1() + " " + ((GaussBean)gaussBuffer.get(i))+ " " + ((GaussBean)gaussBuffer.get(i)).getRoot2()+ " " + ((GaussBean)gaussBuffer.get(i)).getRoot3()  );      
 //    }
 //    try{
 //    muCorr.join();
 //    } catch (InterruptedException e){
 //      System.out.println("mage threadInterrupted");
 //   }      
    try{
     muCorrI.join();
     } catch (InterruptedException e){
       System.out.println("mage threadInterrupted");
     }  




        try{
    tTDBT.join();  
     } catch (InterruptedException e){
       System.out.println("cmage threadInterrupted"); }
    
    
    
    
    
    
    
    
    
    
    }
    
}

