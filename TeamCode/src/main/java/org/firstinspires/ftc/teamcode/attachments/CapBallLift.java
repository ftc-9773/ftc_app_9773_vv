package org.firstinspires.ftc.teamcode.attachments;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.FTCRobot;
import org.firstinspires.ftc.teamcode.util.JsonReaders.JsonReader;
import org.json.JSONException;
import org.json.JSONObject;


/*
 * Copyright (c) 2016 Robocracy 9773
 */

public class CapBallLift implements  Attachment {
    FTCRobot robot;
    LinearOpMode curOpMode;
    DcMotor liftMotor;
    CRServo liftServoCR = null,crownServoCR=null;
    Servo liftServo = null, crownServo= null;
    boolean runToPosition = false;
    public boolean lockLift = false;
    public  boolean useEncoders = false;
    public int downPosition, midPosition, upPosition;


    public CapBallLift(FTCRobot robot, LinearOpMode curOpMode, JSONObject rootObj) {
        String key;
        JSONObject liftObj = null;
        JSONObject motorsObj = null, liftMotorObj = null, liftServoObj=null, crownServoObj=null;

        this.robot = robot;
        this.curOpMode = curOpMode;
        try {
            key = JsonReader.getRealKeyIgnoreCase(rootObj, "CapBallLift");
            liftObj = rootObj.getJSONObject(key);
            key = JsonReader.getRealKeyIgnoreCase(liftObj, "motors");
            motorsObj = liftObj.getJSONObject(key);
            key = JsonReader.getRealKeyIgnoreCase(motorsObj, "liftMotor");
            liftMotorObj = motorsObj.getJSONObject(key);
            useEncoders = liftMotorObj.getBoolean("useEncoders");
            liftMotor = curOpMode.hardwareMap.dcMotor.get("liftMotor");
            if (liftMotorObj.getBoolean("needReverse")) {
                DbgLog.msg("ftc9773: Reversing the lift servo");
                liftMotor.setDirection(DcMotorSimple.Direction.REVERSE);
            }
            liftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            double maxSpeed = liftMotorObj.getDouble("maxSpeed");
            liftMotor.setMaxSpeed((int)(liftMotor.getMaxSpeed() * maxSpeed));
            downPosition = liftMotorObj.getInt("downPosition");
            midPosition = liftMotorObj.getInt("midPosition");
            upPosition = liftMotorObj.getInt("upPosition");

            key = JsonReader.getRealKeyIgnoreCase(motorsObj, "liftServo");
            liftServoObj = motorsObj.getJSONObject(key);
            key = JsonReader.getRealKeyIgnoreCase(liftServoObj, "motorType");
            String motorType = liftServoObj.getString(key);
            if (motorType.equalsIgnoreCase("CRservo")) {
                liftServoCR = curOpMode.hardwareMap.crservo.get("liftServo");
                if (liftServoObj.getBoolean("needReverse")) {
                    DbgLog.msg("ftc9773: Reversing the lift servo");
                    liftServoCR.setDirection(CRServo.Direction.REVERSE);
                }
            } else {
                liftServo = curOpMode.hardwareMap.servo.get("liftServo");
                liftServo.scaleRange(liftServoObj.getDouble("scaleRangeMin"),
                        liftServoObj.getDouble("scaleRangeMax"));
                if (liftServoObj.getBoolean("needReverse")) {
                    DbgLog.msg("ftc9773: Reversing the lift servo");
                    liftServo.setDirection(Servo.Direction.REVERSE);
                }
                liftServo.setPosition(1);
            }
            key = JsonReader.getRealKeyIgnoreCase(motorsObj, "crownServo");
            crownServoObj = motorsObj.getJSONObject(key);
            key = JsonReader.getRealKeyIgnoreCase(crownServoObj,"motorType");
            String crownMotorType = crownServoObj.getString(key);
            if (crownMotorType.equalsIgnoreCase("CRServo")){
                crownServoCR = curOpMode.hardwareMap.crservo.get("crownServo");
                if (crownServoObj.getBoolean("needReverse")){
                    DbgLog.msg("ftc9773: Reversing the crown servo");
                    crownServoCR.setDirection(CRServo.Direction.REVERSE);
                }
            } else {
                crownServo = curOpMode.hardwareMap.servo.get("crownServo");
                crownServo.scaleRange(crownServoObj.getDouble("scaleRangeMin"), crownServoObj.getDouble("scaleRangeMax"));
                if (crownServoObj.getBoolean("needReverse")){
                    DbgLog.msg("ftc9773: Reversing the crown servo");
                    crownServo.setDirection(Servo.Direction.REVERSE);
                }
                crownServo.setPosition(1);
            }

            liftMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            liftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void autoPlacement(){
        //Unfolding
        unfoldFork();
        curOpMode.sleep(700);
        idleFork();
        //raising
        goToMidPosition();
        //lowering
        goToDownPosition();
    }

    public void applyPower(double power){
        if (!runToPosition){
            liftMotor.setPower(power);
        }
        else if (runToPosition){
            if(liftMotor.getMode() != DcMotor.RunMode.RUN_TO_POSITION) {
                liftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            }
            liftMotor.setPower(1);
        }
    }
    public void lockLiftMotor(){
        runToPosition = true;
        lockLift = true;
        liftMotor.setTargetPosition(liftMotor.getCurrentPosition());
        if(liftMotor.getMode() != DcMotor.RunMode.RUN_TO_POSITION) {
            liftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        liftMotor.setPower(1);
    }
    public void unlockLiftMotor(){
        runToPosition = false;
        lockLift = false;
        liftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }
    public void unfoldFork(){
        if (liftServoCR != null) {
            liftServoCR.setPower(-1);
        } else {
            liftServo.setPosition(1);
        }
    }
    public void foldFork(){
        if (liftServoCR != null) {
            liftServoCR.setPower(1);
        } else {
            liftServo.setPosition(0);
        }
    }
    public void idleFork(){
        liftServoCR.setPower(0);
    }

    public void activateCrown(){
        if (crownServo != null){
            crownServo.setPosition(0);
        } else if (crownServoCR != null){
            crownServoCR.setPower(1);
        }
    }
    public void deactivateCrown(){
        if (crownServo != null){
            crownServo.setPosition(1);
        } else if (crownServoCR != null){
            crownServoCR.setPower(-1);
        }
    }
    public void idleCrown(){
        if (crownServoCR != null){
            crownServoCR.setPower(0);
        }
    }

    public void goToDownPosition(){
        runToPosition = true;
        liftMotor.setTargetPosition(downPosition);
        if(liftMotor.getMode() != DcMotor.RunMode.RUN_TO_POSITION) {
            liftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        liftMotor.setPower(1);
    }
    public void goToMidPosition(){
        runToPosition = true;
        liftMotor.setTargetPosition(midPosition);
        if(liftMotor.getMode() != DcMotor.RunMode.RUN_TO_POSITION) {
            liftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        liftMotor.setPower(1);
    }
    public void gotToUpPosition(){
        runToPosition = true;
        liftMotor.setTargetPosition(upPosition);
        if(liftMotor.getMode() != DcMotor.RunMode.RUN_TO_POSITION) {
            liftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        liftMotor.setPower(1);
    }
    public boolean isAtDownPosition(){
        int curPosition = liftMotor.getCurrentPosition();
        int lowerBound = downPosition - 1680;
        int upperBound = downPosition + 1680;

        if (curPosition < lowerBound || curPosition > upperBound){
            return false;
        } else {
            return true;
        }
    }
    public boolean isAtMidPosition(){
        int curPosition = liftMotor.getCurrentPosition();
        int lowerBound = downPosition + 1681;
        int upperBound = (upPosition /2) - 1;

        if (curPosition < lowerBound || curPosition > upperBound){
            return false;
        } else {
            return true;
        }
    }
    public boolean isAtUpPosition(){
        int curPosition = liftMotor.getCurrentPosition();
        int lowerBound = upPosition / 2;
        int upperBound = upPosition + 1680;

        if (curPosition < lowerBound || curPosition > upperBound){
            return false;
        } else {
            return true;
        }
    }



}
