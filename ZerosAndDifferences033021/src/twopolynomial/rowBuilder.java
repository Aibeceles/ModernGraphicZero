/*
 * rowBuilder.java
 *
 * Created on February 6, 2011, 10:54 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package twopolynomial;

import java.math.*;
import java.util.concurrent.Semaphore;
/**
 *
 * @author Tomas
 */
public class rowBuilder {

    BigDecimal tSeqDB=null;
    BigDecimal NdB=null;                    //already in rowBean
    BigDecimal flatFileRowCounterDB=null;   //already in rowBean
    BigDecimal maxNDB=null;                 //already in rowBean
    BigDecimal bTermDB=null;

    vertexVector dbVectorDB=null;
   
    private Semaphore tlSem;
    
    /** Creates a new instance of rowBuilder
     * @param dbVectorDB */
    public rowBuilder(vertexVector dbVectorDB, Semaphore tlSem) {
        this.dbVectorDB=dbVectorDB;
        this.tlSem=tlSem;
    }
    
    public void firstVertex(pArgument argument, vertexVector productListVector, BigDecimal firstScalar, nMaxNBean nMax) {
       BigDecimal constantDegree = new BigDecimal(0);
       BigDecimal scalar= new BigDecimal(1);
       BigDecimal degree= new BigDecimal(1);   
       vertex firstVertex = new vertex(constantDegree,firstScalar.subtract(nMax.formulaN()),constantDegree);  //argument, pLV,decrementSequence,nMax
//       System.out.println("RowBuilder.firstVertex.firstVertex:"+firstVertex.toString());
       vertexVector termsList = new vertexVector();   ///  this is what need to be fixed
       termsList.addVertexAt(firstVertex);
       vertex nextVertex = new vertex(scalar,scalar,degree);
//       System.out.println("RowBuilder.firstVertex.nextVertex:"+nextVertex.toString());
       termsList.addVertexAt(nextVertex);
       productListVector.setVertexVector(termsList);
    //    System.out.println("firstVertex termsList " + termsList.toString());
    }

    public  void plvAdder(vertexVector pLVR, vertexVector tL) {
       vertex nextVertex = null;  // i think plvAdder puts tl on pLVR
       // the plvAdder paradyne:   have pLVR. argue with tL,  if the tl item is on pLVR then add term
       // if it is not, create new one.
       BigDecimal pLVRdegree= new BigDecimal(0);   //  span tl,  accumulate tl from row to row                                                
       BigDecimal pLVRscalar= new BigDecimal(0);                                
       BigDecimal degree= new BigDecimal(1);              // tl degree  
       Integer test;
       BigDecimal thisArgument = new BigDecimal(0);       // tl argument
       BigDecimal pLVRargument = new BigDecimal(0);                           
       BigDecimal thisScalar = new BigDecimal(0);         //  tl scalar
       for (int j=0; j< tL.size(); j++) {     
       for (int k=0; k<((vertexVector)tL.elementAt(j)).size(); k++){
//         System.out.println("                                  rowBuilder.plvAdder.vertex: "+((vertex)((vertexVector)tL.elementAt(j)).elementAt(k)).toString());
         thisArgument = ((vertex)((vertexVector)tL.elementAt(j)).elementAt(k)).getArgument();
         thisScalar = ((vertex)((vertexVector)tL.elementAt(j)).elementAt(k)).getScalar();
         degree=((vertexVector)tL.elementAt(j)).getDegree(); 
         test=pLVR.gettOnVertexVector(degree);
         if (test==-1) {                                                          
            nextVertex= new vertex(thisArgument,thisScalar,degree);                
            pLVR.addElement(nextVertex);                           
         } else {
            pLVRscalar = thisScalar.add(((vertex)pLVR.elementAt(test)).getScalar());
            ((vertex)pLVR.elementAt(test)).setScalar(pLVRscalar);              
         }
       }
      }     
 }
   
