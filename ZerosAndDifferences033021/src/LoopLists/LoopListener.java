/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopLists;

import LoopsLogic.ModuloList;
import MainClass.GaussTable1;
import PArrayReset.PArrayResetFlag;
import PArrayReset.PolynomialArray;
import PArrayReset.ResultListID;
import fractionintegerset.*;                      //* do not need whole library */
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import MainClass.LoopsDriverTwoPManager;
import static java.lang.System.gc;
import static java.lang.Thread.sleep;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.Semaphore;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;




import java.util.logging.Level;
import java.util.logging.Logger;
import mugauss.NewtonInterpolator;

/**  There is only one type of 
 *
 * @author HP_Administrator
 */
public class LoopListener implements PropertyChangeListener {

    LoopList gbList;
    ResultListBean rListB;
    ResultListBean allrListB;
    List muList = new ArrayList();                                 //  just the field not the instance.
    Semaphore muSem = new Semaphore(1);                            //  yep.
    Semaphore listenerRunnableSemaphore = new Semaphore(0);
    ExecutorService exec;
    int rListIndex;
    int updateIncrement;
    int dimension=5;                                               // artifact, is valued in constructor.
    PolynomialArray pArray;
    ModuloList moduloList;
    List deltaList;
    private BigDecimal rnumber = new BigDecimal(1);
    private BigDecimal one = new BigDecimal(1);
    private BigDecimal devides = new BigDecimal(1);
    private BigDecimal wModulo = new BigDecimal(1);
    private BigDecimal rModulo = new BigDecimal(1);
    private List ancestorlist;
    // GaussMain replaced by static NewtonInterpolator calls
    private PolynomialArray copyParray;
    private PolynomialArray figPArray;
    private ResultListBean copyRlistB;
    MatrixA xPowers;
    ResultListID resultListId = new ResultListID();
    int resultListCounter=resultListId.getrListId();;
    private PArrayResetFlag pArrayResetFlag;
    final ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
    ExecutorService executorService1 = Executors.newFixedThreadPool(50);                                       //  get these out of the propertychange method
    CompletionService<String> service = new ExecutorCompletionService<>(executorService1);                      // get these out of the property change method
    ResultListBean cashedrListB; 
    PolynomialArray cashedPArray;    // 
    PolynomialArray cashedFigPArray;
    boolean noBufferRun=false;         // nobuffer worknums custruct with noBufferRun.  defalt is vertical translate will occure.
    int integerRange;
    String muMaskList="";
    
    public LoopListener(ResultListBean allrListB,int dimension,ResultListBean rListB, LoopList gbList, List muList, Semaphore muSem, ExecutorService exec, int rListIndex, PolynomialArray pArray, ModuloList moduloList,List deltaList,List ancestorlist,PolynomialArray figPArray,PArrayResetFlag pArrayResetFlag,ResultListBean cashedrListB,PolynomialArray cashedPArray,PolynomialArray cashedFigPArray,ResultListID resultListId,boolean noBufferRun,int integerRange,String muMaskList ) {
        this.allrListB=allrListB;
        this.dimension=dimension;
        this.gbList = gbList;
        this.muList = muList;
        this.muSem = muSem;
        this.exec = exec;
        this.rListIndex = rListIndex;        //  must rename and refactor.   rListIndex is really the size of rListB.  
        this.pArray = pArray;
        this.moduloList = moduloList;
        this.rListB=rListB;
        this.deltaList=deltaList;
        this.ancestorlist=ancestorlist;
        this.figPArray=figPArray;
        this.pArrayResetFlag=pArrayResetFlag;
//        this.cashedrListB=cashedrListB;
        this.cashedrListB=cashedrListB;
        this.cashedPArray=cashedPArray;
        this.cashedFigPArray=cashedFigPArray;
        this.resultListId=resultListId;
        xPowers=new MatrixA(dimension+1);
        this.noBufferRun=noBufferRun;
        this.integerRange=integerRange;
        this.muMaskList=muMaskList;
        System.out.println("constructor a muMaskList " + muMaskList);
    }



