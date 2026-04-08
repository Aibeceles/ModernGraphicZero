/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PArrayReset;

import LoopLists.GaussBean1;
import LoopLists.LoopList;
import LoopLists.MatrixA;
import LoopsLogic.ModuloList;
import fractionintegerset.ResultListBean;
import fractionintegerset.muNumDen;
import java.beans.*;
import java.io.Serializable;
import static java.lang.System.gc;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import mugauss.NewtonInterpolator;

/**
 *
 * @author Aibes
 */
public class PArrayResetLIstener implements PropertyChangeListener {
    
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    int rListIndex;                                                          // is rListB.size from driver,   really computed as workerNum
    private String sampleProperty;
    private PolynomialArray pArray;
    ResultListBean rListB;
    ResultListID resultListId = new ResultListID();
    private PropertyChangeSupport propertySupport;
    private PArrayResetFlag pArrayResetFlag;
    private PolynomialArray figPArray;
    private int dimension;
    private  ModuloList moduloList;
    private Semaphore muSem = new Semaphore(1);
    private List muList = new ArrayList(); 
    int resultListCounter;
    MatrixA xPowers = new MatrixA(dimension+1);
    ResultListBean cashedrListB = new ResultListBean();
PolynomialArray cashedPArray = new PolynomialArray(dimension);    // 
PolynomialArray cashedFigPArray = new PolynomialArray(dimension,1);
int pArrayResetValue=0;    
    
    public PArrayResetLIstener() {
        propertySupport = new PropertyChangeSupport(this);
    }
    
    public PArrayResetLIstener(PolynomialArray pArray, ResultListBean rListB, PArrayResetFlag pArrayResetFlag,PolynomialArray figPArray,int dimension,ModuloList moduloList,List muList,Semaphore muSem,ResultListBean cashedrListB,PolynomialArray cashedPArray,PolynomialArray cashedFigPArray) {
        this.pArray=pArray;
        this.figPArray=figPArray;
        this.rListB=rListB;
        this.pArrayResetFlag=pArrayResetFlag;
        this.dimension=dimension;
        this.moduloList=moduloList;
        this.cashedrListB=cashedrListB;
        this.cashedFigPArray=cashedFigPArray;
        this.cashedPArray=cashedPArray;
        propertySupport = new PropertyChangeSupport(this);
        
    }
    
    public PArrayResetLIstener(PolynomialArray pArray, ResultListBean rListB, PArrayResetFlag pArrayResetFlag,PolynomialArray figPArray,int dimension,ModuloList moduloList,List muList,Semaphore muSem,ResultListBean cashedrListB,PolynomialArray cashedPArray,PolynomialArray cashedFigPArray, int pArrayResetValue) {
        this.pArray=pArray;
        this.figPArray=figPArray;
        this.rListB=rListB;
        this.pArrayResetFlag=pArrayResetFlag;
        this.dimension=dimension;
        this.moduloList=moduloList;
        this.cashedrListB=cashedrListB;
        this.cashedFigPArray=cashedFigPArray;
        this.cashedPArray=cashedPArray;
        this.pArrayResetValue=pArrayResetValue;
        propertySupport = new PropertyChangeSupport(this);
        
    }
    
    
    
    
    public String getSampleProperty() {
        return sampleProperty;
    }
    