    private void loadDBB(vertexVector productListVector, BigDecimal decrementSeq, pArgument argument, vertexVector termsList,BigDecimal decrementSeqArg) {
   // loadDB must now at each vertex in span(k-1) create a new arguemnt for the kth vector of productListVector.
   // the object at plV(i) is termsList termslist every 3rd plv is to be read by polyBuilder.     
       vertex nextVertex = null;
       BigDecimal zeroDecimal= new BigDecimal(0);
       BigDecimal scalar= new BigDecimal(0);
       BigDecimal degree= new BigDecimal(1);
       BigDecimal degreeIncrement = new BigDecimal(1);
       BigDecimal thisArgument = new BigDecimal(0);
       BigDecimal thisaArgument = new BigDecimal(0);
       thisaArgument = argument.getArgument();
//       System.out.println("RowBuilder.loadDBB  thisaArgument : "+thisaArgument.toString());
       BigDecimal thisScalar = new BigDecimal(0);
       int i=productListVector.size();
//       System.out.println("rowBuilder.loadDBB productListVector.size(): " + i);
       for (int j=0; j<((vertexVector)productListVector.elementAt(i-1)).size(); j++) {  
//         System.out.println("rowBuilder.(vertexVector)productListVector.elementAt(i-1)): "+((vertexVector)productListVector.elementAt(i-1)).toString());
         for (int k=0; k<((vertexVector)((vertexVector)productListVector.elementAt(i-1)).elementAt(j)).size(); k++){
         thisArgument = ((vertex)((vertexVector)((vertexVector)productListVector.elementAt(i-1)).elementAt(j)).elementAt(k)).getArgument();
         thisScalar = ((vertex)((vertexVector)((vertexVector)productListVector.elementAt(i-1)).elementAt(j)).elementAt(k)).getScalar();
         degree=((vertexVector)((vertexVector)productListVector.elementAt(i-1)).elementAt(j)).getDegree(); 
//         System.out.println("thisArgument,thisScalar,degree: "+thisArgument.toString()+" "+thisScalar.toString()+" "+degree.toString());
         if (thisaArgument.compareTo(degreeIncrement)==0) {   // degreeIncrement = 1, so, if argument = 1
          if (thisArgument.compareTo(zeroDecimal) == 1)   {
//              System.out.println("RowBuilder.loadDBB  (thisArgument.compareTo(degreeIncrement)==0) : "+thisArgument.toString());
//              System.out.println("rowBuilder.loadDBB increment degree");
              degree=degree.add(degreeIncrement);
          } else {
            thisArgument=thisArgument.add(degreeIncrement);
            degree=degree.add(degreeIncrement);
 //           System.out.println("rowBuilder.loadDBB increment degree");
          }
          scalar=thisScalar;
          nextVertex= new vertex(thisArgument,scalar,degree);
 
//          System.out.println("rowBuilder.loadDBB same scalar");
         } else {
          scalar=decrementSeq.multiply(thisScalar);
          nextVertex= new vertex(thisArgument,scalar,degree);
//          System.out.println("rowBuilder.loadDBB scalar=decrementSeq.multiply(thisScalar), decrementSeq: "+decrementSeq.toString());
         }
         if (decrementSeq.compareTo(zeroDecimal)==-1) 
         {synchronized(this) { this.dbVectorDB.add(new rowBean(this.NdB,this.flatFileRowCounterDB,this.maxNDB,this.tSeqDB,this.bTermDB,nextVertex)); tlSem.release(); }}
         //synchronized(this) { this.dbVectorDB.add(new rowBean(this.NdB,this.flatFileRowCounterDB,this.maxNDB,this.tSeqDB,this.bTermDB,nextVertex)); tlSem.release(); }
 // moved to after scalar=thisScalar substitute decrementSeq with this.twoseqDB
 //        System.out.println("RowBuilder.loadDBB.vertex:"+nextVertex.toString());
         termsList.addVertexAt(nextVertex);  
       }
      }     
//System.out.println("RowBuilder.loadDBB.termsList:"+termsList.toString());
    }
   
   private void combSum(vertexVector productListVector, BigDecimal lowestBound, BigDecimal decrementSeq, pArgument argument, vertexVector termsList, nMaxNBean nMax) {
  
   BigDecimal decrement = new BigDecimal(-1);
   BigDecimal increment = new BigDecimal(1);  
   BigDecimal newArgument = new BigDecimal(0);

   this.bTermDB=nMax.formulaN();
   if (argument.argument.compareTo(increment)==0) {
   loadDBB(productListVector,decrementSeq.subtract(nMax.formulaN()),argument,termsList,decrementSeq);   // loadDbb nMax
//   System.out.println("rowBuilder.combSum.loadDBB  (argument.argument.compareTo(increment)==0)  return");
   argument.setArgument(newArgument);
   combSum(productListVector, lowestBound,decrementSeq,argument,termsList,nMax);
   if (decrementSeq.compareTo(lowestBound)== 1)    {
   loadDBB(productListVector,decrementSeq.subtract(nMax.formulaN()),argument,termsList,decrementSeq);   // loadDbb nMax
//   System.out.println("rowBuilder.combSum.loadDBB (decrementSeq.compareTo(lowestBound)== 1) return");
   productListVector.add(termsList);
   decrementSeq=decrementSeq.add(decrement);
   argument.setArgument(increment);
   if (decrementSeq.compareTo(lowestBound)== 1) {
       vertexVector termsList1 = new vertexVector(); 
       combSum(productListVector,lowestBound,decrementSeq,argument,termsList1,nMax);
   }
   } 
   }
   }
    
