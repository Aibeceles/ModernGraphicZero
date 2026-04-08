/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PArrayReset;

import fractionintegerset.ResultListBean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Aibes
 */
public class ResultListID {

static int rListId=0;
static int pWasReset=0;
static int highestWorker=0;
ResultListBean cashedrListB;

public ResultListID(ResultListBean cashedRListB) {
   this.cashedrListB=cashedRListB;    
}

public ResultListID() {
      
}

public ResultListID(int rListID) {
   ResultListID.rListId=rListID;      
}


private void setCashedRListBValues(ResultListBean rListB)  {
    List indexList;
    List rIndexList;
    BigDecimal indexValue;
    int integerRange=15;
    int sizeList;
    int sizeRange;
    int rSizeRange;
    //System.out.println();
    //System.out.println("  LoopListener.iterateRlistB:" );
    sizeList=rListB.size();
    int rSizeList=rListB.size();
        //System.out.println("  LoopListener.isizeList,rListBSizelist:: "+sizeList + "  "+ rSizeList);
    rListB.clear();
        for (int l=0; l<sizeList;l++)
    {
        indexList=(ArrayList)rListB.get(l);
        cashedrListB.add(indexList);
        //rIndexList=(ArrayList)rListB.get(l);
       // sizeRange=indexList.size();
       // rSizeRange=rIndexList.size();
  //              System.out.println("  LoopListener.isizeRange, rlistB size range: "+sizeRange + "  " +rSizeRange);
//        for (int m=0; m<sizeRange; m++)  {
//      System.out.println("  LoopListener.l, m: "+l+"  "+m );
//            indexValue=(BigDecimal)indexList.get(m);
//        ((LoopList)rListB.get(l)).set(m,indexValue);
//        System.out.println("  rlistb.listindex.value:" + indexValue);
//        }
    }
//    System.out.println();
}








private void copyCashedRListBValues(ResultListBean rListB)  {
    List indexList;
    List rIndexList;
    BigDecimal indexValue;
    int integerRange=15;
    int sizeList;
    int sizeRange;
    int rSizeRange;
    //System.out.println();
    //System.out.println("  LoopListener.iterateRlistB:" );
    sizeList=cashedrListB.size();
    int rSizeList=rListB.size();
        //System.out.println("  LoopListener.isizeList,rListBSizelist:: "+sizeList + "  "+ rSizeList);
  rListB.clear();
        for (int l=0; l<sizeList;l++)
    {
        indexList=(ArrayList)cashedrListB.get(l);
        rListB.add(indexList);
        //rIndexList=(ArrayList)rListB.get(l);
       // sizeRange=indexList.size();
       // rSizeRange=rIndexList.size();
  //              System.out.println("  LoopListener.isizeRange, rlistB size range: "+sizeRange + "  " +rSizeRange);
//        for (int m=0; m<sizeRange; m++)  {
//      System.out.println("  LoopListener.l, m: "+l+"  "+m );
//            indexValue=(BigDecimal)indexList.get(m);
//        ((LoopList)rListB.get(l)).set(m,indexValue);
//        System.out.println("  rlistb.listindex.value:" + indexValue);
//        }
    }
    System.out.println();
}




synchronized public int gethighestWorker() {
    return(highestWorker);
}


synchronized public void sethighestWorker(int wNum) {
    highestWorker=wNum;
}



synchronized public int getPWasReset() {
    return(pWasReset);
}


synchronized public void setPWasReset() {
    pWasReset=1;
}

synchronized public void resetPWasReset() {
    pWasReset=0;
}


synchronized public int getrListId() {
 // System.out.println("resultListID " + rListId);   
 rListId++;   
 return(ResultListID.rListId);
}

    
}
