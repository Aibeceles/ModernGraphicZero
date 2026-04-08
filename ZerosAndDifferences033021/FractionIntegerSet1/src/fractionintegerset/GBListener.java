/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fractionintegerset;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 *
 * @author HP_Administrator
 */
public class GBListener implements PropertyChangeListener {

    ResultListBean rListB;
    List muList = new ArrayList();
    Semaphore muSem = new Semaphore(1);
    
public    GBListener(ResultListBean rListB,List muList,Semaphore muSem) {
    this.rListB=rListB; 
    this.muList=muList;
    this.muSem=muSem;
}
public void propertyChange(PropertyChangeEvent evt) {
     synchronized (this) {
    if ( !((GBList)evt.getOldValue()).isEmpty()  )   { ((GBList)evt.getOldValue()).remove(0); }
   
     ((GBList)evt.getOldValue()).add(evt.getNewValue());
     muList.add(evt.getNewValue());
     muSem.release();
     //  rListB.

 //  System.out.println(" listener!      muList:  " + muList.toString());   
   }
   }    
    
}