    public void setSampleProperty(String value) {
        String oldValue = sampleProperty;
        sampleProperty = value;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
       private synchronized void pArrayIncrement(PropertyChangeEvent evt)  {
     
//     ResultListBean rListB1 =new ResultListBean();
 int wNum=((muNumDen) evt.getNewValue()).getWorkNum();
     System.out.println();
 //    System.out.println("LoopListener.pArrayIncrement pArray .to string " + pArray.toString() + " " + "wNum: " + wNum + "  rListIndex: "+ rListIndex);   
     pArray.incrementP(rListIndex - 2);
     System.out.println("LoopListener.pArrayIncrement pArray .to string " + pArray.toString() + " " + "wNum: " + wNum + "  rListIndex: "+ rListIndex);     
     System.out.println();
     
     if (pArrayResetFlag.getPArrayReset()) {
           System.out.println("                              pArrayResetFlag!!!!!!!!!!!!!!!!!!!!!!!!!");   
         pArrayResetFlag.setPArrayReset();
     }

//     setRlistB(rListB1); 
//     System.out.println("LoopListener.pArrayIncrement exit "+ "wNum: " + wNum + "  rListIndex: "+ rListIndex);
//         synchronized (rListB) {
//             System.out.println("LoopListener.pArrayIncrement new rListB .to string " + rListB.toString() + " " + "rListIndex: " + rListIndex);
//         }  
    }
    
    private void computeIndexZero (int n)  {
     LoopList list = (LoopList) rListB.get(n+1);
     int degree = dimension - n - 1;
     int halfIR = 15 / 2;
     int evalPoint = pArray.getpValue(n);
     int result = NewtonInterpolator.evaluateAt(list, evalPoint, degree, halfIR, moduloList);
     figPArray.set(n, figPArray.getpValue(n) - result);
    }
    
    /**  differences, factors and gausses the .computIndexZero updated figParray.
     * 
     */
    
    
    private synchronized void updateRlistB() {
      this.rListB.clear();
      resultListCounter=resultListId.getrListId();
   
      LoopList gbList00 = new LoopList(dimension+1,rListB,dimension,15,0,xPowers,figPArray);
 //      System.out.println("UpdateRlistB before gauss");
 //     LoopList.printMatrix(gbList00.getgMatrix());
      int halfIntegerRange = 15 / 2;
      NewtonInterpolator.interpolate(new GaussBean1(figPArray,pArray.toString(),gbList00,resultListCounter), gbList00, dimension - 1, halfIntegerRange, moduloList);
 //     System.out.println("UpdateRlistB after gauss");
 //     LoopList.printMatrix(gbList00.getgMatrix());
      for (int numerator=1; numerator<(dimension) ; numerator++) {
  
          LoopList gbList01 = new LoopList(dimension-(numerator-1),rListB,dimension,15,numerator,xPowers,figPArray);    
      factorGBList(gbList01,(BigDecimal) moduloList.get(numerator-1));
//      System.out.println("UpdateRlistB before gauss");
//      LoopList.printMatrix(gbList01.getgMatrix());
      NewtonInterpolator.interpolate(new GaussBean1(figPArray,pArray.toString(),gbList01,resultListCounter), gbList01, dimension - numerator, halfIntegerRange, moduloList);
//      System.out.println("UpdateRlistB after gauss");
//      LoopList.printMatrix(gbList01.getgMatrix());
     }   
   }
// gMain.gauss(muList,muSem,dimension-(numerator-1),dimension-(numerator-1)+1,(double[][])gbList1.getgMatrix(),new GaussBean1(pArray.toString(),gbList1.toString(),numerator,resultListCounter));


private synchronized void factorGBList(LoopList transferList,BigDecimal rModulo)  {
    BigDecimal term;
//    System.out.println("LoopListenerRunnable.factor transferList, transferList.size(): " + transferList.toString()+ "  " + transferList.size());
    for (int index=0; index<transferList.size() ; index++) {
    term=(BigDecimal)transferList.get(index);
//    System.out.println("term:" + term);
//     term=term.add(modulo);
  try{
    term=term.divide(rModulo);
    transferList.set(index, term);
//    System.out.println("   termed:" + term + " index: "+index);
  }
  catch (ArithmeticException ae){
//     System.out.println("   term.divide exception: " + ae.getMessage() );
  }
    }    
    
}
    
private void copyCashedRListBValues()  {
    List indexList;
    List rIndexList;
    BigDecimal indexValue;
    int integerRange=15;
    int sizeList;
    int sizeRange;
    int rSizeRange;
    System.out.println();
    System.out.println("  LoopListener.iterateRlistB:" );
    sizeList=cashedrListB.size();
    int rSizeList=rListB.size();
        System.out.println("  LoopListener.isizeList,rListBSizelist:: "+sizeList + "  "+ rSizeList);
  rListB.clear();
        for (int l=0; l<sizeList;l++)
    {
        indexList=(ArrayList)cashedrListB.get(l);
        rListB.add(indexList);
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
    System.out.println();
}

private void copyCashedFigPArrayValues() {
    int value;
    int iterateRange=cashedFigPArray.size();
    for (int l=0; l<iterateRange;l++) {
        System.out.println("copyCahedFigPArrayValues l:" +l);
        value=(int)cashedFigPArray.get(l);
        figPArray.set(l, value);
    }
    System.out.println("parrayresetlistener copyCahedFigPArray:" +figPArray.toString());
}

    
    
    
    
    
     public void propertyChange(PropertyChangeEvent evt) {
//      System.out.println();
//      System.out.println();
      
//      System.out.println("pArrayResetListener                                      pArrayResetListener");
//     System.out.println("pArrayResetListener                                      pArrayResetListener    cashedrListB: "+cashedrListB.toString());
//      System.out.println("pArrayResetListener.propertyChange"+" workNum: "+((muNumDen)evt.getNewValue()).getWorkNum()+ " modulo: "+((muNumDen)evt.getNewValue()).getModulo()); 
      rListIndex=((muNumDen)evt.getNewValue()).getWorkNum()+2;
 //     gc();
//      System.out.println();
//      System.out.println();`
      int indexS=((muNumDen)evt.getNewValue()).getWorkNum();
//       System.out.println("pArrayResetListener.propertyChange workNum: " + indexS);
//      int indexS=((muNumDen)evt.getNewValue()).getWorkNum()+1;
       for (int pIndex=0; pIndex<=indexS ; pIndex++) { 
        pArray.set(pIndex,pArrayResetValue);
        figPArray.set(pIndex, 0);
      }
 //     pArray.incrementP(rListIndex - 2);
      
//     copyCashedRListBValues();         //  rListB=(ResultListBean)cashedrListB.clone();                   //just the parray iteration result no updating
//     copyCashedFigPArrayValues();      //figPArray=(PolynomialArray)cashedFigPArray.clone();

//      updateRlistB();
      //System.out.println();
      //System.out.println();
      //System.out.println("pArrayResetListener                       updated pArray: "+ pArray.toString());
      //System.out.println("pArrayResetListener                      new figPArray: "+ figPArray.toString());
//      System.out.println("pArrayResetListener                      new rListB: "+ rListB.toString());
//            System.out.println("pArrayResetListener                      cashedrListB: "+ cashedrListB.toString());
      //System.out.println();
      //System.out.println();
//      rListB.clear();
      resultListId.setPWasReset();
      pArrayResetFlag.setPArrayReset();
     }
    
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
}