    public LoopListener(ResultListBean allrListB,int dimension,ResultListBean rListB, LoopList gbList, List muList, Semaphore muSem, ExecutorService exec, int rListIndex, PolynomialArray pArray, ModuloList moduloList,List deltaList,List ancestorlist,PolynomialArray figPArray,PArrayResetFlag pArrayResetFlag,ResultListBean cashedrListB,PolynomialArray cashedPArray,PolynomialArray cashedFigPArray,ResultListID resultListId,boolean noBufferRun,int integerRange) {
        this.allrListB=allrListB;
        this.dimension=dimension;
        this.gbList = gbList;
        this.muList = muList;
        this.muSem = muSem;
        this.exec = exec;
        this.rListIndex = rListIndex;        //  must rename and refactor.   rListIndex is really the size of rListB.  
        this.pArray = pArray;
        this.moduloList = moduloList;
        this.rListB=rListB;
        this.deltaList=deltaList;
        this.ancestorlist=ancestorlist;
        this.figPArray=figPArray;
        this.pArrayResetFlag=pArrayResetFlag;
//        this.cashedrListB=cashedrListB;
        this.cashedrListB=cashedrListB;
        this.cashedPArray=cashedPArray;
        this.cashedFigPArray=cashedFigPArray;
        this.resultListId=resultListId;
        xPowers=new MatrixA(dimension+1);
        this.noBufferRun=noBufferRun;
        this.integerRange=integerRange;
        System.out.println("constructor b muMaskList " + muMaskList);
    }



    public LoopListener(ResultListBean allrListB,int dimension,ResultListBean rListB, LoopList gbList, List muList, Semaphore muSem, ExecutorService exec, int rListIndex, PolynomialArray pArray, ModuloList moduloList,List deltaList,List ancestorlist,PolynomialArray figPArray,PArrayResetFlag pArrayResetFlag,ResultListBean cashedrListB,PolynomialArray cashedPArray,PolynomialArray cashedFigPArray,    ResultListID resultListId ) {
        this.allrListB=allrListB;
        this.dimension=dimension;
        this.gbList = gbList;
        this.muList = muList;
        this.muSem = muSem;
        this.exec = exec;
        this.rListIndex = rListIndex;        //  must rename and refactor.   rListIndex is really the size of rListB.  
        this.pArray = pArray;
        this.moduloList = moduloList;
        this.rListB=rListB;
        this.deltaList=deltaList;
        this.ancestorlist=ancestorlist;
        this.figPArray=figPArray;
        this.pArrayResetFlag=pArrayResetFlag;
//        this.cashedrListB=cashedrListB;
        this.cashedrListB=cashedrListB;
        this.cashedPArray=cashedPArray;
        this.cashedFigPArray=cashedFigPArray;
        this.resultListId=resultListId;
        xPowers=new MatrixA(dimension+1);
        System.out.println("constructor c muMaskList " + muMaskList);
    }

  
   public void setRlistB(ResultListBean rListB) {
//    System.out.println("LoopListener.setrlistb.to string                                                        before             " +  this.rListB);  
   this.rListB=rListB;
  //      System.out.println("LoopListener.setrlistb.to string                                                     after             " +  this.rListB);    
   }
    
