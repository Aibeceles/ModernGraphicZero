/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopLists;

import fractionintegerset.*;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener; 
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 * this is the implementation of LoopListenerRunnable for naming DOSP's at Psi_i
 * 
 * Psi_i represented as pArray is associated with a collection of LoopList, each a list of DeltaDelta_j values. 
 * LoopList is also a wrapper for state fields.   
 * 
 * LoopListRunnable has a constructor not including the congruence classes, nor does the initializer included operations for these classes. 
 * 
 * method setGBList otherwise overides ArrayList set method.
 * @author HP_Administrator
 * 
 */
public class LoopListRunnable extends ArrayList implements Serializable {
    
    //public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    
    //private String sampleProperty;
    private Long threadId;
    private PropertyChangeSupport propertySupport;
    private ResultListBean rListB;
    private int dimension;
    private int integerRange;
    private int workNum;
    private List deltaList;
 //   private List deltaTerm = new ArrayList();
 //   private List ancestorTerm = new ArrayList();
    private List ancestorlist = new ArrayList();
    private List cashedrListB = new ArrayList();         // maybe just declared and not also constructed because is assigned in constructor
    private List thisList=new ArrayList();
    private MatrixA aMatrix;
    
    public LoopListRunnable() {
        propertySupport = new PropertyChangeSupport(this);
        
    }
 
    public LoopListRunnable(ResultListBean rListB,int dimension,int integerRange,int workNum, MatrixA aMatrix) {
        propertySupport = new PropertyChangeSupport(this);
        this.rListB=rListB;
        this.dimension=dimension;
        this.integerRange=integerRange;
        this.workNum=workNum;
   //     this.deltaList=deltaList;
   //     this.ancestorlist=ancestorlist;
   //     this.cashedrListB=cashedrListB;
        this.aMatrix= new MatrixA(dimension,dimension+2,aMatrix);
        initializeList();
      //  firstdeltaTermInit();
    }   
       
    //private method for populating gbList.  needs reference to rList.   
    
    private void subsequentWorkerListInit() {
        
    //iterate over integer range
    // for each integer in range
    // subtract entries at integer of prior looplist
    BigDecimal arg1=new BigDecimal(1); 
    BigDecimal arg2=new BigDecimal(1);
    BigDecimal arg11=new BigDecimal(1); 
    BigDecimal arg22=new BigDecimal(1);
    BigDecimal difference=new BigDecimal(1);
    BigDecimal listResult=new BigDecimal(1);      //result
    BigDecimal listResult1=new BigDecimal(1);
    BigDecimal zero=new BigDecimal(0);
    BigDecimal bigDecimalRange=new BigDecimal(1); // synchronized with x
    BigDecimal bigDecimalDimension=new BigDecimal(dimension);
    BigDecimal one=new BigDecimal(1);
    
System.out.println("subsequent ancestorlist: "+ancestorlist.toString());
System.out.println("subsequent deltalist: "+deltaList.toString());
for (int x=workNum-1;  x<workNum; x++){    
//  System.out.println("workNum: "+workNum);
//  System.out.println("x: "+x);
 // System.out.println("((LoopList)rListB.get(x)).toString():"+((LoopList)rListB.get(x)).toString());
    for (int y=0;  y<integerRange; y++){
 //    System.out.println("((LoopList)rListB.get(x)).get(y)).toString():"+(((LoopList)rListB.get(x)).get(y)).toString());  
 //    System.out.println("x: "+x+" y: "+y);
     arg1=(BigDecimal)((LoopListRunnable)rListB.get(x)).get(y);
     arg11=(BigDecimal)((List)ancestorlist.get(x)).get(y);
     if (y+1<integerRange)
     { 
         arg2=(BigDecimal)((LoopListRunnable)rListB.get(x)).get(y+1);
         arg22=(BigDecimal)((List)ancestorlist.get(x)).get(y+1);
     }
     else
     {arg2=zero;arg22=zero; }  
     listResult=arg2.subtract(arg1);
     listResult1=arg22.subtract(arg11);
     difference=listResult.subtract(listResult1);
 //    System.out.println("listResult: "+listResult.toString());
 //    this.
     ((List)deltaList.get(workNum)).add(difference);
     this.add(listResult);thisList.add(listResult);
     ((List)ancestorlist.get(workNum)).add(listResult1);
 //    this.ancestorTerm.add(listResult1);
    }   
    }
 //   System.out.println("subsequent this: "+this.toString());
     rListB.add(this);
     cashedrListB.add(this);
 //    ((List)ancestorlist.get(workNum)).add(listPower1);
 //    this.ancestorlist.add(ancestorTerm);
 //    System.out.println("subsequent rListB: "+rListB.toString()); 
    }
       
    
    private void firstWorkerListInit()  {
    BigDecimal listPower=new BigDecimal(1);      //result
    
    BigDecimal zero=new BigDecimal(0);
    BigDecimal bigDecimalRange=new BigDecimal(1); // synchronized with x
    BigDecimal bigDecimalDimension=new BigDecimal(dimension);
    BigDecimal one=new BigDecimal(1);
    BigDecimal listPower1=new BigDecimal(1);
    BigDecimal difference=new BigDecimal(1);
  //  this.add(zero);  // place 0^dimension onto looplist
 //  this.ancestorTerm.add(zero);
 //   System.out.println("firstWorkerListInit() workNum: "+workNum);
 //   System.out.println("integerRange: "+integerRange);
    for (int x=0;  x<integerRange; x++){

       listPower=bigDecimalRange.pow(dimension);
       listPower1=bigDecimalRange.pow(dimension+1);
   //    listPower=bigDecimalRange.multiply(bigDecimalDimension);
       difference=listPower1.subtract(listPower);
       ((List)deltaList.get(workNum)).add(difference);
       this.add(listPower);thisList.add(listPower);
       ((List)ancestorlist.get(workNum)).add(listPower1);
   //    this.ancestorTerm.add(listPower1);
       bigDecimalRange=bigDecimalRange.add(one);
    }   
    
    rListB.add(this);
    cashedrListB.add(this);
  //  this.ancestorlist.add(this.ancestorTerm);
     System.out.println("initial rListB: "+rListB.toString());
     System.out.println("initial ancestorlist: "+ancestorlist.toString());
     System.out.println("initial deltalist: "+deltaList.toString());
    }
    
    
    
    private void initializeList() {
      
        if (workNum==0) {
          System.out.println("workNum==0");
          firstWorkerListInit();   
    //      System.out.println("rListB: "+rListB.toString());
      } else {
       System.out.println("workNum>0 "+workNum);  
       subsequentWorkerListInit();   
       
      }
     this.aMatrix.setMatrix(thisList);
    System.out.println("rListB: "+rListB.toString());
        
    }
       
    public MatrixA getAMatrix() {
        return(this.aMatrix);
    }   
   
    
    
    public long getThreadId() {
        return threadId;
    }
    
    public void setThreadId(long value) {
        threadId=value;
    }
    
    
    
    public List getancestorlist() {
        return this.ancestorlist;
    }
            
            
            
    public String getGBList() {
        return this.toString();
    }
    
     public synchronized void setGBList(muNumDen value) {
 //   public synchronized void setGBList(String value) {
 //    propertySupport.firePropertyChange(this.toString(), this, value);
      System.out.println(" muNumDen.worker" + value.getWorkNum()+ " " );
         propertySupport.firePropertyChange("abc", "abc", value);
  //      System.out.println(" this.to string" + this.toString()+ " " );
  
     }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    private void firstdeltaTermInit() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    
    
    }
    
}
