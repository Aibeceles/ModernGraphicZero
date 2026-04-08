/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopsLogic;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 *
 * @author HP_Administrator
 */
public class ModuloList extends ArrayList{
 
int dimension;
    private BigDecimal factorialCounter=new BigDecimal(1);
    private BigDecimal one=new BigDecimal(1);
    private BigDecimal modulo=new BigDecimal(1);
 
public  ModuloList(int dimension)   {
    
this.dimension= dimension;
setDimension();

}    

private void setDimension() {
  this.add(one);                                      //  refactor attempt 2/25/2021
  for (int n=0;n<dimension; n++) {
     modulo=modulo.multiply(factorialCounter);
     factorialCounter=factorialCounter.add(one);
     this.add(modulo);
 
}
   
  System.out.println("ModuloList:" + this.toString());
}

    
}