  //    rowList.sequenceN(argument,pLV,pLVR,pLVRlist,N,nRow,twoSequ,nMax,tlSem,dbbVector); 
   
    public void sequenceN(pArgument argument, vertexVector pLV, vertexVector pLVR, vertexVector pLVRlist ,BigDecimal N, abcBean nRow, twoSeq twoSequ, nMaxNBean nMax, Semaphore tlSem,vertexVector dbVector) {
       BigDecimal decrementSequence = new BigDecimal(0);
       BigDecimal increment = new BigDecimal(1);
       BigDecimal lowestBound = new BigDecimal(0);
       BigDecimal flatFileRowCounter= new BigDecimal(0);    
       vertexVector termsList = new vertexVector();
       decrementSequence=decrementSequence.subtract(nRow.getRowCounter());
   //    System.out.println("rowBuilder.sequenceN.decrementSequence: "+decrementSequence.toString());
       System.out.println("                                       rowBuilder (N.compareTo(nRow.getRowCounter()) n,nRow.getRowCounter "+ N.toString()+" "+nRow.getRowCounter());
       if (N.compareTo(nRow.getRowCounter())==1) {
       flatFileRowCounter=nRow.getRowCounter();
       this.flatFileRowCounterDB=flatFileRowCounter;
       nRow.incrementRowCounter(increment);
       this.firstVertex(argument, pLV,decrementSequence,nMax); //NmaxNbean adjusts decrementSequence.
       lowestBound=decrementSequence.subtract(increment);
       if (N.compareTo(nRow.getRowCounter())==0) {             // test for last rowcounter, make adjustment
       twoSequ.addOne();                                       //  fresh code  dummy code it dosnt add one 
       this.tSeqDB=twoSequ.gettSeq();
//       System.out.println("twoSeq addone " + twoSequ.gettSeq());//  code in twoSeq s/b  twoS=swoS.add(one);
       }                                                        //   fresh code.
       termsList.setTwoSequence(twoSequ.gettSeq());
                          // origonally after printlns below.
       this.combSum(pLV,lowestBound.subtract(increment),decrementSequence.subtract(increment),argument,termsList, nMax); //NmaxNbean decrementSequence adjustment
  //      System.out.println("rowBuilder.sequenceN.pArgument.setArgument: ");
       argument.setArgument(lowestBound);                       // argument manipulated for tl. 
       vertexVector tl = new vertexVector();                                           //  this should be a function that returns tl
       this.loadDBB(pLV, termsList.getTwoSequence(), argument, tl, termsList.getTwoSequence());              // .loadDBB populates tl.
       pLV.add(tl);                         // this is the stuff needing refinement
 //      System.out.println("rowBuilder.sequenceN plv.size: "+ pLV.size());
       plvAdder(pLVRlist,tl);               //    it is appending plvr with common degree sums.
//       rowBean dBBean= new rowBean();
//       dBBean.setFields(tl, N, flatFileRowCounter, nMax.getmaxN());     //this dBBean not used with grapdb.
//       System.out.println("rowBuilder.dbBean!!!!!!!!!!!!!!!!"+ " N,flatFileRowCounter,maxN"+N.toString()+" "+flatFileRowCounter+" "+nMax.getmaxN());
//       synchronized(this) {   //latest thread issue
//       dbVector.add(dBBean);
//       tlSem.release();
//       } // instead of reading vertexTable and recasting the polynomial, write pLVRlist to results.
//       System.out.println("rowBuilder.sequenceN.pArgument.setArgument: ");
       argument.setArgument(increment);
       twoSequ.incrementtSeq(nRow.getRowCounter()); this.tSeqDB=twoSequ.gettSeq();
       nRow.incrementB(nRow.getB());
       nRow.incrementC(nRow.getC());
//       System.out.println("rowBuilder.sequenceN recurse"); 
       this.sequenceN(argument, pLV,pLVR,pLVRlist ,N, nRow, twoSequ,nMax,tlSem,dbVector);
       }
 //      System.out.println("rowBuilder.sequenceN fallthrough, dbVectordb "); 
    }
}
