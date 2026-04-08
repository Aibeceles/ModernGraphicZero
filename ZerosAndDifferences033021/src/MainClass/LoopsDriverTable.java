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
import static java.lang.Boolean.TRUE;
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
import twopolynomial.plvManager;
import twopolynomial.rowBean;
import twopolynomial.twoTableDBThread;
import twopolynomial.vertex;

/**  The LoopsDriver lifecycle:  ExecutorService instanciates callable workers representing terms in a cartisian product.
 *                               The CompletionService logs progress till compleation of product.  Then returns.
 *   ResultListBean composed of gbList, the logic output of each worker.
 *   
 *   Have pArray the bounded incrementer recording increments at worker_i
 *   Introducing figPArray the run of zeros represented by zeros at Psi_i, delta_j, k  
 * 
 * @author Aibes
 */                                                                                                 
public class LoopsDriverTable extends Thread {
    
int dimension=2;    
int integerRange=20;     //  The loopListener needs to know this value for its runnable.   
int setProductRange=20;
int vDisplacement=0;
//ResultListID resultListId = new ResultListID();

ExecutorService executorService1 = Executors.newFixedThreadPool(50);    
CompletionService<String> service = new ExecutorCompletionService<>(executorService1);    
Semaphore muSem = new Semaphore(1);
List muList = new ArrayList(); 
Semaphore muSem1 = new Semaphore(1);
List muList1 = new ArrayList();
// GaussMain replaced by static NewtonInterpolator calls
List semaphoreListF = new ArrayList();
List semaphoreListB = new ArrayList();
PolynomialArray pArray = new PolynomialArray(dimension);    // 
PolynomialArray figPArray = new PolynomialArray(dimension,2);
ResultListBean rListB =new ResultListBean();
ResultListBean allrListB =new ResultListBean();
List ancestorlist = new ArrayList();
List deltaList=new ArrayList();
ResultListBean cashedrListB =new ResultListBean();
//ResultListBean cashedrListB;
PolynomialArray cashedPArray = new PolynomialArray(dimension);
//PolynomialArray cashedPArray new PolynomialArray(dimension);     
PolynomialArray cashedFigPArray = new PolynomialArray(dimension,1);
//PolynomialArray cashedFigPArray;
EndProduct endProduct=new EndProduct(); 
//LoopsLogicLoopSemaphore loopsLogic;  //any class that extends AbstractLoopSemaphore
//LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(); 
plvManager mage;
LoopsSemaphoreInterface loopsLogic;
PArrayResetFlag pArrayResetFlag = new PArrayResetFlag();
final ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
//int numeratorLow=0;
//int numeratorHigh=5;
    private BigDecimal NN=null;
    private BigDecimal flatFileRowCounterr=null;
    private BigDecimal nMaxx=null;   //  setters, because of setfields, sould be private???
    private BigDecimal tSeqDB=null;
    private BigDecimal bTermDB=null;
    private BigDecimal targetEvaluateDB=null;
    private vertex vertexDB=null;
    private Semaphore tl1Semaphore;
    private List dbVector11= new ArrayList();


public LoopsDriverTable(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,plvManager mage,Semaphore tl1Semaphore,List dbVector11) {
   this.muList=muList;
   this.muSem=muSem;
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
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


@Override
public void run() {
   int semaphoreListSizeF;int semaphoreListSizeB;
   int switchValue;
   int constant=0,linear=0,square=0;
   MatrixA xPowers = new MatrixA(dimension+1);
   ResultListID resultListId = new ResultListID();
   int resultListCounter=resultListId.getrListId();
   allrListB.add(rListB);
   ModuloList moduloList= new ModuloList(dimension+1);
   long threadId = Thread.currentThread().getId();
   System.out.println("LoopDriverTable , Threadid" + threadId  );
   List deltaTerm=new ArrayList();deltaList.add(deltaTerm);List ancestorlist0 = new ArrayList();ancestorlist.add(ancestorlist0);
    try {
        Thread.sleep(7000);
    } catch (InterruptedException ex) {
        Logger.getLogger(LoopsDriverTable.class.getName()).log(Level.SEVERE, null, ex);
    }
    System.out.println("LoopsDriverTable.runLoopsDriver.dbbVector.size(): "+dbVector11.size()+"  "+muList1.toString());
    System.out.println("LoopsDriverTable.runLoopsDriver.t1.getPermits: "+tl1Semaphore.availablePermits());
    int dbVector11Counter=0;
 //   if ((mage.isAlive()) || (dbVector11.size()>0))
    while ((mage.isAlive()) || (dbVector11.size()>0)) 
    {       
    //             PolynomialArray figTArray = new PolynomialArray(3);
             dbVector11Counter++;
        //            System.out.println("                                                GaussCorrolation while loop " + mangee.isAlive() + " "+dbbVector.size());
             try {
                this.tl1Semaphore.acquire();
  //              System.out.println("                       gauss6able      ");
                if (dbVector11.size()>0) {
                try {
                  System.out.println("LoopsDriverTable rowBean:   " + ((rowBean)dbVector11.get(0)).getNMax());     
                  this.NN=((rowBean)dbVector11.get(0)).getNN();
                  System.out.println("this.NN"+" "+this.NN.toString());
                  this.nMaxx=((rowBean)dbVector11.get(0)).getNMax();
                  System.out.println("this.nMaxx"+" "+this.nMaxx.toString());
                  this.vertexDB=((rowBean)dbVector11.get(0)).getvertex();
                  System.out.println("this.vertexDB"+" "+this.vertexDB.toString()+" "+this.vertexDB.getScalar()+" "+this.vertexDB.getDegree());
                  dbVector11.remove(0);
                } catch (NullPointerException e) {
                System.out.println("                                                                                   dbbVector11 empty"+ e.toString());
                dbVector11.remove(0);
                }
                }
                else {System.out.println("if dbbVector.size()>0 else println   " );}
//                dbbVector.removeElementAt(0);
            } catch (InterruptedException ex) {
                Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
            }
             switchValue=vertexDB.getDegree().intValue();
             switch ( switchValue){
                 case 0:
                     constant=(((vertexDB.getScalar().intValue())/2)+NN.intValue())-vDisplacement;
                     break;
                 case 1:
                     linear=vertexDB.getScalar().intValue()/2;
                     break;
                 case 2:
                     square=vertexDB.getScalar().intValue()/2;
                     break;
             }
             if (dbVector11Counter%3==0) {
             rListB.clear();
             PolynomialArray figTArray = new PolynomialArray(constant,linear,square);                     
             System.out.println("LoopsDriverTable figTArray "+figTArray.toString());
             LoopList gbList00 = new LoopList(dimension+1,rListB,dimension,integerRange,0,xPowers,figTArray, moduloList,TRUE);   //function value list,  all subsequent are differneces of it
             System.out.println("LoopsDriverTable gbList00 "+gbList00.toString());
             rListB.add(gbList00);
             cashedrListB.add(gbList00);
             int halfIntegerRange = integerRange / 2;
             NewtonInterpolator.interpolate(new GaussBean1(figTArray,pArray.toString(),gbList00,resultListCounter), gbList00, dimension, halfIntegerRange, moduloList);


   semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0)); semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));
   semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();    
 //  Callable workerStart = new GenerateBinaryStart((Semaphore)semaphoreListF.get(1));    // is last service.submit call in LoopsDriver
   List deltaTerm1=new ArrayList();deltaList.add(deltaTerm1);List ancestorlist1 = new ArrayList();ancestorlist.add(ancestorlist1);
          
