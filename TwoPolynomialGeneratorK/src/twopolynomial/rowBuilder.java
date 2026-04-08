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

import java.math.*;
import java.util.concurrent.Semaphore;
/**
 *
 * @author Tomas
 */
public class rowBuilder {

    BigDecimal tSeqDB=null;
    BigDecimal NdB=null;                    
    BigDecimal flatFileRowCounterDB=null;   
    BigDecimal maxNDB=null;                 
    BigDecimal bTermDB=null;

    vertexVector dbVectorDB=null;
   
    private Semaphore tlSem;
    
     public rowBuilder(vertexVector dbVectorDB, Semaphore tlSem) {
        this.dbVectorDB=dbVectorDB;
        this.tlSem=tlSem;
    }
    
    public void firstVertex(pArgument argument, vertexVector productListVector, BigDecimal firstScalar, nMaxNBean nMax) {
       BigDecimal constantDegree = new BigDecimal(0);
       BigDecimal scalar= new BigDecimal(1);
       BigDecimal degree= new BigDecimal(1);   
       vertex firstVertex = new vertex(constantDegree,firstScalar.subtract(nMax.formulaN()),constantDegree);
       vertexVector termsList = new vertexVector(); 
       termsList.addVertexAt(firstVertex);
       vertex nextVertex = new vertex(scalar,scalar,degree);
       termsList.addVertexAt(nextVertex);
       productListVector.setVertexVector(termsList);
    }

    public  void plvAdder(vertexVector pLVR, vertexVector tL) {
       vertex nextVertex = null;  
       BigDecimal pLVRdegree= new BigDecimal(0);                                                   
       BigDecimal pLVRscalar= new BigDecimal(0);                                
       BigDecimal degree= new BigDecimal(1);                
       Integer test;
       BigDecimal thisArgument = new BigDecimal(0);       
       BigDecimal pLVRargument = new BigDecimal(0);                           
       BigDecimal thisScalar = new BigDecimal(0);         
       for (int j=0; j< tL.size(); j++) {     
       for (int k=0; k<((vertexVector)tL.elementAt(j)).size(); k++){
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
       vertex nextVertex = null;
       BigDecimal zeroDecimal= new BigDecimal(0);
       BigDecimal scalar= new BigDecimal(0);
       BigDecimal degree= new BigDecimal(1);
       BigDecimal degreeIncrement = new BigDecimal(1);
       BigDecimal thisArgument = new BigDecimal(0);
       BigDecimal thisaArgument = new BigDecimal(0);
       thisaArgument = argument.getArgument();
       BigDecimal thisScalar = new BigDecimal(0);
       int i=productListVector.size();
       for (int j=0; j<((vertexVector)productListVector.elementAt(i-1)).size(); j++) {  
       for (int k=0; k<((vertexVector)((vertexVector)productListVector.elementAt(i-1)).elementAt(j)).size(); k++){
         thisArgument = ((vertex)((vertexVector)((vertexVector)productListVector.elementAt(i-1)).elementAt(j)).elementAt(k)).getArgument();
         thisScalar = ((vertex)((vertexVector)((vertexVector)productListVector.elementAt(i-1)).elementAt(j)).elementAt(k)).getScalar();
         degree=((vertexVector)((vertexVector)productListVector.elementAt(i-1)).elementAt(j)).getDegree(); 
         if (thisaArgument.compareTo(degreeIncrement)==0) {   // degreeIncrement = 1, so, if argument = 1
          if (thisArgument.compareTo(zeroDecimal) == 1)   {
              degree=degree.add(degreeIncrement);
          } else {
            thisArgument=thisArgument.add(degreeIncrement);
            degree=degree.add(degreeIncrement);
           }
          scalar=thisScalar;
          nextVertex= new vertex(thisArgument,scalar,degree);
         } else {
          scalar=decrementSeq.multiply(thisScalar);
          nextVertex= new vertex(thisArgument,scalar,degree);
         }
         if (decrementSeq.compareTo(zeroDecimal)==-1) 
         {synchronized(this) { this.dbVectorDB.add(new rowBean(this.NdB,this.flatFileRowCounterDB,this.maxNDB,this.tSeqDB,this.bTermDB,nextVertex)); tlSem.release(); }}
         termsList.addVertexAt(nextVertex);  
       }
      }     
    }
   
   private void combSum(vertexVector productListVector, BigDecimal lowestBound, BigDecimal decrementSeq, pArgument argument, vertexVector termsList, nMaxNBean nMax) {
  
   BigDecimal decrement = new BigDecimal(-1);
   BigDecimal increment = new BigDecimal(1);  
   BigDecimal newArgument = new BigDecimal(0);

   this.bTermDB=nMax.formulaN();
   if (argument.argument.compareTo(increment)==0) {
   loadDBB(productListVector,decrementSeq.subtract(nMax.formulaN()),argument,termsList,decrementSeq);
   argument.setArgument(newArgument);
   combSum(productListVector, lowestBound,decrementSeq,argument,termsList,nMax);
   if (decrementSeq.compareTo(lowestBound)== 1)    {
   loadDBB(productListVector,decrementSeq.subtract(nMax.formulaN()),argument,termsList,decrementSeq);
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
    
   public void sequenceN(pArgument argument, vertexVector pLV, vertexVector pLVR, vertexVector pLVRlist ,BigDecimal N, abcBean nRow, twoSeq twoSequ, nMaxNBean nMax, Semaphore tlSem,vertexVector dbVector) {
       BigDecimal decrementSequence = new BigDecimal(0);
       BigDecimal increment = new BigDecimal(1);
       BigDecimal lowestBound = new BigDecimal(0);
       BigDecimal flatFileRowCounter= new BigDecimal(0);    
       vertexVector termsList = new vertexVector();
       decrementSequence=decrementSequence.subtract(nRow.getRowCounter());
       if (N.compareTo(nRow.getRowCounter())==1) {
       flatFileRowCounter=nRow.getRowCounter();
       this.flatFileRowCounterDB=flatFileRowCounter;
       nRow.incrementRowCounter(increment);
       this.firstVertex(argument, pLV,decrementSequence,nMax); 
       lowestBound=decrementSequence.subtract(increment);
       if (N.compareTo(nRow.getRowCounter())==0) {             
       twoSequ.addOne();                                        
       this.tSeqDB=twoSequ.gettSeq();
       }                                                        
       termsList.setTwoSequence(twoSequ.gettSeq());
       this.combSum(pLV,lowestBound.subtract(increment),decrementSequence.subtract(increment),argument,termsList, nMax); //NmaxNbean decrementSequence adjustment
       argument.setArgument(lowestBound); 
       vertexVector tl = new vertexVector();
       this.loadDBB(pLV, termsList.getTwoSequence(), argument, tl, termsList.getTwoSequence());
       pLV.add(tl);
       plvAdder(pLVRlist,tl);
       argument.setArgument(increment);
       twoSequ.incrementtSeq(nRow.getRowCounter()); this.tSeqDB=twoSequ.gettSeq();
       nRow.incrementB(nRow.getB());
       nRow.incrementC(nRow.getC());
       this.sequenceN(argument, pLV,pLVR,pLVRlist ,N, nRow, twoSequ,nMax,tlSem,dbVector);
       }
    }
}
