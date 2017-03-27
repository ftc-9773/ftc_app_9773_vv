package org.firstinspires.ftc.teamcode.util.JsonWriters;

import android.util.JsonReader;
import android.util.JsonWriter;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import java.io.*;

/**
 * Created by Kids on 3/26/2017.
 */

public class AutonomousSelection{ //Allows the driver to select which autonomous option will be run, using the gamepad
    File file;
    String fileName = "Autonomous_options.json";
    JsonWriter writer;
    JsonReader reader;
    BufferedReader bufferedReader = null;
    BufferedWriter bufferedWriter = null;
    LinearOpMode curOpMode;
    int stat;
    int numOfSelections = 3;

    public AutonomousSelection(){
        stat = 0;
    }

    public void getSelection(){
        if (curOpMode.gamepad1.dpad_right || curOpMode.gamepad1.dpad_up){
            stat +=1;
            stat = stat%numOfSelections;
        }
        else if (curOpMode.gamepad1.dpad_down || curOpMode.gamepad1.dpad_left){
            stat -=1;
            stat = stat%numOfSelections;
        }
        else if (curOpMode.gamepad1.x){
            //write to the file
            return;
        }
    }
}