             LoopList gbList = new LoopList(dimension,rListB,dimension,integerRange,1,xPowers,figPArray, moduloList);
             rListB.add(gbList);
             cashedrListB.add(gbList);
             NewtonInterpolator.interpolate(new GaussBean1(figPArray,pArray.toString(),gbList,resultListCounter), gbList, dimension - 1, halfIntegerRange, moduloList);

    LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(gbList,0,integerRange,pArray); LoopsConditionInterface loopsCondition = new LoopsLogicLoopCondition();
   gbList.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId)); 
   Callable workerZero = new LoopSemaphoreInitial((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic,loopsCondition,setProductRange); 
   service.submit(workerZero);
            
             
             
             
                int len=rListB.size();
//   System.out.println("LoopListener.appendMuBuffer pArray " + pArray.toString() + " " + "figPArray: " + figPArray.toString()+ "  rListIndex: "+ rListIndex);  
   System.out.println("LoopListener.appendMuBuffer pArray " + pArray.toString() + " " + "figPArray: " + figPArray.toString());  

   for (int index=0;index<len;index++) {
       GaussBean1 gB1=new GaussBean1(figTArray,pArray.toString(),((LoopList)rListB.get(index)),resultListCounter); 
       synchronized(this) {   //latest thread issue  numerator should be 1 (the first element on rListB
       muList.add(new GaussBean1(figTArray,pArray.toString(),((LoopList)rListB.get(index)),resultListCounter));
       //  System.out.println("  new gauss bean    new gauss bean");
       muSem.release();
      }    
   }
        resultListCounter=resultListId.getrListId();          
             
      }
