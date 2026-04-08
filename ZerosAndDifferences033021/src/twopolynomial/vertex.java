/*
 * vertex.java
 *
 * Created on February 6, 2011, 11:15 AM
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
    
    //vertex(argument,scalar,degree);
        public vertex(BigDecimal fromVertCity, BigDecimal toVertCity, BigDecimal TransitTime) {
        propertySupport = new PropertyChangeSupport(this);
        this.argument=fromVertCity;
        this.scalar=toVertCity;
        this.degree=TransitTime;
 //       this.WeekEnding=WeekEnding;
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
    
  //  public void setSampleProperty(String value) {
  //      String oldValue = sampleProperty;
  //      sampleProperty = value;
  //      propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
  //  }
    
    
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
