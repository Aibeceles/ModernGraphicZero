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



public class GenerateBinaryLastIterate implements Callable<String> {
  
int numerator;
int denominator;
Semaphore sf1;
Semaphore sb1;
Semaphore sf2;
Semaphore sb2;
Boolean endCproduct;
EndProduct endProduct=new EndProduct();
GBList gbList = new GBList();

GenerateBinaryLastIterate(int num, int denom, Semaphore s1, Semaphore s2,Semaphore s3, Semaphore s4,EndProduct endCproduct,GBList gbList) {
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
//   System.out.println("Generate BinaryLast ThredID " + threadId  );
    nBinaryList.add(nBinary.setFirstBinary(numerator, denominator));
    sf2.acquire();
    nBinaryList1.add((ArrayList)nBinaryList.get(0));
    nextBigDecimal=lTNumber.getIntegerFromList(nBinary.setFirstBinary(numerator, denominator));
 System.out.println(nextBigDecimal.toString() + " ");
    returnList.add(nextBigDecimal.toString());

    gbList.setGBList(new muNumDen(numerator,denominator,nextBigDecimal));
 //   gbList.setGBList(nextBigDecimal);
 //   System.out.println("gbl   ThredID" + threadId + " ReturnList" + returnList.toString());
 //   sb2.release(); 
    while (setSize>1)
    {  //  sf2.acquire(); 
      testList8=nBinary.getNextBinary(setSize,0,nBinaryList);
      testList5.add((ArrayList)testList8.get(0));
      testList10=(ArrayList)testList5.get(0);
      if ((int)testList10.get(0)==0) 
      {if ((setIndex+1==nBinaryList1.size())){ setSize=setSize-1; setIndex=0; } else {setIndex++; } }  
      else
      {  nBinaryList1.add((ArrayList)testList5.get(0));    
         nextBigDecimal=lTNumber.getIntegerFromList((List)testList5.get(0));
    System.out.println(nextBigDecimal.toString() + " ");
         returnList.add(nextBigDecimal.toString());
  
        gbList.setGBList(new muNumDen(numerator,denominator,nextBigDecimal)); 
 //        gbList.setGBList(nextBigDecimal);
  //       System.out.println("gbl inside loop ThredID" + threadId + " ReturnList" + returnList.toString());
         setIndex++;}    
     testList8.clear(); testList5.clear();nBinaryList.clear();
     nBinaryList.add((ArrayList)nBinaryList1.get(setIndex));
 //    sb2.release(); 
    }  // end of while loop
      endProduct.setEndCproduct(true);
    System.out.println("gbl exit    .........." +endProduct.getEndCproduct());

     return("gbl exit     .........");
    }    
    
    
      
    
    
    
}
