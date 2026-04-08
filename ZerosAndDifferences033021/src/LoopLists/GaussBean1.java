/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopLists;

import PArrayReset.PolynomialArray;
import mucorrolationthreaded.*;
import java.math.BigDecimal;

/**
 *
 * @author Aibes
 */
public class GaussBean1 {

private String pArray;
private String figPArray;
private LoopList loopList;
private PolynomialArray fPArray=null;
private int wNum;
private String vmResult;
private int resultlistid;
private int translateIndex=0;
private int pArrayIndexValue=0;
private int workNum;
private boolean isTwoPolynomial=false;
private String evaluateValue=null;
private String muMaskList="";

public GaussBean1(String pArray, LoopList loopList, int wNum  ) {
   this.pArray=pArray;
   this.loopList=loopList;
   this.wNum=wNum;
           
}    


public GaussBean1(PolynomialArray figPArray,String pArray, LoopList loopList, int resultlistid, int translateIndex, int pArrayIndexValue,String muMaskList ) {
   this.figPArray=figPArray.toString();
   this.pArray=pArray;
   this.loopList=loopList;
   this.resultlistid=resultlistid;
   this.fPArray=figPArray;
   this.translateIndex=translateIndex;
   this.pArrayIndexValue=pArrayIndexValue;
   this.muMaskList=muMaskList;
//   System.out.println("                      GaussBean1 constructor translateIndex pArrayIndexValue, pArray: " + translateIndex+"    "+pArrayIndexValue+"   "+pArray);
}


public GaussBean1(PolynomialArray figPArray,String pArray, LoopList loopList, int resultlistid, int translateIndex, int pArrayIndexValue ) {
   this.figPArray=figPArray.toString();
   this.pArray=pArray;
   this.loopList=loopList;
   this.resultlistid=resultlistid;
   this.fPArray=figPArray;
   this.translateIndex=translateIndex;
   this.pArrayIndexValue=pArrayIndexValue;
//   System.out.println("                      GaussBean1 constructor translateIndex pArrayIndexValue, pArray: " + translateIndex+"    "+pArrayIndexValue+"   "+pArray);
}




public GaussBean1(PolynomialArray figPArray,String pArray, LoopList loopList, int resultlistid, int translateIndex  ) {
   this.figPArray=figPArray.toString();
   this.pArray=pArray;
   this.loopList=loopList;
   this.resultlistid=resultlistid;
   this.fPArray=figPArray;
   this.translateIndex=translateIndex;
//   System.out.println("                                                                    GaussBean1 constructor  pArray: " + translateIndex+"    "+pArrayIndexValue+"   "+pArray);


}

public GaussBean1(PolynomialArray figPArray,String pArray, LoopList loopList, int resultlistid, String muMaskList  ) {
   this.figPArray=figPArray.toString();
   this.pArray=pArray;
   
   this.loopList=loopList;
   this.resultlistid=resultlistid;
   this.fPArray=figPArray;
   this.muMaskList=muMaskList;
}

public GaussBean1(PolynomialArray figPArray,String pArray, LoopList loopList, int resultlistid  ) {
   this.figPArray=figPArray.toString();
   this.pArray=pArray;
   this.loopList=loopList;
   this.resultlistid=resultlistid;
   this.fPArray=figPArray;
}

public GaussBean1(PolynomialArray figPArray,String pArray, LoopList loopList, int resultlistid,boolean isTwoPolynomial,String evaluateValue  ) {
   this.figPArray=figPArray.toString();
   this.pArray=pArray;
   this.loopList=loopList;
   this.resultlistid=resultlistid;
   this.fPArray=figPArray;
   this.isTwoPolynomial=isTwoPolynomial;
   this.evaluateValue=evaluateValue;
}

public GaussBean1(PolynomialArray figPArray,String pArray, LoopList loopList, int resultlistid,boolean isTwoPolynomial,String evaluateValue, String muMaskList) {
   this.figPArray=figPArray.toString();
   this.pArray=pArray;
   this.loopList=loopList;
   this.resultlistid=resultlistid;
   this.fPArray=figPArray;
   this.isTwoPolynomial=isTwoPolynomial;
   this.evaluateValue=evaluateValue;
   this.muMaskList=muMaskList;
}


public String getMuMaskList() {
    return(this.muMaskList);
}


public int gettranslateIndex(){
    return(this.translateIndex);
}


public int getpArrayIndexValue(){
    return(this.pArrayIndexValue);
}




public boolean getisTwoPolynomial() {
    return(this.isTwoPolynomial);
}

public String getevaluateValue()  {
    return(this.evaluateValue);
}



public PolynomialArray getfPArray() {
    return (this.fPArray);
}


public void setresultlistid(int resultlistid){
    this.resultlistid=resultlistid;
}

public int getresultlistid(){
    return(this.resultlistid);
}

public void setVmResult(String vmResult){
    this.vmResult=vmResult;
}

public String getfigPArray() {
    return(this.figPArray);
}

public String getpArray(){
    return(this.pArray);
}

public LoopList getLoopList(){
    return(this.loopList);
}

public int getWNum(){
    return(this.wNum);
}
        
public String getVmResult() {
    return(this.vmResult);
}

}





