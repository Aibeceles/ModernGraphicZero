/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IntegerPoly;

import LoopsSemaphorePattern.LoopsSemaphoreInterface;
import MuBinary.ListToNumber;
import MuBinary.NextBinary;
import fractionintegerset.EndProduct;
import fractionintegerset.GBList;
import java.lang.Boolean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;



/**
 *
 * @author HP_Administrator
 */
//public class GenerateBinaryLoopLogic implements LoopsSemaphoreInterface {
public class GenerateBinaryLoopLogic {

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
int setIndex;
int setSize;
GBList gbList = new GBList();    
    
GenerateBinaryLoopLogic(int num, int denom, EndProduct endCproduct,GBList gbList) {
numerator=num;
denominator=denom;
this.endProduct=endCproduct;
this.gbList=gbList;
}


public void preLoopsCondition(int setIndex,int setSize){
  this.setIndex=setIndex;
  this.setSize=setSize;
}

public void sf2AquireInitial(){
    
}

public void sf2AquireLoop() {
    
}


}
