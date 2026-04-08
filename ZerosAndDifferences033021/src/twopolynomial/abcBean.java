/*
 * abcBean.java
 *
 * Created on February 6, 2011, 12:29 PM
 * stricltly a bean for recording a,b,c scalars in binomial P
 */

package twopolynomial;

import java.beans.*;
import java.io.Serializable;
import java.math.*;
/**
 * @author Tomas
 */
public class abcBean extends Object implements Serializable {
    // maybe throw abcBean onto plv: numberical evidence
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
   //     b = value;
 
        b=rowCounter.add(rowCounter.add(increment)); 
        
//        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
  
        public void incrementC(BigDecimal value) {
        BigDecimal oldValue = c;
       // c = value;
  
        c=rowCounter.multiply(rowCounter.add(increment));
//        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    
    
    
    public void setA(BigDecimal value) {
        BigDecimal oldValue = a;
        a = value;
//        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    public BigDecimal getB() {
        return b;
    }
    
    public void setB(BigDecimal value) {
        BigDecimal oldValue = b;
        b = value;
//        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }  
   
    public BigDecimal getC() {
        return c;
    }
    
    public void setC(BigDecimal value) {
        BigDecimal oldValue = c;
        c = value;
//        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
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
        System.out.println("ROWCounter:" + rowCounter);
//        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    public void incrementRowCounter(BigDecimal value) {
        BigDecimal augend = new BigDecimal(1); 
        BigDecimal oldValue = rowCounter;
//      rowCounter = value;
        
       rowCounter=rowCounter.add(augend);
       incrementArgumentRowCounter(value);
       System.out.println("rowCounter:" + rowCounter);
//        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
   private void incrementArgumentRowCounter(BigDecimal value) {
        BigDecimal augend = new BigDecimal(-2); 
        BigDecimal oldValue = rowCounter;
//      rowCounter = value;
        
       argumentRowCounter=rowCounter.add(augend);
       System.out.println("argumentRowCounter:" + argumentRowCounter);
//        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
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