    private BigDecimal setupdateIncrement(BigDecimal wModulo) {
    //    System.out.println("wModulo: " + wModulo.toString() + " rModulo: " + rModulo.toString());
  //setupdateIncrement(rnumber, n, wModulo)   gbList.setGBList(new muNumDen(modulo,workerNum));
    //   try {
        devides = wModulo.divide(rModulo);
   //     }
   //     catch (ArithmeticException ae) {
   //     System.out.println("            ArithmeticException wModulo: " + wModulo.toString() + " rModulo: " + rModulo.toString());   
   //     }
        return (devides);
    }
   
 
/**
 * 
 *  
 */


private void copyRListBValues(int wNum)  {
    List indexList;
    BigDecimal indexValue;
    int integerRange=15;
  //  System.out.println();
  //  System.out.println("  LoopListener.iterateRlistB:" );
     int sizerListB;
    if (resultListId.gethighestWorker()<wNum) {
    
    cashedrListB.clear();
   sizerListB=rListB.size();
    for (int l=0; l<sizerListB;l++)
    {
        indexList=(ArrayList)rListB.get(l);
        cashedrListB.add(indexList);
        //       for (int m=0; m<integerRange; m++)  {
 //       indexValue=(BigDecimal)indexList.get(m);
 //       ((LoopList)cashedrListB.get(l)).set(m,indexValue);
 //       System.out.println("  rlistb.listindex.value:" + indexValue);
 //       }
    }
     resultListId.sethighestWorker(wNum);
    }
//    System.out.println();
}

private void copyFigPArrayValues(int wNum) {
    int value;
    int iterateRange=dimension;
//   System.out.println("LoopListener.copyFigPArrayValues cashedFigPArry,figParray"+cashedFigPArray.toString()+ "  "+ figPArray.toString());
  
  
    if (resultListId.gethighestWorker()<wNum) {
   for (int l=0; l<wNum;l++) {
     //   value=(int)figPArray.get(l);
        cashedFigPArray.set(l, 0);
    }
   
    for (int l=(wNum+1); l<iterateRange;l++) {
        value=(int)figPArray.get(l);
        cashedFigPArray.set(l, value);
    }
    } 
//    System.out.println("LoopListener.copyFigPArrayValues cshedfigpArray, figParray"+cashedFigPArray.toString()+ "  "+ figPArray.toString()); 
}




private synchronized void pArrayIncrement(PropertyChangeEvent evt)  {
     
//     ResultListBean rListB1 =new ResultListBean();
 int wNum=((muNumDen) evt.getNewValue()).getWorkNum();
 //    System.out.println();
 //    System.out.println("LoopListener.pArrayIncrement pre pArray .to string " + pArray.toString() + " " + "wNum: " + wNum + "  rListIndex: "+ rListIndex);   
     if (!pArrayResetFlag.getPArrayReset()) {pArray.incrementP(rListIndex - 1);}
//    System.out.println("LoopListener.pArrayIncrement post pArray .to string " + pArray.toString() + " " + "wNum: " + wNum + "  rListIndex: "+ rListIndex);     
 //    System.out.println();
     if (pArrayResetFlag.getPArrayReset()) {
  //       System.out.println("                              pArrayResetFlag!!!!!!!!!!!!!!!!!!!!!!!!!");   
         pArrayResetFlag.setPArrayReset();
         copyFigPArrayValues(wNum);
         copyRListBValues(wNum);                                              //        cashedrListB=(ResultListBean)rListB.clone();
         copyFigPArrayValues(wNum);                                           // cashedFigPArray=(PolynomialArray)figPArray.clone();
//         System.out.println("                              pArrayResetFlag!!!!!!!!!!!!!!!!!!!!!!!!cashedFigPArray!"   + cashedFigPArray.toString());   
     }
    }
    
 /**
  * Each value on LoopList arguments figPArray[constant] for a quadratic with roots.
  * it is a hybrid of .appendMuBuffer() and .updateRlistB().
  * @param wZero 
  */
     
