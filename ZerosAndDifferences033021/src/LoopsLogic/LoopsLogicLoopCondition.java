/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopsLogic;

import LoopsSemaphorePattern.AbstractLoopCondition;
import LoopsSemaphorePattern.LoopsConditionInterface;

/**
 *
 * @author HP_Administrator
 */
public class LoopsLogicLoopCondition extends AbstractLoopCondition implements LoopsConditionInterface {
 
    boolean value=true;
    public void setLoopCondition() {
        value=false;
    }
    
    public boolean getLoopCondition() {
    boolean vvalue=value;
    setLoopCondition();
        return(vvalue);
    }
    
    
}
