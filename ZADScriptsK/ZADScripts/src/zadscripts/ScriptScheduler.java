/*
 * Copyright (C) 2020 ChiefQuippy
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
package zadscripts;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;


/**
 *
 * @author ChiefQuippy
 */
public class ScriptScheduler {

ScriptParameters sPara;
ScriptsAutomation sAuto;
ScriptReturnBoolean srBol;
BigInteger two= new BigInteger("2");
BigInteger scriptC = new BigInteger("2");

public ScriptScheduler(ScriptParameters sPara,ScriptsAutomation sAuto,ScriptReturnBoolean srBol) {
    this.sPara=sPara;
    this.sAuto=sAuto;
    this.srBol=srBol;
}    


/** 
 * guarded s12/Initial call
 * "Iterates" through lowRange highRange as arguments for conatraintLoop.s3A call after iteration. 
 * Not the best but will wrap method calls with individual degree iterators.
 * @throws com.fasterxml.jackson.core.JsonProcessingException
 * (indxZero<indx) so that iteration up to expected degree.
 */

public void scheduler() throws JsonProcessingException{
    BigInteger zero= new BigInteger("0");
    BigInteger one= new BigInteger("1");
    BigInteger two= new BigInteger("2");
    BigInteger rLower = new BigInteger(sPara.getlowRange());
    BigInteger rUpper = new BigInteger(sPara.gethighRange());
    Map s12Args= new HashMap(); 
    int rangeLower = rLower.intValue();
    int rangeUpper = rUpper.intValue();

    String indexS = sPara.getreadDimension1();
    BigInteger index = new BigInteger(indexS);
    int indx = index.intValue()+1;
    int indxZero=0;
    index=zero;
   
    while (rangeLower<rangeUpper+1) { 
       System.out.println("rLower: "+ rLower.toString());
       if ("2".equals(sPara.getreadDimension1())) {
       indx=5;
       srBol.setBooleanTRUE();
       constraintLoop(1, rangeLower, indexS, rLower.toString(), index.toString() );System.out.println("endend constraint loop 1");
       sAuto.pollS12();                                        // return from constraintLoop(1,...  poll for last map on ResultSetMap.
       indxZero=0;index=zero;
       srBol.setBooleanTRUE();
       constraintLoop(3, rangeLower, indexS, rLower.toString(), index.toString() );
       sAuto.pollS3t();                       // poll again 
       while (indxZero<indx) {  sAuto.s3ADc(rLower.toString(), rLower.toString(),sPara.getindex(),"8","8","8",scriptC.intValue(), index.toString()); index=index.add(one); indxZero++; }
       sAuto.pollS3a();// and one more poll.
       indxZero=0;index=zero;
       }
       else {
       srBol.setBooleanTRUE();
       while (indxZero<indx) { constraintLoop(2, rangeLower, indexS, rLower.toString(), index.toString() ); index=index.add(one); System.out.println("srBol: "+srBol.getBoolean()) ;if (srBol.getBoolean()) { indxZero++; } else {indxZero=indx; } }
       sAuto.pollS12();
       indxZero=0;index=zero;
       srBol.setBooleanTRUE();
       constraintLoop(5, rangeLower, indexS, rLower.toString(), index.toString() );
       sAuto.pollS3t();
       srBol.setBooleanTRUE();
       while (indxZero<indx) 
       { 
           sAuto.s3ADc(rLower.toString(), rLower.toString(),sPara.getindex(),sPara.getwriteDimension(),sPara.getwriteDimension(), sPara.getwriteDimension(),scriptC.intValue(), index.toString());
           index=index.add(one);
           if (srBol.getBoolean()) { indxZero++; } else {indxZero=indx; }
           System.out.println("s3ADC index.toString:" + index.toString());
       }
       sAuto.pollS3a();
       indxZero=0;index=zero; 
       srBol.setBooleanTRUE();       
    }
    rLower=rLower.add(one);
    rangeLower++;
 }
  
}

/**
 * 
 * @param scriptNum
 * @param rangeLower
 * @param indexS
 * @param rLowerS 
 */


private void constraintLoop(int scriptNum, int rangeLower, String indexS, String rLowerS, String dConstraint ) throws JsonProcessingException {
    
    BigInteger index = new BigInteger(indexS);
    BigInteger maxPower = new BigInteger(this.two.toString());
    maxPower=maxPower.pow(rangeLower);
    BigInteger rLower = new BigInteger(sPara.getlowRange());
    int indexI= index.intValue();
//    int indexI=rLower.intValue();
    int rangeCounter=(rangeLower-2);                             // rangeCounter=0;
//    System.out.println("indexI: "+indexI);
    do  {
    scriptC=scriptC.pow(rangeCounter);
    System.out.println("scriptConstraint, rangeCounter: "+scriptC.toString() + "  " + rangeCounter);
    switch (scriptNum) { 
        case 1: {
            
            sAuto.s12Initial(rLowerS.toString(), rLowerS.toString(), sPara.getindex(), sPara.getreadDimension1(), sPara.getreadDimension2(),sPara.getwriteDimension(),scriptC.toString());
            break;
        }
        case 2: {
            sAuto.s12Dc(dConstraint,rLowerS.toString(), rLowerS.toString(), sPara.getindex(), sPara.getreadDimension1(), sPara.getreadDimension2(),sPara.getreadDimension1(),scriptC.intValue());
            break;
        }
        case 3: {
            sAuto.s3TEc(rLowerS.toString(), rLowerS.toString(),sPara.getindex(),"4","4","8",scriptC.intValue());
            break;
        }
//        case 4: {
//            sAuto.s3A(rLowerS.toString(), rLowerS.toString(),sPara.getindex(),"4","4","8",scriptC.intValue());
//            break;
//            }
        case 5: {
            sAuto.s3TEc(rLowerS.toString(), rLowerS.toString(),sPara.getindex(),sPara.getreadDimension1(),sPara.getreadDimension1(), sPara.getwriteDimension(),scriptC.intValue());
            break;
        }
//        case 6: {
//            sAuto.s3A(rLowerS.toString(), rLowerS.toString(),sPara.getindex(),sPara.getreadDimension1(),sPara.getreadDimension2(), sPara.getwriteDimension(),scriptC.intValue());
//            break;
//            }
    }
    scriptC=this.two;
    System.out.println("rangecounter: "+rangeCounter);  
    if ((!srBol.getBoolean()) && (rangeCounter==rangeLower-2))    {rangeCounter=-1; System.out.println("First if");} else {  if (!srBol.getBoolean()) { srBol.setBooleanTRUE(); rangeCounter=-1; System.out.println("second if"); }     }
    rangeCounter--;
    } while (rangeCounter>-1);                          //  while (rangeCounter<(rangeLower-1));
}


}