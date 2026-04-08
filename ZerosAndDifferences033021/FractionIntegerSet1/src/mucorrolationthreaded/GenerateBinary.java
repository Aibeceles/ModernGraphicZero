/*  
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mucorrolationthreaded;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
//import javax.ejb.Stateless;
//import javax.jws.WebMethod;
//import javax.jws.WebParam;
//import javax.jws.WebService;

/**
 *
 * @author Aibes
 *  Note: Generate binary uses the NextBinary algorithm, not in need of recursion
 * to span sets of equivalent binary fractions.  It draws a product based on
 * an initial set of binary fractions composed of a maximum size of subset {A}i in
 * {A} with remaining subsets equal to 1.
 *  Note: List of List is implemented.
 */

//@Stateless()
public class GenerateBinary {

    /**
     * Web service operation
     * @param numerator
     * @param denominator
     * @return 
     */
    
    public List tableN(int numerator, int denominator) {
    
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
    
    nBinaryList.add(nBinary.setFirstBinary(numerator, denominator));
    nBinaryList1.add((ArrayList)nBinaryList.get(0));
    nextBigDecimal=lTNumber.getIntegerFromList(nBinary.setFirstBinary(numerator, denominator));
    returnList.add(nextBigDecimal.toString());
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
         returnList.add(nextBigDecimal.toString());
      //   returnList.add(nextBigDecimal);
         setIndex++;
      }    
     testList8.clear();
     testList5.clear();
     nBinaryList.clear();
     nBinaryList.add((ArrayList)nBinaryList1.get(setIndex));
   }  
 //   returnList.add(nBinaryList1.toString());
    return(returnList);
    }
}

