/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopsSemaphorePattern;
import LoopsLogic.LoopsLogicLoopSemaphore;
import fractionintegerset.*;
import static java.lang.System.gc;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 *
 * @author HP_Administrator
 */
public class LoopSemaphoreLast implements Callable<String>{
EndProduct endProduct=new EndProduct();   
Semaphore sf1;
Semaphore sb1;
Semaphore sf2;
Semaphore sb2;
LoopsSemaphoreInterface loopsLogic;
LoopsConditionInterface loopsCondition;
int setSize;
int lbSetSize;
boolean skip=false;

public LoopSemaphoreLast( Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,LoopsSemaphoreInterface loopsLogic,LoopsConditionInterface loopsCondition,int setSize) {

sf1=s1;
sb1=s2;
sf2=s3;
sb2=s4;
this.endProduct=endCproduct;
this.loopsLogic=loopsLogic;
this.loopsCondition=loopsCondition;
this.setSize=setSize;
}

public LoopSemaphoreLast( Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,LoopsSemaphoreInterface loopsLogic,LoopsConditionInterface loopsCondition,int setSize,int lbSetSize) {

sf1=s1;
sb1=s2;
sf2=s3;
sb2=s4;
this.endProduct=endCproduct;
this.loopsLogic=loopsLogic;
this.loopsCondition=loopsCondition;
this.setSize=setSize+1;
this.lbSetSize=lbSetSize;
}

public LoopSemaphoreLast( Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,LoopsSemaphoreInterface loopsLogic,LoopsConditionInterface loopsCondition,int setSize,int lbSetSize,boolean skip) {

sf1=s1;
sb1=s2;
sf2=s3;
sb2=s4;
this.endProduct=endCproduct;
this.loopsLogic=loopsLogic;
this.loopsCondition=loopsCondition;
this.setSize=setSize+1;
this.lbSetSize=lbSetSize;
this.skip=skip;
}




public String call() throws InterruptedException {
    
    //int setSizee=0;
    long threadId = Thread.currentThread().getId();
         //   System.out.println("LoopSemaphoreLast  sf2.acquire()0"+" threadId: "+threadId);
        //    while(!endProduct.getEndCproduct()){
            sf2.acquire();
        //    System.out.println("LoopSemaphoreLast post sf2.acquire()1");
            loopsLogic.sf2Aquire();lbSetSize++;
//         //   loopsLogic.
//         //   System.out.println("LoopSemaphoreLast pre sb2.release()1");
//            sb2.release();  
//        //    System.out.println("LoopSemaphoreLast post sb2.release()1");    
//            while (lbSetSize<setSize)
//            {     
//          //  gc();
//         //   System.out.println("LoopSemaphoreLast pre sf2.aquire()2");
//            sf2.acquire();//    System.out.println("LoopSemaphoreLast post sf2.acquire()2");
//            loopsLogic.sf2Aquire();
//         //   System.out.println("LoopSemaphoreLast post sf2.aquire(),pre sb2.release()");
//            if (lbSetSize+1==setSize) { endProduct.setEndCproduct(true); }
             endProduct.setEndCproduct(true);
             sb2.release();
         //   System.out.println("LoopSemaphoreLast sb2.release()");
//            lbSetSize++;
//            }  // end of while loop 
          //  sf1.release();
          //  sb1.acquire(); 
        //  } //endcproduct while loop
           System.out.println("LoopSemaphoreLast pre .return");
  return("Loops Semaphore  last"+ threadId);
  }
}
