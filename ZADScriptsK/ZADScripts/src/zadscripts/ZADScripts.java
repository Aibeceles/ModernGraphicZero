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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;



/**
 *
 * @author ChiefQuippy
 */
public class ZADScripts {

// CSV file path loaded from db.properties via DbConfig.get("neo4j.import.path")
  static String maxN = "20";
  static String lowN = "2";
  static String highN = "19";
  static String dimension1 = "2";
  static String dimension2 = "2";

  
  /**
    * @param args the command line arguments
    * @throws java.lang.Exception
    * 
    * 
    * 
   */
    
public static void main(String[] args) throws Exception
{
    final Properties kafkaProps = new Properties();
    kafkaProps.put("bootstrap.servers", DbConfig.get("kafka.bootstrap.servers"));
    kafkaProps.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
    kafkaProps.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");

    Producer<String, String> producer = new KafkaProducer<>(kafkaProps, new StringSerializer(), new StringSerializer());
    
        Map batchMap = new HashMap();
        ScriptParameters sPara = new ScriptParameters(args);
        BigDecimal two = new BigDecimal(2);
if (    "2".equals(args[3])) {      
        sPara.setlowRange(args[0]);
        sPara.sethighRange(args[1]);
        sPara.setindex(args[2]);
        sPara.setreadDimension1(args[3]);
        sPara.setreadDimension2(args[3]);
        BigDecimal wDimension = new BigDecimal(args[3]);
        wDimension=wDimension.multiply(two);
        sPara.setwriteDimension(wDimension.toString());
        BigDecimal wDimension1 = new BigDecimal (wDimension.toString());
        wDimension1=wDimension1.multiply(two);
        sPara.setnextWriteDimension(wDimension1.toString());
        ScriptReturnBoolean srBol = new ScriptReturnBoolean();
        ResultSetMap rSMap = new ResultSetMap(); 
        ScriptsAutomation sAuto = new ScriptsAutomation(srBol,producer,rSMap);
        ScriptScheduler sScheduler = new ScriptScheduler(sPara,sAuto,srBol);
        sScheduler.scheduler();
        sAuto.endConn();
}  else { if (!"4".equals(args[3]))  {

        System.out.println("else if !=4");
        sPara.setlowRange(args[0]);
        sPara.sethighRange(args[1]);
        sPara.setindex(args[2]);
        sPara.setreadDimension1(args[3]);
        sPara.setreadDimension2(args[3]);
        BigDecimal wDimension = new BigDecimal(args[3]);
        wDimension=wDimension.multiply(two);
        System.out.println("writedimension: "+wDimension.toString());
        sPara.setwriteDimension(wDimension.toString());
        sPara.setnextWriteDimension(wDimension.toString());
        ScriptReturnBoolean srBol = new ScriptReturnBoolean();       
        ResultSetMap rSMap = new ResultSetMap();
        ScriptsAutomation sAuto = new ScriptsAutomation(srBol,producer,rSMap);
        ScriptScheduler sScheduler = new ScriptScheduler(sPara,sAuto,srBol);
        sScheduler.scheduler();
        sAuto.endConn();
}
}
          producer.flush();
          producer.close();       
}
  
}
