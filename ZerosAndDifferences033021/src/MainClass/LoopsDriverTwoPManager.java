/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainClass;

import LoopsLogic.LoopsLogicLoopSemaphore;
import LoopsSemaphorePattern.LoopsSemaphoreInterface;
import PArrayReset.PArrayResetFlag;
import PArrayReset.PolynomialArray;
import PArrayReset.ResultListID;
import fractionintegerset.EndProduct;
import fractionintegerset.FractionIntegerDriverIterate;
import fractionintegerset.ResultListBean;
import fractionintegerset.muNumDen;
import java.io.IOException;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import mugauss.GaussMain;
import twopolynomial.plvManager;
import twopolynomial.rowBean;
import twopolynomial.vertex;


/**
 * maxFigPScalar and dimension define the initial P for differencing.
 * dimension is degree of polynomial.
 * @author Aibes
 */
public class LoopsDriverTwoPManager extends Thread    {
//int setProductRange=20;
int vDisplacement=0;            // for noBuffer=false
int workerCounter=0;            // for noBuffer=false
boolean noBufferRun=false;      // differencing twoPolynomials=false differencing figNewton=true  but is set in noBufferRun() is set in constructor.
int maxFigPScalar=1;            // for noBuffeer=true used in figPArray constructor - is appended as last element to figparray  not sure of effect if varied.
int dimension=5; 
int setProductRange=10;          //  pArray.set(4, 5);  splicing second half of setProductRange in noBufferRun.
int setProductRangeIncrement=2; // SetProductRange is incremented by this value after iteration untill maxSetProductRange. 
int maxSetProductRange=10;       // upper bound of setProductRange iteration.
int lbSetProductRange=0;        // splicing pArray value (zero or max from prior run
int pArrayResetValue=0;         //  should be the loSetProductRange;
public int integerRange=20;     //  The loopListener needs to know this value for its runnable.   Now it does.  Range: -10 to 9
public static final int targetCI=64;         // currently for noBuffer=false  (not for zeros in dimensions
public static final int targetVTIlow=0;      // currently for noBuffer=false  (not for zeros in dimensions
public static final int targetVTIhigh=72;    // currently for noBuffer=false  (not for zeros in dimensions

private  ResultListID resultListId=new ResultListID(400000);
//ResultListID resultListId = new ResultListID();

ExecutorService executorService1 = Executors.newFixedThreadPool(150);                   // sufficient for n,nMaxx=8 is 128
CompletionService<String> service = new ExecutorCompletionService<>(executorService1);    
Semaphore muSem = new Semaphore(1);
List muList = new ArrayList();            
GaussMain gMain = new GaussMain();
List semaphoreListF = new ArrayList();
List semaphoreListB = new ArrayList();

PolynomialArray pArray = null;    // 
PolynomialArray figPArray = null;  // the second field variable in figPArray is the leading P scalar.
PolynomialArray cashedFigPArray =  null;

//  PolynomialArray pArray = new PolynomialArray(dimension);    // 
//  PolynomialArray figPArray = new PolynomialArray(dimension,2);  // the second field variable in figPArray is the leading P scalar.
//   PolynomialArray cashedFigPArray = new PolynomialArray(dimension,1);

ResultListBean rListB =new ResultListBean();
ResultListBean allrListB =new ResultListBean();
List ancestorlist = new ArrayList();
List deltaList=new ArrayList();
ResultListBean cashedrListB =new ResultListBean();
//ResultListBean cashedrListB;
PolynomialArray cashedPArray = new PolynomialArray(dimension);
//PolynomialArray cashedPArray new PolynomialArray(dimension);     

//PolynomialArray cashedFigPArray;
EndProduct endProduct=new EndProduct(); 
//LoopsLogicLoopSemaphore loopsLogic;  //any class that extends AbstractLoopSemaphore
//LoopsSemaphoreInterface loopsLogic= new LoopsLogicLoopSemaphore(); 
//LoopsSemaphoreInterface loopsLogic;
LoopsLogicLoopSemaphore loopsLogic;
PArrayResetFlag pArrayResetFlag = new PArrayResetFlag();
final ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
//int numeratorLow=0;
//int numeratorHigh=5;
//plvManager mage=null;
FractionIntegerDriverIterate mage=null;
GaussTable1 magee=null;
private BigDecimal NN=null;
    private BigDecimal one= new BigDecimal(1);
    private BigDecimal flatFileRowCounterr=null;
    private BigDecimal nMaxx= new BigDecimal(1);   //  setters, because of setfields, sould be private???
    private BigDecimal tSeqDB=null;
    private BigDecimal bTermDB=null;
    private BigDecimal targetEvaluateDB=null;
    private vertex vertexDB=null;
    private Semaphore tl1Semaphore;
    private List dbVector11= new ArrayList();

Semaphore muSem1 = new Semaphore(1);
List muList1 = new ArrayList();
Semaphore configSemaphore;
String pArrayConfigure=null;
GaussTable1 gT1= new GaussTable1();

//         LoopsDriverTwoPManager lDriver= new LoopsDriverTwoPManager(muList,muSem,loopsLogic,t11Semaphore,dbVector11,t11Semaphore,dbVector11,false,configSemaphore);



public LoopsDriverTwoPManager(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,Semaphore tl1Semaphore,List dbVector11) {
   this.muList=muList;
   this.muSem=muSem;
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
   //this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
}

public LoopsDriverTwoPManager(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,plvManager mage,Semaphore tl1Semaphore,List dbVector11,boolean noBufferRun,Semaphore configSemaphore) {
   this.muList=muList;
   this.muSem=muSem;
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
//   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;
   this.dbVector11=dbVector11;
   this.noBufferRun=noBufferRun;
   this.configSemaphore=configSemaphore;
}    
                                                      
public LoopsDriverTwoPManager(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,FractionIntegerDriverIterate mage,Semaphore tl1Semaphore,List dbVector11,boolean noBufferRun,Semaphore configSemaphore) {
//   this.muList=muList;
//   this.muSem=muSem;
   this.muList=muList;
   this.muSem=muSem;
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
   this.mage=mage;
   this.tl1Semaphore=tl1Semaphore;  //  the cuffent gaussTable1 buffer and semaphore.
   this.dbVector11=dbVector11;
   this.noBufferRun=noBufferRun;
   this.configSemaphore=configSemaphore;
}

public LoopsDriverTwoPManager(List muList,Semaphore muSem,LoopsLogicLoopSemaphore loopsLogic,Semaphore muSem1,List muList1,Semaphore tl1Semaphore,List dbVector11,boolean noBufferRun,Semaphore configSemaphore) {
//   this.muList=muList;
//   this.muSem=muSem;
   this.muList=muList;
   this.muSem=muSem;
   this.muList1=muList;
   this.muSem1=muSem;
   this.loopsLogic=loopsLogic;
   this.tl1Semaphore=tl1Semaphore;  //  the cuffent gaussTable1 buffer and semaphore.
   this.dbVector11=dbVector11;
   this.noBufferRun=noBufferRun;
   this.configSemaphore=configSemaphore; 
}

private void noBufferRun()   {

   int workerCount=0;
   System.out.println("LoopsDriverTwoPManager.noBufferRun");    
   figPArray = new PolynomialArray(dimension,maxFigPScalar,true);
   cashedFigPArray = new PolynomialArray(dimension,maxFigPScalar,true);
   pArray = new PolynomialArray(dimension,TRUE);    //    Constructor ok for nobufferrun.  List of size dimension+1 with each value=0. 
//   pArray.set(1, 4);   pArray.set(2, 4);  pArray.set(3, 4);  pArray.set(4, 4);     //  pArray.set(4, 5);  splicing second half of setProductRange in noBufferRun
   System.out.println("LoopsDriverTwoPManager.run.figPArray: "+figPArray.toString());
   System.out.println("LoopsDriverTwoPManager.run.cashedFigPArray: "+cashedFigPArray.toString());
   System.out.println("LoopsDriverTwoPManager.run.pArray: "+pArray.toString());
//   Callable worker = (Callable) new LoopsDriverTwoP(muList,muSem,loopsLogic,muSem1,muList1,mage,tl1Semaphore,dbVector11,(PolynomialArray)figPArray.clone(),(PolynomialArray)cashedFigPArray.clone(),(PolynomialArray)pArray.clone(),nMaxx.intValue(),resultListId,setProductRange,TRUE);
    Callable worker = (Callable) new LoopsDriverTwoP(muList,muSem,loopsLogic,muSem1,muList1,tl1Semaphore,dbVector11,(PolynomialArray)figPArray.clone(),(PolynomialArray)cashedFigPArray.clone(),(PolynomialArray)pArray.clone(),nMaxx.intValue(),resultListId,setProductRange,TRUE,dimension,integerRange,lbSetProductRange,pArrayResetValue);
   service.submit(worker); workerCount++;
   try {
       executorService1.shutdown();
       while (workerCount>0)   {
//       while (!executorService1.isTerminated()) {    
       final Future<String> future = service.take();
       System.out.println("service.take "+future.get());
       System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
       System.out.println();System.out.println();System.out.println();
       workerCount--;
//       System.out.println("AllRlistB  " + allrListB.toString()  );
        }   
   } catch (ExecutionException | InterruptedException ex) {
   ex.printStackTrace();
   } 
}

/*   pArray  
*   figPArray 
*   cashedFigPArray  Are initialized.
*
*/

private void noBufferRun(ArrayList theList)   {

   int workerCount=0;
   
   ExecutorService executorService1 = Executors.newFixedThreadPool(150);                   // sufficient for n,nMaxx=8 is 128
   CompletionService<String> service = new ExecutorCompletionService<>(executorService1);
   
   System.out.println("LoopsDriverTwoPManager.noBufferRun");    
   figPArray = new PolynomialArray(dimension,maxFigPScalar,true);
   cashedFigPArray = new PolynomialArray(dimension,maxFigPScalar,true);
//   pArray = new PolynomialArray(dimension,TRUE,pArrayResetValue);    //  used with no pArrayString on configNode          Constructor ok for nobufferrun.  List of size dimension+1 with each value=0. 
   pArray = new PolynomialArray(dimension,TRUE,arrayStringToIntegerArrayList(pArrayConfigure));
//   pArray.set(1, 4);   pArray.set(2, 4);  pArray.set(3, 4);  pArray.set(4, 4);     //  pArray.set(4, 5);  splicing second half of setProductRange in noBufferRun
   System.out.println("LoopsDriverTwoPManager.run.figPArray: "+figPArray.toString());
   System.out.println("LoopsDriverTwoPManager.run.cashedFigPArray: "+cashedFigPArray.toString());
   System.out.println("LoopsDriverTwoPManager.run.pArray: "+pArray.toString());
//   Callable worker = (Callable) new LoopsDriverTwoP(muList,muSem,loopsLogic,muSem1,muList1,mage,tl1Semaphore,dbVector11,(PolynomialArray)figPArray.clone(),(PolynomialArray)cashedFigPArray.clone(),(PolynomialArray)pArray.clone(),nMaxx.intValue(),resultListId,setProductRange,TRUE);
    Callable worker = (Callable) new LoopsDriverTwoP(muList,muSem,loopsLogic,muSem1,muList1,tl1Semaphore,dbVector11,(PolynomialArray)figPArray.clone(),(PolynomialArray)cashedFigPArray.clone(),(PolynomialArray)pArray.clone(),nMaxx.intValue(),resultListId,setProductRange,TRUE,dimension,integerRange,lbSetProductRange,pArrayResetValue,theList);
   service.submit(worker); workerCount++;
   try {
       executorService1.shutdown();
       while (workerCount>0)   {
//       while (!executorService1.isTerminated()) {    
       final Future<String> future = service.take();
       System.out.println("service.take noBufferRun"+future.get());
       System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
       System.out.println();System.out.println();System.out.println();
       workerCount--;
//       System.out.println("AllRlistB  " + allrListB.toString()  );
        }   
   } catch (ExecutionException | InterruptedException ex) {
   ex.printStackTrace();
   }
      System.out.println("Exit LoopsDriverTwoPManager.noBufferRun");  
}

private Boolean validMu(ArrayList theList) {
    int size=0;
    size=size+(int)theList.get(theList.size()-1)-1+theList.size();
    if(size<dimension)
    { return(true);}
    else 
    { return(false);}   
}

/*
*   List argumented with skip worker num booleans.
*/

private List muListBoolean(ArrayList theList)  {
List muListBoolean = new ArrayList(); 
int currentTheListValue;
int mLBIndex;

for (int x=0; x<dimension; x++)  {
   muListBoolean.add(false); 
}
//System.out.println("muListBoolean, theList: "+theList.toString());
for (int x=0; x<theList.size(); x++)  {
////    System.out.println(x);
    currentTheListValue=(int)theList.get(x);
    mLBIndex=currentTheListValue-1+x;
    muListBoolean.set(mLBIndex, true);
}
return(muListBoolean);    
}

/*
*   Blank List for no skip in semaphore patter.
*/

private List muListBoolean()  {
List muListBoolean = new ArrayList(); 
int currentTheListValue;
int mLBIndex;

for (int x=0; x<dimension; x++)  {
   muListBoolean.add(false); 
}
//System.out.println("muListBoolean, theList: "+theList.toString());
//for (int x=0; x<theList.size(); x++)  {
////    System.out.println(x);
//    currentTheListValue=(int)theList.get(x);
//    mLBIndex=currentTheListValue-1+x;
//    muListBoolean.set(mLBIndex, true);
//}
return(muListBoolean);    
}

/*  Query for runtime parameters  
*
*/

private void configureQuery()  {    
    ResultSet rs;
    rs=gT1.configureQuery();
    Boolean hasRS = TRUE;
    try {
        if ( hasRS ) {
        if (rs.next())  {
           this.setProductRange=rs.getInt(1);
           this.setProductRangeIncrement=rs.getInt(2);
           this.maxSetProductRange=rs.getInt(3);
           this.dimension=rs.getInt(4);
           this.maxFigPScalar=rs.getInt(5);
           this.integerRange=rs.getInt(6);
           hasRS=FALSE;
        } else { hasRS=FALSE; } 
        }
    } catch (SQLException ex) {
        Logger.getLogger(LoopsDriverTwoPManager.class.getName()).log(Level.SEVERE, null, ex);
    }
    gT1.setDimension(dimension);
//    System.out.println("ConfigureQuery hasRS: "+hasRS);

}

/*  Query for runtime parameters  
*
*/

private void configureQuery1()  {    
    ResultSet rs;
    boolean claimed = false;
    int retries = 0;
    int maxRetries = 30;

    while (!claimed && retries < maxRetries) {
        rs = gT1.configureQuery1();
        try {
            if (rs.next()) {
                this.setProductRange = rs.getInt(1);
                this.setProductRangeIncrement = rs.getInt(2);
                this.maxSetProductRange = rs.getInt(3);
                this.dimension = rs.getInt(4);
                this.maxFigPScalar = rs.getInt(5);
                this.integerRange = rs.getInt(6);
                this.pArrayConfigure = rs.getString(7);
                claimed = true;
            } else {
                retries++;
                System.out.println("No Configure node available, retry " + retries + "/" + maxRetries);
                Thread.sleep(2000);
            }
        } catch (SQLException ex) {
            Logger.getLogger(LoopsDriverTwoPManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            break;
        }
    }

    if (!claimed) {
        throw new RuntimeException("Failed to claim a Configure node after " + maxRetries + " retries");
    }

    System.out.println("configureQuery1 dimension:" + this.dimension);
    gT1.setDimension(this.dimension);
}

/*   Query for next worker skip pattern
*
*/

private List muQuerry()  {    
    ResultSet rs;
    List valueList=new ArrayList();   
    rs=gT1.muQuerry();
    Boolean hasRS = TRUE;
    System.out.println("ValueList: " + valueList.toString());
    try {
        if ( hasRS ) {
        if (rs.next())  {
            for (int j=1; j< 4; j++) {  // only 3 items on resultSet row.
                try {
                    valueList.add(rs.getString(j));
                    //                System.out.println("rs(j): " + rs.getString(j).toString());
                }
                catch(NullPointerException npe) {
                    System.out.println("ResultSet Null");
                }
            }
             System.out.println(valueList.toString());
             hasRS=FALSE;
        } else { hasRS=FALSE; } 
        }
    } catch (SQLException ex) {
        Logger.getLogger(LoopsDriverTwoPManager.class.getName()).log(Level.SEVERE, null, ex);
    }
    System.out.println("MuQuerry hasRS: "+hasRS);
    return(valueList);
}

/*
*   https://stackoverflow.com/questions/29674354/java-arraylistinteger-to-arrayliststring-vice-versa
*/

public ArrayList<Integer> arrayStringToIntegerArrayList(String arrayString){
    String removedBrackets = arrayString.substring(1, arrayString.length() - 1);
    String[] individualNumbers = removedBrackets.split(",");
    ArrayList<Integer> integerArrayList = new ArrayList<>();
    for(String numberString : individualNumbers){
        integerArrayList.add(Integer.parseInt(numberString.trim()));
    }
    return integerArrayList;
}

/*
* Peculiar...  gTable1 is a thread but the lable is onle scoped within startbuffer().
*/



private void startbuffer() {
   try { 
        GaussTable1 gTable1 = new GaussTable1(tl1Semaphore,dbVector11,configSemaphore);
        gTable1.start();
        gTable1.setDimension(this.dimension);
      } catch (IOException ex) {
        Logger.getLogger(LoopsDriverTwoPManager.class.getName()).log(Level.SEVERE, null, ex);
       }     
  
}

@Override
public void run() {
 
   List valueList;
   List nBList;
   int workerCount=0;
 //  configureQuery();     //  pArray, a string on configureNode now used for constructing pArrary
   configureQuery1();     // CofigeNode querry with pArray string.
   GaussTable1 gTable1=null;
    try {
        gTable1 = new GaussTable1(tl1Semaphore,dbVector11,configSemaphore);
        gTable1.start();
    } catch (IOException ex) {
        Logger.getLogger(LoopsDriverTwoPManager.class.getName()).log(Level.SEVERE, null, ex);
    }
   //startbuffer();

   System.out.println("SetProductRange: " + setProductRange + "maxSetProductRange: " + maxSetProductRange);
   noBufferRun((ArrayList)muListBoolean()); // commented out because already on graph.
   valueList=muQuerry();

   int takeCount=0;
   System.out.println( "valuList is empty"+valueList.isEmpty()); //
     System.out.println( "valuList "+valueList.toString()); 
   while (!valueList.isEmpty() ) 
// while (!valueList.isEmpty())
    {   
       System.out.println( "nBinary.toList" + arrayStringToIntegerArrayList((String)valueList.get(0)).toString());
       takeCount++;
       nBList=arrayStringToIntegerArrayList((String)valueList.get(0));
       System.out.println("nBlist: " + nBList.toString());
       Collections.reverse(nBList);
       System.out.println("LoopsDriverTwoP nBList reverse:  " + nBList.toString());
       System.out.println(validMu((ArrayList)nBList));
        if (validMu((ArrayList)nBList)) noBufferRun((ArrayList)muListBoolean((ArrayList)nBList));
       Collections.reverse(nBList);     //  Restore to origonal nBList for delete match.
       gT1.muDelete(nBList.toString());
       valueList.clear();
       valueList=muQuerry();
   try {
       executorService1.shutdownNow();
       while (workerCount>0)   {
       final Future<String> future = service.take();
       System.out.println("service.take LoopsDriverTwoPManager.run()"+future.get());
       System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
       System.out.println();System.out.println();System.out.println();
       workerCount--;
       }   
   } catch (ExecutionException | InterruptedException ex) {
   ex.printStackTrace();
   }   
   System.out.println("LoopsDriverTwoPManager Exit ");
//   pArrayResetValue=setProductRange+1;
//   lbSetProductRange=pArrayResetValue;
//   setProductRange=setProductRange+setProductRangeIncrement;
//   try {
//       configSemaphore.acquire();
//   } catch (InterruptedException ex) {
//       Logger.getLogger(LoopsDriverTwoPManager.class.getName()).log(Level.SEVERE, null, ex);
//   }
   }   //  end while loop
   
   gTable1.setProducerDone();
   try{
       gTable1.join();
       System.out.println("LoopsMain post gTable1.join()");
      } catch (InterruptedException e){
       System.out.println("gtable threadInterrupted");
     }
   
   System.out.println("LoopsDriverTwoPManager.run() Exit ");
}

public void runney() {
   List valueList;
   List nBList;
   int workerCount=0;
   int fIDI=-1;
   configureQuery();
   System.out.println("SetProductRange: " + setProductRange + "maxSetProductRange: " + maxSetProductRange);
   while (setProductRange<=maxSetProductRange)   
   {   
       try { 
           GaussTable1 gTable1 = new GaussTable1(tl1Semaphore,dbVector11,configSemaphore);
           gTable1.start();
       } catch (IOException ex) {
           Logger.getLogger(LoopsDriverTwoPManager.class.getName()).log(Level.SEVERE, null, ex);
       }
   if (fIDI==-1) {  noBufferRun((ArrayList)muListBoolean()); } else {
   valueList=muQuerry();
   System.out.println( "nBinary.toList" + arrayStringToIntegerArrayList((String)valueList.get(0)).toString());
   while (!valueList.isEmpty() && fIDI<1 ) 
    {       
       nBList=arrayStringToIntegerArrayList((String)valueList.get(0));
       System.out.println("nBlist: " + nBList.toString());
       Collections.reverse(nBList);
       System.out.println("LoopsDriverTwoP nBList reverse:  " + nBList.toString());
       System.out.println(validMu((ArrayList)nBList));
       if (fIDI<1) { if (validMu((ArrayList)nBList)) noBufferRun((ArrayList)muListBoolean((ArrayList)nBList));}
       fIDI++;
       Collections.reverse(nBList);     //  Restore to origonal nBList for delete match.
       gT1.muDelete(nBList.toString());
       valueList.clear();
       valueList=muQuerry();
   }   //  end while loop      
   }   // end else
   try {
       executorService1.shutdownNow();
       while (workerCount>0)   {
       final Future<String> future = service.take();
       System.out.println("service.take "+future.get());
       System.out.println("isTerminated() ThreadMXBean:    "+Arrays.toString(bean.getAllThreadIds()));
       System.out.println();System.out.println();System.out.println();
       workerCount--;
       }   
   } catch (ExecutionException | InterruptedException ex) {
   ex.printStackTrace();
   } 
//   System.out.println("LoopsDriverTwoPManager Exit ");
//   pArrayResetValue=setProductRange+1;
//   lbSetProductRange=pArrayResetValue;
//   setProductRange=setProductRange+setProductRangeIncrement;
//   try {
//       configSemaphore.acquire();
//   } catch (InterruptedException ex) {
//       Logger.getLogger(LoopsDriverTwoPManager.class.getName()).log(Level.SEVERE, null, ex);
//   }
  }  //  Endo of setProductRange While
} //  End of run   
} //  End of LDTPM
