package org.firstinspires.ftc.teamcode.navigation;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.FTCRobot;
import org.firstinspires.ftc.teamcode.drivesys.DriveSystem;

/**
 * Created by Luke on 10/15/2016.
 */

/*
 * Copyright (c) 2016 Robocracy 9773
 */

public class LineFollow{

    OpticalDistanceSensor lightSensorFront, lightSensorBack;
    double white, black, mid;
    double lowSpeed, highSpeed;
    double odsOffset;
    double basePower, Kp;
    DriveSystem driveSystem;
    long stopTimeStamp=0;
    long timoutNanoSec=0;
    FTCRobot robot;

    public LineFollow(FTCRobot robot, String lightSensorName, double lowSpeed,
                      double highSpeed, double lineFollowTimeOut,
                      double white, double black) {
        this.robot = robot;
        this.driveSystem = robot.driveSystem;
        this.lowSpeed = lowSpeed;
        this.highSpeed = highSpeed;
        this.basePower = (lowSpeed+highSpeed)/2;
        this.white = white;
        this.black = black;
        this.mid = (white + black) / 2;
        //this.Kp = (highSpeed-this.basePower) / (white - this.mid);
        this.Kp = 0.5;
        this.odsOffset = robot.distanceLeft / (robot.distanceLeft + robot.distanceRight);
        this.timoutNanoSec = (long) (lineFollowTimeOut * 1000000000L);
        if (lightSensorName.contains(",")) {
            // 2 ODS sensors
            String[] sensorNames = lightSensorName.split(",");
            this.lightSensorFront = robot.curOpMode.hardwareMap.opticalDistanceSensor.get(sensorNames[0]);
            this.lightSensorBack = robot.curOpMode.hardwareMap.opticalDistanceSensor.get(sensorNames[1]);
        } else {
            this.lightSensorBack = robot.curOpMode.hardwareMap.opticalDistanceSensor.get(lightSensorName);
        }
        DbgLog.msg("ftc9773: sensorName=%s, lowSpeed=%f, highSpeed=%f, timeoutNanoSec=%d",
                lightSensorName, lowSpeed, highSpeed, this.timoutNanoSec);
        DbgLog.msg("ftc9773: Kp = %f, odsOffset=%f", this.Kp, this.odsOffset);
//        this.white = -1;
//        this.black = -1;
    }

    public void turnUntilWhiteLine(boolean spinClockwise) {
        double leftInitialPower=0.0, rightInitialPower=0.0;
        driveSystem.setMaxSpeedCPS((int)2500);
        if(spinClockwise){
            leftInitialPower = 0.3;
            rightInitialPower = -leftInitialPower;
        }
        else{
            leftInitialPower = -0.3;
            rightInitialPower = -leftInitialPower;
        }
        while ((lightSensorBack.getLightDetected()<this.mid) && robot.curOpMode.opModeIsActive()) {
            driveSystem.turnOrSpin(leftInitialPower,rightInitialPower);
//            if (lightSensor.getLightDetected()<this.mid)
//                break;
        }
        driveSystem.stop();
        driveSystem.resumeMaxSpeed();

    }

    public void printMinMaxLightDetected() {
        double minLight = 1.0, maxLight=0.0, curLight;
        driveSystem.setMaxSpeedCPS((int)2500);
        robot.driveSystem.turnOrSpin(-0.4, 0.4);
        double initialYaw = robot.navigation.gyro.getYaw();
        double diffYaw=0.0;
        while ((diffYaw < 45.0) && robot.curOpMode.opModeIsActive()) {
            curLight = robot.navigation.lf.lightSensorBack.getLightDetected();
            if (minLight > curLight) minLight = curLight;
            if (maxLight < curLight) maxLight = curLight;
            diffYaw = Math.abs(robot.navigation.gyro.getYaw() - initialYaw);
            DbgLog.msg("ftc9773: diffYaw=%f, minLight=%f, maxLight=%f, curLight=%f", diffYaw,
                    minLight, maxLight, curLight);
        }
        driveSystem.stop();
        driveSystem.resumeMaxSpeed();
        robot.curOpMode.telemetry.addData("Light Detected:", "minLight=%f, maxLight=%f", minLight, maxLight);
        robot.curOpMode.telemetry.update();
    }

    public double getLightDetectedFront() {
        if (lightSensorFront != null) {
            return (lightSensorFront.getLightDetected());
        } else {
            return (0.0);
        }
    }

    public double getLightDetectedBack() {
        if (lightSensorBack != null) {
            return (lightSensorBack.getLightDetected());
        } else {
            return (0.0);
        }
    }

    public boolean FrontODSonWhiteLine() {
        return (lightSensorFront.getLightDetected() >= this.mid);
    }
    public boolean BackODSonWhiteLine() {
        return (lightSensorBack.getLightDetected() >= this.mid);
    }
}
