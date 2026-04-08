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


package MainClass;

import java.math.BigDecimal;
import java.util.concurrent.Semaphore;
import twopolynomial.plvManager;
import twopolynomial.vertexVector;
import twopolynomial.zaddbTable1;


/**
 *
 * @author Aibes
 */
public class LoopsMain {

String csvFile;    
BigDecimal NN;    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     * Generates quadratic progression data on single plvRunThread thread.
     * String[] args is looking for  filename.csv, index.
     * 
    */
 
private void parseArguments(String[] args)
    {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("derbyclient"))
            {
              this.csvFile=args[0];
              this.NN = new BigDecimal(args[1]);
            }
        }
    }
    
public static void main(String[] args) throws Exception {
 
    Semaphore tlSemaphore = new Semaphore(1);
    Semaphore tl1Semaphore = new Semaphore(1);
    Semaphore t2Semaphore = new Semaphore (1);
    Semaphore cosetSemaphore = new Semaphore(1);     
    vertexVector dbVector = new vertexVector();
    vertexVector dbVector11 = new vertexVector();
    vertexVector dbVector1 = new vertexVector();
    vertexVector dbVectorCST = new vertexVector();
   
 
    if (args.length > 0) {
    String csvFile=args[0];
    BigDecimal NN = new BigDecimal(args[1]);
    }
       
    plvManager mage = new plvManager(tlSemaphore,t2Semaphore,cosetSemaphore,dbVector,dbVector1,dbVectorCST,tl1Semaphore,dbVector11,new BigDecimal(args[1]),new BigDecimal(args[2]),new BigDecimal(args[3]));
    zaddbTable1 bmage = new zaddbTable1(tlSemaphore,dbVector,mage,args[0]);
    mage.start();
    bmage.start();
                                                               
  try{
    mage.join();
  } 
  catch (InterruptedException e){
     System.out.println("plvManager threadInterrupted");
  }
    
 try{
    bmage.join();
  } 
  catch (InterruptedException e){
     System.out.println("zaddbTable1 threadInterrupted");
  }
 }
}

