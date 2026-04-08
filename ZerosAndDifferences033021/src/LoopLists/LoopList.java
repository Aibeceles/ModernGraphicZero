/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopLists;

import LoopsLogic.ModuloList;
import PArrayReset.PolynomialArray;
import fractionintegerset.*;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener; 
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Psi_i represented as pArray is associated with a collection of LoopList, each a list of DeltaDelta_j values. 
 * LoopList is also a wrapper for state fields.   
 * 
 * 
 * method setGBList otherwise overides ArrayList set method.
 * @author HP_Administrator
 */
public class LoopList extends ArrayList implements Serializable {
    
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

    private List muList=new ArrayList();
    private List rootList=new ArrayList();
    private int numerator;
    private int denominator;
    private int totalZero;

    
    private ModuloList moduloList;
    private String vmResult;
    private BigDecimal[] newtonCoeffs;
    
    
    public LoopList() {
        propertySupport = new PropertyChangeSupport(this);
        
    }
     
    /* The copy constructor
    
    */
        
    public LoopList (LoopList origonalLoopList, ResultListBean cashedRListB)  {
  //   rListB=cashedRListB;                                                            // wrong 
     this.dimension=origonalLoopList.dimension;
     this.integerRange=origonalLoopList.integerRange;
     this.workNum=origonalLoopList.workNum;
//    int halfIntegerRange=integerRange/2;
     for (int i=0;i<thisList.size();i++) {
         ((List)this.thisList).add(((List)origonalLoopList.thisList).get(i));
     }   
     BigDecimal listVariable;
     for (int i=0;i<origonalLoopList.size();i++) {
         listVariable=(BigDecimal)origonalLoopList.get(i);
         this.add(listVariable);
     }
//     System.out.println("LoopList  copy constructor  old LoopList "+ origonalLoopList.toString());
//     System.out.println("LoopList  copy constructor  new LoopList "+ this.toString());
    }
    
   
    
       public LoopList(int mDimension, ResultListBean rListB,int dimension,int integerRange,int workNum, MatrixA aMatrix, PolynomialArray pArray,ModuloList moduloList,Boolean b) {
        propertySupport = new PropertyChangeSupport(this);
        this.rListB=rListB;
        this.dimension=dimension;
        this.integerRange=integerRange;
        this.workNum=workNum;
        this.moduloList=moduloList;
        initializePArrayLoopList(pArray,b);
     } 
    

    
/*
 * pArray is infact figParry...   Did not properly name variable.     
*/
       
    public LoopList(int mDimension, ResultListBean rListB,int dimension,int integerRange,int workNum, MatrixA aMatrix, PolynomialArray pArray,ModuloList moduloList) {
        propertySupport = new PropertyChangeSupport(this);
        this.rListB=rListB;
        this.dimension=dimension;
        this.integerRange=integerRange;
        this.workNum=workNum;
        this.moduloList=moduloList;
        initializePArrayLoopList(pArray);
     } 





    public LoopList(int mDimension, ResultListBean rListB,int dimension,int integerRange,int workNum, MatrixA aMatrix,List transferList) {
        propertySupport = new PropertyChangeSupport(this);
        this.rListB=rListB;
        this.dimension=dimension;
        this.integerRange=integerRange;
        this.workNum=workNum;
       initializeTransferLoopList(transferList);
     }  
    
    public LoopList(int mDimension, ResultListBean rListB,int dimension,int integerRange,int workNum,List deltaList,List ancestorlist,List cashedrListB, MatrixA aMatrix) {
        propertySupport = new PropertyChangeSupport(this);
        this.rListB=rListB;
        this.dimension=dimension;
        this.integerRange=integerRange;
        this.workNum=workNum;
        this.deltaList=deltaList;
        this.ancestorlist=ancestorlist;
        this.cashedrListB=cashedrListB;
        initializeList();
    }   
      
    private void LoadTransferList(List transferList){
        System.out.println("                                                            LoopListLoadTransferList start");
 //    int halfIntegerRange=integerRange/2; 
     for (int n=0;n<transferList.size();n++) {
          this.add(transferList.get(n));
          thisList.add(transferList.get(n));
      } 
 //       System.out.println("LoopListLoadTransferList fall through" + this.toString());
 
    }
    
//  evaluate pArray (figPArray) over integerRange are Looplists_i  
     
