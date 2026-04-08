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
public class nMaxNBean implements Serializable {



    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";

    private String sampleProperty;
    private BigDecimal n,maxN;




    private PropertyChangeSupport propertySupport;

    public nMaxNBean() {
        propertySupport = new PropertyChangeSupport(this);
    }


    public BigDecimal getn() {
        return n;
    }

    public void setn(BigDecimal formName) {
                n=formName;
   }


     public BigDecimal getmaxN() {
        return maxN;
    }

    public void setmaxN(BigDecimal formName) {
                maxN=formName;
   }

 
     public BigDecimal formulaN() { 
     BigDecimal formula = new BigDecimal(0);
     BigDecimal bTerm = new BigDecimal(0);
     bTerm=this.maxN.subtract(this.n);
     formula=this.maxN.subtract(bTerm);
     return bTerm;
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
