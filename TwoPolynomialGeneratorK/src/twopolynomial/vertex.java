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

import java.math.*;
import java.beans.*;
import java.io.Serializable;

/**
 * @author Tomas
 */
public class vertex extends Object implements Serializable {
  private BigDecimal argument,scalar;
  private BigDecimal degree; 
  
    private String sampleProperty;
    private long aritDepth ;
    private int termMatch;
    
    public vertex(BigDecimal fromVertCity, BigDecimal toVertCity, BigDecimal TransitTime) {
        propertySupport = new PropertyChangeSupport(this);
        this.argument=fromVertCity;
        this.scalar=toVertCity;
        this.degree=TransitTime;
       }
        
    public long getaritDepth() {
        return aritDepth;
    }
    
    public void setaritDepth(long formName) {
                aritDepth=formName;
   }   
    
    public int getTermMatch() {
        return termMatch;
    }
    
    public void setTermMatch(int formName) {
                termMatch=formName;
   }  
    
   private PropertyChangeSupport propertySupport;
    
    public vertex() {
        propertySupport = new PropertyChangeSupport(this);
    }
    
      public BigDecimal getDegree() {
        return degree;
    }
    
    public void setdegree(BigDecimal formName) {
                degree=formName;
   }
 
    public BigDecimal getArgument() {
        return argument;
    }
    
    public void setArgument(BigDecimal formName) {
                argument=formName;                
   }  
    
    public BigDecimal getScalar() {
        return scalar;
    }
    
    public void setScalar(BigDecimal formName) {
                scalar=formName;  
    }
    
   public String getSampleProperty() {
        return sampleProperty;
    }
    
   public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
    @Override
     public String toString()  {
         return new String
     (argument + " " + scalar +  " " + degree + " " + aritDepth + " " + termMatch);
     }


}
