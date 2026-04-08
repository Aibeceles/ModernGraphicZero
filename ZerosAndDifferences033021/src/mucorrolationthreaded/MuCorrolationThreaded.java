/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mucorrolationthreaded;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
/**
 *
 * @author Aibes
 */
public class MuCorrolationThreaded {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
  //   newDB nDB = new newDB();    
     Semaphore t1Sema = new Semaphore(1);
     List rootBuffer = new ArrayList();
     
     Semaphore t2Sema = new Semaphore(1);
     List gaussBuffer = new ArrayList();
     
     
 //    MuCorrolation1 muCorr = new MuCorrolation1(t1Sema,rootBuffer);
 //    twoTableDBThread tTDBT = new twoTableDBThread(t1Sema,rootBuffer,muCorr);

     GaussCorrolation gCorr = new GaussCorrolation(t2Sema,gaussBuffer);
     GaussTable gTable = new GaussTable(t2Sema,gaussBuffer,gCorr);
             
     // GaussTable(Semaphore tlSema,List dbVector,MuCorrolation1 mang)        

//     twoTableDBThread_1 tTdbt1 = new twoTableDBThread_1();
//     nDB.go(args);
     gCorr.start();
     gTable.start();
//     tTdbt1.start();
//     muCorr.start();
//     tTDBT.start();

     try{
      gCorr.join();
      
     // System.out.println("Data offeringgg: " + gaussBuffer.toString());
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
//     try{
//     muCorr.join();
//     } catch (InterruptedException e){
//       System.out.println("mage threadInterrupted");
//     }      
//        try{
//    tTDBT.join();  
//     } catch (InterruptedException e){
//       System.out.println("cmage threadInterrupted");
//     }
     
     
     

    
}
            
}    