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
 * 
 * @author Tomas
 */
public class pArgument extends Object implements Serializable, PropertyChangeListener {
    
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
        if (aArgument==1) {
          aArgument=0;   
          } else {
          aArgument=1;
         }
        this.aArgument = aArgument;
        propertySupport.firePropertyChange ("aArgument", new Integer (oldAArgument), new Integer (aArgument));
    }
   
    
        public void propertyChange(PropertyChangeEvent evt) {
    
  
    }
}

    

