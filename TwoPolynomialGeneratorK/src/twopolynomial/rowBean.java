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
 * @author Aibes
 */
public class rowBean implements Serializable {
    
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private String sampleProperty;
    private PropertyChangeSupport propertySupport;
    private vertexVector tLL=null;
    private BigDecimal NN=null;
    private BigDecimal flatFileRowCounterr=null;
    private BigDecimal nMaxx=null;   
    private BigDecimal tSeqDB=null;
    private BigDecimal bTermDB=null;
    private vertex vertexDB=null;
    
     public rowBean() {
        propertySupport = new PropertyChangeSupport(this);
    }

    public rowBean(BigDecimal NN,BigDecimal flatFileRowCounterr,BigDecimal nMaxx,BigDecimal tSeqDB,BigDecimal bTermDB,vertex vertexDB) {
        this.NN=NN;
        this.flatFileRowCounterr=flatFileRowCounterr;
        this.nMaxx=nMaxx;
        this.tSeqDB=tSeqDB;
        this.bTermDB=bTermDB;
        this.vertexDB=vertexDB;
        propertySupport = new PropertyChangeSupport(this);
    }     
     
    public void setvertex(vertex value){
      this.vertexDB=value;    
    }   
       
    public vertex getvertex() {
        return (this.vertexDB);
    } 
     
    public void setflatFileRowCounterr(BigDecimal value){
      this.flatFileRowCounterr=value;    
    }   
       
    public BigDecimal getflatFileRowCounterr() {
        return (this.flatFileRowCounterr);
    }     
     
    public void settSeqDB(BigDecimal value){
      this.tSeqDB=value;    
    }   
       
    public BigDecimal gettSeqDB() {
        return (this.tSeqDB);
    }   
    
   public void setbTermDB(BigDecimal value){
      this.bTermDB=value;    
    }   
       
    public BigDecimal getbTermDB() {
        return (this.bTermDB);
    }   
      
     public void setRunThreadFields(BigDecimal N, BigDecimal nMax){
   
    setNN(N);   
    setNMaxx(nMax);
    
   }  
    
   public void setFields(vertexVector tL,BigDecimal N, BigDecimal flatFileRowCounter, BigDecimal nMax){
    setTL(tL);   
    setNN(N);   
    setFlatFileRowCounter(flatFileRowCounter);
    setNMaxx(nMax);
    
   }
       
       
    public vertexVector getTL() {
        return tLL;
    }
    
    public void setTL(vertexVector value) {
        vertexVector oldValue = tLL;
        tLL = value;
     //   propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
   
        public BigDecimal getNN() {
        return NN;
    }
    
    public void setNN(BigDecimal value) {
        BigDecimal oldValue = NN;
        NN = value;
    }
    
        public BigDecimal getFlatFileRowCounter() {
        return flatFileRowCounterr;
    }
    
    public void setFlatFileRowCounter(BigDecimal value) {
        BigDecimal oldValue = flatFileRowCounterr;
        flatFileRowCounterr = value;
   }
       
    public BigDecimal getNMax() {
        return nMaxx;
    }
    
    public void setNMaxx(BigDecimal value) {
        BigDecimal oldValue = nMaxx;
        nMaxx = value;
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