 private void verticalTranslateWZero(LoopList wZero ) {
   PolynomialArray figPArrayTranslate=null;
   int wZeroValue;
   int newConstant;
   int targetVTIHigh=LoopsDriverTwoPManager.targetVTIhigh;
//   BigDecimal one= new BigDecimal(1);
   int len=wZero.size();
   int pArrayValue=(int)pArray.get(1);
   int indexRange;
   //if (pArrayValue<len/2){indexRange=pArrayValue+pArrayValue+1;}   //   the iterate the length of looplist value of indexRange.
   //else { indexRange=len; }
   indexRange=pArrayValue+targetVTIHigh;      //   prune the iteration to relevance.
   int translateIndex=-1;        
   figPArrayTranslate=(PolynomialArray)figPArray.clone();
   System.out.println("LoopListener.verticalTranslateWZero figPArrayTranslate: "+figPArrayTranslate.toString());
   System.out.println("LoopListener.verticalTranslateWZero figPArrayTranslate: "+figPArrayTranslate.toString());
   System.out.println("LoopListener.verticalTranslateWZero figPArray: "+figPArray.toString());
 //  System.out.println("LoopListener.verticalTranslateWZero looplist: "+wZero.toString());
   System.out.println("LoopListener.verticalTranslateWZero pArray " + pArray.toString()); 
   System.out.println("LoopListener.verticalTranslateWZero indexRange " + indexRange);
   System.out.println("LoopListener.verticalTranslateWZero wZero.size() " + len);
   System.out.println("LoopListener.verticalTranslateWZero pArrayValue " + pArrayValue);
//  for (int index=0;index<3;index++) {
   for (int index=pArrayValue;index<indexRange;index++) {
 //      resultListCounter=resultListId.getrListId();
       wZeroValue=((BigDecimal)wZero.get(index)).intValue();
       System.out.println("LoopListner.verticalTranslateWZero index: "+index);
       System.out.println("LoopListner.verticalTranslateWZero wZeroValue: "+wZeroValue);
       newConstant=(int)figPArray.get(0)-wZeroValue;
       figPArrayTranslate.set(0, newConstant);
       System.out.println("LoopListner.verticalTranslateWZero newfigPArrayTranslate: "+figPArrayTranslate.toString());
       this.rListB.clear();
       LoopList gbList00 = new LoopList(dimension+1,rListB,dimension,integerRange,0,xPowers,figPArrayTranslate,moduloList);
       //System.out.println("LoopListner.verticalTranslateWZero new looplist: "+gbList00.toString());
       System.out.println("LoopListner.verticalTranslateWZero new looplist: "+gbList00.toString());
       rListB.add(gbList00);
       int halfIR = integerRange / 2;
       NewtonInterpolator.interpolate(new GaussBean1(figPArrayTranslate, pArray.toString(), gbList00, resultListCounter), gbList00, dimension, halfIR, moduloList);
       for (int numerator=1; numerator<=(dimension) ; numerator++) {
          LoopList gbList01 = new LoopList(dimension-(numerator-1),rListB,dimension,integerRange,numerator,xPowers,figPArrayTranslate,moduloList);    
          rListB.add(gbList01);
          NewtonInterpolator.interpolate(new GaussBean1(figPArrayTranslate, pArray.toString(), gbList01, resultListCounter), gbList01, dimension - numerator, halfIR, moduloList);
       }       
       int lenn=rListB.size();
          for (int indexx=0;indexx<lenn;indexx++) {
       synchronized(this) {
             System.out.println("LoopListener.verticalTranslateWZero muList loop pArray: " + pArray.toString());  
        muList.add(new GaussBean1(figPArrayTranslate,pArray.toString(),((LoopList)rListB.get(indexx)),resultListCounter,translateIndex,(int)pArray.get(1)));
       muSem.release();
      }    
   }
   synchronized (muList) {
       muList.notifyAll();
   }
      translateIndex++;
   } 
}    
   
 /*  appendMuBuffer includes muList.wait().  Is .notified in gausstable1 when muList is again managable in size.
 *   
 */
  
 private void appendMuBuffer(boolean noBufferRun) {

   if (muList.size() > 25000) 
   {
       System.out.println("Large mu sleep......");
       try {

           synchronized (muList) {
           muList.wait();
           }
       } catch (InterruptedException ex) {
           Logger.getLogger(LoopListener.class.getName()).log(Level.SEVERE, null, ex);
       }
   }
       
       int len=rListB.size();
   
   for (int index=0;index<len;index++) {
       synchronized(this) {
        muList.add(new GaussBean1(figPArray,pArray.toString(),((LoopList)rListB.get(index)),resultListCounter,-2,(int)pArray.get(1),muMaskList));
       muSem.release();
      }    
   }
   synchronized (muList) {
       muList.notifyAll();
   }
}

