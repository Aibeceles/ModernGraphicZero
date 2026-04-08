/*
 * Copyright (C) 2020 Aibes
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Aibes
 */
public class ResultSetMap {
  
private HashMap rsMap=new HashMap();
private HashMap rsMap3t=new HashMap();
private HashMap rsMap3a=new HashMap();
private HashMap s12Map=new HashMap();
private HashMap s3tMap=new HashMap();
private HashMap s3aMap=new HashMap();

public ResultSetMap() {
    
}  

public void setMap(HashMap rsMap) {
    this.rsMap.putAll(rsMap);
        
}

public void setMap3t(HashMap rsMap) {
    this.rsMap3t.putAll(rsMap);
        
}

public void setMap3a(HashMap rsMap) {
    this.rsMap3a.putAll(rsMap);
        
}

public Map getMap(){
    return(this.rsMap);
}

public void setKeyValueS12Map(String key, Object value ) {
//    System.out.println("setKeyValueS12Map key value "+ key + " "+ value);
    Object putKeyValue = new Object();
    try {
    putKeyValue=s12Map.put(key, value);
    } catch (NullPointerException ex)
    {   }

}

public void setKeyValueS3tMap(String key, Object value ) {
//    System.out.println("setKeyValueS12Map key value "+ key + " "+ value);
    Object putKeyValue = new Object();
    try {
    putKeyValue=s12Map.put(key, value);
    } catch (NullPointerException ex)
    {   }
}




public Object getValueRS(String key) {
  return(rsMap.get(key));   
}

public Object getValueRS3t(String key) {
  return(rsMap3t.get(key));   
}

public Object getValueRS3a(String key) {
  return(rsMap3a.get(key));   
}

public Object getValueS12(String key) {
  return(s12Map.get(key));   
}

public Object getValueS3t(String key) {
  return(s3tMap.get(key));   
}

public Object getValueS3a(String key) {
  return(s3aMap.get(key));   
}

public void setKeyValueS3aMap(String key, Object value ) {
    s3aMap.put(key, value);
}

public void printMap(HashMap rMap) {
    Set keySett= new HashSet();
    keySett=rMap.keySet();
    Iterator iterator=keySett.iterator();
    while(iterator.hasNext())  
    {
        String element = (String) iterator.next();
        System.out.println("KeySet: "+ element   );
        System.out.println("(Map)rs.toString: " + rMap.get(element));
    }
}

public void printMap() {
    Set keySett= new HashSet();
    keySett=rsMap.keySet();
    Iterator iterator=keySett.iterator();
    while(iterator.hasNext())  
    {
        String element = (String) iterator.next();
        System.out.print("KeySet: "+ element  + " " );
        System.out.print( rsMap.get(element) + " ");
    }
    System.out.println(" ");
}

public void printMapt() {
    Set keySett= new HashSet();
    keySett=rsMap3t.keySet();
    Iterator iterator=keySett.iterator();
    while(iterator.hasNext())  
    {
        String element = (String) iterator.next();
        System.out.print("KeySet: "+ element  + " " );
        System.out.print( rsMap3t.get(element) + " ");
    }
    System.out.println(" ");
}


public void printMapa() {
    Set keySett= new HashSet();
    keySett=rsMap3a.keySet();
    Iterator iterator=keySett.iterator();
    while(iterator.hasNext())  
    {
        String element = (String) iterator.next();
        System.out.print("KeySet: "+ element  + " " );
        System.out.print( rsMap3a.get(element) + " ");
    }
    System.out.println(" ");
}



}
