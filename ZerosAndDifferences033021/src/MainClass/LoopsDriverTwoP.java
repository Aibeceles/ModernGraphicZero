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
import twopolynomial.vertex;
import twopolynomial.rowBean;

/**  The LoopsDriver lifecycle:  ExecutorService instanciates callable workers representing terms in a cartisian product.
 *                               The CompletionService logs progress till compleation of product.  Then returns.
 *   ResultListBean composed of gbList, the logic output of each worker.
 *   
 *   Have pArray the bounded incrementer recording increments at worker_i
 *   Introducing figPArray the run of zeros represented by zeros at Psi_i, delta_j, k  
 * 
 * @author Aibes
 */                                                                                                 
// public class LoopsDriverTwoP extends Thread {
public class LoopsDriverTwoP implements Callable<String> {
    
int dimension=5;              //  constructor will override this value in nobufferrun.  
int integerRange=200;         //  constructor will override this value in nobufferrun.    
int setProductRange=4;        //  constructor will override this value in nobufferrun.
int lbSetProductRange=0;      //  constructor will override this value in nobufferrun.
int pArrayResetValue=0;       //  constructor will override this value in nobufferrun.
//int setProductRange=20;
int vDisplacement=0;
int workerCounter=0;
boolean noBufferRun=false;    //  constructor will override this value in nobufferrun.
//ResultListID resultListId = new ResultListID();

ExecutorService executorService1 = Executors.newFixedThreadPool(50);    
CompletionService<String> service = new ExecutorCompletionService<>(executorService1);    
Semaphore muSem = new Semaphore(1);
List muList = new ArrayList();
List muMaskList = new ArrayList();
List semaphoreListF = new ArrayList();
List semaphoreListB = new ArrayList();


PolynomialArray pArray = null;    // 
PolynomialArray figPArray = null;  // the second field variable in figPArray is the leading P scalar.
PolynomialArray cashedFigPArray =  null;


//  PolynomialArray pArray = new PolynomialArray(dimension);    // 
//  PolynomialArray figPArray = new PolynomialArray(dimension,2);  // the second field variable in figPArray is the leading P scalar.
//   PolynomialArray cashedFigPArray = new PolynomialArray(dimension,1);

ResultListBean rListB =new ResultListBean();
ResultListBean allrListB =new ResultListBean();
List ancestorlist = new ArrayList();
List deltaList=new ArrayList();
ResultListBean cashedrListB =new ResultListBean();
//ResultListBean cashedrListB;
PolynomialArray cashedPArray = new PolynomialArray(dimension);
//PolynomialArray cashedPArray new PolynomialArray(dimension);     

//PolynomialArray cashedFigPArray;
EndProduct endProduct=new EndProduct(); 
//LoopsLogicLoopSemaphore loopsLogic;  //any class that extends AbstractLoopSemaphore
//LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(); 
LoopsSemaphoreInterface loopsLogic;
PArrayResetFlag pArrayResetFlag = new PArrayResetFlag();
final ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
//int numeratorLow=0;
//int numeratorHigh=5;
plvManager mage;
    private BigDecimal NN=null;
    private BigDecimal flatFileRowCounterr=null;
    private BigDecimal nMaxx=null;   //  setters, because of setfields, sould be private???
    private BigDecimal tSeqDB=null;
    private BigDecimal bTermDB=null;
    private BigDecimal targetEvaluateDB=null;
    private vertex vertexDB=null;
    private Semaphore tl1Semaphore;
    private List dbVector11= new ArrayList();
    private int nMax;
    private  ResultListID resultListId=null;
    
    
Semaphore muSem1 = new Semaphore(1);
List muList1 = new ArrayList();


public LoopsDriverTwoP(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic) {
   this.muList=muList;
   this.muSem=muSem;
   this.loopsLogic=loopsLogic;
}

public LoopsDriverTwoP(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,plvManager mage,Semaphore tl1Semaphore,List dbVector11,PolynomialArray figPArray,PolynomialArray cashedFigPArray,PolynomialArray pArray,int nMaxx,ResultListID resultListId) {
   this.muList=muList;
   this.muSem=muSem;
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
   this.figPArray=figPArray;
   this.cashedFigPArray=(PolynomialArray)figPArray.clone();
   this.pArray=pArray;
   this.nMax=nMaxx;
   this.resultListId=resultListId;
}

public LoopsDriverTwoP(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,plvManager mage,Semaphore tl1Semaphore,List dbVector11,PolynomialArray figPArray,PolynomialArray cashedFigPArray,PolynomialArray pArray,int nMaxx,ResultListID resultListId,int setProductRange) {
   this.muList=muList;
   this.muSem=muSem;
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
   this.figPArray=figPArray;
   this.cashedFigPArray=(PolynomialArray)figPArray.clone();
   this.pArray=pArray;
   this.nMax=nMaxx;
   this.resultListId=resultListId;
   this.setProductRange=setProductRange;
}

public LoopsDriverTwoP(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,plvManager mage,Semaphore tl1Semaphore,List dbVector11,PolynomialArray figPArray,PolynomialArray cashedFigPArray,PolynomialArray pArray,int nMaxx,ResultListID resultListId,int setProductRange,boolean noBufferRun,int dimension,int integerRange) {
   this.muList=muList; 
   this.muSem=muSem;   
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
   this.figPArray=figPArray;
   this.cashedFigPArray=(PolynomialArray)figPArray.clone();
   this.pArray=pArray;
   this.nMax=nMaxx;
   this.resultListId=resultListId;
   this.setProductRange=setProductRange;
   this.noBufferRun=noBufferRun;
   this.dimension=dimension;    
   this.integerRange=integerRange;
}

public LoopsDriverTwoP(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,plvManager mage,Semaphore tl1Semaphore,List dbVector11,PolynomialArray figPArray,PolynomialArray cashedFigPArray,PolynomialArray pArray,int nMaxx,ResultListID resultListId,int setProductRange,boolean noBufferRun,int dimension,int integerRange,int lbSetProductRange) {
   this.muList=muList; 
   this.muSem=muSem;   
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
   this.figPArray=figPArray;
   this.cashedFigPArray=(PolynomialArray)figPArray.clone();
   this.pArray=pArray;
   this.nMax=nMaxx;
   this.resultListId=resultListId;
   this.setProductRange=setProductRange;
   this.noBufferRun=noBufferRun;
   this.dimension=dimension;    
   this.integerRange=integerRange;
   this.lbSetProductRange=lbSetProductRange;
}

public LoopsDriverTwoP(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,plvManager mage,Semaphore tl1Semaphore,List dbVector11,PolynomialArray figPArray,PolynomialArray cashedFigPArray,PolynomialArray pArray,int nMaxx,ResultListID resultListId,int setProductRange,boolean noBufferRun,int dimension,int integerRange,int lbSetProductRange, int pArrayResetValue) {
   this.muList=muList; 
   this.muSem=muSem;   
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
   this.figPArray=figPArray;
   this.cashedFigPArray=(PolynomialArray)figPArray.clone();
   this.pArray=pArray;
   this.nMax=nMaxx;
   this.resultListId=resultListId;
   this.setProductRange=setProductRange;
   this.noBufferRun=noBufferRun;
   this.dimension=dimension;    
   this.integerRange=integerRange;
   this.lbSetProductRange=lbSetProductRange;
   this.pArrayResetValue=pArrayResetValue;
}

public LoopsDriverTwoP(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,Semaphore tl1Semaphore,List dbVector11,PolynomialArray figPArray,PolynomialArray cashedFigPArray,PolynomialArray pArray,int nMaxx,ResultListID resultListId,int setProductRange,boolean noBufferRun,int dimension,int integerRange,int lbSetProductRange, int pArrayResetValue) {
   this.muList=muList; 
   this.muSem=muSem;   
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
//   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
   this.figPArray=figPArray;
   this.cashedFigPArray=(PolynomialArray)figPArray.clone();
   this.pArray=pArray;
   this.nMax=nMaxx;
   this.resultListId=resultListId;
   this.setProductRange=setProductRange;
   this.noBufferRun=noBufferRun;
   this.dimension=dimension;    
   this.integerRange=integerRange;
   this.lbSetProductRange=lbSetProductRange;
   this.pArrayResetValue=pArrayResetValue;
}

public LoopsDriverTwoP(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,Semaphore tl1Semaphore,List dbVector11,PolynomialArray figPArray,PolynomialArray cashedFigPArray,PolynomialArray pArray,int nMaxx,ResultListID resultListId,int setProductRange,boolean noBufferRun,int dimension,int integerRange,int lbSetProductRange, int pArrayResetValue, ArrayList muMaskList) {
//   this.muList=muList;     //  The skip pattern does not use these variables.
//   this.muSem=muSem;       // 
   this.muList=dbVector11;
   this.muSem=tl1Semaphore;

   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
//   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
   this.figPArray=figPArray;
   this.cashedFigPArray=(PolynomialArray)figPArray.clone();
   this.pArray=pArray;
   this.nMax=nMaxx;
   this.resultListId=resultListId;
   this.setProductRange=setProductRange;
   this.noBufferRun=noBufferRun;
   this.dimension=dimension;    
   this.integerRange=integerRange;
   this.lbSetProductRange=lbSetProductRange;
   this.pArrayResetValue=pArrayResetValue;
   this.muMaskList=muMaskList;
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


private void setpArrayFigpArray()  {
    
    
}

/*
*    .run() depricated.  Functionality is refactored into LoopsDriverTwoPManager.run.
*/


public void run() {
   int switchValue;
   int constant=0,linear=0,square=0;
    int dbVector11Counter=0;
 //   if ((mage.isAlive()) || (dbVector11.size()>0))
   try {
        Thread.sleep(7000);
    } catch (InterruptedException ex) {
        Logger.getLogger(LoopsDriverTable.class.getName()).log(Level.SEVERE, null, ex);
    }
//  while ((mage.isAlive()) || (dbVector11.size()>0)) 
    {       
    //             PolynomialArray figTArray = new PolynomialArray(3);
             dbVector11Counter++;
        //            System.out.println("                                                GaussCorrolation while loop " + mangee.isAlive() + " "+dbbVector.size());
             try {
                this.tl1Semaphore.acquire();
  //              System.out.println("                       gauss6able      ");
                if (dbVector11.size()>0) {
                try {
                  System.out.println("LoopsDriverTwoP rowBean:   " + ((rowBean)dbVector11.get(0)).getNMax());     
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
                Logger.getLogger(LoopsDriverTwoP.class.getName()).log(Level.SEVERE, null, ex);
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
                figPArray = new PolynomialArray(constant,linear,square);
                cashedFigPArray = new PolynomialArray(constant,linear,square);
                //               figPArray = new PolynomialArray(96,-2,2);
                 pArray = new PolynomialArray(dimension,TRUE);    // 
               
                
                 
                 
                System.out.println("LoopsDriverTwoP.run.figPArray: "+figPArray.toString());
       //        if (dbVector11Counter==6) { zadTwoP(); }
             }
    }         
}


@Override
public String call() {
//   resultListId = new ResultListID(56);
   System.out.println("Dimension "+dimension);
   MatrixA xPowers = new MatrixA(dimension+1);
   int resultListCounter=resultListId.getrListId();
   allrListB.add(rListB);
   ModuloList moduloList= new ModuloList(dimension+1);
//                              System.out.println("                                   moduloList:"+moduloList.toString());
   long threadId = Thread.currentThread().getId();
   System.out.println("LoopDriverTwoP.zadTwoP , Threadid" + threadId  );
   List deltaTerm=new ArrayList();deltaList.add(deltaTerm);List ancestorlist0 = new ArrayList();ancestorlist.add(ancestorlist0);
   System.out.println("LoopDriverTwoP , pre gList00" );
//                              //   figPArray.set(0,96 );figPArray.set(1,-2);figPArray.set(2,2);  // testing gauss.main()
//                                     LoopList gbList00 = new LoopList(dimension+1,rListB,dimension,integerRange,0,xPowers,figPArray, moduloList);   //function value list,  all subsequent are differneces of it
//                                     rListB.add(gbList00);
//                                     cashedrListB.add(gbList00);
//                                     LoopList.printMatrix( ((LoopList)rListB.get(0)).getgMatrix()    );
//                                     GaussBean1 gBean00=new GaussBean1(figPArray,pArray.toString(),gbList00,resultListCounter,true,((BigDecimal)gbList00.get(nMax)).toString());
//                                     gMain.gauss(muList,muSem,dimension+1,dimension+2,(double[][])gbList00.getgMatrix(),gBean00);
//                                     LoopList.printMatrix( ((LoopList)rListB.get(0)).getgMatrix()    );
//                                     synchronized(this) {   //latest thread issue  numerator should be 1 (the first element on rListB
//                                     muList.add(gBean00);
//                                     //  System.out.println("  new gauss bean    new gauss bean");
//                                     muSem.release();
//                                     }
   int semaphoreListSizeF;int semaphoreListSizeB;
   semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0)); semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));
   semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();    
   Callable workerStart = new GenerateBinaryStart((Semaphore)semaphoreListF.get(1));    // is last service.submit call in LoopsDriver
   List deltaTerm1=new ArrayList();deltaList.add(deltaTerm1);List ancestorlist1 = new ArrayList();ancestorlist.add(ancestorlist1);
    System.out.println("LoopDriverTwoP , pre gList" ); 
// LoopList gbList = new LoopList(dimension,rListB,dimension,integerRange,1,xPowers,figPArray, moduloList);  refacotor attempt 2/25/2021
    System.out.println("LoopDriverTwoP , 6/27/2021 first figParray" + figPArray.toString() ); 
//   LoopList gbList = new LoopList(dimension,rListB,dimension,integerRange,0,xPowers,figPArray, moduloList);   //  pre 6/27/2021  
   LoopList gbList = new LoopList(dimension+1,rListB,dimension,integerRange,0,xPowers,figPArray, moduloList);  // 6/27/2021 test

