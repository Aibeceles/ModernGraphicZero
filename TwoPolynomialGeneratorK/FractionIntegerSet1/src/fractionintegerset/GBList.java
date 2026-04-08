/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fractionintegerset;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener; 
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;

/**
 *
 * @author HP_Administrator
 */
public class GBList extends ArrayList implements Serializable {
    
    //public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    
    //private String sampleProperty;
    private Long threadId;
    private PropertyChangeSupport propertySupport;
    
    public GBList() {
        propertySupport = new PropertyChangeSupport(this);
    }
 
    public long getThreadId() {
        return threadId;
    }
    
    public void setThreadId(long value) {
        threadId=value;
    }
    
    
    public String getGBList() {
        return this.toString();
    }
    
     public synchronized void setGBList(muNumDen value) {
 //   public synchronized void setGBList(String value) {
 //    propertySupport.firePropertyChange(this.toString(), this, value);
         propertySupport.firePropertyChange("abc", this, value);
  //      System.out.println(" this.to string" + this.toString()+ " " );
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
}
