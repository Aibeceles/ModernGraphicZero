/*
 * Main.java
 *
 * Created on February 6, 2011, 10:52 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package twopolynomial;
import java.util.concurrent.Semaphore;
/**
 *
 * @author Tomas
 */
public class Main {
    
    /** Creates a new instance of Main */
    public Main() {
    }
    
     /**
     * creates and runs the the plv thread and the database thread
     * both threads reference the same semaphore and the database buffer 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    //   newDB nDB = new newDB();
       exportDB eDB = new exportDB();
       vertexVector dbVector = new vertexVector();
       vertexVector dbVector1 = new vertexVector();
       vertexVector dbVectorCST = new vertexVector();
       Semaphore tlSemaphore = new Semaphore(1);
       Semaphore t2Semaphore = new Semaphore (1);
       Semaphore cosetSemaphore = new Semaphore(1);
   //    nDB.go(args); //  database method, maybe even first thing to do in cmage.
       plvManager mage = new plvManager(tlSemaphore,t2Semaphore,cosetSemaphore,dbVector,dbVector1,dbVectorCST);
       zaddbTable1 bmage = new zaddbTable1(tlSemaphore,dbVector,mage);
       CypherArithmetic cA = new CypherArithmetic();
       twoTableDBThread cmage = new twoTableDBThread(eDB,tlSemaphore,dbVector,mage);   //  what happens if mage notifiy() before 
       twoTableDBThread1 dmage = new twoTableDBThread1(eDB,t2Semaphore,dbVector1,mage);
       twtableDBThreadCoset cosetThread = new twtableDBThreadCoset(cosetSemaphore,dbVectorCST,mage);
       fakeLP lpThread = new fakeLP();


   mage.start();                    //mage in twoTableDBThred becase test if alive
   bmage.start();
//    cA.start();
//     cmage.start();
//      dmage.start();                   //  this is the current working thread i think it starts ttblthread1
//       cosetThread.start();             //  coset thread is disabled for goresultsupdate method
//     lpThread.start();

     try{
     cA.join();
     } catch (InterruptedException e){
       System.out.println("mage threadInterrupted");
     } 



     try{
     mage.join();
     } catch (InterruptedException e){
       System.out.println("mage threadInterrupted");
     }  
          try{
     bmage.join();
     } catch (InterruptedException e){
       System.out.println("bmage threadInterrupted");
     }
        try{
    cmage.join();  
     } catch (InterruptedException e){
       System.out.println("cmage threadInterrupted");
     }  
       try{
     dmage.join();
     } catch (InterruptedException e){
       System.out.println("dmage threadInterrupted");
     }
  //     try{
  //   cosetThread.join();
  //   } catch (InterruptedException e){
  //     System.out.println("dmage threadInterrupted");
  //   }   
       System.out.println("done,  allthreads done");

    //  the myfile1 product start/join  
    // best that it works on the coset data   
    // cancel that, it is goning in the myfile1 update class   
       
   //    eDB.go();  
    }
    }
