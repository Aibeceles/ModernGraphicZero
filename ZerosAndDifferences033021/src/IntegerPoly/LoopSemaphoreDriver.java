/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package IntegerPoly;

import fractionintegerset.*;
import java.lang.Boolean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;

/**  FractionIntegerDriver has denominator, numeratorlow,numberatorhigh
 *   hard coded (needs to be argument in .main).
 * @author Aibes
 */
public class LoopSemaphoreDriver extends Thread implements GBIterator{

ExecutorService executorService1 = Executors.newFixedThreadPool(50);    
CompletionService<String> service = new ExecutorCompletionService<>(executorService1);    
Semaphore gaSemap = new Semaphore(1);
List gaussList = new ArrayList();
Semaphore muSem = new Semaphore(1);
List muList = new ArrayList();            
List semaphoreListF = new ArrayList();
List semaphoreListB = new ArrayList();
int denominator=6;
int numeratorLow=1;
int numeratorHigh=6;
boolean numeratorBoolean=true;  //not used 
Boolean endCproduct=false;
EndProduct endProduct=new EndProduct();
ResultListBean rListB =new ResultListBean();
final ThreadMXBean bean = ManagementFactory.getThreadMXBean( );

//GBListener gListener new GBListener(rListB,muList,muSem); 
//GBListener nut needed injected into gbList.

public LoopSemaphoreDriver(List muList,Semaphore muSem) {
   this.muList=muList;
   this.muSem=muSem;        
}

public void run() {
   long threadId = Thread.currentThread().getId();
 System.out.println("                         FractionIntegerDriver, Threadid" + threadId  );
   endProduct.setEndCproduct(endCproduct);
   int semaphoreListSizeF;int semaphoreListSizeB;
   semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0)); semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));
   semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();    
   Callable workerStart = new GenerateBinaryStart((Semaphore)semaphoreListF.get(1));
   service.submit(workerStart); 
   GBList gbList = new GBList();gbList.addPropertyChangeListener(new GBListener(rListB,muList,muSem));
   rListB.add(gbList);
   Callable workerZero = new GenerateBinaryZero(numeratorLow,denominator,(Semaphore)semaphoreListF.get(0),(Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,gbList,rListB); 
    System.out.println("semaphorelistsize  "+semaphoreListSizeF);
   service.submit(workerZero);
   
  for (int numerator=numeratorLow+1; numerator<numeratorHigh ; numerator++) {
          semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
          GBList gbList1 = new GBList();gbList1.addPropertyChangeListener(new GBListener(rListB,muList,muSem));
          rListB.add(gbList1);
          Callable worker = new GenerateBinary(numerator,denominator,(Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,gbList1); 
          service.submit(worker); 
  }
   semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
   semaphoreListSizeF=semaphoreListF.size(); semaphoreListSizeB=semaphoreListB.size();
   GBList gbList2 = new GBList();
   gbList2.addPropertyChangeListener(new GBListener(rListB,muList,muSem));
   rListB.add(gbList2);
   Callable workerLast = new GenerateBinaryLast(numeratorHigh,denominator,(Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,gbList2); 
   service.submit(workerLast);
   
   System.out.println("before .isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
   executorService1.shutdown();
  
   try {
       executorService1.shutdown();
       while (!executorService1.isTerminated()) {    
       final Future<String> future = service.take();
       System.out.println("service.take "+future.get());
       
   //     System.out.println("SemaphoreListF  "+semaphoreListF.toString());
   //     System.out.println("SemaphoreListB  "+semaphoreListB.toString());
       System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
        }   
   } catch (ExecutionException | InterruptedException ex) {
   ex.printStackTrace();
   } 
   System.out.println("fid exit");

}
}
