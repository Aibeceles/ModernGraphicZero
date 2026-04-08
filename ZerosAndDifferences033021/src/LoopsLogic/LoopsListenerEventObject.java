/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopsLogic;

import fractionintegerset.*;
import java.beans.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author HP_Administrator
 */
public class LoopsListenerEventObject implements Serializable {
    
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    
    private String sampleProperty;
    private BigDecimal numerator = new BigDecimal(0);
    private BigDecimal denominator = new BigDecimal(0);
    private BigDecimal mu = new BigDecimal(0);
    
    private PropertyChangeSupport propertySupport;
    
    public LoopsListenerEventObject(int numerator,int denominator ,BigDecimal mu) {
      propertySupport = new PropertyChangeSupport(this);
      this.numerator=new BigDecimal(numerator);
      this.denominator=new BigDecimal(denominator);
      this.mu=mu;
    }
    
    public BigDecimal getnumerator() {
        return numerator;
    }
    
    public void setNumerator(BigDecimal value) {
    //    String oldValue = sampleProperty;
        numerator = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }    
    
    public BigDecimal getDenominator() {
        return denominator;
    }
    
    public void setDenominator(BigDecimal value) {
    //    String oldValue = sampleProperty;
        denominator = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    public BigDecimal getMu() {
        return mu;
    }
    
    public void setMu(BigDecimal value) {
    //    String oldValue = sampleProperty;
        mu = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
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
