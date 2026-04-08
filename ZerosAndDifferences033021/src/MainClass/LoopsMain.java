/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package MainClass;

import LoopsLogic.LoopsLogicLoopSemaphore;
import fractionintegerset.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import mucorrolationthreaded.GaussCorrolation;
import mucorrolationthreaded.GaussTable;
import mucorrolationthreaded.MuCorrolation1;
import mucorrolationthreaded.MuCorrolation11Iterated;
import SqlErata.newDB;
import mucorrolationthreaded.twoTableDBThread;
import twopolynomial.plvManager;
import twopolynomial.vertexVector;
import twopolynomial.zaddbTable1;
//import javax.enterprise.concurrent
//import javax.enterprise.concurrent.ManagedFutureTask;


/**
 *
 * @author Aibes
 */
public class LoopsMain {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     *
     * Importantly, as of sept 26 the LoopsSemaphoreInterface is implemented
    */
 
    
    
        
public static void main(String[] args) throws Exception {
 
 
    Semaphore tlSemaphore = new Semaphore(1);

    Semaphore t2Semaphore = new Semaphore (1);
    Semaphore cosetSemaphore = new Semaphore(1);     
    vertexVector dbVector = new vertexVector();

    vertexVector dbVector1 = new vertexVector();
    vertexVector dbVectorCST = new vertexVector();
   
   // ProductBean productBean = new ProductBean();
   // productBean.addPropertyChangeListener(new ProductPropertyChangeListener());

 //    newDB nDB = new newDB();    
     Semaphore t1Sema = new Semaphore(0);
     List rootBuffer = new ArrayList();
     Semaphore t2Sema = new Semaphore(1);
     List gaussBuffer = new ArrayList(); 
     Semaphore muSem = new Semaphore(0);
     List muList = new ArrayList();   
     LoopsLogicLoopSemaphore loopsLogic = new LoopsLogicLoopSemaphore();

    Semaphore t11Semaphore = new Semaphore(1);          // LoopsDriverTwoP buffer and semaphore 4/2/2021
    Semaphore tl1Semaphore = new Semaphore(1);          // 6/23/2021
    List dbVector11 = new vertexVector();

    Semaphore configSemaphore = new Semaphore(0);    
    
    
//   nDB.go(args);
//  Thread.sleep(100000);
        //  twoPolnomial n,nMaxx iterator.    // 2/26/2021 plvManager need instance.  do not start thread do not instance zaddbtable1 or start
//       plvManager mage = new plvManager(tlSemaphore,t2Semaphore,cosetSemaphore,dbVector,dbVector1,dbVectorCST,tl1Semaphore,dbVector11);
//       zaddbTable1 bmage = new zaddbTable1(tlSemaphore,dbVector,mage);
//       mage.start();
//       bmage.start();




//       LoopsDriverTable lDriver= new LoopsDriverTable(muList,muSem,loopsLogic,tl1Semaphore,dbVector11,mage,tl1Semaphore,dbVector11);  
//       GaussTable1 gTable1 = new GaussTable1(muSem,muList,lDriver);  
//       lDriver.start();
//       gTable1.start();
    
//                              List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,plvManager mage,Semaphore tl1Semaphore,List dbVector11
        
//       LoopsDriverTwoPManager lDriver= new LoopsDriverTwoPManager(muList,muSem,loopsLogic,tl1Semaphore,dbVector11,mage,tl1Semaphore,dbVector11);  
//       GaussTable1 gTable1 = new GaussTable1(muSem,muList,lDriver);  
//       lDriver.start();
//       gTable1.start();
    
       
       
       
 //      LoopsDriver lDriver= new LoopsDriver(muList,muSem,loopsLogic);  
 //      GaussTable1 gTable1 = new GaussTable1(muSem,muList,lDriver);  
 //      lDriver.start();
 //      gTable1.start();  
                                                                                                                            //  6/23/2021
//       plvManager mage = new plvManager(tlSemaphore,t2Semaphore,cosetSemaphore,dbVector,dbVector1,dbVectorCST,tl1Semaphore,(vertexVector)dbVector11);
//       zaddbTable1 bmage = new zaddbTable1(tlSemaphore,dbVector,mage);
//       mage.start();
//       bmage.start();


                              //       FractionIntegerDriver fig= new FractionIntegerDriver(muList,muSem);        
//       FractionIntegerDriverIterate figI= new FractionIntegerDriverIterate(muList,muSem);     
//       GaussTable1 gTable1 = new GaussTable1(muSem,muList,figI);
                             //     fig.start();
//       figI.start();
//       gTable1.start();

                                      //  noBufferRun is boolean at end of signature true=noBuffer
                                      //  Looks like most current ldtpm
        // need both instances need both started.                              
                   //      LoopsDriverTwoPManager lDriver= new LoopsDriverTwoPManager(muList,muSem,loopsLogic,tl1Semaphore,dbVector11,figI,tl1Semaphore,dbVector11,true);  
                  //       LoopsDriverTwoPManager lDriver= new LoopsDriverTwoPManager(muList,muSem,loopsLogic,t11Semaphore,dbVector11,figI,t11Semaphore,dbVector11,false); 

//       the usual 6/30/21                 
//       LoopsDriverTwoPManager lDriver= new LoopsDriverTwoPManager(muList,muSem,loopsLogic,t11Semaphore,dbVector11,t11Semaphore,dbVector11,false,configSemaphore);
//       GaussTable1 gTable1 = new GaussTable1(t11Semaphore,dbVector11,lDriver,configSemaphore); 
//       lDriver.start();
//       gTable1.start();

//attempt to move Gtable1 into loopsDrivertwoPmanager    MEANING no need for GaussTable1 to be instinciated in main.



// 7/2 is shut of for FIDi run. 

       LoopsDriverTwoPManager lDriver= new LoopsDriverTwoPManager(muList,muSem,loopsLogic,t11Semaphore,dbVector11,t11Semaphore,dbVector11,true,configSemaphore);
 //                        GaussTable1 gTable1 = new GaussTable1(t11Semaphore,dbVector11,lDriver,configSemaphore); 
       lDriver.start();
//                        gTable1.start();
       System.out.println("Started lDriver.start()");
       
       
//   MuCorrolation1 muCorr = new MuCorrolation1(t1Sema,rootBuffer,fig,muList,muSem);
//   MuCorrolation11Iterated muCorrI = new MuCorrolation11Iterated(t1Sema,rootBuffer,figI,muList,muSem);
//   twoTableDBThread tTDBT = new twoTableDBThread(t1Sema,rootBuffer,muCorrI);
//   GaussCorrolation gCorr = new GaussCorrolation(t2Sema,gaussBuffer);
//  GaussTable gTable = new GaussTable(t2Sema,gaussBuffer,gCorr);
// GaussTable(Semaphore tlSema,List dbVector,MuCorrolation1 mang)        
//     twoTableDBThread_1 tTdbt1 = new twoTableDBThread_1();
//  nDB.go(args);   
//  gCorr.start();
//     tTdbt1.start();
//     muCorr.start();
//    muCorrI.start();     
//   tTDBT.start();
    
try{
lDriver.join();
   System.out.println("Exitied lDriver.join()");
} 
catch (InterruptedException e){
   System.out.println("FractionIntegerDriver threadInterrupted");
}
  
//  try{
//    mage.join();
// } 
//  catch (InterruptedException e){
//     System.out.println("FractionIntegerDriver threadInterrupted");
//  }
    
// try{
//    bmage.join();
//  } 
//  catch (InterruptedException e){
//     System.out.println("FractionIntegerDriver threadInterrupted");
//  }
  
  
  
  
  
   
//   try{
//      fig.join();
//    } 
//    catch (InterruptedException e){
//       System.out.println("FractionIntegerDriver threadInterrupted");
//    }
// try{
//     figI.join();
//   } 
//    catch (InterruptedException e){
//       System.out.println("FractionIntegerDriver threadInterrupted");
//    }     
     
   
//     try{
//       gTable1.join();
//       System.out.println("LoopsMain post gTable1.join()");
//     } catch (InterruptedException e){
//       System.out.println("gtable threadInterrupted");
//     }
     
     
 //    System.out.println("Data offering: " + gaussBuffer.toString());
 //    for (int i=0;i<gaussBuffer.size();i++) {
 //     System.out.println(((GaussBean)gaussBuffer.get(i)).getX1() + " " + ((GaussBean)gaussBuffer.get(i)).getY1() + " " + ((GaussBean)gaussBuffer.get(i)).getX2() + " " + ((GaussBean)gaussBuffer.get(i)).getY2() + " " + ((GaussBean)gaussBuffer.get(i)).getX3() + " " + ((GaussBean)gaussBuffer.get(i)).getY3()+ " " + ((GaussBean)gaussBuffer.get(i)).getTrTr()+ " " + ((GaussBean)gaussBuffer.get(i)).getTrZero()+ " " + ((GaussBean)gaussBuffer.get(i)).getRoot1() + " " + ((GaussBean)gaussBuffer.get(i))+ " " + ((GaussBean)gaussBuffer.get(i)).getRoot2()+ " " + ((GaussBean)gaussBuffer.get(i)).getRoot3()  );      
 //    }
 //    try{
 //    muCorr.join();
 //    } catch (InterruptedException e){
 //      System.out.println("mage threadInterrupted");
 //   }      
 //   try{
 //    muCorrI.join();
 //    } catch (InterruptedException e){
 //      System.out.println("mage threadInterrupted");
 //    }  
   System.out.println("Exited main");
       for (Thread t : Thread.getAllStackTraces().keySet()) {
        if (t.isAlive() && !t.isDaemon()) {
            System.out.println("NON-DAEMON: " + t.getName() + " state=" + t.getState());
        }
    }
    System.exit(0);
 //   try{
 //   tTDBT.join();  
 //    } catch (InterruptedException e){
 //      System.out.println("cmage threadInterrupted"); }
      
//     try{
//      gCorr.join();     
//      System.out.println("Data offeringgg: " + gaussBuffer.toString());
//     } catch (InterruptedException e){
//       System.out.println("gCorr threadInterrupted");
//     }   
 
 
 
 
        
    }
    
}

