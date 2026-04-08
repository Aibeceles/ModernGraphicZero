/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package MuBinary;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author Aibes
 */


public class ListToNumber {
 
public BigDecimal getIntegerFromList(List nList)    
{
int currentInt=0;
int currentPowerIndex=0;
int whichPowerIndex=1;
BigDecimal resultInt = new BigDecimal(0);  
BigDecimal currentResult = new BigDecimal(0);
BigDecimal twoPower = new BigDecimal(2);

int loopIndex=nList.size();

BigDecimal loop = new BigDecimal(loopIndex);
for (int j=(loopIndex-1); j>-1; j--)
{
 currentInt=(int)nList.get(j);   
//currentPowerIndex=currentPowerIndex-whichPowerIndex+currentInt;
currentPowerIndex=(whichPowerIndex-1)+(currentInt-1);
currentResult=twoPower.pow((currentPowerIndex));
 //currentPowerIndex=currentPowerIndex+currentInt;
 resultInt=resultInt.add(currentResult);
 whichPowerIndex++;
}     
return(resultInt);
}    

}
