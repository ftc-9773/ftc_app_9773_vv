/*
 * Copyright (c) 2017 Robocracy 9773
 */

package org.firstinspires.ftc.teamcode.util.JsonReaders;

import com.qualcomm.ftccommon.DbgLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class JsonReader {
    public static final String baseDir = new String("/sdcard/FIRST/team9773/");
    public static final String sensorSpecsFile = new String(baseDir + "specs/sensor_specs.json");
    public static final String wheelSpecsFile = new String(baseDir + "specs/wheel_specs.json");
    public static final String motorSpecsFile = new String(baseDir + "specs/motor_specs.json");
    public static final String attachments = new String(baseDir + "attachments.json");
    public static final String navigationFile = new String(baseDir + "navigation_options.json");
    public static final String autonomousOptFile = new String(baseDir + "autonomous_options.json");
    public static final String driveSystemsFile = new String(baseDir + "drivesystems.json");
    public static final String opModesDir = new String(baseDir + "/opmodes/");
    public static final String autonomousRedDir =  new String(baseDir + "autonomous/red/");
    public static final String autonomousBlueDir = new String(baseDir + "autonomous/blue/");

    private String jsonFilePath;
    public String jsonStr;
    public JSONObject jsonRoot;
    public java.util.List<String> jsonRootNames;
    public JsonReader(String filePath) {
        FileReader fileReader = null;
        BufferedReader bufReader = null;
        StringBuilder strBuilder = new StringBuilder();
        String line = null;
        // If the given file path does not exist, give an error
        try {
            fileReader = new FileReader(filePath);
            bufReader = new BufferedReader(fileReader);
        }
        catch (IOException except) {
            DbgLog.error("ftc9773: Error while trying to open the json file %s", filePath);
            DbgLog.error("ftc9773: %s", except.getMessage());
        }

        // Read the file and append to the string builder
        try {
            while ((line = bufReader.readLine()) != null) {
                strBuilder.append(line);
            }
            // Now initialize the main variable that holds the entire json config
            jsonStr = new String(strBuilder);
        }
        catch (IOException except) {
            DbgLog.error("ftc9773: Error while reading the json file %s", filePath);
            DbgLog.error("ftc9773: %s", except.getMessage());
        }
        try {//TODO: Check this section (Does jsonRoot.keys() return ALL the root objects in the file (all the autonomousOptions?))
            jsonRoot = new JSONObject(jsonStr);
            jsonRootNames = new ArrayList<>();
            while(jsonRoot.keys().hasNext()){
                jsonRootNames.add(jsonRoot.keys().next());
            }
        }
        catch (JSONException except) {
            DbgLog.error("ftc9773: Error while parsing the json file.  Error message = %s",
                    except.getMessage());
        }
        return;
    }

    // This is a class method
    public static String getRealKeyIgnoreCase(JSONObject jobj, String key) throws JSONException {
        Iterator<String> iter = jobj.keys();
        while (iter.hasNext()) {
            String key1 = iter.next();
            if (key1.equalsIgnoreCase(key)) {
                return (key1);
            }
        }
        return null;
    }

//    public void getObject(){
//
//    }


}
