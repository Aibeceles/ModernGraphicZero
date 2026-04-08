/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package twopolynomial;
import java.beans.*;
import java.io.Serializable;
import java.math.*;
import java.util.concurrent.Semaphore;
/**
 *
 * @author Aibes
 */

public class plvRunThread extends Thread {
    private Semaphore tlSem;
    private Semaphore tl1Sem;
    private Semaphore t2Sem;
    private Semaphore cosetSem;
    int jLowe;
    int jUppp;
    BigDecimal N = new BigDecimal(2);        //  5  origonally syncronized with j loop as 8
    BigDecimal NN = new BigDecimal(8);

    abcBean nRow = new abcBean();
    twoSeq twoSequ = new twoSeq();
    BigDecimal increment = new BigDecimal(1);    
    BigDecimal zero = new BigDecimal(0);
    BigDecimal decrement = new BigDecimal(-1);
    BigDecimal jCounter = new BigDecimal(1);  // 4  
    vertexVector pLV = new vertexVector();
    vertexVector pLVR = new vertexVector();
    pArgument argument = new pArgument();
    nMaxNBean nMax = new nMaxNBean();
    rowBean dBbean = new rowBean();
    vertexVector dbbVector=null;
    vertexVector dbbVector11=null;
    vertexVector dbb1Vector = new vertexVector();
    vertexVector dbbCSTVector = new vertexVector();
//    rowBuilder rowList = new rowBuilder(dbbVector);
    
    plvRunThread(int jLower, int jUpper, BigDecimal nNN, Semaphore tlSemaphore,Semaphore t2Semaphore, Semaphore cosetSemaphore,vertexVector dbVector,vertexVector db1Vector,vertexVector db2Vector){
       jLowe=jLower;
       jUppp=jUpper;    // to just look at one equivelance class  jLowe=jUpper 
       NN=nNN;
       tlSem=tlSemaphore;
       t2Sem=t2Semaphore;
       cosetSem=cosetSemaphore; 
       dbbVector=dbVector;
       dbb1Vector=db1Vector;
       dbbCSTVector=db2Vector; 
    }
    
    plvRunThread(int jLower, int jUpper, BigDecimal nNN, Semaphore tlSemaphore,Semaphore t2Semaphore, Semaphore cosetSemaphore,vertexVector dbVector,vertexVector db1Vector,vertexVector db2Vector, Semaphore tl1Semaphore,vertexVector dbVector11){
       jLowe=jLower;
       jUppp=jUpper;    // to just look at one equivelance class  jLowe=jUpper 
       NN=nNN;
       tlSem=tlSemaphore;
       tl1Sem=tl1Semaphore;
       t2Sem=t2Semaphore;
       cosetSem=cosetSemaphore; 
       dbbVector=dbVector;
       dbbVector11=dbVector11;
       dbb1Vector=db1Vector;
       dbbCSTVector=db2Vector; 
    }
    
