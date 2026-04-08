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
 * @author Tomas    abcededdasdf
 */
public class twoSeq extends Object implements Serializable {
    
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private String sampleProperty;
    private BigDecimal tSeq = new BigDecimal(0);
    private PropertyChangeSupport propertySupport;
    
    public twoSeq() {
        propertySupport = new PropertyChangeSupport(this);
    }
  
        public BigDecimal gettSeq() {
        return tSeq;
    }
    
    public void settSeq(BigDecimal value) {
        String oldValue = sampleProperty;
        tSeq = value;
     }
    
    public void addOne() {     //  dummy method.  needs to be tSeq=tSeq.add(one);
    BigDecimal one = new BigDecimal(1);
    tSeq.add(one);
    }
    
       
    public void incrementtSeq(BigDecimal rowC) {
        String oldValue = sampleProperty;
        BigDecimal four = new BigDecimal(4);
        BigDecimal three = new BigDecimal(3);
        BigDecimal one = new BigDecimal(1);
        BigDecimal two = new BigDecimal(2);
        if (rowC.compareTo(two)==-1)   {
            tSeq=one;
        }
        else {
        if (rowC.compareTo(two)==0) {
            tSeq=two;
        } else {
            tSeq=tSeq.multiply(two);
        }    
        }
   }
    
   public String getSampleProperty() {
        return sampleProperty;
    }
    
    public void setSampleProperty(String value) {
        String oldValue = sampleProperty;
        sampleProperty = value;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
}
