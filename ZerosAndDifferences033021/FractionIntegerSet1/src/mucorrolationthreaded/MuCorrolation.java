/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mucorrolationthreaded;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 *
 * @author Aibes
 */
public class MuCorrolation extends Thread{

    /**
     * @param args the command line arguments
    */
    Semaphore tiSemap = new Semaphore(1);
    List rootLis = new ArrayList();
    
    
    
    MuCorrolation(Semaphore t1Sem, List rootLi) {
    tiSemap=t1Sem;
    rootLis=rootLi;        
    }
    
    public void run() {
    
    int numerator=1;
    int denominator=7;
    BigDecimal maxTrTriangle = new BigDecimal(25);
    BigDecimal trTriangle = new BigDecimal(1);
    BigDecimal trZero = new BigDecimal(1);
    BigDecimal fOfx = new BigDecimal(28);
    BigDecimal one = new BigDecimal(1);
    BigDecimal  rootTest= new BigDecimal(1);
    Long rootTest1= new Long(1);  
    List testList5 = new ArrayList();
    List testList8 = new ArrayList();
    List testList10 = new ArrayList();
    List muList = new ArrayList();
    String mu;
    int muListIndex = 0;
    int muListObjectNum = 0;
    List rootList = new ArrayList();
    
    SumOfTriangleY sumTriangle = new SumOfTriangleY();     
    GenerateBinary gBinary = new GenerateBinary();
 
    while (numerator<(denominator+1)) {
    muList.add(gBinary.tableN(numerator, denominator));
    System.out.println(muList.toString());
//    muList.add(sumTriangle.quadraticResult());

    System.out.println(muList.toString());
//    while (muListIndex<muList.size()) {
     testList5=(ArrayList)muList.get(muListIndex);   
     System.out.println("testList5:  " + testList5.toString());
//     testList10=(ArrayList)testList5.get(muListIndex);
     muListIndex++;
     muListObjectNum=0;
     while (muListObjectNum<testList5.size()) {
         mu=(String)testList5.get(muListObjectNum);
         BigDecimal muDecimal = new BigDecimal(mu);
         muListObjectNum++;
         while (trTriangle.compareTo(maxTrTriangle)==-1) {
              while (trZero.compareTo(trTriangle)==-1) {
                 sumTriangle.setArgs(trZero, trTriangle, muDecimal);
                 sumTriangle.setABC();
                 List rootList1 = new ArrayList();
                 rowBean dbBean = new rowBean();
                 rootList1.add(sumTriangle.quadraticResult());
                 testList8=(ArrayList)rootList1.get(0);
                 BigDecimal numeratorr=new BigDecimal(numerator);
                 BigDecimal denominatorr=new BigDecimal(denominator);
                 dbBean.setFields(trZero, trTriangle, muDecimal, (BigDecimal)testList8.get(0), (BigDecimal)testList8.get(1), (BigDecimal)testList8.get(2), (BigDecimal)testList8.get(3), (BigDecimal)testList8.get(4), (BigDecimal)testList8.get(5),numeratorr,denominatorr);
                 rootTest=dbBean.getRootOne();
               try {  
                   rootTest1=rootTest.longValueExact();
                   System.out.println("integer root");
                    synchronized(this) {
                    rootLis.add(dbBean);
                    tiSemap.release();
                    System.out.println("             integer root");
                } 
               }
               catch (ArithmeticException e){
                }
                 rootList.add(rootList1);
     //            System.out.println("MuDecimal:  " + muDecimal.toString()); 
                 trZero=trZero.add(one);
               }
             trTriangle=trTriangle.add(one);
             trZero=one;
         }
         trTriangle=one;
         trZero=one;
       }
  //  }   //  completed mulist loop
        numerator++;    
    }
    System.out.println(rootList.toString());        
    }   //end of run method
  
}
