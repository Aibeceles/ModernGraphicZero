/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package MainClass;
                           
import PArrayReset.PolynomialArray;
import LoopLists.LoopList;
import LoopLists.LoopListener;
import LoopLists.MatrixA;
import PArrayReset.PArrayResetLIstener;
import LoopsLogic.LoopsLogicLoopCondition;
import LoopsLogic.LoopsLogicLoopSemaphore;
import LoopsLogic.ModuloList;
import LoopsSemaphorePattern.LoopSemaphore;
import LoopsSemaphorePattern.LoopSemaphoreInitial;
import LoopsSemaphorePattern.LoopSemaphoreLast;
import LoopsSemaphorePattern.LoopsConditionInterface;
import LoopsSemaphorePattern.LoopsSemaphoreInterface;
import fractionintegerset.*;
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
import java.util.concurrent.Semaphore;

/**  
 * The iteration for initializing LoopListRunnable's generated at Psi_i.
 * 
 * resultListBean composed of gbList, the logic output of each worker.
 *  
 * Unused class, funcionality intended is within listener runnable method.
 * 
 * @author Aibes
 */                                                                                                 
public class LoopsDriverRunnable extends Thread {

    
int dimension=4;    
int integerRange=15;

ExecutorService executorService1 = Executors.newFixedThreadPool(50);    
CompletionService<String> service = new ExecutorCompletionService<>(executorService1);    

Semaphore muSem = new Semaphore(1);
List muList = new ArrayList();            

List semaphoreListF = new ArrayList();
List semaphoreListB = new ArrayList();
PolynomialArray pArray = new PolynomialArray(dimension);
ResultListBean rListB =new ResultListBean();
List ancestorlist = new ArrayList();
List deltaList=new ArrayList();
List cashedrListB = new ArrayList();
EndProduct endProduct=new EndProduct(); 
//LoopsLogicLoopSemaphore loopsLogic;  //any class that extends AbstractLoopSemaphore
//LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(); 
LoopsSemaphoreInterface loopsLogic; 

final ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
//int numeratorLow=0;
//int numeratorHigh=5;

public LoopsDriverRunnable(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic) {
   this.muList=muList;
   this.muSem=muSem;
   this.loopsLogic=loopsLogic;
}

@Override
public void run() {
   MatrixA xPowers = new MatrixA(dimension);
 //  xPowers.printMatrix();
 //  pArray.addPropertyChangeListener(new PArrayResetLIstener());
   
   ModuloList moduloList= new ModuloList(dimension);
   long threadId = Thread.currentThread().getId();
   System.out.println("LoopDriver Runnable , Threadid" + threadId  );
//   List deltaTerm=new ArrayList();deltaList.add(deltaTerm);List ancestorlist0 = new ArrayList();ancestorlist.add(ancestorlist0);
  //  MatrixA xPowers = new MatrixA(dimension);
 //  LoopList gbList00 = new LoopList(rListB,dimension,integerRange,0,deltaList,ancestorlist,cashedrListB,xPowers);
 //  int semaphoreListSizeF;int semaphoreListSizeB;
 //  semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0)); semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));
 //  semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();    
//   Callable workerStart = new GenerateBinaryStart((Semaphore)semaphoreListF.get(1));
 //  service.submit(workerStart);
//   List deltaTerm1=new ArrayList();deltaList.add(deltaTerm1);List ancestorlist1 = new ArrayList();ancestorlist.add(ancestorlist1);
//   MatrixA xPowers1 = new MatrixA(dimension,dimension+2,xPowers);
//   LoopList gbList = new LoopList(rListB,dimension,integerRange,1,deltaList,ancestorlist,cashedrListB,xPowers);
 //rListB.add(gbList);
   
   
   
//   LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(gbList,0,integerRange,pArray); LoopsConditionInterface loopsCondition = new LoopsLogicLoopCondition();
//   gbList.addPropertyChangeListener(new LoopListener(rListB,gbList,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist)); 
   
 //  Callable workerZero = new LoopSemaphoreInitial((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic,loopsCondition,integerRange); 
 //   System.out.println("semaphorelistsize  "+semaphoreListSizeF);
 //  service.submit(workerZero);
   
  for (int numerator=2; numerator<=dimension ; numerator++) {
   //       semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
   System.out.println("rListB.size()+1 "+rListB.size()+1);
  //        List deltaTerm2=new ArrayList();deltaList.add(deltaTerm2);List ancestorlist2 = new ArrayList();ancestorlist.add(ancestorlist2);
  //        MatrixA xPowers2 = new MatrixA(dimension,dimension+2,xPowers);
//          LoopList gbList1 = new LoopList(rListB,dimension,integerRange,numerator,deltaList,ancestorlist,cashedrListB,xPowers);
   // rListB.add(gbList1);
     
          
  //        LoopsSemaphoreInterface loopsLogic1= new LoopsLogicLoopSemaphore(gbList1,numerator,integerRange,pArray);LoopsConditionInterface loopsCondition1 = new LoopsLogicLoopCondition();
  //        gbList1.addPropertyChangeListener(new LoopListener(rListB,gbList1,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist));
    //      Callable worker = new LoopSemaphore((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic1,loopsCondition1,integerRange); 
    //      service.submit(worker); 
  }
//   semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
//   semaphoreListSizeF=semaphoreListF.size(); semaphoreListSizeB=semaphoreListB.size();
//   List deltaTerm3=new ArrayList();deltaList.add(deltaTerm3);List ancestorlist3 = new ArrayList();ancestorlist.add(ancestorlist3);
//   MatrixA xPowers3 = new MatrixA(dimension,dimension+2,xPowers);
//   LoopList gbList2 = new LoopList(rListB,dimension,integerRange,dimension,deltaList,ancestorlist,cashedrListB,xPowers);
//  rListB.add(gbList2);
   
   
//   LoopsSemaphoreInterface loopsLogic2= new LoopsLogicLoopSemaphore(gbList2,dimension,integerRange,pArray);LoopsConditionInterface loopsCondition2 = new LoopsLogicLoopCondition();
   
//   gbList2.addPropertyChangeListener(new LoopListener(rListB,gbList2,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist));
   
//   Callable workerLast = new LoopSemaphoreLast((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic2,loopsCondition2,integerRange); 
//   service.submit(workerLast);
//   service.submit(workerStart);
   System.out.println("before .isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
//   executorService1.shutdown();
  
//   try {
 //      executorService1.shutdown();
 //      while (!executorService1.isTerminated()) {    
 //      final Future<String> future = service.take();
 //      System.out.println("service.take "+future.get());
 //      System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
        }   
  // } catch (ExecutionException | InterruptedException ex) {
  // ex.printStackTrace();
 //  } 
//   System.out.println("ny exit");

//}
}
