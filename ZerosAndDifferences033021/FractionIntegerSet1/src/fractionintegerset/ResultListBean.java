/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fractionintegerset;

import java.beans.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author HP_Administrator
 */
public class ResultListBean extends ArrayList implements Serializable {
    
    public static final String PROP_SAMPLE_PROPERTY = "resultList";
    private String sampleProperty;
    
    private List returnList = new ArrayList();
    
    private PropertyChangeSupport propertySupport;
    
    public ResultListBean() {
        propertySupport = new PropertyChangeSupport(this);
    }
    
    public String getlist() {
        return returnList.toString();
    }
    
    public void setList(String value) {
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
