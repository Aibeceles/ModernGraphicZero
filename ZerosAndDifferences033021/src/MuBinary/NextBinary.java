/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package MuBinary;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Aibes
 */

public class NextBinary extends ArrayList {

/** Returns properly ordered list describing binary represented fraction
 * .
 */    
    
private List invertList(List bList)
{
List invertedList = new ArrayList();

for (int j=(bList.size()-1); j>-1; j--)
{
 invertedList.add(bList.get(j));    
}   
return(invertedList);    
}        
    
/** Native list of binary represented fraction returned as a numeric list
*
*/

private List setListRepresentation (List bList)
{
List numberedList = new ArrayList();
int one = 1;
int elementCounter=1;

for (Object bList1 : bList) {
    if ((int) bList1 == one) {
        numberedList.add(elementCounter);
        elementCounter=one;
    } else {
        elementCounter++;
    }
}    
return(invertList(numberedList));   
}        
    
/** All subsequent equivalent binary represented fractions are derived from this representation
 * of numerator, denominator
 * It is first generated as a traditional binary12 then it is interpreted and a
 * numerical list is returned.
 * @param numerator
 * @param denominator
 * @return 
 */


public List setFirstBinary(int numerator, int denominator)
{
int one = 1;
int zero = 0;
int j=0;
int currentJ = -1;
Boolean oldJ=FALSE;
int forBoundary=(denominator-numerator);
List binaryList = new ArrayList<>();

for(j=zero; j < forBoundary; j++)
{
 binaryList.add(j, one);
 System.out.println("setFirstBinary(): " + binaryList.toString());
 currentJ=j;
}  
++currentJ;
forBoundary=currentJ+numerator-1;
for (j=currentJ; j<forBoundary; j++)
{
 binaryList.add(j,zero);   
 currentJ=j;
 oldJ=TRUE;
}
if (oldJ)
{++currentJ;}
binaryList.add(currentJ,one);
System.out.println("setFirstBinary(): " + binaryList.toString());
return(setListRepresentation(binaryList));
}    
    
/** Ability to generate next equivalent binary fraction (another instance of same
 * number of set elements in different {A}) is tested.
 * 
 * @param bList
 * @param numerator
 * @return 
 */
    
private Boolean canNextBinary(int numerator,int bIndex, ArrayList bList) {
int singletonCounter = 0;   
int loopIndex=0;
List testList3 = new ArrayList();
    testList3=(ArrayList)bList.get(bIndex);
    loopIndex=testList3.size();
    for (int j=0; j<loopIndex; j++)
 {
    if ((int)testList3.get(j)==1)
  {
    singletonCounter=singletonCounter+1;    
  }  
}
if (singletonCounter>=numerator)
  return(TRUE);
else
{
  return(FALSE);
}
}

/** Next permutation of Binary12 is returned.
 * 
 * @param numerator 
 */

private List writeNextBinary(int numerator,int bIndex,ArrayList bList) {
int one=1;
int loopIndex=0;
int numeratorCounter=0;
List testList3 = new ArrayList();
List transferList = new ArrayList();
testList3=(ArrayList)bList.get(bIndex);
loopIndex=testList3.size();
//System.out.println(" ");
for (int j=0; j<loopIndex;j++)
{
    if ((int)testList3.get(j)==one)
  {
    if (numeratorCounter<numerator)
    {
        if (numeratorCounter+1==numerator)
        {
         transferList.add(numerator);    
        }
        numeratorCounter++;
      }
    else
    {
      transferList.add((int)testList3.get(j));
    }
  }  
  else
  {
    transferList.add((int)testList3.get(j));
  }
}
return(transferList);    
}


/** Routine for generating next equivalent binary fraction.
 *  Initial numerator provided by GenerateBinary.
 *  One List is initially on this.
 * It is passed initially and by itself a lower bound index of
 * this relevant for the current numerator.
 * @param b12SetSize
 * @param nBinaryLowerIndex
 * @param binaryList 
 * @return  
 */    

public List getNextBinary(int b12SetSize, int nBinaryLowerIndex, ArrayList binaryList) {
int bIndex = nBinaryLowerIndex;    
List getNextBinaryList = new ArrayList();
List transferList = new ArrayList();
    
if (canNextBinary(b12SetSize,bIndex, binaryList))
{
  getNextBinaryList.add(writeNextBinary(b12SetSize,bIndex,binaryList));
}
else 
{
  transferList.add(0);
  getNextBinaryList.add(transferList);
}
// System.out.println("getNextBinary: "+getNextBinaryList.toString());
 return(getNextBinaryList);
}    


}