//     Callable workerStart = new GenerateBinaryStart((Semaphore)semaphoreListF.get(1));           
//   int semaphoreListSizeF;int semaphoreListSizeB;
//   semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0)); semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));
//   semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();    
//   Callable workerStart = new GenerateBinaryStart((Semaphore)semaphoreListF.get(1));    // is last service.submit call in LoopsDriver
//   List deltaTerm1=new ArrayList();deltaList.add(deltaTerm1);List ancestorlist1 = new ArrayList();ancestorlist.add(ancestorlist1);
  
//   LoopList gbList = new LoopList(dimension,rListB,dimension,integerRange,1,xPowers,figPArray, moduloList);
//   rListB.add(gbList);
//   cashedrListB.add(gbList);
//   gMain.gauss(muList,muSem,dimension,dimension+1,(double[][])gbList.getgMatrix(),new GaussBean1(figPArray.toString(),pArray.toString(),gbList,resultListCounter));
   
//   LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(gbList,0,integerRange,pArray); LoopsConditionInterface loopsCondition = new LoopsLogicLoopCondition();
//   gbList.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId)); 
//   Callable workerZero = new LoopSemaphoreInitial((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic,loopsCondition,setProductRange); 
//   service.submit(workerZero);

//   for (int numerator=2; numerator<dimension ; numerator++) {
//      semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
//      System.out.println("LoopsDriver rListB.size()+1 "+rListB.size()+1);
//      List deltaTerm2=new ArrayList();deltaList.add(deltaTerm2);List ancestorlist2 = new ArrayList();ancestorlist.add(ancestorlist2);
//      MatrixA xPowers2 = new MatrixA(dimension,dimension+2,xPowers);      //looplist constructs its owne MatrixA's, and avoides schyronization problems
 
//      LoopList gbList1 = new LoopList(dimension-(numerator-1),rListB,dimension,integerRange,numerator,xPowers,figPArray, moduloList);
//      rListB.add(gbList1);
//      cashedrListB.add(gbList1);
//      gMain.gauss(muList,muSem,dimension-(numerator-1),dimension-(numerator-1)+1,(double[][])gbList1.getgMatrix(),new GaussBean1(figPArray.toString(),pArray.toString(),gbList1,resultListCounter));

//     LoopsSemaphoreInterface loopsLogic1= new LoopsLogicLoopSemaphore(gbList1,numerator-1,integerRange,pArray);LoopsConditionInterface loopsCondition1 = new LoopsLogicLoopCondition();
//      gbList1.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList1,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId));
//      Callable worker = new LoopSemaphore((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic1,loopsCondition1,setProductRange); 
//      service.submit(worker); 
//  }
//      semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
//      semaphoreListSizeF=semaphoreListF.size(); semaphoreListSizeB=semaphoreListB.size();
//      List deltaTerm3=new ArrayList();deltaList.add(deltaTerm3);List ancestorlist3 = new ArrayList();ancestorlist.add(ancestorlist3);
//      LoopList gbList2 = new LoopList(0,rListB,dimension,integerRange,dimension,deltaList,ancestorlist,cashedrListB,xPowers);
///      rListB.add(gbList2);
//      cashedrListB.add(gbList2);
//      LoopsSemaphoreInterface loopsLogic2= new LoopsLogicLoopSemaphore(gbList2,dimension-1,integerRange,pArray);LoopsConditionInterface loopsCondition2 = new LoopsLogicLoopCondition();
//      gbList2.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList2,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId));
//      Callable workerLast = new LoopSemaphoreLast((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic2,loopsCondition2,setProductRange); 
//      service.submit(workerLast);
//      System.out.println("LoopDriver.run pArray .to string " + pArray.toString());
//      System.out.println("LoopDriver.run figPArray .to string " + figPArray.toString());
//      System.out.println("LoopDriver.run rListB .to string " + rListB.toString());
//      pArray.addPropertyChangeListener(new PArrayResetLIstener(pArray,rListB,pArrayResetFlag,figPArray,dimension,moduloList,muList,muSem,cashedrListB,cashedPArray,cashedFigPArray));
//      Callable workerStart = new GenerateBinaryStart((Semaphore)semaphoreListF.get(1));    
//      service.submit(workerStart);
//      System.out.println("before .isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
//      executorService1.shutdown();
  
//   try {
//       executorService1.shutdown();
//       while (!executorService1.isTerminated()) {    
//       final Future<String> future = service.take();
//       System.out.println("service.take "+future.get());
//       System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
//       System.out.println();System.out.println();System.out.println();
//       System.out.println("AllRlistB  " + allrListB.toString()  );
//        }   
//   } catch (ExecutionException | InterruptedException ex) {
//   ex.printStackTrace();
//   } 
   System.out.println("ny exit");

}
}
}
