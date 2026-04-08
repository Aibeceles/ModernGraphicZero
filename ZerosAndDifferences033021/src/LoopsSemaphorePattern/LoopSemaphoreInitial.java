/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopsSemaphorePattern;
import LoopsLogic.LoopsLogicLoopSemaphore;
import fractionintegerset.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 *
 * @author HP_Administrator
 */
public class LoopSemaphoreInitial implements Callable<String>{
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

public LoopSemaphoreInitial( Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,LoopsSemaphoreInterface loopsLogic,LoopsConditionInterface loopsCondition,int setSize) {

sf1=s1;
sb1=s2;
sf2=s3;
sb2=s4;
this.endProduct=endCproduct;
this.loopsLogic=loopsLogic;
this.loopsCondition=loopsCondition;
this.setSize=setSize;
}

public LoopSemaphoreInitial( Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,LoopsSemaphoreInterface loopsLogic,LoopsConditionInterface loopsCondition,int setSize,int lbSetSize) {

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

public LoopSemaphoreInitial( Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,LoopsSemaphoreInterface loopsLogic,LoopsConditionInterface loopsCondition,int setSize,int lbSetSize,boolean skip) {

sf1=s1;
sb1=s2;
sf2=s3;
sb2=s4;
this.endProduct=endCproduct;
this.loopsLogic=loopsLogic;
this.loopsCondition=loopsCondition;
// this.setSize=setSize+1;
this.setSize=setSize+1;
this.lbSetSize=lbSetSize;
this.skip=skip;
}



public String call() throws InterruptedException {
    
    int setSizee;   //=   this.lbSetSize;
    long threadId = Thread.currentThread().getId();
//    System.out.println("LoopSemaphoreInitial sf2.acquire()0"+" threadId: "+threadId);
    while(!endProduct.getEndCproduct()){
    //System.out.println("LoopSemaphore initial !endProduct "+"threadId: "+threadId);    
    setSizee=this.lbSetSize;    
    sf2.acquire();
    //System.out.println("LoopSemaphoreInitial sf2.acquire()1"+threadId);
    if (!skip) {
    loopsLogic.sf2Aquire();
    setSizee++;
    //   Set semaphore patern to iterate only first Fig.   11/9   
    //  setSizee=setSize;         //  loop flowthrough
    //  sb2.release();  
//    System.out.println("LoopSemaphoreInitial sb2.release()1 setSizee<setSize"+setSizee+" "+setSize);  
    while (setSizee<setSize)
    {     
//     System.out.println("LoopSemaphoreInitial setSizee<setSize"+" threadId: "+threadId);
//      sf2.acquire();
//     System.out.println("LoopSemaphoreInitial sf2.acquire()2"+threadId+" setSizee "+setSizee+"setSize "+setSize);
     loopsLogic.sf2Aquire();
//         System.out.println("LoopSemaphore initial post loopsLogic.sf2Aquire"+threadId+" setSizee "+setSizee+"setSize "+setSize);
     //    sb2.release(); 
     setSizee++;
    }  // end of while loop 
//     System.out.println("loopsemaphoreinitial pre sf1.release (it iterated)");
     loopsLogic.rentrentReset();
    }  // close skip guard 
     sf1.release();
//    System.out.println("loopsemaphoreinitial post sf1.release");
//    System.out.println("loopsemaphoreinitial pre sb1.aquire");
    sb1.acquire();
 //   System.out.println("loopsemaphore initial post sb1.aquire");
    sf2.release();   // this permit starts its own iteration. 
   
    
    } //endcproduct while loop
    
  return("Loops Semaphore "+ threadId);
  }   
}
