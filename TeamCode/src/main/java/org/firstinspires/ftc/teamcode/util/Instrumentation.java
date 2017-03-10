package org.firstinspires.ftc.teamcode.util;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.FTCRobot;
import org.firstinspires.ftc.teamcode.attachments.BeaconClaim;
import org.firstinspires.ftc.teamcode.drivesys.DriveSystem;
import org.firstinspires.ftc.teamcode.navigation.GyroInterface;
import org.firstinspires.ftc.teamcode.navigation.LineFollow;
import org.firstinspires.ftc.teamcode.navigation.Navigation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ftcrobocracy on 1/22/17.
 */

public class Instrumentation {
    LinearOpMode curOpMode;
    FTCRobot robot;
    public enum InstrumentationID {LOOP_RUNTIME, RANGESENSOR_CM, NAVX_DEGREES, NAVX_YAW_MONITOR}
    public enum LoopType {DRIVE_TO_DISTANCE, DRIVE_UNTIL_WHITELINE, DRIVE_TILL_BEACON, TURN_ROBOT}
    private List<InstrBaseClass> instrObjects = new ArrayList<InstrBaseClass>();
    public String loopRuntimeLog, rangeSensorLog, gyroLog, odsLog, colorLog;

    public class InstrBaseClass {
        public InstrumentationID instrID;
        public int iterationCount;
        public String description;
        public void reset() {return;}
        public void addInstrData() {return;}
        public void writeToFile() {return;}
        public void printToConsole() {return;}
        public void closeLog() {return;}
        public void setDescription(String description) {
            this.description = description;
            return;
        }
    }

    public class LoopRuntime extends InstrBaseClass {
        ElapsedTime timer;
        double minTime, maxTime, avgTime, totalTime;
        LoopType loopType;
        String logFile;
        FileRW fileObj;

        public LoopRuntime(LoopType loopType) {
            instrID = InstrumentationID.LOOP_RUNTIME;
            this.loopType = loopType;
            timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
            timer.reset();
            minTime = Integer.MAX_VALUE;
            maxTime = avgTime = totalTime = 0.0;
            iterationCount = 0;
            if (loopRuntimeLog != null) {
                // Create a FileRW object
                logFile = FileRW.getTimeStampedFileName(loopRuntimeLog);
                logFile = logFile + ".csv";
                this.fileObj = new FileRW(logFile, true);
                if (this.fileObj == null) {
                    DbgLog.error("Error! Could not create the file %s", logFile);
                }
            }
        }

        @Override
        public void reset() {
            timer.reset();
            minTime = Integer.MAX_VALUE;
            maxTime = avgTime = totalTime = 0.0;
            iterationCount = 0;
        }

        @Override
        public void addInstrData() {
            double millis = timer.milliseconds();
            timer.reset();
            if (minTime > millis) {
                minTime = millis;
            }
            if (maxTime < millis) {
                maxTime = millis;
            }
            totalTime += millis;
            iterationCount++;
        }

        @Override
        public void writeToFile() {
            avgTime = totalTime / iterationCount;
            String strToWrite = String.format("Timestamp:, %s, totalTime=, %f, minTime=%, f, " +
                    "avgTime=, %f, maxTime=, %f, count=, %d",
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                    totalTime, minTime, avgTime, maxTime, iterationCount);
            fileObj.fileWrite(strToWrite);
        }

        @Override
        public void printToConsole() {
            avgTime = totalTime / iterationCount;
            DbgLog.msg("ftc9773: totalTime=%f, minTime=%f, avgTime=%f, maxTime=%f, count=%d",
                    totalTime, minTime, avgTime, maxTime, iterationCount);
        }

        @Override
        public void closeLog() {
            if (this.fileObj != null) {
                DbgLog.msg("ftc9773: Instrumentation: Closing loopRuntime fileobj");
                this.fileObj.close();
            }
        }
    }

    public class ColorSensorInstr extends InstrBaseClass {
        BeaconClaim beaconClaimObj;
        ElapsedTime timer;
        DriveSystem.ElapsedEncoderCounts elapsedCounts;
        DriveSystem.DriveSysPosition driveSysPosition, redMaxPostion, blueMaxPosition;
        boolean redBeaconFound, blueBeaconFound;
        int updateCnt, redMaxUpdateCnt, blueMaxUpdateCnt;
        int prevRed, prevBlue;
        int maxRed, maxBlue;
        boolean printEveryUpdate;
        String logFile;
        FileRW fileObj;

