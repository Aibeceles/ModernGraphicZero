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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;




/**
 *
 * @author ChiefQuippy
 */
public class WriteCSV {
 
    private final String csvFile;             
    FileWriter writer;
    List keyList = new ArrayList();
    List rsMapList = new ArrayList();
    List batchList = new ArrayList();


    
public WriteCSV(String passedCsvFile) throws IOException {
    
    csvFile=passedCsvFile;
//    this.writer = new FileWriter(csvFile,true);  
    
}

public void keyListHeader() {
   
    keyList.add("iM");keyList.add("iN");keyList.add("pD");keyList.add("p2Eval");
    keyList.add("vTwoSeq");keyList.add("TTDegree");keyList.add("TTScalar");
    keyList.add("vDivisor");keyList.add("vvDivisor");keyList.add("vvTwoSeq");
    keyList.add("AADegree");keyList.add("AAScalar");keyList.add("degreeSum");
    keyList.add("scalarProduct");keyList.add("resultDivisor");keyList.add("rKey");    
    
}



public ArrayList s1Header() {
    ArrayList valueList=new ArrayList();
    valueList.add("iM");valueList.add("iN");valueList.add("pD");valueList.add("p2Eval");
    valueList.add("vTwoSeq");valueList.add("TTDegree");valueList.add("TTScalar");
    valueList.add("vDivisor");valueList.add("vvDivisor");valueList.add("vvTwoSeq");
    valueList.add("AADegree");valueList.add("AAScalar");valueList.add("degreeSum");
    valueList.add("scalarProduct");valueList.add("resultDivisor");valueList.add("rKey");    
    return(valueList);
}

public ArrayList s3THeader() {
    ArrayList valueList=new ArrayList();
    valueList.add("tDegree");valueList.add("tScalar");valueList.add("iN");valueList.add("iM");
    valueList.add("pTD");valueList.add("eTrm");
    return(valueList);
}

public ArrayList s3AHeader() {
    ArrayList valueList=new ArrayList();
    valueList.add("tDegree");valueList.add("tScalar");valueList.add("pTD");valueList.add("iN");valueList.add("iM");
    return(valueList);
}





public void closeWriter() {
        try {
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(WriteCSV.class.getName()).log(Level.SEVERE, null, ex);
        }
}


private static String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;
}


public void writeLine( List<String> values, char separators, char customQuote) throws IOException {
        Map rsMap = new HashMap();
        int index = 0; 
        boolean first = true;
        //default customQuote is empty
        if (separators == ' ') {
            separators = ',';
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                 // System.out.println(value  + "   !first");
                sb.append(separators);
            }
            if (customQuote == ' ') {
                  //System.out.println(value  + "   customQuote");
                 if (value==null) {  System.out.println("null Value  qustomeQuote ");  rsMap.put(keyList.get(index), value);   }
                 else {sb.append(followCVSformat(value));  rsMap.put(keyList.get(index), value);   }
            } else {
                 // System.out.println(value  + "     customQuote else");
                if (value==null) { sb.append(customQuote).append(customQuote); rsMap.put(keyList.get(index), value); }    //  System.out.println("null Value  qustomeQuote else"); 
                else { sb.append(customQuote).append(followCVSformat(value)).append(customQuote); rsMap.put(keyList.get(index), value); }
            }
            first = false;
            index++;
        }
        sb.append("\n");
 //       this.writer.append(sb.toString());
        batchList.add(rsMap);
}

public List returnBatchList() {
    return(batchList);
    
}


public String buildBatch(Map batchMap ) {
    batchMap.put("batch", batchList);
 //   return(batchMap);
    String batchString=batchMap.toString();
    //String theBatch = 
    return(batchString.replace('=', ':'));
}

}
