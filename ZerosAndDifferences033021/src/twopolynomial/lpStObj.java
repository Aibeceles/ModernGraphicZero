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
public class lpStObj {
 
    
BigDecimal value = new BigDecimal(0);    
String name;

public lpStObj(BigDecimal vValue, String nName)
{
 name=nName;
 value=vValue;
}


public String getName() {
        return name;
    }

public BigDecimal getValue() {
        return value;
    }


public void setName(String nName){
name=nName;
}

public void setValue(BigDecimal vValue){
value=vValue;
}
 
public String toString(){

return(name + " " + value);

}

}