        public ColorSensorInstr(BeaconClaim beaconClaimObj, boolean printEveryUpdate) {
            this.beaconClaimObj = beaconClaimObj;
            this.printEveryUpdate = printEveryUpdate;

            elapsedCounts = robot.driveSystem.getNewElapsedCountsObj();
            redMaxPostion = robot.driveSystem.getNewDrivesysPositionObj();
            blueMaxPosition = robot.driveSystem.getNewDrivesysPositionObj();
            redBeaconFound = blueBeaconFound = false;
            timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
            timer.reset();
            iterationCount = updateCnt = redMaxUpdateCnt = blueMaxUpdateCnt = 0;
            prevRed = prevBlue = maxRed = maxBlue = 0;
            if (colorLog != null) {
                // Create a FileRW object
                logFile = FileRW.getTimeStampedFileName(colorLog);
                logFile = logFile + ".csv";
                this.fileObj = new FileRW(logFile, true);
                if (this.fileObj == null) {
                    DbgLog.error("Error! Could not create the file %s", logFile);
                }
            }
        }

        @Override
        public void reset() {
            timer.reset();
            elapsedCounts.reset();
            redMaxPostion.resetPosition();
            blueMaxPosition.resetPosition();
            redBeaconFound = blueBeaconFound = false;
            iterationCount = updateCnt = redMaxUpdateCnt = blueMaxUpdateCnt = 0;
            prevRed = prevBlue = maxRed = maxBlue = 0;
            if (printEveryUpdate) {
                String strToWrite = String.format("method being instrumented=, %s", description);
                fileObj.fileWrite(strToWrite);
                strToWrite = String.format("voltage, millis, iteration, updateCnt, red, prevRed, blue, prevBlue, inches, speed");
                fileObj.fileWrite(strToWrite);
            }
        }

        @Override
        public void addInstrData() {
            iterationCount++;
            int red = beaconClaimObj.getRed();
            int blue = beaconClaimObj.getBlue();

            if ((red != prevRed) || (blue != prevBlue)) {
                updateCnt++;
                double distanceTravelled = elapsedCounts.getDistanceTravelledInInches();
                double millis = timer.milliseconds();
                double speed = distanceTravelled / millis;
                if (red > maxRed) {
                    redMaxUpdateCnt = updateCnt;
                    redMaxPostion.savePostion();
                }
                if (blue > maxBlue) {
                    blueMaxUpdateCnt = updateCnt;
                    blueMaxPosition.savePostion();
                }
                if (printEveryUpdate) {
                    String strToWrite = String.format("%f, %f, %d, %d, %d, %d, %d, %d, %f, %f", robot.getVoltage(),
                            timer.milliseconds(), iterationCount, updateCnt, red, prevRed,
                            blue, prevBlue, distanceTravelled, speed);
                    fileObj.fileWrite(strToWrite);
                }
                prevRed = red;
                prevBlue = blue;
                timer.reset();
            }
        }

        @Override
        public void printToConsole() {
            DbgLog.msg("ftc9773: Starting time=%f, iter_count=%d, updateCnt=%d",
                    timer.startTime(), iterationCount, updateCnt);
        }

        @Override
        public void writeToFile() {
            fileObj.fileWrite(String.format("TimeStamp=, %s, iter_count=, %d, updateCnt=, %d",
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                    iterationCount, updateCnt));
        }

        @Override
        public void closeLog() {
            if (this.fileObj != null) {
                DbgLog.msg("ftc9773: Instrumentation: Closing colorSensor fileobj");
                this.fileObj.close();
            }
        }

        public void driveToColor(String redOrBlue, float speed) {
            if (redOrBlue.equalsIgnoreCase("red")) {
                redMaxPostion.driveToPosition(speed);
            } else if (redOrBlue.equalsIgnoreCase("blue")) {
                blueMaxPosition.driveToPosition(speed);
            }
        }
    }