   rListB.add(gbList);
   cashedrListB.add(gbList);
//   new GaussBean1(figPArray,pArray.toString(),((LoopList)rListB.get(index)),resultListCounter,-2,(int)pArray.get(1),muMaskList)  looplistener.appendmubuffer
   int halfIntegerRange = integerRange / 2;
   GaussBean1 gBean=new GaussBean1(figPArray,pArray.toString(),gbList,resultListCounter,muMaskList.toString());
   NewtonInterpolator.interpolate(gBean, gbList, dimension - 1, halfIntegerRange, moduloList);
   synchronized(this) {   //latest thread issue  numerator should be 1 (the first element on rListB
       muList.add(gBean);
       //  System.out.println("  new gauss bean    new gauss bean");
       muSem.release();
      }
   LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(gbList,0,integerRange,pArray); LoopsConditionInterface loopsCondition = new LoopsLogicLoopCondition();
   gbList.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId,noBufferRun,integerRange,muMaskList.toString())); 
   Callable workerZero = new LoopSemaphoreInitial((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic,loopsCondition,setProductRange,lbSetProductRange,(boolean)muMaskList.get(0)); 
   service.submit(workerZero);workerCounter++;
   System.out.println("LoopDriverTwoP , pre gList1" );
//   dimension=dimension-1;
   for (int numerator=1; numerator<dimension ; numerator++) {
      semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
      System.out.println("LoopsDriverTwoP inside for loop                                               rListB.size()+1 "+rListB.size()+1);
      List deltaTerm2=new ArrayList();deltaList.add(deltaTerm2);List ancestorlist2 = new ArrayList();ancestorlist.add(ancestorlist2);
      MatrixA xPowers2 = new MatrixA(dimension,dimension+2,xPowers);      //looplist constructs its owne MatrixA's, and avoides schyronization problems
     System.out.println("LoopDriverTwoP , 6/27/2021" + numerator + " figParray" + figPArray.toString() ); 
      LoopList gbList1 = new LoopList(dimension-(numerator-1),rListB,dimension,integerRange,numerator,xPowers,figPArray, moduloList);

  //    if (numerator<dimension) {    // custom condition otherwise if not running in ldtp, no condition
      rListB.add(gbList1);
      cashedrListB.add(gbList1);
  //    }
      GaussBean1 gBean1=new GaussBean1(figPArray,pArray.toString(),gbList1,resultListCounter,muMaskList.toString());
      NewtonInterpolator.interpolate(gBean1, gbList1, dimension - numerator, halfIntegerRange, moduloList);
      synchronized(this) {   //latest thread issue  numerator should be 1 (the first element on rListB
       muList.add(gBean1);
       //  System.out.println("  new gauss bean    new gauss bean");
       muSem.release();
      }
      
//      LoopsSemaphoreInterface loopsLogic1= new LoopsLogicLoopSemaphore(gbList1,numerator-1,integerRange,pArray);LoopsConditionInterface loopsCondition1 = new LoopsLogicLoopCondition();
  LoopsSemaphoreInterface loopsLogic1= new LoopsLogicLoopSemaphore(gbList1,numerator,integerRange,pArray);LoopsConditionInterface loopsCondition1 = new LoopsLogicLoopCondition();
      gbList1.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList1,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId,noBufferRun,integerRange,muMaskList.toString()));
      Callable worker = new LoopSemaphore((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic1,loopsCondition1,setProductRange,lbSetProductRange,(boolean)muMaskList.get(numerator)); 
      service.submit(worker);workerCounter++; 
  }
      semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
      semaphoreListSizeF=semaphoreListF.size(); semaphoreListSizeB=semaphoreListB.size();
      List deltaTerm3=new ArrayList();deltaList.add(deltaTerm3);List ancestorlist3 = new ArrayList();ancestorlist.add(ancestorlist3);
      System.out.println("LoopDriverTwoP , pre gList2" );
      LoopList gbList2 = new LoopList(1,rListB,dimension,integerRange,dimension,deltaList,ancestorlist,cashedrListB,xPowers);
      rListB.add(gbList2);
      cashedrListB.add(gbList2);
      GaussBean1 gBean2=new GaussBean1(figPArray,pArray.toString(),gbList2,resultListCounter,muMaskList.toString());
      NewtonInterpolator.interpolate(gBean2, gbList2, 0, halfIntegerRange, moduloList);
    synchronized(this) {   //latest thread issue  numerator should be 1 (the first element on rListB
       muList.add(gBean2);
       //  System.out.println("  new gauss bean    new gauss bean");
       muSem.release();
      }
   
      
      
