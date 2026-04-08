/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mucorrolationthreaded;

//import twopolynomial.*;
import java.beans.*;
import java.io.Serializable;
import java.math.*;
import java.util.List;
/**
 *
 * @author Aibes
 */
public class rowBean implements Serializable {
    
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private String sampleProperty;
    private PropertyChangeSupport propertySupport;
//    private List tLL=null;
    private BigDecimal trZero =null;
    private BigDecimal trTriangle=null;
    private BigDecimal muDecimal=null;
    private BigDecimal rootOne=null;
    private BigDecimal rootTwo=null;
    private BigDecimal a=null;
    private BigDecimal b=null;
    private BigDecimal c=null;
    private BigDecimal determinat=null;
    private BigDecimal numerator=null;
    private BigDecimal denominator=null;
    
    
       public rowBean() {
        propertySupport = new PropertyChangeSupport(this);
    }
  
      public BigDecimal getDenominator() {
        return denominator;
    }
    
    public void setDenominator(BigDecimal value) {
        BigDecimal oldValue = value;
        denominator = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
       
       
       
       
    public BigDecimal getNumerator() {
        return numerator;
    }
    
    public void setNumerator(BigDecimal value) {
        BigDecimal oldValue = value;
        numerator = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }    
       
       
       
   public BigDecimal getDeterminat() {
        return determinat;
    }
    
    public void setDeterminat(BigDecimal value) {
        BigDecimal oldValue = value;
        determinat = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
       
       
       
    public BigDecimal getC() {
        return c;
    }
    
    public void setC(BigDecimal value) {
        BigDecimal oldValue = value;
        c = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
       
   public BigDecimal getB() {
        return b;
    }
    
    public void setB(BigDecimal value) {
        BigDecimal oldValue = value;
        b = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    } 
       
    public BigDecimal getA() {
        return a;
    }
    
    public void setA(BigDecimal value) {
        BigDecimal oldValue = value;
        a = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
       
       
    public void setFields(BigDecimal trZeroo , BigDecimal trTrianglee , BigDecimal muDecimall, BigDecimal rootOnee , BigDecimal rootTwoo, BigDecimal aa , BigDecimal bb , BigDecimal cc , BigDecimal det,BigDecimal numeratorr, BigDecimal denominatorr) {
    setTrZero(trZeroo);
    setTrTriangle(trTrianglee);
    setMuDecimal(muDecimall);
    setRootOne(rootOnee);
    setRootTwo(rootTwoo);     
    setA(aa);
    setB(bb);
    setC(cc);
    setDeterminat(det);
    setNumerator(numeratorr);
    setDenominator(denominatorr);
    }    
       
    public BigDecimal getRootTwo() {
        return rootTwo;
    }
    
    public void setRootTwo(BigDecimal value) {
        BigDecimal oldValue = value;
        rootTwo = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
   
   public BigDecimal getRootOne() {
        return rootOne;
    }
    
    public void setRootOne(BigDecimal value) {
        BigDecimal oldValue = value;
        rootOne = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
   
   public BigDecimal getMuDecimal() {
        return muDecimal;
    }
    
    public void setMuDecimal(BigDecimal value) {
        BigDecimal oldValue = value;
        muDecimal = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
   
   public BigDecimal getTrTriangle() {
        return trTriangle;
    }
    
    public void setTrTriangle(BigDecimal value) {
        BigDecimal oldValue = value;
        trTriangle = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
      
   public BigDecimal getTrZero() {
        return trZero;
    }
    
    public void setTrZero(BigDecimal value) {
        BigDecimal oldValue = value;
        trZero = value;
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
