/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mucorrolationthreaded;

import java.math.BigDecimal;

/**
 *
 * @author Aibes
 */
public class GaussBean {

double x1;
double y1;
double x2;
double y2;
double x3;
double y3;
BigDecimal trZero=new BigDecimal(1);
BigDecimal trTr=new BigDecimal(1);
BigDecimal root1=new BigDecimal(1); 
BigDecimal root2=new BigDecimal(1);
BigDecimal root3=new BigDecimal(1);

public GaussBean(double x11, double y11,double x22,double y22, double x33, double y33,BigDecimal trZero1,BigDecimal trTr1, BigDecimal root11, BigDecimal root22, BigDecimal root33  ) {

x1=x11;
x2=x22;
x3=x33;
y1=y11;
y2=y22;
y3=y33;
root1=root11;
root2=root22;
root3=root33;
trZero=trZero1;
trTr=trTr1;
}    
    public BigDecimal getRoot3() {
        return root3;
    }
    
    public void setRoot3(BigDecimal value) {
        BigDecimal oldValue = value;
        root3 = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    } 
    

    public BigDecimal getRoot2() {
        return root2;
    }
    
    public void setRoot2(BigDecimal value) {
        BigDecimal oldValue = value;
        root2 = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }    


    public BigDecimal getRoot1() {
        return root1;
    }
    
    public void setRoot1(BigDecimal value) {
        BigDecimal oldValue = value;
        root1 = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
   

    public BigDecimal getTrTr() {
        return trTr;
    }
    
    public void setTrTr(BigDecimal value) {
        BigDecimal oldValue = value;
        trTr = value;
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



    public double getX1() {
        return x1;
    }
    
    public void setX1(double value) {
        double oldValue = value;
        x1 = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }


   public double getX2() {
        return x2;
    }
    
    public void setX2(double value) {
        double oldValue = value;
        x2 = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    public double getX3() {
        return x3;
    }
    
    public void setX3(double value) {
        double oldValue = value;
        x3 = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    public double getY1() {
        return y1;
    }
    
    public void setY1(double value) {
        double oldValue = value;
        y1 = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    
  public double getY2() {
        return y2;
    }
    
    public void setY2(double value) {
        double oldValue = value;
        y2 = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }  
    
    public double getY3() {
        return y3;
    }
    
    public void setY3(double value) {
        double oldValue = value;
        y3 = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    
}





