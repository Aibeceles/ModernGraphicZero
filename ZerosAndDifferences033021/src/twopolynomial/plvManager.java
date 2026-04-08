/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package twopolynomial;

import java.math.*;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Aibes
 */

public class plvManager extends Thread{
    private Semaphore tlSem;
    private Semaphore tl1Sem;
    private Semaphore t2Sem;
    private Semaphore cosetSem;
    BigDecimal NN = new BigDecimal(4);
    BigDecimal increment = new BigDecimal(1);    
    BigDecimal zero = new BigDecimal(0);
    vertexVector dbbVector = new vertexVector();
    vertexVector dbbVector11 = new vertexVector();
    vertexVector dbb1Vector = new vertexVector();
    vertexVector dbbCSTVector = new vertexVector();
 
  public  plvManager(Semaphore tlSemaphore,Semaphore t2Semaphore,Semaphore cosetSemaphore,vertexVector dbVector,vertexVector db1Vector,vertexVector db2Vector,Semaphore tl1Semaphore,vertexVector dbVector11){
       tlSem=tlSemaphore;
       t2Sem=t2Semaphore;
       cosetSem=cosetSemaphore;
       dbbVector=dbVector;
       dbb1Vector=db1Vector;
       dbbCSTVector=db2Vector;
       tl1Sem=tl1Semaphore;
       this.dbbVector11=dbVector11;
       dbbVector=dbVector;
    }
    
    
    
    public  plvManager(Semaphore tlSemaphore,Semaphore t2Semaphore,Semaphore cosetSemaphore,vertexVector dbVector,vertexVector db1Vector,vertexVector db2Vector){
       tlSem=tlSemaphore;
       t2Sem=t2Semaphore;
       cosetSem=cosetSemaphore;
       dbbVector=dbVector;
       dbb1Vector=db1Vector;
       dbbCSTVector=db2Vector;
    }                                     
                             
    @Override
    public void run(){
        try {
            tlSem.acquire(); 
        } catch (InterruptedException ex) {
            Logger.getLogger(plvManager.class.getName()).log(Level.SEVERE, null, ex);
        }
     NN=NN.add(zero);   //  the lower bound below, 2,20 does not make any sense, see actual runthread code.
     plvRunThread mage = new plvRunThread(2,4,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector,tl1Sem,dbbVector11);
//     NN=NN.add(increment);
//     plvRunThread mage1= new plvRunThread(4,11,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage2= new plvRunThread(4,12,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage3= new plvRunThread(4,13,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage4= new plvRunThread(4,14,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage5= new plvRunThread(4,15,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage6= new plvRunThread(4,16,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage7= new plvRunThread(4,17,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage8= new plvRunThread(4,18,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage9= new plvRunThread(4,19,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage10= new plvRunThread(4,20,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage11= new plvRunThread(4,21,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
//     NN=NN.add(increment);
//     plvRunThread mage12= new plvRunThread(4,22,NN,tlSem,t2Sem,cosetSem,dbbVector,dbb1Vector,dbbCSTVector);
     mage.start();
 //    mage1.start();
 //    mage2.start();
 //    mage3.start();
 //    mage4.start();
 //    mage5.start();
 //    mage6.start();
 //    mage7.start();
 //    mage8.start();
 //    mage9.start();
 //    mage10.start();
 //    mage11.start();
 //    mage12.start();
     try{
     mage.join();
     } catch (InterruptedException e){
       System.out.println("threadInterrupted");
     }      
//    try{
//   mage1.join();  
//    } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     }   
//       try{
//    mage2.join();
//     } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     } 
//         try{
//     mage3.join();
//     } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     } 
//      try{
//     mage4.join();
//     } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     }
//      try{
//     mage5.join();
//     } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     }      
//     try{
//    mage6.join();
//     } catch (InterruptedException e){
//      System.out.println("threadInterrupted");
//    }     
//     try{
//     mage7.join();
//    } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     } 
//      try{
//     mage8.join();
//     } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     } 
//    try{
//     mage9.join();
//     } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     } 
//  try{
//     mage10.join();
//     } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     }
//    try{
//     mage11.join();
//     } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//     } 
//     try{
//     mage12.join();
//     } catch (InterruptedException e){
//       System.out.println("threadInterrupted");
//    } 
//    System.out.println("plvmanager ended");
    }    
 } 