 /**  Really  want an executor for the vertical translation.  have one in zad, the origonal.     
  * In java land, if verticalTranslatWzero was in beginning of method, rListB would be senseless later on.
 */    
   private void appendMuBuffer() {
   int len=rListB.size();

   if (LoopsDriverTwoPManager.targetCI==(int)pArray.get(1))  {
   
   for (int index=0;index<len;index++) {
       synchronized(this) {
        muList.add(new GaussBean1(figPArray,pArray.toString(),((LoopList)rListB.get(index)),resultListCounter,-2,(int)pArray.get(1),muMaskList));
       muSem.release();
      }    
   }
   synchronized (muList) {
       muList.notifyAll();
   }
     verticalTranslateWZero(((LoopList)rListB.get(0)));
     
   }
   } 
    
    /**  differences, factors and gausses the .computIndexZero updated figParray.
     *   this is the task for the appendMuBuffer executor..  hopfully rlistb dosn't collect objects.
     */
   
    private synchronized void updateRlistB() {
      this.rListB.clear();
//      resultListCounter=resultListId.getrListId();
//      System.out.println("LoopListener.updateRlistB.figPArray: "+figPArray.toString());

 //    System.out.println("LoopListener.updateRlistB.gbList00: ");
 //    LoopList gbListTest = new LoopList(dimension+1,rListB,dimension,15,0,xPowers,figPArray,moduloList);

//       LoopList gbList00 = new LoopList(dimension+1,rListB,dimension,LoopsDriverTwoPManager.integerRange,0,xPowers,figPArray,moduloList);
//       rListB.add(gbList00);
       rListB.add(new LoopList(dimension+1,rListB,dimension,integerRange,0,xPowers,figPArray,moduloList));
 //      (double[][])((LoopList)rListB.get(0)).getgMatrix()
       
 //        gMain.gauss(dimension,dimension+1,muSem,(double[][])gbListTest.getgMatrix());
      //  this following used to have dimension,dimension+1
 //     gMain.gauss(muList,muSem,dimension+1,dimension+2,(double[][])gbList00.getgMatrix(),new GaussBean1(figPArray, pArray.toString()  ,gbList00,resultListCounter));

 
 int halfIntegerRange = integerRange / 2;
 NewtonInterpolator.interpolate(new GaussBean1(figPArray, pArray.toString(), (LoopList)rListB.get(0), resultListCounter), (LoopList)rListB.get(0), dimension, halfIntegerRange, moduloList);

     for (int numerator=1; numerator<=(dimension) ; numerator++) {

          rListB.add(new LoopList(dimension-(numerator-1),rListB,dimension,integerRange,numerator,xPowers,figPArray,moduloList)); 

          NewtonInterpolator.interpolate(new GaussBean1(figPArray, pArray.toString(), (LoopList)rListB.get(numerator), resultListCounter), (LoopList)rListB.get(numerator), dimension - numerator, halfIntegerRange, moduloList);
     }   
//         System.out.println("LoopListener.updateRlistB  fallthrough."  + rListB.toString());
    }

private synchronized void factorGBList(LoopList transferList,BigDecimal rModulo)  {
    BigDecimal term;
    for (int index=0; index<transferList.size() ; index++) {
    term=(BigDecimal)transferList.get(index);
    try{
    term=term.divide(rModulo);
    transferList.set(index, term);
  }
  catch (ArithmeticException ae){
 }
 }    
}
    
private void copyCashedRListBValues()  {
    LoopList indexList;
    List rIndexList;
    BigDecimal indexValue;
    int integerRange=15;
    int sizeList;
    int sizeRange;
    int rSizeRange;
 //   System.out.println();
 //   System.out.println("  LoopListener.copyCashedRListBValues:" );
    sizeList=cashedrListB.size();
    int rSizeList=rListB.size();
//        System.out.println("  LoopListener.copyCashedRListBValues isizeList,rListBSizelist:: "+sizeList + "  "+ rSizeList);
  rListB.clear();
        for (int l=0; l<(sizeList);l++)
    {
  //   MatrixA testMatrixCopy = new MatrixA(gbList.getAMatrix());
//   LoopList testCopyGBlist = new LoopList(gbList,cashedrListB);
//   System.out.println("cashedrListB: "+ cashedrListB.toString());
        indexList=(LoopList)cashedrListB.get(l);
        rListB.add(new LoopList(indexList,cashedrListB));
        //rIndexList=(ArrayList)rListB.get(l);
       // sizeRange=indexList.size();
       // rSizeRange=rIndexList.size();
  //              System.out.println("  LoopListener.isizeRange, rlistB size range: "+sizeRange + "  " +rSizeRange);
//        for (int m=0; m<sizeRange; m++)  {
//      System.out.println("  LoopListener.l, m: "+l+"  "+m );
//            indexValue=(BigDecimal)indexList.get(m);
//        ((LoopList)rListB.get(l)).set(m,indexValue);
//        System.out.println("  rlistb.listindex.value:" + indexValue);
//        }
    }
//    System.out.println("LoopListener.copyCashedRListBValues(): "+rListB.toString());
}

private void copyCashedFigPArrayValues() {
    int value;
    int iterateRange=cashedFigPArray.size();
    for (int l=0; l<iterateRange;l++) {
//        System.out.println("LoopListener.copyCahedFigPArrayValues l:" +l);
        value=(int)cashedFigPArray.get(l);
        figPArray.set(l, value);
    }
//    System.out.println(" LoopListener.copyCahedFigPArray   figPArray,cashedFigPArray:" +figPArray.toString()+" "+cashedFigPArray.toString());
}


//* .computeIndexZero gets the delta_currentworker polynomial and arguments it with
  /*                pArray_currentworker index value.                                    // Peform the subtraction.
  /*                The result is added to figParray.
  */
     