//      LoopsSemaphoreInterface loopsLogic2= new LoopsLogicLoopSemaphore(gbList2,dimension-1,integerRange,pArray);LoopsConditionInterface loopsCondition2 = new LoopsLogicLoopCondition();
 LoopsSemaphoreInterface loopsLogic2= new LoopsLogicLoopSemaphore(gbList2,dimension,integerRange,pArray);LoopsConditionInterface loopsCondition2 = new LoopsLogicLoopCondition();

      gbList2.addPropertyChangeListener(new LoopListener(allrListB,dimension,rListB,gbList2,muList,muSem,Executors.newFixedThreadPool(rListB.size()),rListB.size(),pArray,moduloList,deltaList,ancestorlist,figPArray,pArrayResetFlag,cashedrListB,cashedPArray,cashedFigPArray,resultListId,noBufferRun,integerRange));  // workerLast does not need the boolean, it is using the ususal constructor
      Callable workerLast = new LoopSemaphoreLast((Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,loopsLogic2,loopsCondition2,setProductRange,lbSetProductRange); 
      service.submit(workerLast);workerCounter++;
      System.out.println("LoopDriverTwoP.zadTwoP pArray .to string " + pArray.toString());
      System.out.println("LoopDriverTwoP.zadTwoP figPArray .to string " + figPArray.toString());
      System.out.println("LoopDriverTwoP.zadTwoP rListB .to string " + rListB.toString());
      pArray.addPropertyChangeListener(new PArrayResetLIstener(pArray,rListB,pArrayResetFlag,figPArray,dimension,moduloList,muList,muSem,cashedrListB,cashedPArray,cashedFigPArray,pArrayResetValue));
      service.submit(workerStart);workerCounter++;
      System.out.println("before .isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
  //    executorService1.shutdown();
  
   try {
       executorService1.shutdown();
   
       while (workerCounter>0)   {
//       while (!executorService1.isTerminated()) {    
       final Future<String> future = service.take();
       System.out.println("service.take "+future.get());
       System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
       System.out.println();System.out.println();System.out.println();
       workerCounter--;
//       System.out.println("AllRlistB  " + allrListB.toString()  );
        }   
   } catch (ExecutionException | InterruptedException ex) {
   ex.printStackTrace();
   } 
  
   
   System.out.println(".LoopsDriverTwoP fall throug,  resultCounter: " + resultListId.getrListId());
   return("LoopsDriverTwoP");
}
}
