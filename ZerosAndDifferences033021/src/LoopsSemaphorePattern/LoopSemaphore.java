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
 * Accepts a type of 
 * @author Aibes
 */
public class LoopSemaphore implements Callable<String>{

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




public LoopSemaphore( Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,LoopsSemaphoreInterface loopsLogic,LoopsConditionInterface loopsCondition,int setSize) {

sf1=s1;
sb1=s2;
sf2=s3;
sb2=s4;
this.endProduct=endCproduct;
this.loopsLogic=loopsLogic;
this.loopsCondition=loopsCondition;
this.setSize=setSize;
}

public LoopSemaphore( Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,LoopsSemaphoreInterface loopsLogic,LoopsConditionInterface loopsCondition,int setSize,int lbSetSize) {

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

public LoopSemaphore( Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,LoopsSemaphoreInterface loopsLogic,LoopsConditionInterface loopsCondition,int setSize,int lbSetSize, boolean skip) {

sf1=s1;
sb1=s2;
sf2=s3;
sb2=s4;
this.endProduct=endCproduct;
this.loopsLogic=loopsLogic;
this.loopsCondition=loopsCondition;
this.setSize=setSize;
//this.setSize=setSize;
this.lbSetSize=lbSetSize;
this.skip=skip;
}



public String call() throws InterruptedException {
    int setSizee=lbSetSize;
    
    long threadId = Thread.currentThread().getId();
    System.out.println("LoopSemaphore sf2.acquire()0"+"threadId: "+threadId);
    while(!endProduct.getEndCproduct()){
     //System.out.println("LoopSemaphore !endProduct "+"threadId: "+threadId);
     setSizee=lbSetSize;   
    sf2.acquire();
 //  (LoopsLogicLoopSemaphore)loopsLogic.
 //  System.out.println("LoopSemaphore sf2.acquire()1"+" threadId: "+threadId);
 if (!skip) {
    loopsLogic.sf2Aquire();setSizee++;
    sb2.release();
 //   System.out.println("LoopSemaphore sb2.release()"+" threadId: "+threadId);
 //   System.out.println("LoopSemaphore sb2.release()1 setSizee>setSize"+setSizee+" "+setSize);
    while (setSizee<setSize)
    {   
 //      System.out.println("LoopSemaphore setSizee<setSize"+" threadId: "+threadId+" "+setSizee+" "+setSize);
       sf2.acquire(); 
 //      System.out.println("LoopSemaphore sf2.acquire()2 "+threadId);
       loopsLogic.sf2Aquire();setSizee++;
  //     if (setSizee<setSize){ sb2.release(); }
       sb2.release();
    }  // end of while loop 
 //   loopsLogic.rentrentReset();
 //   System.out.println("loopsemaphore pre sf1.release (it iterated) " + threadId);

     sf2.acquire(); 
     loopsLogic.rentrentReset();
 }  // close skip block    
     
     sf1.release();
//     System.out.println("loopsemaphore post sf1.release"+ threadId);
//     System.out.println("loopsemaphore pre sb1.aquire"+ threadId);
    sb1.acquire();
    sb2.release();
        
  } //endcproduct while loop
  return("Loops Semaphore "+ threadId);
  }
}