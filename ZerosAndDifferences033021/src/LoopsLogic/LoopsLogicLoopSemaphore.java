/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopsLogic;

import LoopLists.LoopList;
import LoopsSemaphorePattern.AbstractLoopSemaphore;
import LoopsSemaphorePattern.LoopsSemaphoreInterface;
import PArrayReset.PolynomialArray;
import fractionintegerset.muNumDen;
import java.math.BigDecimal;

/**
 *               
 * @author HP_Administrator
 */
public class  LoopsLogicLoopSemaphore extends AbstractLoopSemaphore  implements LoopsSemaphoreInterface {
private BigDecimal maxIncrement=new BigDecimal(1);
private int integerRange;    
private int workerNum;
BigDecimal modulo = new BigDecimal(1);
BigDecimal one = new BigDecimal(1);
BigDecimal factorialCounter = new BigDecimal(1);
BigDecimal bintegerRange = new BigDecimal(1);
LoopList gbList;
PolynomialArray pArray;
PolynomialArray copyPArray;

public LoopsLogicLoopSemaphore() {
    
} 

public LoopsLogicLoopSemaphore(LoopList gbList,int workerNum,int integerRange, PolynomialArray pArray ) {
this.workerNum=workerNum;
this.integerRange=integerRange;
this.gbList=gbList;
this.pArray=pArray;
setModulo();
// setMaxIncrement();   funcion implementd in runnable
}    

private void setMaxIncrement(){
 bintegerRange.equals(integerRange);
 maxIncrement=modulo.multiply(bintegerRange);
 System.out.println(" LoopsLogic setMaxIncrement: " + maxIncrement.toString() );    
}
private void setModulo() {

 for (int n=0;n<workerNum; n++) {
 modulo=modulo.multiply(factorialCounter);
 factorialCounter=factorialCounter.add(one);
 
}
 System.out.println(" LoopsLogic.setModulo workerNum, modulo: " + workerNum+" " +modulo );    
    
}
@Override
    public void sf2Aquire() {        
  
        synchronized (this) {//    returnList.add(nextBigDecimal.toString());
 //    gbList.setGBList(new muNumDen(modulo,workerNum,gbList.getancestorlist()));   
         synchronized (pArray){
         copyPArray=pArray;
        }
         gbList.setGBList(new muNumDen(modulo,workerNum,gbList.getancestorlist()));
  //      gbList.setGBList(nextBigDecimal);  //avoid .tostring just .add bigdecimal
    }
    
    }
    public void sb2Release() {        
    }    
    public void sf1Release(){    
    }
    public void sb1Aquire(){   
    }
   
    public void rentrentReset() {
        //System.out.println("rentrentReset                                                           rentrentReset");
        pArray.setpArray(new muNumDen(modulo,workerNum,gbList.getancestorlist()));
    }
    
}
