/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PArrayReset;

/**Is constructed with pArrayReset flag = false.
 *
 * @author Aibes
 */
public class PArrayResetFlag {
   
private boolean pArrayReset;    
    
public PArrayResetFlag() {
   pArrayReset=false;    
}    
    
public boolean getPArrayReset() {
    return(pArrayReset);
}    
    
public void setPArrayReset() {
    if (pArrayReset) {
        pArrayReset=false;
    }
    else {
        pArrayReset=true;
    }
}
    
    
    
}
