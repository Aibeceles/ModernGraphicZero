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

import java.io.Serializable;
import java.util.Vector;
import java.math.BigDecimal;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
/**
 * @author Tomas   
 */
public class vertexVector extends Vector implements Serializable {
 public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private Boolean onVertexVector; 
    private BigDecimal degree;
    private BigDecimal twoSequence;
    private long aritPower;
    private String sampleProperty;
    private PropertyChangeSupport propertySupport;
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
    public vertexVector() {
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
    
    public long getaritPower () {
      return aritPower;
    }
    
    public void setaritPower (long tNum) {
        aritPower=tNum;
    }

    public void setTwoSequence(BigDecimal ccity) {
        this.twoSequence=ccity;
    }

    public BigDecimal getTwoSequence () {
      return this.twoSequence;
    }

    public void setDegree(BigDecimal ccity) {
        this.degree=ccity;
    }
    
    public BigDecimal getDegree () {
      return this.degree;
    }
  
    public void setVertex()  {
       this.add(degree); 
    }
    
    public void setVertexVector(vertexVector loopvector) {
       this.add(loopvector);
    }
    
    public vertexVector getVertexx(int index) {
        return(this.getVertexx(0));  
    }
     
    public Object getVertex(int index) {
        return(this.elementAt(index));  
    }
    
    public void addVertexAt (vertex thisVertex){
    int  j = -1;
     int tLindex = getOnVertexVector(thisVertex.getDegree());
     if (tLindex==j) {
          vertexVector degreeTerms = new vertexVector();
          degreeTerms.add(thisVertex);
          degreeTerms.setDegree(((vertex)thisVertex).getDegree()); 
          this.addElement(degreeTerms);
      } else {
         ((vertexVector)this.elementAt(tLindex)).add(thisVertex);
      }
    }
        
    public Integer getOnVertexVector(BigDecimal degree) {
    int index = -1;
      for (int i=0; i<this.size(); i++) {
      if ((   ((vertexVector)this.elementAt(i)).getDegree().compareTo(degree)) ==0 )    
      {
         index=i;
      }  
     }
     if (index==-1) {
     return -1;    
     } else {
     return index;  
     }
    }
    
    public Integer gettOnVertexVector(BigDecimal degree) {
    int index = -1; 
      for (int i=0; i<this.size(); i++) {
      if ((   ((vertex)this.elementAt(i)).getDegree().compareTo(degree)) ==0 )    
      {
         index=i;
      }
     }
     if (index==-1) {
     return -1;    
     } else {
     return index;  
     }
    }
    
   public boolean getOnVertexVector(String atCty, vertex vert) {
      
    return ((vertexVector)this.getVertex(0)).getDegree()== vert.getDegree();
   }
    
    public void setOnVertexVector(Boolean onVertexVector) {
        this.onVertexVector = onVertexVector;
    }

    public void propertyChange(PropertyChangeEvent evt) {
    }
       
    
    
    
    
    
    
}
 
      
