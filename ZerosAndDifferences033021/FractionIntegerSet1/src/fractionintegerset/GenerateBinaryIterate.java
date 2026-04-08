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
 *
 * @author Aibes
 */
public class GenerateBinaryIterate implements Callable<String>{

NextBinary nBinary = new NextBinary();
ListToNumber lTNumber = new ListToNumber();
ArrayList nBinaryList = new ArrayList();
ArrayList nBinaryList1 = new ArrayList();
List returnList = new ArrayList();
List testList5 = new ArrayList();
List testList8 = new ArrayList();
List testList10 = new ArrayList();
BigDecimal nextBigDecimal = new BigDecimal(0);
Boolean endCproduct;
 EndProduct endProduct=new EndProduct();   
int numerator;
int denominator;
Semaphore sf1;
Semaphore sb1;
Semaphore sf2;
Semaphore sb2;
GBList gbList = new GBList();

GenerateBinaryIterate(int num, int denom, Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,GBList gbList) {
numerator=num;
denominator=denom;
sf1=s1;
sb1=s2;
sf2=s3;
sb2=s4;
this.endProduct=endCproduct;
this.gbList=gbList;
}

public String call() throws InterruptedException {
    
    int setIndex=0;
    int setSize=numerator;
    long threadId = Thread.currentThread().getId();
 sf2.acquire();
    while(!endProduct.getEndCproduct()){
    setIndex=0; 
    nBinaryList.clear();
    nBinaryList1.clear();
    setSize=numerator;    // System.out.println("GenerateBinary before sf2 increment, Threadid" + threadId  );
   // sf2.acquire();
    nBinaryList.add(nBinary.setFirstBinary(numerator, denominator));
    nBinaryList1.add((ArrayList)nBinaryList.get(0));
    nextBigDecimal=lTNumber.getIntegerFromList(nBinary.setFirstBinary(numerator, denominator));
    synchronized (this) {//    returnList.add(nextBigDecimal.toString());
     gbList.setGBList(new muNumDen(numerator,denominator,nextBigDecimal));
//    gbList.setGBList(nextBigDecimal);  //avoid .tostring just .add bigdecimal
    } System.out.println("GenerateBinary before sb2 incremented, Threadid" + threadId  );
  //  sb2.release();  
    while (setSize>1)
    {  //  System.out.println("GenerateBinary before sf2 increment, Threadid" + threadId  );    
//    sf2.acquire();
    testList8=nBinary.getNextBinary(setSize,0,nBinaryList);
    testList5.add((ArrayList)testList8.get(0));
    testList10=(ArrayList)testList5.get(0);
    if ((int)testList10.get(0)==0) 
      { if ((setIndex+1==nBinaryList1.size())){ setSize=setSize-1; setIndex=0;} else { setIndex++; }}  
      else{  nBinaryList1.add((ArrayList)testList5.get(0));    
         nextBigDecimal=lTNumber.getIntegerFromList((List)testList5.get(0));
         synchronized (this) {//returnList.add(nextBigDecimal.toString());
         gbList.setGBList(new muNumDen(numerator,denominator,nextBigDecimal));
//             gbList.setGBList(nextBigDecimal);
         }
         setIndex++; }    
     testList8.clear();testList5.clear();nBinaryList.clear();
     nBinaryList.add((ArrayList)nBinaryList1.get(setIndex));
     System.out.println("GenerateBinary before sb2 incremented, Threadid" + threadId  );
 //    sb2.release(); 
    }  // end of while loop     System.out.println("GenerateBinary before sf1 iterated, Threadid" + threadId  );// System.out.println("GenerateBinary resultlisr, Threadid" + returnList.toString()  );
    sf1.release();   System.out.println("GenerateBinary before sb1 aquite ok to iterate, Threadid" + threadId  );
    sb1.acquire();   System.out.println("GenerateBinary after sb1 aquire ok to iterate, Threadid" + threadId  );
    sf2.release();
  }
    return("generateBinary "+ threadId);
    }
}