package org.firstinspires.ftc.teamcode.opmodes;

import android.util.JsonWriter;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.AutonomousActions;
import org.firstinspires.ftc.teamcode.FTCRobot;
import org.firstinspires.ftc.teamcode.util.JsonReaders.AutonomousOptionsReader;
import org.firstinspires.ftc.teamcode.util.JsonReaders.JsonReader;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * Created by michaelzhou on 3/26/17.
 */

@TeleOp(name = "Autonomous Select", group = "TeleOp")
public class AutonomousSelect extends LinearOpMode {
    FTCRobot robot;
    String alliance;
    String autonomousOption;
    JsonReader jsonReader;
    List<String> autonomousOptions;

    @Override
    public void runOpMode() throws InterruptedException {
        try {
            redOrBlue();
            allOptions();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void redOrBlue(){
        alliance = "red";
        waitForStart();

        telemetry.addData("Current alliance", alliance);
        telemetry.update();

        while(opModeIsActive()) {
            if(gamepad1.dpad_down){
                alliance = "blue";
                telemetry.addData("Current alliance", alliance);
                telemetry.update();
            }
            if(gamepad1.dpad_up){
                alliance = "red";
                telemetry.addData("Current alliance", alliance);
                telemetry.update();
            }
            else if(gamepad1.a){
                return;
            }
        }
    }

    public void allOptions() throws JSONException {
        int index = 0;
        jsonReader = new JsonReader(JsonReader.autonomousOptFile);
        autonomousOptions = filtered(jsonReader.jsonRootNames, alliance);
        autonomousOption = autonomousOptions.get(index);

        telemetry.addData("Current autonomous option", autonomousOption);
        telemetry.update();
        DbgLog.msg("ftc9773: Option: ", autonomousOption);

        while(opModeIsActive()) {
            if(gamepad1.dpad_down){
                autonomousOption = autonomousOptions.get(index<autonomousOptions.size() ? index+1 : autonomousOptions.size()-1);
                telemetry.addData("Current autonomous option", autonomousOption);
                telemetry.update();
            }
            else if (gamepad1.dpad_up){
                autonomousOption = autonomousOptions.get(index>0 ? index-1 : 0);
                telemetry.addData("Current autonomous option", autonomousOption);
                telemetry.update();
            }
            else if(gamepad1.a){
                writeToFile(JsonReader.opModesDir + (alliance.equals("red") ? "AutonomousRed.json": "AutonomousBlue.json"));
                return;
            }
            idle();
        }
    }

    private List<String> filtered(List<String> ls, String alliance){
        while(ls.iterator().hasNext()){
            String e = ls.iterator().next();
            if(!e.contains(alliance)){
                ls.remove(e);
            }
        }
        return ls;
    }

    private void writeToFile(String path) throws JSONException {
        JsonReader reader = new JsonReader(path);
        List<String> objects = reader.jsonRootNames;
        reader.jsonRoot.getString("autonomousOption");
        robot = new FTCRobot(this, reader.jsonRoot.getString("robot"), "Autonomous");
        robot.runAutonomous(autonomousOption, alliance, reader.jsonRoot.getLong("startingDelay"), reader.jsonRoot.getInt("startingPosition"), reader.jsonRoot.getBoolean("enableBackGroundTasks"));
    }
}
