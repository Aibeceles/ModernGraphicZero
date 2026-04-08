/*
 * pArgument.java
 *
 * Created on February 16, 2011, 2:34 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package twopolynomial;

import java.beans.*;
import java.io.Serializable;
import java.math.*;
/**
 * Work in progress,  need to dissassociate loadDBB
 * @author Tomas
 */
public class pArgument extends Object implements Serializable, PropertyChangeListener {
    
    // pArgument should extend BigDecimal
   // argument needs the stuff so it can new combSum = new combSumPea 
    
    public static final String PROP_SAMPLE_PROPERTY = "argument";
    
    public BigDecimal argument;
    
    private PropertyChangeSupport propertySupport;
    
    public pArgument() {
        propertySupport = new PropertyChangeSupport(this);
    }
    
    public BigDecimal getArgument() {
        return argument;
    }
    
    public void setArgument(BigDecimal value) {
                BigDecimal oldValue = argument;
        argument = value;
        System.out.println("pArgument.setArgument: "+argument.toString());
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, argument);
    }
    
    
    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(name, listener);   
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    /**
     * Holds value of property aArgument.
     */
    private int aArgument;
       
    /**
     * Getter for property aArgument.
     * @return Value of property aArgument.
     */
    public int getAArgument() {
        return this.aArgument;
    }

    /**
     * Setter for property aArgument.
     * @param aArgument New value of property aArgument.
     */
    public void setAArgument(int aArgument) {
        int oldAArgument = this.aArgument;
                                           // alway remember modulo arithmetic instead of if 
                                         //  anything on a servlt becomes unreachable after iteration.. 
          if (aArgument==1) {
          aArgument=0;   
          } else {
          aArgument=1;
         }
        this.aArgument = aArgument;
        propertySupport.firePropertyChange ("aArgument", new Integer (oldAArgument), new Integer (aArgument));
    }
   
    
        public void propertyChange(PropertyChangeEvent evt) {
    
    // oldArgument, newArgument    I need productListVector,termsList     !!!
    
    
    
    }
}

    