 private void loadPArray(PolynomialArray pArray)  {
     
    BigDecimal listValue=new BigDecimal(1);      //result
    BigDecimal listValueResult=new BigDecimal(0);   
    BigDecimal zero=new BigDecimal(0);
    BigDecimal bigDecimalRange=new BigDecimal(0); // synchronized with x
    BigDecimal bigDecimalDimension=new BigDecimal(dimension);
    BigDecimal one=new BigDecimal(1);
    BigDecimal listPower1=new BigDecimal(1);
    BigDecimal result=new BigDecimal(0);
    int caiIndex=0;
//    BigDecimal currentPcoeff=new BigDecimal(0);
//    System.out.println("looplist.loadPArray.dimension: " + dimension); 
//    System.out.println("looplist.loadPArray-no boolean: " + pArray.toString());
    int halfIntegerRange=integerRange/2;
//      for(int x=0;  x<(integerRange); x++){
    for(int x=-halfIntegerRange;  x<(halfIntegerRange); x++){
      listValueResult=zero;
      BigDecimal currentK=new BigDecimal(x);                                                     // k as in Psi_i, triag,triag_j, k   
 //     System.out.println("LoopList.loadPArray.x: " + x); 
      for (int y=0;  y<(dimension+1); y++){          // currentPcoeff is scalar 
          BigDecimal currentPcoeff=new BigDecimal((int)pArray.get(y));
   //       System.out.println("currentPcoeff: " + currentPcoeff.toString() +  "  y: "+y+"    currentK: "+ currentK.toString()); 
   //       System.out.println("currentK.pow(y+1): " + currentK.pow(y+1).toString()); 
          listValue=currentK.pow(y);                                   // listValue=currentK.pow(y+1);
   //       System.out.println("listValue: " + listValue.toString() +  "  "); 
          listValueResult=listValueResult.add(listValue.multiply(currentPcoeff));
      }
//      System.out.println("LoopList.loadPArray.listValueResult: "+listValueResult.toString());
      this.add(listValueResult);thisList.add(listValueResult);
//      if (listValueResult.compareTo(zero)==0)) {muList.add(x+1-caiIndex); caiIndex++;}
//      if (listValueResult.compareTo(zero)==0 && x>(halfIntegerRange-1)) {muList.add(x+1-caiIndex-halfIntegerRange-1); caiIndex++;}
//       if (listValueResult.compareTo(zero)==0 && (x>0)) {muList.add(x+1-caiIndex); caiIndex++;}
    if (listValueResult.compareTo(zero)==0 && (x>0)) {
 //       System.out.println("loadPArray x,cai: " + x + " " + caiIndex);
        muList.add(x-caiIndex); caiIndex++;
    }
    if (listValueResult.compareTo(zero)==0) {
        rootList.add(x);  // Store ALL roots (including negative)
        totalZero++;
    }
      //  bigDecimalRange=bigDecimalRange.add(one);
         listValue=zero;
    }   
  //    rListB.add(this);

//  System.out.println("loadPArray(PolynomialArray pArray) ");
  setNumDen();  
 }
  
