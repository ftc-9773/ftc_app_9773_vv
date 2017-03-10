package org.firstinspires.ftc.teamcode.drivesys;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.navigation.NavigationChecks;

/*
 * Copyright (c) 2016 Robocracy 9773
 */

public class FourMotorSteeringDrive extends DriveSystem {
    DcMotor motorL1 = null;
    DcMotor motorL2 = null;
    DcMotor motorR1 = null;
    DcMotor motorR2 = null;
    double prevPowerL1, prevPowerL2, prevPowerR1, prevPowerR2;
    double frictionCoefficient;
    int maxSpeedCPS; // encoder counts per second
    Wheel wheel;
    int motorCPR;  // Cycles Per Revolution.  == 1120 for Neverest40, 560 for Neverest20
    boolean driveSysIsReversed = false;
    double distBetweenWheels;
    boolean L1IsZero, L2IsZero, R1IsZero, R2IsZero;
    ElapsedTime L1Timer, L2Timer, R1Timer, R2Timer;
    double scaleMultiplier = 1.0;
    int reverseMultiplier = 1;

    public class ElapsedEncoderCounts implements DriveSystem.ElapsedEncoderCounts {
        long encoderCountL1;
        long encoderCountL2;
        long encoderCountR1;
        long encoderCountR2;

        public ElapsedEncoderCounts() {
            encoderCountL1 = encoderCountL2 = encoderCountR1 = encoderCountR2 = 0;
        }

        public void reset() {
            encoderCountL1 = getNonZeroCurrentPos(motorL1);
            encoderCountL2 = getNonZeroCurrentPos(motorL2);
            encoderCountR1 = getNonZeroCurrentPos(motorR1);
            encoderCountR2 = getNonZeroCurrentPos(motorR2);
            DbgLog.msg("ftc9773: In reset(): encoder counts: L1=%d, L2=%d, R1=%d, R2=%d", encoderCountL1,
                    encoderCountL2, encoderCountR1, encoderCountR2);
        }

        public  void printCurrentEncoderCounts() {
            DbgLog.msg("ftc9773: In printCurrent...(): encoder counts: L1=%d, L2=%d, R1=%d, R2=%d",
                    motorL1.getCurrentPosition(), getNonZeroCurrentPos(motorL2),
                    getNonZeroCurrentPos(motorR1), getNonZeroCurrentPos(motorR2));
        }

        public double getDistanceTravelledInInches() {
            double avgEncoderCounts = 0.0;
            double distanceTravelled = 0.0;

            avgEncoderCounts = (Math.abs(motorL1.getCurrentPosition() - encoderCountL1) +
                    Math.abs(getNonZeroCurrentPos(motorL2) - encoderCountL2) +
                    Math.abs(getNonZeroCurrentPos(motorR1) - encoderCountR1) +
                    Math.abs(getNonZeroCurrentPos(motorR2) - encoderCountR2)) / 4;

            distanceTravelled = (avgEncoderCounts / motorCPR) * wheel.getCircumference();
            return (distanceTravelled);
        }

        public double getDegreesTurned() {
            double distanceTravelledInInches, degreesTurned;
            double leftDegreesTurned;

            distanceTravelledInInches = this.getDistanceTravelledInInches();
            degreesTurned = 360 * distanceTravelledInInches / (Math.PI * distBetweenWheels);
            leftDegreesTurned = ((getNonZeroCurrentPos(motorL1) - encoderCountL1) +
                    (getNonZeroCurrentPos(motorL2) - encoderCountL2)) / 2;
            if (leftDegreesTurned < 0) {
                degreesTurned *= -1; // Negate the number to indicate counterclockwise spin
            }
//            DbgLog.msg("ftc9773: distanceTravelledInInches: %f, degreesTurned: %f", distanceTravelledInInches, degreesTurned);

            return (degreesTurned);
        }
    }

