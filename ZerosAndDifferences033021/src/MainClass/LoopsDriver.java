/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package MainClass;
                           
import LoopLists.GaussBean1;
import PArrayReset.PolynomialArray;
import LoopLists.LoopList;
import LoopLists.LoopListener;
import LoopLists.LoopListenerRunnable;
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
import PArrayReset.PArrayResetFlag;
import PArrayReset.ResultListID;
import fractionintegerset.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import mugauss.NewtonInterpolator;

/**  The LoopsDriver lifecycle:  ExecutorService instanciates callable workers representing terms in a cartisian product.
 *                               The CompletionService logs progress till compleation of product.  Then returns.
 *   ResultListBean composed of gbList, the logic output of each worker.
 *   
 *   Have pArray the bounded incrementer recording increments at worker_i
 *   Introducing figPArray the run of zeros represented by zeros at Psi_i, delta_j, k  
 * 
 * @author Aibes
 */                                                                                                 
public class LoopsDriver extends Thread {
    
int dimension=3;    
int integerRange=20;     //  The loopListener needs to know this value for its runnable.   
int setProductRange=20;

//ResultListID resultListId = new ResultListID();

ExecutorService executorService1 = Executors.newFixedThreadPool(50);    
CompletionService<String> service = new ExecutorCompletionService<>(executorService1);    
Semaphore muSem = new Semaphore(1);
List muList = new ArrayList();            
List semaphoreListF = new ArrayList();
List semaphoreListB = new ArrayList();
PolynomialArray pArray = new PolynomialArray(dimension);    // 
PolynomialArray figPArray = new PolynomialArray(dimension,1,true);
ResultListBean rListB =new ResultListBean();
ResultListBean allrListB =new ResultListBean();
List ancestorlist = new ArrayList();
List deltaList=new ArrayList();
ResultListBean cashedrListB =new ResultListBean();
//ResultListBean cashedrListB;
PolynomialArray cashedPArray = new PolynomialArray(dimension,1,true);
//PolynomialArray cashedPArray new PolynomialArray(dimension);     
PolynomialArray cashedFigPArray = new PolynomialArray(dimension,1);
//PolynomialArray cashedFigPArray;
EndProduct endProduct=new EndProduct(); 
//LoopsLogicLoopSemaphore loopsLogic;  //any class that extends AbstractLoopSemaphore
//LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(); 
LoopsSemaphoreInterface loopsLogic;
PArrayResetFlag pArrayResetFlag = new PArrayResetFlag();
final ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
//int numeratorLow=0;
//int numeratorHigh=5;

public LoopsDriver(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic) {
   this.muList=muList;
   this.muSem=muSem;
   this.loopsLogic=loopsLogic;
}


private void copyCashedRListBValues()  {
    List indexList;
    List rIndexList;
    BigDecimal indexValue;
//    int integerRange=15;
    int sizeList;
    int sizeRange;
    int rSizeRange;
    System.out.println();
    System.out.println("  LoopDriver.iterateRlistB:" );
    sizeList=rListB.size();
    int rSizeList=rListB.size();
        System.out.println("  LoopDriver.isizeList,rListBSizelist:: "+sizeList + "  "+ rSizeList);
    for (int l=0; l<sizeList;l++)
    {
        indexList=(ArrayList)rListB.get(l);
 //       rIndexList=(ArrayList)rListB.get(l);
        sizeRange=indexList.size();
//        rSizeRange=rIndexList.size();
                System.out.println("  LoopDriver. rlistB size range: "+sizeRange);
                cashedrListB.add(indexList);
  //      for (int m=0; m<sizeRange; m++)  {
 //     System.out.println("  LoopDriver.l, m: "+l+"  "+m );
//            indexValue=(BigDecimal)indexList.get(m);
//        ((LoopList)cashedrListB.get(l)).add(indexValue);
//        System.out.println("  rlistb.listindex.value:" + indexValue);
//        }
    }
    System.out.println();
}

private void copyCashedFigPArrayValues() {
    int value;
    int iterateRange=figPArray.size();
    for (int l=0; l<iterateRange;l++) {
        System.out.println("copyCahedFigPArrayValues l:" +l);
        value=(int)figPArray.get(l);
  System.out.println("copyCahedFigPArrayValues value:" +value);
        cashedFigPArray.add(value);
    }
    
}

private void cashState()  {
   copyCashedRListBValues();// cashedrListB=(ResultListBean)rListB.clone();
    cashedPArray=(PolynomialArray)pArray.clone();
 // copyCashedFigPArrayValues();  //cashedFigPArray=(PolynomialArray)figPArray.clone();
 //   rListB.add(999);
    System.out.println("                              casheState!!!!!!!!!!!!!!!!!!!!!!!!cashedFigPArray!"   + cashedFigPArray.toString()); 
    System.out.println("                              casheState!!!!!!!!!!!!!!!!!!!!!!!!cashedrListB!"   + cashedrListB.toString()); 
}
;

@Override
public void run() {
   MatrixA xPowers = new MatrixA(dimension+1);
   ResultListID resultListId = new ResultListID();
   int resultListCounter=resultListId.getrListId();
   allrListB.add(rListB);
   ModuloList moduloList= new ModuloList(dimension+1);
   long threadId = Thread.currentThread().getId();
   System.out.println("LoopDriver , Threadid" + threadId  );
   List deltaTerm=new ArrayList();deltaList.add(deltaTerm);List ancestorlist0 = new ArrayList();ancestorlist.add(ancestorlist0);

   LoopList gbList00 = new LoopList(dimension+1,rListB,dimension,integerRange,0,xPowers,figPArray, moduloList);   //function value list,  all subsequent are differneces of it
   rListB.add(gbList00);
   cashedrListB.add(gbList00);
   int halfIntegerRange = integerRange / 2;
   NewtonInterpolator.interpolate(new GaussBean1(figPArray,pArray.toString(),gbList00,resultListCounter), gbList00, dimension, halfIntegerRange, moduloList);
   int semaphoreListSizeF;int semaphoreListSizeB;
   semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0)); semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));
   semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();    
   Callable workerStart = new GenerateBinaryStart((Semaphore)semaphoreListF.get(1));    // is last service.submit call in LoopsDriver
   List deltaTerm1=new ArrayList();deltaList.add(deltaTerm1);List ancestorlist1 = new ArrayList();ancestorlist.add(ancestorlist1);
  
   LoopList gbList = new LoopList(dimension,rListB,dimension,integerRange,1,xPowers,figPArray, moduloList,true);
   rListB.add(gbList);
   cashedrListB.add(gbList);
   NewtonInterpolator.interpolate(new GaussBean1(figPArray,pArray.toString(),gbList,resultListCounter), gbList, dimension - 1, halfIntegerRange, moduloList);
   LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(gbList,0,integerRange,pArray); LoopsConditionInterface loopsCondition = new LoopsLogicLoopCondition();
   gbList.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId,true,integerRange)); 
   Callable workerZero = new LoopSemaphoreInitial((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic,loopsCondition,setProductRange); 
   service.submit(workerZero);

   for (int numerator=2; numerator<dimension ; numerator++) {
      semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
      System.out.println("LoopsDriver rListB.size()+1 "+rListB.size()+1);
      List deltaTerm2=new ArrayList();deltaList.add(deltaTerm2);List ancestorlist2 = new ArrayList();ancestorlist.add(ancestorlist2);
      MatrixA xPowers2 = new MatrixA(dimension,dimension+2,xPowers);      //looplist constructs its owne MatrixA's, and avoides schyronization problems
 
      LoopList gbList1 = new LoopList(dimension-(numerator-1),rListB,dimension,integerRange,numerator,xPowers,figPArray, moduloList);
      rListB.add(gbList1);
      cashedrListB.add(gbList1);
      NewtonInterpolator.interpolate(new GaussBean1(figPArray,pArray.toString(),gbList1,resultListCounter), gbList1, dimension - numerator, halfIntegerRange, moduloList);
        
      LoopsSemaphoreInterface loopsLogic1= new LoopsLogicLoopSemaphore(gbList1,numerator-1,integerRange,pArray);LoopsConditionInterface loopsCondition1 = new LoopsLogicLoopCondition();
      gbList1.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList1,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId,true,integerRange));
      Callable worker = new LoopSemaphore((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic1,loopsCondition1,setProductRange); 
      service.submit(worker); 
  }
      semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
      semaphoreListSizeF=semaphoreListF.size(); semaphoreListSizeB=semaphoreListB.size();
      List deltaTerm3=new ArrayList();deltaList.add(deltaTerm3);List ancestorlist3 = new ArrayList();ancestorlist.add(ancestorlist3);
      LoopList gbList2 = new LoopList(0,rListB,dimension,integerRange,dimension,deltaList,ancestorlist,cashedrListB,xPowers);
      rListB.add(gbList2);
      cashedrListB.add(gbList2);
      LoopsSemaphoreInterface loopsLogic2= new LoopsLogicLoopSemaphore(gbList2,dimension-1,integerRange,pArray);LoopsConditionInterface loopsCondition2 = new LoopsLogicLoopCondition();
      gbList2.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList2,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId,true,integerRange));
      Callable workerLast = new LoopSemaphoreLast((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic2,loopsCondition2,setProductRange); 
      service.submit(workerLast);
      System.out.println("LoopDriver.run pArray .to string " + pArray.toString());
      System.out.println("LoopDriver.run figPArray .to string " + figPArray.toString());
      System.out.println("LoopDriver.run rListB .to string " + rListB.toString());
      pArray.addPropertyChangeListener(new PArrayResetLIstener(pArray,rListB,pArrayResetFlag,figPArray,dimension,moduloList,muList,muSem,cashedrListB,cashedPArray,cashedFigPArray));
      service.submit(workerStart);
      System.out.println("before .isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
      executorService1.shutdown();
  
   try {
       executorService1.shutdown();
       while (!executorService1.isTerminated()) {    
       final Future<String> future = service.take();
       System.out.println("service.take "+future.get());
       System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
       System.out.println();System.out.println();System.out.println();
//       System.out.println("AllRlistB  " + allrListB.toString()  );
        }   
   } catch (ExecutionException | InterruptedException ex) {
   ex.printStackTrace();
   } 
   System.out.println("ny exit");

}
}
