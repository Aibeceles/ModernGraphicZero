/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fractionintegerset;

import LoopLists.MatrixA;
import PArrayReset.PolynomialArray;
import java.beans.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author HP_Administrator
 */
public class muNumDen implements Serializable {
    
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    
    private String sampleProperty;
    private String bString;
    private int bStringSize;
    private BigDecimal numerator = new BigDecimal(0);
    private BigDecimal denominator = new BigDecimal(0);
    private BigDecimal mu = new BigDecimal(0);
    private PropertyChangeSupport propertySupport;
    private int workNum;
    private BigDecimal modulo = new BigDecimal(0);
    private List ancestorlist;
    private MatrixA aMatrix;
    private PolynomialArray pArray;
    private ArrayList bList;
    private int centerIndex;
    private int verticalTranslateIndex;
  
    
//new muNumDen(numerator,denominator,nextBigDecimal,((ArrayList)nBinaryList.get(0)).toString(),((ArrayList)nBinaryList.get(0)).size(),((ArrayList)nBinaryList.get(0)))    
    
    public muNumDen(int numerator,int denominator ,BigDecimal mu, String bString, int bStringSize, ArrayList bList) {
      propertySupport = new PropertyChangeSupport(this);
      this.numerator=new BigDecimal(numerator);
      this.denominator=new BigDecimal(denominator);
      this.mu=mu;
      this.bString=bString;
      this.bStringSize=bStringSize;
      System.out.println("muNumDen constructor bString,bStringSize,bList: "+ bString+"  "+ this.bStringSize+" "+ bList.toString());
      this.bList=bList;
      System.out.println("bStringSize: "+this.bStringSize);
      if (this.bStringSize==2) {                                     //  functionality not included in current build.
          System.out.println("this.bStringSize==2 condition.");
          setVerticalTranslateIndexCenterIndex();
      }
    }


    public muNumDen(int numerator,int denominator ,BigDecimal mu, String bString, int bStringSize) {
      propertySupport = new PropertyChangeSupport(this);
      this.numerator=new BigDecimal(numerator);
      this.denominator=new BigDecimal(denominator);
      this.mu=mu;
      this.bString=bString;
      this.bStringSize=bStringSize;
      System.out.println("muNumDen constructor: "+ bString+"  "+ bStringSize);
      
    }


    public muNumDen(int numerator,int denominator ,BigDecimal mu) {
      propertySupport = new PropertyChangeSupport(this);
      this.numerator=new BigDecimal(numerator);
      this.denominator=new BigDecimal(denominator);
      this.mu=mu;
    }
    
    public muNumDen() {

    }
    
    public muNumDen(BigDecimal modulo ,int workNum) {
      this.workNum=workNum;
      this.modulo=modulo; 
    }
    
    public muNumDen(BigDecimal modulo ,int workNum,List aList) {
      this.workNum=workNum;
      this.modulo=modulo;
      this.ancestorlist=aList;
    }
    
    public muNumDen(BigDecimal modulo ,int workNum,List aList,MatrixA aMatrix,PolynomialArray pArray) {
      this.workNum=workNum;
      this.modulo=modulo;
      this.ancestorlist=aList;
      this.aMatrix=aMatrix;
      this.pArray=pArray;
    }
   
     
    private void setVerticalTranslateIndexCenterIndex() {
    int leastSignificant;
    int mostSignificant;

    mostSignificant=(int)bList.get(0);
    leastSignificant=(int)bList.get(1);
    System.out.println("");
    System.out.println("muNumDen.setVerticalTranslateIndexCenterIndex bList,mostSignificant,leastSignificant: "+ bList.toString()+" "+mostSignificant+" "+leastSignificant);
    System.out.println("");
    this.verticalTranslateIndex=(mostSignificant-leastSignificant)/2;  
    this.centerIndex=((this.verticalTranslateIndex)+leastSignificant);
    }
    
    public int getverticalTranslateIndex() {
        return(verticalTranslateIndex);
    }
    
    public int getcenterIndex () {
        return(centerIndex);
    }
    
    
    
    
    
    public PolynomialArray getpArray() {
        return(this.pArray);
    }
    

    public MatrixA getAMatrix() {
        return(this.aMatrix);
    }
    
  
    public void setbStringSize(int bStringSize) {
        this.bStringSize=bStringSize;
    }
    
    public int getbStringSize() {
        return(bStringSize);
    }
    
    public int getWorkNum() {
        return workNum;
    }
    
    public void setWorkNum(int value) {
    //    String oldValue = sampleProperty;
        workNum = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
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

   public String getBString() {
       return bString;
   }
   
   public ArrayList getBList() {
       return bList;
   }  
   
   
    public BigDecimal getMu() {
        return mu;
    }
    
    public void setMu(BigDecimal value) {
    //    String oldValue = sampleProperty;
        mu = value;
    //    propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }    
    
        public BigDecimal getModulo() {
        return modulo;
    }
    
    public void setModulo(BigDecimal value) {
    //    String oldValue = sampleProperty;
        modulo = value;
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
