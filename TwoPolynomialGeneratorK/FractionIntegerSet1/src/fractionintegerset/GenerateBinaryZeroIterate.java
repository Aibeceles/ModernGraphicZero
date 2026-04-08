/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fractionintegerset;
import MuBinary.ListToNumber;
import MuBinary.NextBinary;
import java.lang.Boolean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * this has a number of available permits constantly increasing.
 * @author Aibes
 */
public class GenerateBinaryZeroIterate implements Callable<String> {
  
int numerator;
int denominator;
Semaphore sf1;
Semaphore sb1;
Semaphore sf2;
Semaphore sb2;
Semaphore startSemap;
boolean endCproduct;
NextBinary nBinary = new NextBinary();
ListToNumber lTNumber = new ListToNumber();
ArrayList nBinaryList = new ArrayList();
ArrayList nBinaryList1 = new ArrayList();
List returnList = new ArrayList();
List testList5 = new ArrayList();
List testList8 = new ArrayList();
List testList10 = new ArrayList();
BigDecimal nextBigDecimal = new BigDecimal(0);   
int setIndex=0;
int setSize=numerator;
long threadId = Thread.currentThread().getId();
EndProduct endProduct=new EndProduct();
GBList gbList = new GBList();
ResultListBean rListB = new ResultListBean();

GenerateBinaryZeroIterate(int num, int denom, Semaphore startS,Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,GBList gbList,ResultListBean rListB) {
    numerator=num;
    denominator=denom;
    startSemap=startS;
    sf1=s1;
    sb1=s2;
    sf2=s3;      
    sb2=s4;
    this.endProduct=endCproduct;
    this.gbList=gbList;
    this.rListB=rListB;
}    
    
    
public String call() throws InterruptedException, Exception {
     

    while (!endProduct.getEndCproduct()) {
    setIndex=0; 
    nBinaryList.clear();
    nBinaryList1.clear();
    setSize=numerator;
    System.out.println("GenerateBinaryZero Before aquire" + startSemap.availablePermits()); System.out.println(sf1.availablePermits()); System.out.println(sf2.availablePermits());
//   System.out.println("GenerateBinary before sf2 increment, Threadid" + threadId  );
    sf2.acquire();
    System.out.println("GenerateBinary after sf2 increment, Threadid" + threadId  );
    nBinaryList.add(nBinary.setFirstBinary(numerator, denominator));
    nBinaryList1.add((ArrayList)nBinaryList.get(0));
    nextBigDecimal=lTNumber.getIntegerFromList(nBinary.setFirstBinary(numerator, denominator));
    synchronized (this) {
   //     returnList.add(nextBigDecimal.toString());

     gbList.setGBList(new muNumDen(numerator,denominator,nextBigDecimal));   
    //    gbList.setGBList(nextBigDecimal);
    }
  //System.out.println(sf1.availablePermits());  System.out.println(sf2.availablePermits());   
    while (setSize>1)
    {
      testList8=nBinary.getNextBinary(setSize,0,nBinaryList);
      testList5.add((ArrayList)testList8.get(0));
      testList10=(ArrayList)testList5.get(0);
      if ((int)testList10.get(0)==0) 
      {
       if ((setIndex+1==nBinaryList1.size()))
       {
        setSize=setSize-1;
        setIndex=0;
        }
       else
       {
        setIndex++;    
       }
       }  
      else
      {
         nBinaryList1.add((ArrayList)testList5.get(0));    
         nextBigDecimal=lTNumber.getIntegerFromList((List)testList5.get(0));
   //      returnList.add(nextBigDecimal.toString());
  
     gbList.setGBList(new muNumDen(numerator,denominator,nextBigDecimal));    
  //       gbList.setGBList(nextBigDecimal);
         setIndex++;
      }    
     testList8.clear();
     testList5.clear();
     nBinaryList.clear();
     nBinaryList.add((ArrayList)nBinaryList1.get(setIndex));
    }  // end of while loop 
 //System.out.println("GenerateBinary before sf1 iterated, Threadid" + threadId  );
    sf1.release();
   System.out.println("GenerateBinary before sb1 ok to iterate, Threadid" + threadId  );
    sb1.acquire();
    sf2.release();
   }
    return("generateBinaryZero return");
    
    }    
    
    
    
}