    public class ODSlightDetected extends  InstrBaseClass {
        public LineFollow lfObj;
        ElapsedTime timer;
        DriveSystem.ElapsedEncoderCounts elapsedCounts;
        int updateCnt;
        double prevLightDetected;
        double totalLight, avgLight, minLight, maxLight;
        boolean printEveryUpdate;
        String logFile;
        FileRW fileObj;

        public ODSlightDetected(LineFollow lfObj, boolean printEveryUpdate) {
            this.lfObj = lfObj;
            this.printEveryUpdate = printEveryUpdate;
            elapsedCounts = robot.driveSystem.getNewElapsedCountsObj();
            timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
            timer.reset();
            iterationCount = updateCnt = 0;
            avgLight = totalLight = maxLight = 0;
            minLight = Double.MAX_VALUE;
            if (odsLog != null) {
                // Create a FileRW object
                logFile = FileRW.getTimeStampedFileName(odsLog);
                logFile = logFile + ".csv";
                this.fileObj = new FileRW(logFile, true);
                if (this.fileObj == null) {
                    DbgLog.error("Error! Could not create the file %s", logFile);
                }
            }
        }

        @Override
        public void reset() {
            timer.reset();
            elapsedCounts.reset();
            iterationCount = updateCnt = 0;
            avgLight = totalLight = maxLight = 0;
            minLight = Double.MAX_VALUE;
            if (printEveryUpdate) {
                String strToWrite = String.format("method being instrumented=, %s", description);
                fileObj.fileWrite(strToWrite);
                strToWrite = String.format("voltage, millis, iteration, updateCnt, curLight, prevLight, inches, speed");
                fileObj.fileWrite(strToWrite);
            }
        }

        @Override
        public void addInstrData() {
            iterationCount++;
            double curLightDetected = lfObj.getLightDetected();
            minLight = (curLightDetected < minLight) ? curLightDetected : minLight;
            maxLight = (curLightDetected > maxLight) ? curLightDetected : maxLight;
            if ((curLightDetected != prevLightDetected)) {
                totalLight += curLightDetected;
                updateCnt++;
                double distanceTravelled = elapsedCounts.getDistanceTravelledInInches();
                double millis = timer.milliseconds();
                double speed = distanceTravelled / millis;
                if (printEveryUpdate) {
                    String strToWrite = String.format("%f, %f, %d, %d, %f, %f, %f, %f", robot.getVoltage(),
                            timer.milliseconds(), iterationCount, updateCnt, curLightDetected,
                            prevLightDetected, distanceTravelled, speed);
                    fileObj.fileWrite(strToWrite);
                }
                prevLightDetected = curLightDetected;
                elapsedCounts.reset();
                timer.reset();
            }
        }

        @Override
        public void printToConsole() {
            avgLight = totalLight / updateCnt;
            DbgLog.msg("ftc9773: Starting time=%f, minLight=%f, maxLight=%f, avgLight=%f, iter_count=%d, " +
                    "updateCnt=%d",
                    timer.startTime(), minLight, maxLight, avgLight, iterationCount, updateCnt);
        }

        @Override
        public void writeToFile() {
            // Write to file is different than printToConsole because we write to a .csv file format
            avgLight = totalLight / iterationCount;
            fileObj.fileWrite(String.format("TimeStamp=, %s, minLight=, %f, maxLight=, %f, avgLight=, %f, " +
                    "iter_count=, %d, updateCnt=, %d",
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                    minLight, maxLight, avgLight, iterationCount, updateCnt));
        }

        @Override
        public void closeLog() {
            if (this.fileObj != null) {
                DbgLog.msg("ftc9773: Instrumentation: Closing rangeSensor fileobj");
                this.fileObj.close();
            }
        }
    }

    public class RangeSensorDistance extends InstrBaseClass {
        public ModernRoboticsI2cRangeSensor rangeSensor;
        ElapsedTime timer;
        private double minDistance, maxDistance, totalDistance, avgDistance;
        public double runningAvg;
        public double runningAvgWeight;
        private double prevDistance;
        boolean printEveryUpdate=true;
        String logFile;
        FileRW fileObj;

