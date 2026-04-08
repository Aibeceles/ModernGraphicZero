/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

 
     public BigDecimal formulaN() {    //I still have to check that this is synceronized with f(N)=2^N, F(n-1)=  2^(n-1)
     // compute scalar adjustment f(n,maxN)
    // can return formula or bTerm,  bTerm seems to be the correct value.
         BigDecimal formula = new BigDecimal(0);
         BigDecimal bTerm = new BigDecimal(0);
         bTerm=this.maxN.subtract(this.n);
         System.out.println("nMaxNBean.formulaN bTerm,maxN,n: "+bTerm.toString()+" "+this.maxN+" "+this.n.toString());
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
