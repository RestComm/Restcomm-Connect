package org.mobicents.servlet.restcomm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// TODO: REMOVE THIS CLASS AFTER TESTING
// THIS IS A TEST CLASS, for some quick local tests, MUST BE IGNORED
public class Test {

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("rms1", "ms1");
        map.put("rms2", "ms2");
        map.put("rms3", "ms3");
        map.put("rms4", "ms4");

        Set<String> keys = map.keySet();
        String[] keyArr = keys.toArray(new String[0]);

        for(int i=0;i<10;i++){
            //System.out.println(keyArr[SimpleRoundRobin.getInstance(keyArr.length).getNextMediaGatewayIndex()]);
        }

    }

}
