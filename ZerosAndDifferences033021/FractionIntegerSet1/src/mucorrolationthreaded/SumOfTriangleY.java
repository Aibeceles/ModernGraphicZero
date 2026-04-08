/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mucorrolationthreaded;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Aibes
 */
public class SumOfTriangleY {

BigDecimal triangleZero = new BigDecimal(0);
BigDecimal triangleTriange = new BigDecimal(0);
BigDecimal muInteger = new BigDecimal(0);
BigDecimal zero = new BigDecimal(0);
BigDecimal two = new BigDecimal(2);
BigDecimal minusTwo = new BigDecimal(-2);
BigDecimal minusOne = new BigDecimal(-1);
BigDecimal four = new BigDecimal(4);
BigDecimal a = new BigDecimal(0);
BigDecimal b = new BigDecimal(0);
BigDecimal c = new BigDecimal(0);

public void setArgs(BigDecimal trZero, BigDecimal trTr, BigDecimal muIn) {
  triangleZero=trZero;
  triangleTriange=trTr;
  muInteger=muIn;
} 

public void setABC() {
    
a=triangleTriange;
b=triangleTriange.add(triangleZero.multiply(two));
c=muInteger.multiply(minusTwo).add(triangleZero.multiply(two));
 //System.out.println("a " + a.toString());
 //System.out.println("b " + b.toString());
 //System.out.println("c " + c.toString());
}

public List quadraticResult() {

List rootList = new ArrayList();
BigDecimal quadraticResult = new BigDecimal(0);
BigDecimal discriminant = new BigDecimal(0); 
BigDecimal fourAC = new BigDecimal(0);
BigDecimal one = new BigDecimal(1);
double discriminateSqRoot = 0;


discriminant=b.pow(2).subtract(four.multiply(a).multiply(c));
//fourAC=one.multiply(four.multiply(a.multiply(c)));
fourAC=one.multiply(four).multiply(a).multiply(c);
//System.out.println("fourAC " + fourAC.toString());
//System.out.println("Discriminant " + discriminant.toString());
if (discriminant.compareTo(minusOne)==1) {
discriminateSqRoot=discriminant.floatValue();
discriminateSqRoot=Math.sqrt(discriminateSqRoot);
//System.out.println("Dis squr root" + discriminateSqRoot);
//BigDecimal discriminantt= new BigDecimal(discriminateSqRoot);
BigDecimal discriminantt = new BigDecimal(discriminateSqRoot);
rootList.add(zero.subtract(b).add(discriminantt).divide(a.multiply(two),15, RoundingMode.HALF_UP));
rootList.add(zero.subtract(b).subtract(discriminantt).divide(a.multiply(two),15, RoundingMode.HALF_UP));    
rootList.add(a);
rootList.add(b);
rootList.add(c);
rootList.add(discriminantt);
//a.divide(b, 2, RoundingMode.HALF_UP);

}
return(rootList);
}
    
}