 private void loadPArray(PolynomialArray pArray,boolean b)  {
    BigDecimal listValue=new BigDecimal(1);      //result
    BigDecimal listValueResult=new BigDecimal(0);   
    BigDecimal zero=new BigDecimal(0);
    BigDecimal bigDecimalRange=new BigDecimal(0); // synchronized with x
    BigDecimal bigDecimalDimension=new BigDecimal(dimension);
    BigDecimal one=new BigDecimal(1);
    BigDecimal listPower1=new BigDecimal(1);
    BigDecimal result=new BigDecimal(0);
    int caiIndex=0;
//    BigDecimal currentPcoeff=new BigDecimal(0);
//    System.out.println("looplist.loadPArray.dimension: " + dimension); 
    int halfIntegerRange=integerRange/2;     // will throw error if odd integerRange
//    System.out.println("looplist.loadPArray: " + pArray.toString());
    for(int x=-halfIntegerRange;  x<(halfIntegerRange); x++){
      listValueResult=zero;
      BigDecimal currentK=new BigDecimal(x);                                                     // k as in Psi_i, triag,triag_j, k   
  //    System.out.println("loadPArray: " + x); 
      BigDecimal currentPcoeff1=new BigDecimal((int)pArray.get(0));
      listValueResult=listValueResult.add(currentPcoeff1);
//      for (int y=0;  y<(dimension+1); y++){          // currentPcoeff is scalar 
      for (int y=0;  y<(dimension); y++){          // currentPcoeff is scalar
          BigDecimal currentPcoeff=new BigDecimal((int)pArray.get(y+1));
//          System.out.println("currentPcoeff: " + currentPcoeff.toString() +  "  "); 
          listValue=currentK.pow(y+1);
//          System.out.println("listValue: " + listValue.toString() +  "  ");
          listValueResult=listValueResult.add(listValue.multiply(currentPcoeff));
//          System.out.println("listValueResult: " + listValueResult.toString() +  "  ");
      }
//      System.out.println();
//      if (listValueResult.compareTo(zero)==0) {muList.add(x+1-caiIndex); caiIndex++;}
//      if (listValueResult.compareTo(zero)==0 && x>(halfIntegerRange-1)) {muList.add(x+1-caiIndex-halfIntegerRange-1); caiIndex++;}
      if (listValueResult.compareTo(zero)==0 && (x>0)) {muList.add(x-caiIndex); caiIndex++;}
      if (listValueResult.compareTo(zero)==0) {
          rootList.add(x);  // Store ALL roots (including negative)
          totalZero++;
      }
      this.add(listValueResult);thisList.add(listValueResult);
     //  bigDecimalRange=bigDecimalRange.add(one);
         listValue=zero;
    }   
  //    rListB.add(this);
//  System.out.println("LOADPARRAY:"+"                          "+this.toString());
  //System.out.println("loadPArray(PolynomialArray pArray,boolean b) ");
   setNumDen();
 }
 
   private void initializePArrayLoopList(PolynomialArray pArray,boolean b) {
        if (workNum==0) {
          loadPArray(pArray,b);   
      } else {
       subsequentWorkerListInit();   
      }
    }

    private void initializePArrayLoopList(PolynomialArray pArray) {
        if (workNum==0) {
          loadPArray(pArray,true);        
      } else {
       subsequentWorkerListInit();   
      }
    }

    private void initializeTransferLoopList(List transferList) {
        if (workNum==0) {
          LoadTransferList(transferList);   
      } else {
       subsequentWorkerListInit();   
      }
    }
      
      
       
