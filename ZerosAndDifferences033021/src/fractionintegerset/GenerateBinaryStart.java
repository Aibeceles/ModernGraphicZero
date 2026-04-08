/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fractionintegerset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 *
 * @author Aibes
 */
public class GenerateBinaryStart implements Callable<String>{

/* numerator and denominator need to be initialized in constructor
 */    
int numerator;
int denominator;
Semaphore startSemap;

public GenerateBinaryStart(Semaphore startS) {

    startSemap=startS;
}

public String call() throws InterruptedException {
       long threadId = Thread.currentThread().getId();
    System.out.println("Generate BinaryStart ThredID " + threadId + " StartSemaphore permits" + startSemap.availablePermits());
    System.out.println( startSemap.availablePermits());
    startSemap.release();
 
 //   System.out.println("ThredID" + threadId + " ReturnList" + returnList.toString());

    return("generateBinaryStart return");
    }
}

