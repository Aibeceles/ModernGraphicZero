/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopLists;

import LoopLists.LoopList;
import LoopsLogic.ModuloList;
import PArrayReset.PolynomialArray;
import PArrayReset.ResultListID;
import fractionintegerset.ResultListBean;
import static java.lang.Thread.sleep;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import mugauss.GaussMain;

/**
 *  Looplistener TriangleTriangle i  I rList update runnable.
 *  
 *   create a rListB' with rListB'(0)=rListB(i).
 *    extend rListB'
 *    gauss each on completed rlist()'
 *    remit rlistB' to propertyChange()
 * @author HP_Administrator
 */
public class LoopListenerRunnable implements Runnable,Callable<String> {

 LoopList gbList = new LoopList();   
 BigDecimal modulo;
 ResultListBean allrListB;
 BigDecimal moduloFactor;
 PolynomialArray pArray;
 int wNum;
 int dimension;
 private List transferList=new ArrayList();
 ResultListBean rListB;
 List muList = new ArrayList();
 Semaphore muSem = new Semaphore(1);
 Semaphore listenerRunnableSemaphore;
 int resultListCounter;


//                   exec.submit(new LoopListenerRunnable(muList,muSem,allrListB,dimension,n,gbList, rModulo, copyParray,rListB,resultListCounter));
 
public LoopListenerRunnable(List muList, Semaphore muSem,ResultListBean allrListB,int dimension, int wNum,LoopList gbList,BigDecimal modulo,PolynomialArray pArray,ResultListBean rListB,int resultListCounter,Semaphore listenerRunnableSemaphore){
  this.muList=muList;
  this.muSem=muSem;
  this.allrListB=allrListB;
  this.gbList=gbList;  
  this.modulo=modulo;                         // don't care what modulo is referenced.  will compute in run() appropriate value.
//  this.moduloFactor= moduloFactor;
  this.pArray=pArray;
  this.wNum=wNum;
  this.dimension=dimension;
  this.rListB=rListB;
  this.resultListCounter=resultListCounter;
  this.listenerRunnableSemaphore=listenerRunnableSemaphore;
//  System.out.println();
//  System.out.println("LoopListenerRunnable constuctor: dimension  " + this.dimension );
//  System.out.println();
}
    


private void copyGBList() {
    transferList.clear();
    for (int index=0; index<gbList.size() ; index++) {
     transferList.add(gbList.get(index));
    } 
}  
  
private synchronized void copyGBList(ArrayList gbList) {
    transferList.clear();
//    System.out.println("LoopListenerRunnable.copyGBList gbList, gbList.size(): " + gbList.toString()+ "  " + gbList.size());
    for (int index=0; index<gbList.size() ; index++) {
     transferList.add(gbList.get(index));
}  
}  
  

private synchronized void factorGBList()  {
    BigDecimal term;
//    System.out.println("LoopListenerRunnable.factor transferList, transferList.size(): " + transferList.toString()+ "  " + transferList.size());
    for (int index=0; index<transferList.size() ; index++) {
    term=(BigDecimal)transferList.get(index);
//    System.out.println("term:" + term);
//     term=term.add(modulo);
  try{
    term=term.divide(modulo);
    transferList.set(index, term);
//    System.out.println("   termed:" + term + " index: "+index);
  }
  catch (ArithmeticException ae){
//     System.out.println("   term.divide exception: " + ae.getMessage() );
  }
    }    
    
}

/**
 *  this gbList needs gauss.main
 *  the new factored gblists need MatrixA's constructed.
 * instanciate a LoopList with  = gBList_i   A_0
 *
 */
    
public synchronized  void run() {
    GaussMain gMain = new GaussMain();
    MatrixA xPowers = new MatrixA(dimension-(wNum-1));
    ModuloList moduloList= new ModuloList(dimension);
    ResultListBean rListB1 =new ResultListBean();

//    System.out.println("                                            LoopListenerRunnable.run(), modulo, wNum,dimension:  " + modulo + " " + wNum + " " + dimension );
  //  modulo=(BigDecimal)moduloList.get(0);
    copyGBList((LoopList)this.rListB.get(wNum));
    factorGBList();
 //   System.out.println("                                            LoopListenerRunnable.run pre new loopList, transferList: "+ transferList.toString());
 //   LoopList gbListRunnable = new LoopList((dimension-(wNum+1)),rListB1,(dimension-(wNum+1)),10,0,xPowers,transferList);  old looplist very funky                // integerrange should be a static field.
 //    System.out.println("LoopListenerRunnable.run  loop resultListCounter: "  + resultListCounter);
     LoopList gbListRunnable = new LoopList((dimension-(wNum-1)),rListB1,(dimension),15,0,xPowers,transferList);
 //   System.out.println("                                            LoopListenerRunnable.run gbListRunnable.toString: " + gbListRunnable.toString());
 //   System.out.println("                                            LoopListenerRunnable.run  first gauss pArray: "+ pArray.toString());
                                      //   gMain.gauss(muList,muSem,(dimension-(wNum-1)),dimension-(wNum-1)+1,(double[][])gbListRunnable.getgMatrix(),new GaussBean1(pArray.toString(),gbListRunnable.toString(),wNum,resultListCounter));
  
    
    rListB1.add(gbListRunnable);
//    allrListB.add(rListB1);
//    System.out.println("LoopListenerRunnable.run loop  dimension , wNum+1 , wNum: " + dimension + "  " + (wNum+1) + "  " + wNum);
           for (int numerator=1; numerator<(dimension-wNum+1) ; numerator++) {                                               // this loop shut off for
  //              System.out.println("LoopListenerRunnable.run  loop numerator: "  + numerator);
  //              System.out.println("LoopListenerRunnable.run  loop resultListCounter: "  + resultListCounter);
                LoopList gbList1 = new LoopList((dimension-((wNum-1)+numerator)),rListB1,dimension,15,numerator,xPowers,transferList);
  //               System.out.println("LoopListenerRunnable.run loop   gbList1.to string: " + gbList1.toString());
                //         System.out.println("LoopListenerRunnable.run loop   gbList1.Amatrix: " );gbList1.printMatrix(gbList.getAMatrixA());
//System.out.println("LoopListenerRunnable.run  pregauss loop  pArray: "+ pArray.toString()+  "  " +  (dimension-((wNum-1)-numerator)+1));
                                      //  gMain.gauss(muList,muSem,dimension-((wNum-1)+numerator),dimension-((wNum-1)-numerator-1),(double[][])gbList1.getgMatrix(),new GaussBean1(pArray.toString(),gbList1.toString(),wNum,resultListCounter));
   //    public void gauss(List gList, Semaphore s1,int n,int m,double[][] A,GaussBean1 gBean1)
        
//          rListB1.add(gbList1);
           }
 //   System.out.println("LoopListenerRunnable.run post loop rListB1: "+rListB1.toString());
 //   System.out.println("LoopListenerRunnable.run post loop allrListB: "+allrListB.toString());
    System.out.println("LoopListenerRunnable.run fallthrough. ");
  listenerRunnableSemaphore.release();
  }    
    
    
public synchronized  String call() {
    GaussMain gMain = new GaussMain();
    MatrixA xPowers = new MatrixA(dimension-(wNum-1));
    ModuloList moduloList= new ModuloList(dimension);
    ResultListBean rListB1 =new ResultListBean();
    long threadId = Thread.currentThread().getId();
//    System.out.println("                                            LoopListenerRunnable.run(), modulo, wNum,dimension:  " + modulo + " " + wNum + " " + dimension );
  //  modulo=(BigDecimal)moduloList.get(0);
    copyGBList((LoopList)this.rListB.get(wNum));
    factorGBList();
 //   System.out.println("                                            LoopListenerRunnable.run pre new loopList, transferList: "+ transferList.toString());
 //   LoopList gbListRunnable = new LoopList((dimension-(wNum+1)),rListB1,(dimension-(wNum+1)),10,0,xPowers,transferList);  old looplist very funky                // integerrange should be a static field.
 //    System.out.println("LoopListenerRunnable.run  loop resultListCounter: "  + resultListCounter);
     LoopList gbListRunnable = new LoopList((dimension-(wNum-1)),rListB1,(dimension),15,0,xPowers,transferList);
 //   System.out.println("                                            LoopListenerRunnable.run gbListRunnable.toString: " + gbListRunnable.toString());
 //   System.out.println("                                            LoopListenerRunnable.run  first gauss pArray: "+ pArray.toString());
                                                    // gMain.gauss(muList,muSem,(dimension-(wNum-1)),dimension-(wNum-1)+1,(double[][])gbListRunnable.getgMatrix(),new GaussBean1(pArray.toString(),gbListRunnable.toString(),wNum,resultListCounter));
  
    
    rListB1.add(gbListRunnable);
//    allrListB.add(rListB1);
//    System.out.println("LoopListenerRunnable.run loop  dimension , wNum+1 , wNum: " + dimension + "  " + (wNum+1) + "  " + wNum);
           for (int numerator=1; numerator<(dimension-wNum+1) ; numerator++) {                                               // this loop shut off for
  //              System.out.println("LoopListenerRunnable.run  loop numerator: "  + numerator);
  //              System.out.println("LoopListenerRunnable.run  loop resultListCounter: "  + resultListCounter);
                LoopList gbList1 = new LoopList((dimension-((wNum-1)+numerator)),rListB1,dimension,15,numerator,xPowers,transferList);
  //               System.out.println("LoopListenerRunnable.run loop   gbList1.to string: " + gbList1.toString());
                //         System.out.println("LoopListenerRunnable.run loop   gbList1.Amatrix: " );gbList1.printMatrix(gbList.getAMatrixA());
//System.out.println("LoopListenerRunnable.run  pregauss loop  pArray: "+ pArray.toString()+  "  " +  (dimension-((wNum-1)-numerator)+1));
                                                   //   gMain.gauss(muList,muSem,dimension-((wNum-1)+numerator),dimension-((wNum-1)-numerator-1),(double[][])gbList1.getgMatrix(),new GaussBean1(pArray.toString(),gbList1.toString(),wNum,resultListCounter));
   //    public void gauss(List gList, Semaphore s1,int n,int m,double[][] A,GaussBean1 gBean1)
        
//          rListB1.add(gbList1);
           }
 //   System.out.println("LoopListenerRunnable.run post loop rListB1: "+rListB1.toString());
 //   System.out.println("LoopListenerRunnable.run post loop allrListB: "+allrListB.toString());
    System.out.println("LoopListenerRunnable.run fallthrough. ");
 
  return("HiThere"); 
}
    }   
    
   