    /*it performs the triagTriag subtraction. private method for populating gbList.  needs reference to rList.   
    */
    
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
    BigDecimal factordResult=new BigDecimal(1);
    int moduloIndex;
    int caiIndex=0;
//System.out.println("LoopList.subsequentWorkerListInitWorkNum : ");
//System.out.println("subsequent deltalist: "+deltaList.toString());
//System.out.println("LoopList.subsequentWorkerListInit.worknum,integerRange: "+workNum+ "  " + integerRange);
//System.out.println("LoopList.subsequentWorkerListInit.rListB1: "+rListB.toString());
 int halfIntegerRange=integerRange/2;
for (int x=workNum-1;  x<workNum; x++){    
//  System.out.println("workNum: "+workNum);
//  System.out.println("x: "+x);
 // System.out.println("((LoopList)rListB.get(x)).toString():"+((LoopList)rListB.get(x)).toString());
 for (int y=0;  y<integerRange; y++){
// for(int y=-halfIntegerRange;  y<(halfIntegerRange); y++){    
 //for(int y=-halfIntegerRange;  y<(halfIntegerRange); y++){
 //    System.out.println("((LoopList)rListB.get(x)).get(y)).toString():"+(((LoopList)rListB.get(x)).get(y)).toString());  
 //    System.out.println("x: "+x+" y: "+y);
     arg1=(BigDecimal)((LoopList)rListB.get(x)).get(y);
 //    arg11=(BigDecimal)((List)ancestorlist.get(x)).get(y);
    // System.out.println("arg1, arg11: "+arg1.toString()+"   "+arg11.toString());
     
     if (y+1<integerRange)
     { 
         arg2=(BigDecimal)((LoopList)rListB.get(x)).get(y+1);
 //        arg22=(BigDecimal)((List)ancestorlist.get(x)).get(y+1);
     }
     else
     {arg2=zero;arg22=zero; }  
     listResult=arg2.subtract(arg1);
//     System.out.println("arg2,arg1: "+arg2.toString()+"  "+arg1);
     //  listResult1=arg22.subtract(arg11);
   //  difference=listResult.subtract(listResult1);
    moduloIndex=workNum-1;
//     System.out.println("listResult, moduloIndex: "+listResult.toString()+"  "+moduloIndex);
//     System.out.println("moduloList: "+moduloList.toString()); 
//  try {  //System.out.println("modulo value: "+(BigDecimal)moduloList.get(moduloIndex)); 
//   try {
//     factordResult=listResult.divide((BigDecimal)moduloList.get(moduloIndex));
//   } catch (ArithmeticException exx) {factordResult=listResult; }
//   }
//  catch (NullPointerException ex) {
//     System.out.println(ex); 
//  }
//    this.
 
 //    ((List)deltaList.get(workNum)).add(difference);
     this.add(listResult);thisList.add(listResult);
//if (listResult.compareTo(zero)==0 || (y >= halfIntegerRange - 2 && y <= halfIntegerRange + 2)) {
//    System.out.println("SWLI_DEBUG wNum=" + workNum + " y=" + y + " mapped=" + (y-halfIntegerRange) + " arg1=" + arg1 + " arg2=" + arg2 + " diff=" + listResult + " isZero=" + (listResult.compareTo(zero)==0));
//}


//     if (listResult.compareTo(zero)==0) {muList.add(y+1-caiIndex); caiIndex++;}
//        if (listResult.compareTo(zero)==0 && x>(halfIntegerRange-1)) {muList.add(x+1-caiIndex-halfIntegerRange-1); caiIndex++;}   
//      if (listResult.compareTo(zero)==0 && (y>halfIntegerRange) ) {muList.add(y+1-caiIndex-halfIntegerRange); caiIndex++;}
if (listResult.compareTo(zero)==0 && (y>halfIntegerRange) ) {
//    System.out.println("SWLI y,cai: "+y+" "+caiIndex);
    muList.add(y-caiIndex-halfIntegerRange); caiIndex++;    
}
        if (listResult.compareTo(zero)==0) {
            rootList.add(y-halfIntegerRange);  // Store ALL roots (including negative)
            totalZero++;
        } 
//    ((List)ancestorlist.get(workNum)).add(listResult1);
 //    this.ancestorTerm.add(listResult1);
    }   
    }
//    System.out.println("subsequentWorkerListInit.this: "+this.toString());
//    System.out.println("subsequent this: "+this.toString());
//    rListB.add(this);
//     cashedrListB.add(this);
 //    ((List)ancestorlist.get(workNum)).add(listPower1);
 //    this.ancestorlist.add(ancestorTerm);
 //    System.out.println("subsequent rListB: "+rListB.toString()); 
 //    System.out.println("subsequentWorkerListInit()");
     setNumDen();
    }
  
    
    private void setNumDen()  {
        
     int num=0;
     int den=0;
     int totalZero=0;
     int y;     
     int halfIntegerRange=integerRange/2;
//


//System.out.println("LOOPLIST: ");
//for (int xx=0; xx<100; xx++) {
//   System.out.print(this.get(xx)+" ");
//}
//System.out.println("");
//for (int xx=100; xx<199; xx++) {
//   System.out.print(this.get(xx)+" ");
//}
//System.out.println("MuList:   "+ muList.toString());

for (int x=0;  x<muList.size(); x++){
        y=(int)muList.get(x);
        if (y>num) {num=y;}
        den=den+y;
    }
    this.numerator=num;
    this.denominator=den;
//      System.out.println("SETNUMDEN: "+muList.toString()+" "+num+" "+den);
    }
    
    /* raises each integerRange to power of dimension
    */    
    
    private void firstWorkerListInit()  {
     int halfIntegerRange=integerRange/2;
     BigDecimal listPower=new BigDecimal(1);      //result
    
    BigDecimal zero=new BigDecimal(0);
//    BigDecimal bigDecimalRange=new BigDecimal(0); // synchronized with x
    BigDecimal bigDecimalRange=new BigDecimal(-halfIntegerRange);
    BigDecimal bigDecimalDimension=new BigDecimal(dimension);
    BigDecimal one=new BigDecimal(1);
    BigDecimal listPower1=new BigDecimal(1);
    BigDecimal difference=new BigDecimal(1);
    //  this.add(zero);  // place 0^dimension onto looplist
 //  this.ancestorTerm.add(zero);
 //   System.out.println("firstWorkerListInit() workNum: "+workNum);
 //   System.out.println("integerRange: "+integerRange);

//    for (int x=0;  x<integerRange; x++){
    for(int x=-halfIntegerRange;  x<(halfIntegerRange); x++){
       listPower=bigDecimalRange.pow(dimension);
       listPower1=bigDecimalRange.pow(dimension+1);
   //    listPower=bigDecimalRange.multiply(bigDecimalDimension);
       difference=listPower1.subtract(listPower);
       ((List)deltaList.get(workNum)).add(difference);
       this.add(listPower);thisList.add(listPower);
//      if (listValueResult.compareTo(zero)==0) {muList.add(x+caiIndex); caiIndex++;}
       ((List)ancestorlist.get(workNum)).add(listPower1);
   //    this.ancestorTerm.add(listPower1);
       bigDecimalRange=bigDecimalRange.add(one);
    }   
    
//    rListB.add(this);
//    cashedrListB.add(this);
  //  this.ancestorlist.add(this.ancestorTerm);
  //   System.out.println("initial rListB: "+rListB.toString());
  //   System.out.println("initial ancestorlist: "+ancestorlist.toString());
  //   System.out.println("initial deltalist: "+deltaList.toString());
    }
    
     public  static void printMatrix( double[][] A){
     int n = A.length - 1;
     int m = A[0].length - 1;
    
     
     System.out.println();
     for(int i=1; i<=n; i++){
         for(int j=1; j<=m; j++) System.out.print(A[i][j] + "  ");
         System.out.println();
      }
      
      System.out.println();
      System.out.println();
   }

    
    
    
    private void initializeList() {
        if (workNum==0) {
          firstWorkerListInit();
      } else {
       subsequentWorkerListInit();   
      }
    }
       
    public int getworkNum() {
        return(workNum);
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
    
    public ArrayList getmuListt() {
        return (ArrayList)muList;
    }
    
  public String getmuList() {
    return muList.toString();
}  
    
    public ArrayList getrootListt() {
        return (ArrayList)rootList;
    }
    
    public String getrootList() {
        return rootList.toString();
    }
    
     public synchronized void setGBList(muNumDen value) {
 //   public synchronized void setGBList(String value) {
 //    propertySupport.firePropertyChange(this.toString(), this, value);
//      System.out.println();
//      System.out.println("LoopList.setGBList (muNumDen)value.getWorkNum: " + value.getWorkNum()+ " " );
//      System.out.println();
         propertySupport.firePropertyChange("abc", "abc", value);
  //      System.out.println(" this.to string" + this.toString()+ " " );
//        System.out.println("                       setGBList fallthrough                    (  propertySupport.firePropertyChange  )                       " );
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

public void setVmResult(String vmResult){
    this.vmResult=vmResult;
}    
    
public String getVmResult() {
    return(this.vmResult);
}

public void setNewtonCoeffs(BigDecimal[] coeffs) { this.newtonCoeffs = coeffs; }
public BigDecimal[] getNewtonCoeffs() { return this.newtonCoeffs; }

public int getNumerator()  {
    return this.numerator;
}

public int getDenominator() {
    return this.denominator;
}

public int getTotalZero() {
    return this.totalZero;
}





}
