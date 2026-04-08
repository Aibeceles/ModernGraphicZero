 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mucorrolationthreaded;


import fractionintegerset.FractionIntegerDriver;
import fractionintegerset.FractionIntegerDriverIterate;
import fractionintegerset.GBIterator;
import fractionintegerset.muNumDen;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Note on run: maxtriangletriangle predicates compleation of integers for 
 * intiger i between 1 and maxtriangle triangle.
 * @author Aibes
 */
public class MuCorrolation11Iterated extends Thread{

    /**
     * @param args the command line arguments
    */
    Semaphore muSem = null;
    Semaphore tiSemap = new Semaphore(1);
    List rootLis = new ArrayList();
    List muList = new ArrayList();
    FractionIntegerDriverIterate fig= null;
 //   GBIterator fig = (GBIterator) new Thread();
    
  //  public MuCorrolation11Iterated(Semaphore t1Sem, List rootLi,FractionIntegerDriver fig, List muList,Semaphore muSem) {
  //  tiSemap=t1Sem;
  //  rootLis=rootLi;        
  //  this.fig=fig;
  //  this.muList=muList;
  //  this.muSem=muSem;
  //  }
 
    public MuCorrolation11Iterated(Semaphore t1Sem, List rootLi,FractionIntegerDriverIterate fig, List muList,Semaphore muSem) {
    tiSemap=t1Sem;
    rootLis=rootLi;        
    this.fig=fig;
    this.muList=muList;
    this.muSem=muSem;
    }   
    
    @Override
    public void run() {
    
    int numerator=0;
    int denominator=0;
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
   System.out.println("             before wait");
   synchronized(this) {
    try {
            this.wait(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(MuCorrolation11Iterated.class.getName()).log(Level.SEVERE, null, ex);
        }
   }
   System.out.println("             after wait");
    int mu;
    int muListIndex = 0;
    int muListObjectNum = 0;
    int maxMu=500;
    List rootList = new ArrayList();
    BigDecimal muDecimal = new BigDecimal(1);
    BigDecimal nNumerator = new BigDecimal(1);
    BigDecimal dDenominator = new BigDecimal(1);
    String muDecimalS = new String();
    SumOfTriangleY sumTriangle = new SumOfTriangleY();     
    GenerateBinary gBinary = new GenerateBinary();
 
   
     muListObjectNum=0;
     while ((fig.isAlive()) || (muList.size()>0))   {
   //  while (muListObjectNum<maxMu) {
     
         try {
                muSem.acquire();
           //     muDecimal = (BigDecimal)muList.get(0);
        
                
                muDecimal = (BigDecimal)((muNumDen)muList.get(0)).getMu();
                nNumerator = (BigDecimal)((muNumDen)muList.get(0)).getnumerator();
                dDenominator = (BigDecimal)((muNumDen)muList.get(0)).getDenominator();
//        muDecimal = (BigDecimal)muList.get(0);
                System.out.println("             muDecimal: " + muDecimal);
                //  tlToTable(((rowBean)dbbVector.get(0)).getTrZero(),((rowBean)dbbVector.get(0)).getTrTriangle(),((rowBean)dbbVector.get(0)).getMuDecimal(),((rowBean)dbbVector.get(0)).getRootOne(),((rowBean)dbbVector.get(0)).getRootTwo(),((rowBean)dbbVector.get(0)).getA(),((rowBean)dbbVector.get(0)).getB(),((rowBean)dbbVector.get(0)).getC(),((rowBean)dbbVector.get(0)).getDeterminat(),((rowBean)dbbVector.get(0)).getNumerator(),((rowBean)dbbVector.get(0)).getDenominator());
//                tlToTable(((rowBean)dbbVector.elementAt(0)).getTL(),((rowBean)dbbVector.elementAt(0)).getNN(),((rowBean)dbbVector.elementAt(0)).getFlatFileRowCounter(),((rowBean)dbbVector.elementAt(0)).getNMax());
                muList.remove(0);
//                dbbVector.removeElementAt(0);
            } catch (InterruptedException ex) {
                Logger.getLogger(twoTableDBThread.class.getName()).log(Level.SEVERE, null, ex);
            }
         
         mu=muListObjectNum;
    //     BigDecimal muDecimal = new BigDecimal(mu);
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
  //               System.out.println("             numeratorr: " + nNumerator.toString());
                 BigDecimal denominatorr=new BigDecimal(denominator);
 //                System.out.println("             denominatorr: " + dDenominator.toString());
                 dbBean.setFields(trZero, trTriangle, muDecimal, (BigDecimal)testList8.get(0), (BigDecimal)testList8.get(1), (BigDecimal)testList8.get(2), (BigDecimal)testList8.get(3), (BigDecimal)testList8.get(4), (BigDecimal)testList8.get(5),nNumerator,dDenominator);
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
//   //              rootList.add(rootList1);
//     //            System.out.println("MuDecimal:  " + muDecimal.toString()); 
                 trZero=trZero.add(one);
               }
             trTriangle=trTriangle.add(one);
             trZero=one;
        }//triangletriangl loop
         trTriangle=one;
         trZero=one;
       }//}
  //  }   //  completed mulist loop
        numerator++;    
 //       System.out.println(rootList.toString());        
    }   //end of run method
  
}
