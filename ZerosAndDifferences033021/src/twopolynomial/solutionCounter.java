/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package twopolynomial;
import java.math.*;
/**
 *
 * @author Aibes
 */
public class solutionCounter {

BigDecimal max = new BigDecimal(5);    
BigDecimal current = new BigDecimal(1);
BigDecimal one = new BigDecimal(1);
BigDecimal mOne = new BigDecimal(-1);

public void setCurrent(BigDecimal cUr){
current=cUr;
}

public BigDecimal getCurrent() {
        return current;
    }

public BigDecimal getMax() {
        return max;
    }


public void increment(){
current=current.add(one);
}

public void decrement(){
current=current.add(mOne);
}

public void reset(){
 current=max;
}


    
    
    
    
    
}