        public RangeSensorDistance(ModernRoboticsI2cRangeSensor rangeSensor, double runningAvgWeight, boolean printEveryUpdate) {
            instrID = InstrumentationID.RANGESENSOR_CM;
            iterationCount = 0;
            this.rangeSensor = rangeSensor;
            this.printEveryUpdate = printEveryUpdate;
            minDistance = Double.MAX_VALUE;
            maxDistance = totalDistance = avgDistance = 0.0;
            prevDistance = 0.0;
            runningAvg = 0.0;
            this.runningAvgWeight = runningAvgWeight;
            timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
            timer.reset();
            if (rangeSensorLog != null) {
                // Create a FileRW object
                logFile = FileRW.getTimeStampedFileName(rangeSensorLog);
                logFile = logFile + ".csv";
                this.fileObj = new FileRW(logFile, true);
                if (this.fileObj == null) {
                    DbgLog.error("Error! Could not create the file %s", logFile);
                }
            }
        }

        @Override
        public void reset() {
            minDistance = Double.MAX_VALUE;
            maxDistance = totalDistance = avgDistance = 0.0;
            runningAvg = 0.0;
            prevDistance = 0.0;
            iterationCount = 0;
            timer.reset();
            if (printEveryUpdate) {
                String strToWrite = String.format("method being instrumented=, %s", description);
                fileObj.fileWrite(strToWrite);
                strToWrite = String.format("voltage, millis, iteration, cm, runningAvg");
                fileObj.fileWrite(strToWrite);
            }
        }

        @Override
        public void addInstrData() {
            iterationCount++;
            double curDistance = rangeSensor.getDistance(DistanceUnit.CM);
            // If the curDistance is > 255 cm or 100 inches, ignore this measurement
            if (curDistance >= 255) { return; }
            if (runningAvg <= 0.0) { runningAvg = curDistance; }
            if (curDistance < minDistance) { minDistance = curDistance; }
            if (curDistance > maxDistance) { maxDistance = curDistance; }
            totalDistance += curDistance;
            runningAvg = curDistance * runningAvgWeight + (1 - runningAvgWeight) * runningAvg;
            if (printEveryUpdate && (curDistance != prevDistance)) {
                String strToWrite = String.format("%f, %f, %d, %f, %f", robot.getVoltage(),
                        timer.milliseconds(), iterationCount, curDistance, runningAvg);
                fileObj.fileWrite(strToWrite);
                prevDistance = curDistance;
            }
        }

        @Override
        public void printToConsole() {
            avgDistance = totalDistance / iterationCount;
            DbgLog.msg("ftc9773: Starting time=%f, minDistance=%f, maxDistance=%f, avgDistance=%f, count=%d, runningAvg=%f",
                    timer.startTime(), minDistance, maxDistance, avgDistance, iterationCount, runningAvg);
        }

        @Override
        public void writeToFile() {
            avgDistance = totalDistance / iterationCount;
            fileObj.fileWrite(String.format("TimeStamp=, %s, minDistance=, %f, maxDistance=, %f, " +
                    "avgDistance=, %f, count=, %d, runningAvg=, %f",
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                    minDistance, maxDistance, avgDistance, iterationCount, runningAvg));
        }

        @Override
        public void closeLog() {
            if (this.fileObj != null) {
                DbgLog.msg("ftc9773: Instrumentation: Closing rangeSensor fileobj");
                this.fileObj.close();
            }
        }

        public double getRunningAvg() {
            return (runningAvg);
        }

        public double getElapsedTime() {
            return (timer.milliseconds());
        }
    }

    public class GyroDegrees extends InstrBaseClass {
        GyroInterface gyro;
        double updateCount, prevUpdateCount;
        ElapsedTime timer;
        boolean printEveryUpdate=true;
        double minDegrees, maxDegrees, totalDegrees, avgDegrees;
        double prevDegrees;
        double minSpeed, maxSpeed, totalSpeed, avgSpeed; // speed is in degrees per millisecond
        double prevTimeStamp;
        public int numUpdates;
        String logFile;
        FileRW fileObj;