    public FourMotorSteeringDrive(DcMotor motorL1, DcMotor motorL2, DcMotor motorR1, DcMotor motorR2,
                                  int maxSpeedCPS, double frictionCoefficient,
                                  double distanceBetweenWheels, Wheel wheel, int motorCPR) {
        this.motorL1 = motorL1;
        this.motorL2 = motorL2;
        this.motorR1 = motorR1;
        this.motorR2 = motorR2;
        this.motorR1.setDirection(DcMotorSimple.Direction.REVERSE);
        this.motorR2.setDirection(DcMotorSimple.Direction.REVERSE);
        this.motorL1.setDirection(DcMotorSimple.Direction.FORWARD);
        this.motorL2.setDirection(DcMotorSimple.Direction.FORWARD);
        this.setDriveSysMode(DcMotor.RunMode.RUN_USING_ENCODER);
        this.setZeroPowerMode(DcMotor.ZeroPowerBehavior.BRAKE);
        this.frictionCoefficient = frictionCoefficient;
        this.maxSpeedCPS = maxSpeedCPS;
        DbgLog.msg("ftc9773: max speed CPS = %d", maxSpeedCPS);
        motorL1.setMaxSpeed(maxSpeedCPS);
        motorL2.setMaxSpeed(maxSpeedCPS);
        motorR1.setMaxSpeed(maxSpeedCPS);
        motorR2.setMaxSpeed(maxSpeedCPS);
        this.wheel = wheel;
        this.motorCPR = motorCPR;
        this.prevPowerL1 = this.prevPowerL2 = this.prevPowerR1 = this.prevPowerR2 = 0.0;
        this.distBetweenWheels = distanceBetweenWheels; // 14.75 or 15.5;
        this.L1IsZero = this.L2IsZero = this.R1IsZero = this.R2IsZero = true;
        this.L1Timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        this.L2Timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        this.R1Timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        this.R2Timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        L1Timer.reset();
        L2Timer.reset();
        R1Timer.reset();
        R2Timer.reset();
    }

    @Override
    public void drive(float speed, float direction) {
        double left = ((reverseMultiplier*speed + direction) * frictionCoefficient) * scaleMultiplier;
        double right = ((reverseMultiplier*speed - direction) * frictionCoefficient) * scaleMultiplier;

        if (prevPowerL1 != left) {
            motorL1.setPower(left);
            prevPowerL1 = left;
        }
        if (prevPowerL2 != left) {
            motorL2.setPower(left);
            prevPowerL2 = left;
        }
        if (prevPowerR1 != right) {
            motorR1.setPower(right);
            prevPowerR1 = right;
        }

        if (prevPowerR2 != right) {
            motorR2.setPower(right);
            prevPowerR2 = right;
        }
    }

    @Override
    public void turnOrSpin(double left, double right) {
        if (prevPowerL1 != left) {
            motorL1.setPower(left);
            prevPowerL1 = left;
        }
        if (prevPowerL2 != left) {
            motorL2.setPower(left);
            prevPowerL2 = left;
        }
        if (prevPowerR1 != right) {
            motorR1.setPower(right);
            prevPowerR1 = right;
        }

        if (prevPowerR2 != right) {
            motorR2.setPower(right);
            prevPowerR2 = right;
        }
    }

    @Override
    public void stop() {
        motorL1.setPower(0.0);
        motorL2.setPower(0.0);
        motorR1.setPower(0.0);
        motorR2.setPower(0.0);
        prevPowerL1 = prevPowerL2 = prevPowerR1 = prevPowerR2 = 0.0;
    }

    @Override
    public void setZeroPowerMode(DcMotor.ZeroPowerBehavior zp_behavior) {
        this.motorL1.setZeroPowerBehavior(zp_behavior);
        this.motorL2.setZeroPowerBehavior(zp_behavior);
        this.motorR1.setZeroPowerBehavior(zp_behavior);
        this.motorR2.setZeroPowerBehavior(zp_behavior);
    }

    @Override
    public DcMotor.ZeroPowerBehavior getZeroPowerBehavior() {
        return (motorL1.getZeroPowerBehavior());
    }

