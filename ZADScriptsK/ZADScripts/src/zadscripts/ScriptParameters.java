/*
 * Copyright (C) 2020 ChiefQuippy
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
package zadscripts;

import java.beans.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author ChiefQuippy
 */
public class ScriptParameters implements Serializable {
    
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    
    private String sampleProperty;
    
    private final String S1ScriptSuffex="S1"; 
    private final String S2ScriptSuffex="S3T";            
    private final String S3ScriptSuffex="S3A";

    private String writeDimension;
    private String index;
    private String lowRange;
    private String highRange;
    private String readDimension1;
    private String readDimension2;
    private String nextWriteDimension;           

   
   public String getS1ScriptSuffex() {
        return S1ScriptSuffex;
   }  
    
   public String getS2ScriptSuffex() {
        return S2ScriptSuffex;
   }
   
   public String getS3ScriptSuffex() {
        return S3ScriptSuffex;
   }
   
    
   public String getnexWriteDimension() {
        return nextWriteDimension;
    }
    
    public void setnextWriteDimension(String value) {
        nextWriteDimension = value;
    }

    public String getreadDimension2() {
        return readDimension2;
    }
    
    public void setreadDimension2(String value) {
        readDimension2 = value;
    }    

    public String getreadDimension1() {
        return readDimension1;
    }
    
    public void setreadDimension1(String value) {
        readDimension1 = value;
    }
 
    public String gethighRange() {
        return highRange;
    }
    
    public void sethighRange(String value) {
        highRange = value;
    }

    public String getwriteDimension() {
        return writeDimension;
    }
    
    public void setwriteDimension(String value) {
        writeDimension = value;
    }
    
    public String getindex() {
        return index;
    }
    
    public void setindex(String value) {
        index = value;
    }
    
    public String getlowRange() {
        return lowRange;
    }
    
    public void setlowRange(String value) {
        lowRange = value;
    }   
    
    
    
    
    
    
    
            
            
    private PropertyChangeSupport propertySupport;
    
    public ScriptParameters(String[] args) {
        propertySupport = new PropertyChangeSupport(this);
        BigDecimal two = new BigDecimal(2);
        this.setlowRange(args[0]);
        this.sethighRange(args[1]);
        this.setindex(args[2]);
        this.setreadDimension1(args[3]);
        this.setreadDimension2(args[3]);
        BigDecimal wDimension = new BigDecimal(args[3]);
        wDimension=wDimension.multiply(two);
        this.setwriteDimension(wDimension.toString());
        BigDecimal wDimension1 = new BigDecimal (wDimension.toString());
        wDimension1=wDimension1.multiply(two);
        this.setnextWriteDimension(wDimension1.toString());


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