        public GyroDegrees(GyroInterface gyro, boolean printEveryUpdate) {
            instrID = InstrumentationID.NAVX_DEGREES;
            iterationCount = 0;
            this.gyro = gyro;
            timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
            timer.reset();
            updateCount = prevUpdateCount =0;
            this.printEveryUpdate = printEveryUpdate;
            minDegrees = minSpeed = Double.MAX_VALUE;
            maxDegrees = totalDegrees = avgDegrees = 0.0;
            maxSpeed = totalSpeed = avgSpeed = 0.0;
            prevDegrees = prevTimeStamp = -1; // getYaw() always returns 0 to 359 degrees; so this is safe initial value
            numUpdates = 0;
            if (gyroLog != null) {
                // Create a FileRW object
                logFile = FileRW.getTimeStampedFileName(gyroLog);
                logFile = logFile + ".csv";
                this.fileObj = new FileRW(logFile, true);
                if (this.fileObj == null) {
                    DbgLog.error("ftc9773: Error! Could not create the file %s", logFile);
                }
            }
        }

        @Override
        public void reset() {
            timer.reset();
            updateCount = prevUpdateCount = 0;
            iterationCount = 0;
            numUpdates = 0;
            minDegrees = minSpeed = Double.MAX_VALUE;
            maxDegrees = totalDegrees = avgDegrees = 0.0;
            maxSpeed = totalSpeed = avgSpeed = 0.0;
            prevDegrees = prevTimeStamp = -1;
            if (printEveryUpdate) {
                String strToWrite = String.format("method being instrumented=, %s", description);
                fileObj.fileWrite(strToWrite);
                if (robot.driveSystem.getDriveSysType() == DriveSystem.DriveSysType.FOUR_MOTOR_6WD) {
                    strToWrite = String.format("voltage, millis, iteration, yaw degrees, speed, pitch, updateCount, " +
                            "L1power, L2power, R1power, R2power, L1counts, L2counts, R1counts, R2counts");
                } else if (robot.driveSystem.getDriveSysType() == DriveSystem.DriveSysType.TWO_MOTOR_DRIVE) {
                    strToWrite = String.format("voltage, millis, iteration, yaw degrees, speed, pitch, updateCount," +
                            "Lpower, Rpower, Lcounts, Rcounts");
                }
                fileObj.fileWrite(strToWrite);
            }
        }

        @Override
        public void addInstrData() {
            iterationCount++;
            prevUpdateCount = updateCount;
            updateCount = gyro.getUpdateCount();
            if (printEveryUpdate && (updateCount != prevUpdateCount)) {
                double speed = 0;
                numUpdates++;
                double curDegrees = gyro.getYaw();
                if (curDegrees < minDegrees) { minDegrees = curDegrees; }
                if (curDegrees > maxDegrees) { maxDegrees = curDegrees; }

                double curTimeStamp = timer.milliseconds();
                if (prevDegrees >= 0 && prevTimeStamp >= 0) { // not the first time
                    speed = Math.abs(curDegrees - prevDegrees) / (curTimeStamp - prevTimeStamp);
                    if (speed < minSpeed) { minSpeed = speed; }
                    if (speed > maxSpeed) { maxSpeed = speed; }
                }
                prevDegrees = curDegrees;
                prevTimeStamp = curTimeStamp;
                totalDegrees += curDegrees;

                String strToWrite = String.format("%f, %f, %d, %f, %f, %f, %f, %s", robot.getVoltage(),
                        curTimeStamp, iterationCount, curDegrees, speed, gyro.getPitch(),
                        updateCount, robot.driveSystem.getDriveSysInstrData());
                fileObj.fileWrite(strToWrite);
            }
        }

        @Override
        public void printToConsole() {
            avgDegrees = totalDegrees / iterationCount;
            DbgLog.msg("ftc9773: Starting time=%f, minDegrees=%f, maxDegrees=%f, avgDegreese=%f, " +
                    "count=%d, updateCount=%f, minSpeed=%f, maxSpeed=%f",
                    timer.startTime(), minDegrees, maxDegrees, avgDegrees, iterationCount, updateCount, minSpeed, maxSpeed);
        }