    @Override
    public void run(){
    rowBuilder rowList = new rowBuilder(dbbVector,tlSem);
    System.out.println("MyThread running"); 
    nMax.setmaxN(N);             //  looks useless to me
    nMax.setmaxN(NN);
    System.out.println("jLow,jupp: "+jLowe+" "+jUppp);
    for (int j=jLowe; j<jUppp; j++)  {    //  these are integers but syncronized with bigDecimal // for (int j=jLowe; j<jLowe+1; j++)  {  opportunity for concurrency here.
    jCounter=jCounter.add(increment);         //   jCounter and the homogonous representation of twoPolynomial                            the tasks in this loop are independent.
    nMax.setn(jCounter);
    argument.setArgument(increment);
    nRow.setA(increment);      // maybe all of this in the constructor
    nRow.setB(increment);      // and this
    nRow.setC(zero);           // and this
    nRow.setRowCounter(zero);  // and this      
    twoSequ.settSeq(increment); 
    vertexVector pLVRlist = new vertexVector();     ////   this needs to be fixed.
    rowList.NdB=N;rowList.tSeqDB=twoSequ.gettSeq();rowList.maxNDB=nMax.getmaxN();   
    vertex nextVertex=new vertex(zero,jCounter.add(increment),decrement);         //  the following is the vertex not to be divided by two.
    rowBean nextRowBean = new rowBean(this.N,this.decrement,this.NN,increment,increment,nextVertex);
    System.out.println("!!!!!!plvRunthreat                            constant vertex: "+nextVertex.toString() + "  "+ nextRowBean.toString());
   {synchronized(this) { this.dbbVector.add(nextRowBean); tlSem.release(); }}
   System.out.println("this.dbbVector.toString: "+this.dbbVector.toString());
   rowList.sequenceN(argument,pLV,pLVR,pLVRlist,N,nRow,twoSequ,nMax,tlSem,dbbVector);    //   pLVR is usless in sequenceN
    System.out.println(".plvRunThread.pLV: "+pLV.toString());
    System.out.println("plvRunThread.rowlist.sequenceN                                plvrunThread.rowList.sequenceN  return");
    pLVR.add(pLVRlist);
    N=N.add(increment);  //  this N increment needs to be fixed and syncronized with nBean and rowList.
    System.out.println("plvRunThresd    plVRlist: " + pLVR.toString());

    synchronized(this) {   //latest thread issue
    rowBean dBBean= new rowBean();    // can be initialized in the constructor.
    dBBean.setvertex((vertex)pLVRlist.get(0));dBBean.setNMaxx(nMax.getmaxN());dBBean.setNN(N);
    System.out.println("plvRunTrhread.run.rowBean: "+    ((vertex)dBBean.getvertex()).getArgument().toString() + " " + ((vertex)dBBean.getvertex()).getScalar().toString() + " " +((vertex)dBBean.getvertex()).getDegree().toString() +" "+dBBean.getNN().toString());
    dbbVector11.add(dBBean);
//     System.out.println("plvRunThread dbbvector11.lastelement " + ((rowBean)dbbVector11.lastElement()).getTL().toString()+"  "+ ((rowBean)dbbVector11.lastElement()).getNN());
     System.out.println("plvRunThread t11sem " + tl1Sem.availablePermits());
     tl1Sem.release();
    rowBean dBBean1= new rowBean();    // can be initialized in the constructor.
    dBBean1.setvertex((vertex)pLVRlist.get(1));dBBean1.setNMaxx(nMax.getmaxN());dBBean1.setNN(N);
      System.out.println("plvRunTrhread.run.rowBean: "+    ((vertex)dBBean1.getvertex()).getArgument().toString() + " " + ((vertex)dBBean1.getvertex()).getScalar().toString() + " " +((vertex)dBBean1.getvertex()).getDegree().toString() +  " "+dBBean1.getNN().toString() );
  
    dbbVector11.add(dBBean1);
    tl1Sem.release();
    rowBean dBBean2= new rowBean();    // can be initialized in the constructor.
    dBBean2.setvertex((vertex)pLVRlist.get(2));dBBean2.setNMaxx(nMax.getmaxN());dBBean2.setNN(N);
    System.out.println("plvRunTrhread.run.rowBean: "+    ((vertex)dBBean2.getvertex()).getArgument().toString() + " " + ((vertex)dBBean2.getvertex()).getScalar().toString() + " " +((vertex)dBBean2.getvertex()).getDegree().toString() +" "+dBBean2.getNN().toString() );
    dbbVector11.add(dBBean2);
    tl1Sem.release();
    }
    }   // dbb bean was origonall inside the loop 
    System.out.println("plvRunThread plv.toString: "+pLV.toString());
     System.out.println("plvRunThread pLVR.toString: "+pLVR.toString());
  //   System.out.println(".plvRunThread.dbbVector: "+dbbVector.toString());
  }    // experementing where dbb bean accepts a new pLVRlist firt, inside the loop, second outside the loop

}