    @Override
    public void driveToDistance(float speed, double distanceInInches) {

        double countsPerInch = motorCPR / wheel.getCircumference();
        double targetCounts = countsPerInch * distanceInInches;

        motorL1.setTargetPosition(motorL1.getCurrentPosition() + (int) targetCounts);
        motorL2.setTargetPosition(getNonZeroCurrentPos(motorL2) + (int) targetCounts);
        motorR1.setTargetPosition(getNonZeroCurrentPos(motorR1) + (int) targetCounts);
        motorR2.setTargetPosition(getNonZeroCurrentPos(motorR2) + (int) targetCounts);

        setDriveSysMode(DcMotor.RunMode.RUN_TO_POSITION);

        this.drive((float) (speed * frictionCoefficient), 0.0f);

        while (motorL1.isBusy() && motorL2.isBusy() && motorR1.isBusy() && motorR2.isBusy() && curOpMode.opModeIsActive()) {
            curOpMode.idle();
        }

        this.stop();

        setDriveSysMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    @Override
    public void turnDegrees(double degrees, float speed, NavigationChecks navExc) {

        double distInInches = (Math.abs(degrees) / 360) * Math.PI * this.distBetweenWheels;
        double countsPerInch = motorCPR / wheel.getCircumference();
        double targetCounts = countsPerInch * distInInches;
        double L1targetCounts, L2targetCounts, R1targetCounts, R2targetCounts;

        if (degrees < 0) {
            // Spin counterclockwise => left motors backward, right motors forward
            motorL1.setTargetPosition(getNonZeroCurrentPos(motorL1) - (int) targetCounts);
            motorL2.setTargetPosition(getNonZeroCurrentPos(motorL2) - (int) targetCounts);
            motorR1.setTargetPosition(getNonZeroCurrentPos(motorR1) + (int) targetCounts);
            motorR2.setTargetPosition(getNonZeroCurrentPos(motorR2) + (int) targetCounts);
        } else {
            // Spin clockwise => left motors forward, right motors backward
            motorL1.setTargetPosition(getNonZeroCurrentPos(motorL1) + (int) targetCounts);
            motorL2.setTargetPosition(getNonZeroCurrentPos(motorL2) + (int) targetCounts);
            motorR1.setTargetPosition(getNonZeroCurrentPos(motorR1) - (int) targetCounts);
            motorR2.setTargetPosition(getNonZeroCurrentPos(motorR2) - (int) targetCounts);
        }
        DbgLog.msg("ftc9773: motorL1 current position = %d", getNonZeroCurrentPos(motorL1));

        setDriveSysMode(DcMotor.RunMode.RUN_TO_POSITION);

        this.drive((float) (speed * frictionCoefficient), 0.0f);

        while (motorL1.isBusy() && motorL2.isBusy() && motorR1.isBusy() && motorR2.isBusy()
                && curOpMode.opModeIsActive()) {
//                && !navExc.stopNavigation() && curOpMode.opModeIsActive()) {
            curOpMode.idle();
        }

        this.stop();

        DbgLog.msg("ftc9773: motorL1 current position = %d", getNonZeroCurrentPos(motorL1));
        setDriveSysMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void setDriveSysMode(DcMotor.RunMode runMode) {
        motorL1.setMode(runMode);
        motorL2.setMode(runMode);
        motorR1.setMode(runMode);
        motorR2.setMode(runMode);
    }

    // Note: setMaxSpeed should be set once during the init time.
    //  Calling setMaxSpeed and resumeMaxSpeed() will not work well with the
    //  new optimization in the drive() method, where the prevPower values are saved and
    //  the new power values do not get applied if they are same as the previous power values.
    @Override
    public void setMaxSpeed(float speed) {
        DbgLog.msg("ftc9773: Current max speed =%d", maxSpeedCPS);
        motorL1.setMaxSpeed((int) (maxSpeedCPS * speed));
        motorL2.setMaxSpeed((int) (maxSpeedCPS * speed));
        motorR1.setMaxSpeed((int) (maxSpeedCPS * speed));
        motorR2.setMaxSpeed((int) (maxSpeedCPS * speed));
    }

    @Override
    public void resumeMaxSpeed() {
        motorL1.setMaxSpeed((int) maxSpeedCPS);
        motorL2.setMaxSpeed((int) maxSpeedCPS);
        motorR1.setMaxSpeed((int) maxSpeedCPS);
        motorR2.setMaxSpeed((int) maxSpeedCPS);
    }

    @Override
    public void reverse() {
        if (driveSysIsReversed) {
            motorL1.setDirection(DcMotorSimple.Direction.REVERSE);
            motorL2.setDirection(DcMotorSimple.Direction.REVERSE);
            motorR1.setDirection(DcMotorSimple.Direction.FORWARD);
            motorR2.setDirection(DcMotorSimple.Direction.FORWARD);
            driveSysIsReversed = false;
        } else {
            motorL1.setDirection(DcMotorSimple.Direction.FORWARD);
            motorL2.setDirection(DcMotorSimple.Direction.FORWARD);
            motorR1.setDirection(DcMotorSimple.Direction.REVERSE);
            motorR2.setDirection(DcMotorSimple.Direction.REVERSE);
            driveSysIsReversed = true;
        }
    }

    public ElapsedEncoderCounts getNewElapsedCountsObj() {
        ElapsedEncoderCounts encoderCountsObj = new ElapsedEncoderCounts();
        return (encoderCountsObj);
    }

    @Override
    public void printCurrentPosition() {
        DbgLog.msg("ftc9773: L1:%d, L2:%d, R1:%d, R2:%d", getNonZeroCurrentPos(motorL1),
                getNonZeroCurrentPos(motorL2), getNonZeroCurrentPos(motorR1), getNonZeroCurrentPos(motorR2));
    }

    @Override
    public void initForPlay() {
        L1Timer.reset();
        L2Timer.reset();
        R1Timer.reset();
        R2Timer.reset();
    }

    @Override
    public String getDriveSysInstrData() {
        String instrData = String.format("%f,%f,%f,%f,%d,%d,%d,%d", prevPowerL1, prevPowerL2,
                prevPowerR1, prevPowerR2, motorL1.getCurrentPosition(), motorL2.getCurrentPosition(),
                motorR1.getCurrentPosition(), motorR2.getCurrentPosition());

        return instrData;
    }

    public int getNonZeroCurrentPos(DcMotor motor){
        int curPos = motor.getCurrentPosition();
        boolean skipWhileLoop = false;
//        DbgLog.msg("ftc9773: Motor = %s, curPos = %d, isZeroPos = %b", motor.toString(), curPos, motor==motorL1 ? L1IsZero : motor==motorR1 ? R1IsZero : motor==motorL2 ? L2IsZero : R2IsZero);
        if(motor==motorL1 && L1IsZero) {
//            DbgLog.msg("ftc9773: Motor = L1, curPos = %d, isZeroPos = %b", curPos, L1IsZero);
            if(L1Timer.milliseconds()>100) L1IsZero = false;
            skipWhileLoop = true;}
        if(motor==motorL2 && L2IsZero) {
//            DbgLog.msg("ftc9773: Motor = L2, curPos = %d, isZeroPos = %b", curPos, L2IsZero);
            if(L2Timer.milliseconds()>100) L2IsZero = false;
            skipWhileLoop = true;
        }
        if(motor==motorR1 && R1IsZero) {
//            DbgLog.msg("ftc9773: Motor = R1, curPos = %d, isZeroPos = %b",  curPos, R1IsZero);
            if(R1Timer.milliseconds()>100) R1IsZero = false;
            skipWhileLoop = true;
        }
        if(motor==motorR2 && R2IsZero) {
//            DbgLog.msg("ftc9773: Motor = R2, curPos = %d, isZeroPos = %b", curPos, R2IsZero);
            if(R2Timer.milliseconds()>100) R2IsZero = false;
            skipWhileLoop = true;
        }
        if (skipWhileLoop) return curPos;

       while(curPos==0){
           curOpMode.sleep(10);
           curPos = motor.getCurrentPosition();
        }
//        DbgLog.msg("ftc9773: Motor = %s, curPos = %d, isZeroPos = %b", motor.toString(), curPos, motor==motorL1 ? L1IsZero : motor==motorR1 ? R1IsZero : motor==motorL2 ? L2IsZero : R2IsZero);
        return curPos;
    }

    /*public void driveToDistance(float speed, float direction, double distance){
        double startingPositionL = getNonZeroCurrentPos(motorL1);
        double startingPositionR = getNonZeroCurrentPos(motorR1);

        double targetPosition =(distance / wheelValues[1]) * 1120;

        while(((getNonZeroCurrentPos(motorL1)-startingPositionL)<targetPosition) && ((motorR.getCurrentPosition()-startingPositionR)<targetPosition)){
            drive(speed, direction);
        }
        motorR.setPower(0);
        motorL.setPower(0);
    }*/

    @Override
    public void testEncoders() {
//        this.setDriveSysMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
//        ElapsedTime timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
//        timer.reset();
//        DbgLog.msg("ftc9773: L1 Encoder: %d L2 Encoder: %d R1 Encoder: %d R2 Encoder: %d", getNonZeroCurrentPos(motorL1), getNonZeroCurrentPos(motorL2), getNonZeroCurrentPos(motorR1), getNonZeroCurrentPos(motorR2));
//        while (curOpMode.opModeIsActive() && timer.milliseconds()<4000){
//            this.drive(1.0F,0);
//        }
//        this.stop();
//        DbgLog.msg("ftc9773: L1 Encoder: %d L2 Encoder: %d R1 Encoder: %d R2 Encoder: %d", getNonZeroCurrentPos(motorL1), getNonZeroCurrentPos(motorL2), getNonZeroCurrentPos(motorR1), getNonZeroCurrentPos(motorR2));
        this.motorL1.setMaxSpeed(2500);
        this.motorL2.setMaxSpeed(2500);
        this.motorR1.setMaxSpeed(2500);
        this.motorR2.setMaxSpeed(2500);
    }

    public boolean motorControllerIsConnected() {
        boolean connected = false;

        return (connected);
    }

    @Override
    public void scalePower(double scaleMultiplier){
        this.scaleMultiplier = scaleMultiplier;
    }

    @Override
    public double getScaleMultiplier(){ return scaleMultiplier;}

    @Override
    public void reverseTeleop(){
        reverseMultiplier *= -1;
    }
}