        @Override
        public void writeToFile() {
            avgDegrees = totalDegrees / iterationCount;
            fileObj.fileWrite(String.format("TimeStamp=, %s, minDegrees=, %f, " +
                    "maxDegrees=, %f, avgDegreese=, %f, count=, %d, updateCount=, %f, minSpeed=, %f, maxSpeed=, %f",
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                    minDegrees, maxDegrees, avgDegrees, iterationCount, updateCount, minSpeed, maxSpeed));
        }

        @Override
        public void closeLog() {
            if (this.fileObj != null) {
                DbgLog.msg("ftc9773: Instrumentation: Closing navxDegrees fileobj");
                this.fileObj.close();
            }
        }
    }

    public class GyroYawMonitor extends InstrBaseClass {
        double yawToMonitor, tolerance;
        GyroInterface gyro;
        Navigation navigation;
        int numUpdatesToCheck, totalUpdatesChecked;
        int numWithinRange;
        double updateCount, prevUpdateCount;
        public boolean targetYawReachedAndStable;

        public GyroYawMonitor(Navigation navigation, GyroInterface gyro, double yawToMonitor,
                              double tolerance, int numUpdatesToCheck) {
            instrID = InstrumentationID.NAVX_YAW_MONITOR;
            this.navigation = navigation;
            this.gyro = gyro;
            this.yawToMonitor = yawToMonitor;
            this.tolerance = tolerance;
            this.numUpdatesToCheck = numUpdatesToCheck;
            numWithinRange = 0;
            updateCount = prevUpdateCount =0;
            totalUpdatesChecked = 0;
            targetYawReachedAndStable = false;
            DbgLog.msg("ftc9773: yawToMonitor = %f, tolerance=%f, numUpdatesToCheck=%d",
                    yawToMonitor, tolerance, numUpdatesToCheck);
        }

        @Override
        public void reset() {
            numWithinRange = 0;
            updateCount = prevUpdateCount =0;
            totalUpdatesChecked = 0;
            targetYawReachedAndStable = false;
        }

        @Override
        public void addInstrData() {
            double curYaw = gyro.getYaw();
            prevUpdateCount = updateCount;
            updateCount = gyro.getUpdateCount();
            if (updateCount != prevUpdateCount) {
                totalUpdatesChecked++;
                if (navigation.distanceBetweenAngles(curYaw, yawToMonitor) <= tolerance) {
                    numWithinRange++;
                } else {
                    numWithinRange--;
                }
                numWithinRange = Range.clip(numWithinRange, 0, numUpdatesToCheck);
                if (numWithinRange >= numUpdatesToCheck) {
                    targetYawReachedAndStable = true;
                }
            }
        }

        @Override
        public void printToConsole() {
            DbgLog.msg("ftc9773: totalupdatesChecked = %d, numWithinRange=%d, targetYawReached=%b",
                    totalUpdatesChecked, numWithinRange, targetYawReachedAndStable);
        }

        @Override
        public void writeToFile() {
            return;
        }

        @Override
        public void closeLog() {
            return;
        }
    }

    public Instrumentation(FTCRobot robot, LinearOpMode curOpMode, String loopRuntimeLog,
                           String rangeSensorLog, String gyroLog, String odsLog, String colorLog) {
        this.robot = robot;
        this.curOpMode = curOpMode;
        this.loopRuntimeLog = loopRuntimeLog;
        this.rangeSensorLog = rangeSensorLog;
        this.gyroLog = gyroLog;
        this.odsLog = odsLog;
        this.colorLog = colorLog;
    }

    public void addAction(InstrBaseClass action) {
        if (action!= null)
            this.instrObjects.add(action);
    }

    public void removeAction(InstrBaseClass action) {
        this.instrObjects.remove(action);
    }

    public void reset(String description) {
        for (InstrBaseClass a: this.instrObjects) {
            a.setDescription(description);
            a.reset();
        }
    }

    public void addInstrData() {
        for (InstrBaseClass a: this.instrObjects) {
            a.addInstrData();
        }
    }

    public void printToConsole() {
        for (InstrBaseClass a: this.instrObjects) {
            a.printToConsole();
        }
    }

    public void writeToFile() {
        for (InstrBaseClass a: this.instrObjects) {
            a.writeToFile();
        }
    }

    public void closeLog() {
        for (InstrBaseClass a: this.instrObjects) {
            a.closeLog();
        }
    }
}