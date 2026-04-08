/*
 * Copyright (C) 2021 Aibes
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
package differenceScripts;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Utility for determining number of bits in mu representation 
 * @author Aibes
 */
public class NBinaryUtility {
    
    
public NBinaryUtility() {
    
}    
    
public static ArrayList<Integer> arrayStringToIntegerArrayList(String arrayString){
    String removedBrackets = arrayString.substring(1, arrayString.length() - 1);
    String[] individualNumbers = removedBrackets.split(",");
    ArrayList<Integer> integerArrayList = new ArrayList<>();
    for(String numberString : individualNumbers){
        integerArrayList.add(Integer.parseInt(numberString.trim()));
    }
    Collections.reverse(integerArrayList);
    return integerArrayList;
}

private static Boolean validMu(ArrayList theList, int dimension) {
    int size=0;
    size=size+(int)theList.get(theList.size()-1)-1+theList.size();
    if(size<dimension)
    { return(true);}
    else 
    { return(false);}   
}

public static int sizeMu(ArrayList theList) {
    int size=0;
    return(size+(int)theList.get(theList.size()-1)-1+theList.size());
    
} 

}
