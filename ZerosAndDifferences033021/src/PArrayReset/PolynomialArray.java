/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PArrayReset;

import fractionintegerset.muNumDen;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener; 
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author HP_Administrator
 */
public class PolynomialArray extends ArrayList implements Serializable, Cloneable{
    
private int pDimension;
 private PropertyChangeSupport propertySupport;

public PolynomialArray(int pDimension) {
        propertySupport = new PropertyChangeSupport(this);
        this.pDimension=pDimension;
        initPArray();
}

public PolynomialArray(int pDimension, int fig) {
        propertySupport = new PropertyChangeSupport(this);
        this.pDimension=pDimension;
        initPArrayFig(fig);
}

public PolynomialArray(int pDimension, int fig, boolean noBufferRun ) {
        propertySupport = new PropertyChangeSupport(this);
        this.pDimension=pDimension;
        initPArrayFig(fig,noBufferRun);
}



public PolynomialArray(int constant, int linear, int square) {
        propertySupport = new PropertyChangeSupport(this);
        this.add(constant);this.add(linear);this.add(square);
}

public PolynomialArray(int pDimension, Boolean ok) {
        propertySupport = new PropertyChangeSupport(this);
        this.pDimension=pDimension;
        initTPPArray();
}

public PolynomialArray(int pDimension, Boolean ok, int resetValue) {
        propertySupport = new PropertyChangeSupport(this);
        this.pDimension=pDimension;
        initTPPArray(resetValue);
}

public PolynomialArray(int pDimension, Boolean ok, ArrayList resetValue) {
        propertySupport = new PropertyChangeSupport(this);
        this.pDimension=pDimension;
        initTPPArray(resetValue);
}

private synchronized void initTPPArray(ArrayList resetValue)  {
    this.add(resetValue.get(0)); 
 //   this.add(-1);
    for (int x=0; x<(pDimension); x++){
    this.add(resetValue.get(x+1)); 
    }    
}

private synchronized void initTPPArray(int resetValue)  {
    this.add(0); 
 //   this.add(-1);
    for (int x=0; x<(pDimension); x++){
    this.add(resetValue); 
    }    
}

/*
*   List of size dimension+1 with each value=0.  
*/

private synchronized void initTPPArray()  {
    this.add(0); 
 //   this.add(-1);
    for (int x=0; x<(pDimension); x++){
    this.add(0); 
    }    
}



public void pSetFields(int constant, int linear, int square) {
   this.add(constant);this.add(linear);this.add(square); 
    
}

@Override
public Object clone() {
   
        PolynomialArray pArrayCopy = (PolynomialArray)super.clone();
        return(pArrayCopy);
       
}

private synchronized void initPArray() {
     this.add(1); 
    for (int x=0; x<(pDimension); x++){
      this.add(0);
   } 

 //  this.add(0);
}

private synchronized void initPArrayFig(int fig) {
   for (int x=0; x<(pDimension-1); x++){
      this.add(0);
   } 
   this.add(fig); 
  // this.add(0);
}


private synchronized void initPArrayFig(int fig, boolean noBufferRun) {
   for (int x=0; x<(pDimension); x++){
      this.add(0);
   } 
   this.add(fig); 
  // this.add(0);
}





private synchronized void initTPFigPArray( )  {
    
    
}









public synchronized void incrementP(int index){             
    // System.out.println("                                                              PolynomialArray.incrementP   pArray:   "+this.toString());
    // System.out.println("                                                              PolynomiaArray.increment index:   "+index);
    int coefficient=(int)this.get(index)+1;
    this.set(index, coefficient);
}


public synchronized int getpValue(int index) {
    return (int)this.get(index);
}




public synchronized void decrementP(int index){
    int coefficient=(int)this.get(index)-1;
    this.set(index, coefficient);
}


public synchronized int getPdimension(){
return(pDimension);    
}

public synchronized void setPdimension(int pDimension){
    this.pDimension=pDimension;    
}

public synchronized void setpArray(muNumDen value){
//    System.out.println("PolynomialArray.setpArray                                PolynomialArray.setpArray");
    propertySupport.firePropertyChange("abc", "abc", value) ;    
}


public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }


}
