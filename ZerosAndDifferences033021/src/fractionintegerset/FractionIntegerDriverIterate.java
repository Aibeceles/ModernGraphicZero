/*
 * Define semaphore pattern parameters.
 * Start semaphore pattern.
 * Implement NextBinary, ListToNumber.
 * 
 */

package fractionintegerset;

import java.lang.Boolean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;

/**  FractionIntegerDriver has denominator, numeratorlow,numberatorhigh
 *   hard coded (needs to be argument in .main).
 * @author Aibes
 */
public class FractionIntegerDriverIterate extends Thread implements GBIterator{

ExecutorService executorService1 = Executors.newFixedThreadPool(120);    
CompletionService<String> service = new ExecutorCompletionService<>(executorService1);    
Semaphore gaSemap = new Semaphore(1);
List gaussList = new ArrayList();
Semaphore muSem = new Semaphore(1);
List muList = new ArrayList();            
List semaphoreListF = new ArrayList();
List semaphoreListB = new ArrayList();


int numeratorLow=1;
int numeratorHigh=25;   // numeratorhigh and denominator are usually the same.
int denominator=25;
boolean isValidMu=false;
boolean isMu=false;     //  isMu=true will prune generateBinary.  isMu=false will completely iterate.  Criptic use.


boolean numeratorBoolean=true;  //not used 
Boolean endCproduct=false;
EndProduct endProduct=new EndProduct();
ResultListBean rListB =new ResultListBean();
final ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
//GBListener gListener new GBListener(rListB,muList,muSem); 
//GBListener nut needed injected into gbList.

public FractionIntegerDriverIterate(List muList,Semaphore muSem) {
   this.muList=muList;
   this.muSem=muSem;        
}

public void run() {

    for (int x=1; x<25; x++) {
    this.denominator=x;
    this.numeratorHigh=x;    
    runGBpattern();
    System.out.println("X: "+x);
    }
}

public void runGBpattern() {
  
   ExecutorService executorService1 = Executors.newFixedThreadPool(120);    
   CompletionService<String> service = new ExecutorCompletionService<>(executorService1);
   Boolean endCproduct=false;
   List semaphoreListF = new ArrayList();
   List semaphoreListB = new ArrayList();  
   
   endProduct.setEndCproduct(endCproduct);
   int semaphoreListSizeF;int semaphoreListSizeB;
   semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0)); semaphoreListF.add(new Semaphore(0)); semaphoreListB.add(new Semaphore(0));
   semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();    
   Callable workerStart = new GenerateBinaryStartIterate((Semaphore)semaphoreListF.get(1));
   service.submit(workerStart); 
   GBList gbList = new GBList();
   gbList.addPropertyChangeListener(new GBListener(rListB,muList,muSem));
   rListB.add(gbList);
   Callable workerZero = new GenerateBinaryZeroIterate(numeratorLow,denominator,(Semaphore)semaphoreListF.get(0),(Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,gbList,rListB,isMu); 
    System.out.println("semaphorelistsize  "+semaphoreListSizeF);
   service.submit(workerZero);
   
  for (int numerator=numeratorLow+1; numerator<numeratorHigh ; numerator++) {
          semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
          GBList gbList1 = new GBList();
          gbList1.addPropertyChangeListener(new GBListener(rListB,muList,muSem));
          rListB.add(gbList1);
          Callable worker = new GenerateBinaryIterate(numerator,denominator,(Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,gbList1,isMu); 
          service.submit(worker); 
  }
   semaphoreListF.add(new Semaphore(0));semaphoreListB.add(new Semaphore(0));semaphoreListSizeF=semaphoreListF.size();semaphoreListSizeB=semaphoreListB.size();
   semaphoreListSizeF=semaphoreListF.size(); semaphoreListSizeB=semaphoreListB.size();
   GBList gbList2 = new GBList();
   gbList2.addPropertyChangeListener(new GBListener(rListB,muList,muSem));
   rListB.add(gbList2);
   Callable workerLast = new GenerateBinaryLastIterate(numeratorHigh,denominator,(Semaphore)semaphoreListF.get(semaphoreListSizeF-2),(Semaphore)semaphoreListB.get(semaphoreListSizeB-2),(Semaphore)semaphoreListF.get(semaphoreListSizeF-3),(Semaphore)semaphoreListB.get(semaphoreListSizeB-3),endProduct,gbList2,isMu); 
   service.submit(workerLast);
   
   System.out.println("Fraction Integer Driver Iterate before .isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
   executorService1.shutdown();
  
   try {
       executorService1.shutdown();
       while (!executorService1.isTerminated()) {    
       final Future<String> future = service.take();
       System.out.println("service.take "+future.get());
          System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
        }   
   } catch (ExecutionException | InterruptedException ex) {
   ex.printStackTrace();
   } 
   System.out.println("fid exit");

}
}