    private void computeIndexZero (int n,BigDecimal rModulo)  {
     LoopList list = (LoopList) rListB.get(n);
     int degree = dimension - n;
     int halfIntegerRange = integerRange / 2;
     int evalPoint = pArray.getpValue(n);
     int result = NewtonInterpolator.evaluateAt(list, evalPoint, degree, halfIntegerRange, moduloList);
     int intModulo = rModulo.intValue();
     result = result / intModulo;
     figPArray.set(n, figPArray.getpValue(n) - result);
    }

    @Override
 /**
 *  With each gbList in subset 1..workerNumber on rListB  .
 * @author HP_Administrator
 */  
public void propertyChange(PropertyChangeEvent evt) {
//      gc();
//     System.out.println("LoopListener.propertyChange figPArray,cashedFigPArray: "+figPArray.toString()+" "+cashedFigPArray.toString());
       rModulo = (BigDecimal) moduloList.get(((muNumDen)evt.getNewValue()).getWorkNum());  
       int wNum=((muNumDen) evt.getNewValue()).getWorkNum();                                                                     // wNum is now a loop from high to low.
       if (pArrayResetFlag.getPArrayReset()) {                                                                              // loose this condition in recursive.
         pArray.incrementP(rListIndex - 1);
       }
//      System.out.println("LoopListener.propertyChange.wNum, moduloList "+wNum + "   "+ moduloList.toString());
//       for (int wCount=(dimension-1); wCount>0; wCount--) { 
     for (int wCount=(dimension-1); wCount>-1; wCount--) {
//     System.out.println(".propertyChange first for loop.wCount: "+wCount+" ");
//     rModulo=(BigDecimal)moduloList.get(wCount-1);         // origonal, before refactor 2/26/2021
     rModulo=(BigDecimal)moduloList.get(wCount);
     computeIndexZero(wCount,rModulo);
      updateRlistB();                                                 //  it does clear rListB
     } 
      if (noBufferRun) {appendMuBuffer(noBufferRun);} else
     {     appendMuBuffer(); }
     resultListCounter=resultListId.getrListId();
     wModulo = ((muNumDen) evt.getNewValue()).getModulo();
     copyCashedRListBValues();
     copyCashedFigPArrayValues();
     pArrayIncrement(evt);
////     System.out.println("                                                       LoopListener.propertyChnge Exit. "); 
    }
}
