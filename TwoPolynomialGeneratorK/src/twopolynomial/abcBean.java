/*
 * Copyright (C) 2019 Aibes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package twopolynomial;

import java.beans.*;
import java.io.Serializable;
import java.math.*;
/**
 * @author Tomas
 */
public class abcBean extends Object implements Serializable {
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    
    private String sampleProperty;
    
    private PropertyChangeSupport propertySupport;
    private BigDecimal a,b,c,rowCounter;
    private BigDecimal argumentRowCounter;
    private BigDecimal increment = new BigDecimal(1);

    public abcBean() {
        propertySupport = new PropertyChangeSupport(this);
    }
    
    public String getSampleProperty() {
        return sampleProperty;
    }
    
    public void setSampleProperty(String value) {
        String oldValue = sampleProperty;
        sampleProperty = value;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    public BigDecimal getA() {
        return a;
    }

    public void incrementB(BigDecimal value) {
        BigDecimal oldValue = b;
        b=rowCounter.add(rowCounter.add(increment)); 
    }
    
  
    public void incrementC(BigDecimal value) {
        BigDecimal oldValue = c;
        c=rowCounter.multiply(rowCounter.add(increment));
    }
    
    
    
    
    public void setA(BigDecimal value) {
        BigDecimal oldValue = a;
        a = value;
    }
    
    public BigDecimal getB() {
        return b;
    }
    
    public void setB(BigDecimal value) {
        BigDecimal oldValue = b;
        b = value;
    }  
   
    public BigDecimal getC() {
        return c;
    }
    
    public void setC(BigDecimal value) {
        BigDecimal oldValue = c;
        c = value;
    }
    
    public BigDecimal getRowCounter() {
        return rowCounter;
    }
    
    public BigDecimal getArgumentRowCounter() {
        return argumentRowCounter;
    }
        
        
    public void setRowCounter(BigDecimal value) {
        BigDecimal oldValue = rowCounter;
        rowCounter = value;
    }
    
    public void incrementRowCounter(BigDecimal value) {
        BigDecimal augend = new BigDecimal(1); 
        BigDecimal oldValue = rowCounter;
        rowCounter=rowCounter.add(augend);
        incrementArgumentRowCounter(value);
    }
    
   private void incrementArgumentRowCounter(BigDecimal value) {
        BigDecimal augend = new BigDecimal(-2); 
        BigDecimal oldValue = rowCounter;
        argumentRowCounter=rowCounter.add(augend);
    } 
    
    
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

     public String toString()  {
         return new String
     (a + " " + b +  " " + c + "             ");
     }
    
    
}
