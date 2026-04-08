/*
 * vertexVector.java
 *
 * Created on February 6, 2011, 11:15 AM
 */

package twopolynomial;

import java.beans.*;
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
    // sending termsList needs to know where thisVertex belongs.  it is either at an existing degreeTerms or in next degreeTerms.     
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
     }                           //  big may 2011 modification with these to methods.
     if (index==-1) {
     return -1;    
     } else {
     return index;  
     }
    }
    
    public Integer gettOnVertexVector(BigDecimal degree) {
    int index = -1;                                            // this may not need to be implemented as modified getonVertexVector
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
 //      boolean result = false;
//        for (int i=0; i<this.size(); i++) {
      // return result;       
  //    if (   ((VertexVector)this.getVertex(0)).getCity()== atCty);
      {
 //        result = true;
//            i = (boolean)key.getOnVertexVector(((webbutton.VertexVector)key.getVertex(1)).getCity()); 
  //    }
        }    
 return ((vertexVector)this.getVertex(0)).getDegree()== vert.getDegree();
//      return result;   
    }
    
//* setOnVertexVector compares city.at to each element and is true if a match.
    
    public void setOnVertexVector(Boolean onVertexVector) {
    //  this.contains()
        this.onVertexVector = onVertexVector;
    }

    public void propertyChange(PropertyChangeEvent evt) {
    }
       
    
    
    
    
    
    
}
 
      
