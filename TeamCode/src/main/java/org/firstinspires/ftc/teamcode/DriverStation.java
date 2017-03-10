package org.firstinspires.ftc.teamcode;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.util.StateMachine;

import java.util.ArrayList;
import java.util.List;
import org.firstinspires.ftc.teamcode.FTCRobot;
import org.firstinspires.ftc.teamcode.util.StateMachine;

/**
 * Created by pranavburugula on 3/5/2017.
 */

public class DriverStation {
    FTCRobot robot;
    LinearOpMode curOpMode;
    StateMachine drvrStationStateMachine;
    List<String> drvrStationStates;
    StateMachine liftStateMachine;
    List<String> liftStates;
    StateMachine driveSysStateMachine;
    List<String> driveSysStates;
    StateMachine partAccStateMachine;
    List<String>partAccStates;
    ElapsedTime timer;
    double previousLiftGamepadPower = 0.0;

    public DriverStation(FTCRobot robot, LinearOpMode curOpMode){
        this.robot = robot;
        this.curOpMode = curOpMode;
        timer = new ElapsedTime(ElapsedTime.Resolution.SECONDS);
        drvrStationStates = new ArrayList<>();
        drvrStationStates.add("TeleOp");
        drvrStationStates.add("EndGame");
        drvrStationStateMachine = new StateMachine(drvrStationStates);
        drvrStationStateMachine.switchState("TeleOp");
        liftStates = new ArrayList<>();
        liftStates.add("Closed");
        liftStates.add("Down");
        liftStates.add("Mid");
        liftStates.add("Up");
        liftStates.add("Lifting");
        liftStateMachine = new StateMachine(liftStates);
        liftStateMachine.switchState("Closed");
        driveSysStates = new ArrayList<>();
        driveSysStates.add("Idle");
        driveSysStates.add("Driving");
        driveSysStateMachine = new StateMachine(driveSysStates);
        driveSysStateMachine.switchState("Idle");
        partAccStates = new ArrayList<>();
        partAccStates.add("Off");
        partAccStates.add("On");
        partAccStateMachine = new StateMachine(partAccStates);
        partAccStateMachine.switchState("Off");
        DbgLog.msg("ftc9773: DriverStation initialized");
    }

    public void getNextCmd(){
//        DbgLog.msg("ftc9773: getNextCmd: current states: driverstation = %s, lift = %s," +
//                "particle Acc = %s, drivesys= %s",
//                drvrStationStateMachine.getCurState(), liftStateMachine.getCurState(),
//                partAccStateMachine.getCurState(), driveSysStateMachine.getCurState());
        if (timer.seconds() >= 10){
            drvrStationStateMachine.switchState("EndGame");
        } else if (timer.seconds() < 10){
            drvrStationStateMachine.switchState("TeleOp");
        }

        switch (drvrStationStateMachine.getCurState()) {
            case "TeleOp":
                switch (liftStateMachine.getCurState()) {
                    case "Closed":
                        if (!robot.capBallLiftObj.lockLift) {
                            robot.capBallLiftObj.lockLiftMotor();
                        }
                        if (curOpMode.gamepad2.a) {
                            if (robot.capBallLiftObj.lockLift) {
                                robot.capBallLiftObj.unlockLiftMotor();
                            }
                            robot.capBallLiftObj.autoPlacement();
                            liftStateMachine.switchState("Down");
                        } else if (curOpMode.gamepad2.y) {
                            robot.capBallLiftObj.foldFork();
                        } else{
                            robot.capBallLiftObj.idleFork();
                        }
                        break;
                    case "Down":
                        if (curOpMode.gamepad2.y) {
                            robot.capBallLiftObj.foldFork();
                            liftStateMachine.switchState("Closed");
                        } else{
                            robot.capBallLiftObj.idleFork();
                        }
                        if (-curOpMode.gamepad2.right_stick_y < 0.0){
                            if (robot.capBallLiftObj.lockLift){
                                robot.capBallLiftObj.unlockLiftMotor();
                            }
                            robot.capBallLiftObj.applyPower(-curOpMode.gamepad2.right_stick_y);
                        } else {
                            if (!robot.capBallLiftObj.lockLift){
                                robot.capBallLiftObj.lockLiftMotor();
                            }
                        }
                        break;
                }

                switch (driveSysStateMachine.getCurState()) {
                    case "Idle":
                        if ((curOpMode.gamepad1.left_stick_y != 0.0) || (curOpMode.gamepad1.right_stick_x != 0.0)) {
                            robot.driveSystem.drive(Range.clip(curOpMode.gamepad1.left_stick_y, -1, 1),
                                    Range.clip(curOpMode.gamepad1.right_stick_x, -1, 1));
                            driveSysStateMachine.switchState("Driving");
                        } else {
                            robot.driveSystem.drive(0.0f, 0.0f);
                        }
                        break;
                    case "Driving":
                        if ((curOpMode.gamepad1.left_stick_y != 0.0) || (curOpMode.gamepad1.right_stick_x != 0.0)) {
                            robot.driveSystem.drive(Range.clip(curOpMode.gamepad1.left_stick_y, -1, 1),
                                    Range.clip(curOpMode.gamepad1.right_stick_x, -1, 1));
                        } else {
                            robot.driveSystem.drive(0.0f, 0.0f);
                            driveSysStateMachine.switchState("Idle");
                        }
                        break;
                }
                switch (partAccStateMachine.getCurState()) {
                    case "Off":
                        if (curOpMode.gamepad1.dpad_up) {
                            robot.partAccObj.activateParticleAccelerator();
                            partAccStateMachine.switchState("On");
                        }
                        robot.particleObj.keepParticles();
                        break;
                    case "On":
                        if (curOpMode.gamepad1.dpad_down) {
                            robot.partAccObj.deactivateParticleAccelerator();
                            partAccStateMachine.switchState("Off");
                        }
                        if (curOpMode.gamepad1.a) {
                            robot.particleObj.releaseParticles();
                        } else if (curOpMode.gamepad1.y) {
                            robot.particleObj.keepParticles();
                        }
                        break;
                }

                if (curOpMode.gamepad2.x) {
                    robot.beaconClaimObj.pushBeacon();
                } else if (curOpMode.gamepad2.b) {
                    robot.beaconClaimObj.retractBeacon();
                } else {
                    robot.beaconClaimObj.idleBeacon();
                }

                if (curOpMode.gamepad2.dpad_down){
                    robot.harvesterObj.intake();
                } else if (curOpMode.gamepad2.dpad_up){
                    robot.harvesterObj.output();
                } else {
                    robot.harvesterObj.idle();
                }

                if (curOpMode.gamepad1.x){
                    robot.driveSystem.reverseTeleop();
                }
                break;
            case "EndGame":
                //DbgLog.msg("ftc9773:  lift motor current position = %f", robot.capBallLiftObj.getCurrentPosition());
                switch (liftStateMachine.getCurState()) {
                    case "Closed":
                        if (!robot.capBallLiftObj.lockLift) {
                            robot.capBallLiftObj.lockLiftMotor();
                        }
                        if (robot.driveSystem.getScaleMultiplier() != 1.0){
                            robot.driveSystem.scalePower(1.0);
                        }
                        if (curOpMode.gamepad2.a) {
                            robot.capBallLiftObj.autoPlacement();
                            liftStateMachine.switchState("Down");
                        } else if (curOpMode.gamepad2.y) {
                            robot.capBallLiftObj.foldFork();
                        } else{
                            robot.capBallLiftObj.idleFork();
                        }
                        break;
                    case "Down":
                        if (robot.driveSystem.getScaleMultiplier() != 1.0){
                            robot.driveSystem.scalePower(1.0);
                        }
                        if (curOpMode.gamepad2.y) {
                            robot.capBallLiftObj.foldFork();
                            liftStateMachine.switchState("Closed");
                        } else{
                            robot.capBallLiftObj.idleFork();
                        }
                        if (!robot.capBallLiftObj.lockLift) {
                            robot.capBallLiftObj.lockLiftMotor();
                        }
                        if (-curOpMode.gamepad2.right_stick_y != 0.0) {
                            if (robot.capBallLiftObj.lockLift) {
                                robot.capBallLiftObj.unlockLiftMotor();
                            }
                            robot.capBallLiftObj.applyPower(-curOpMode.gamepad2.right_stick_y);
                            liftStateMachine.switchState("Lifting");
                        }
                        if (robot.capBallLiftObj.useEncoders && curOpMode.gamepad2.right_bumper){
                            robot.capBallLiftObj.goToMidPosition();
                            liftStateMachine.switchState("Mid");
                        }
                        break;
                    case "Lifting":
                        if (robot.driveSystem.getScaleMultiplier() != 0.0){
                            robot.driveSystem.scalePower(0.0);
                        }
                        if (robot.capBallLiftObj.lockLift) {
                            robot.capBallLiftObj.unlockLiftMotor();
                        }
                        if (curOpMode.gamepad2.right_stick_y == 0.0){
                            if (!robot.capBallLiftObj.lockLift) {
                                robot.capBallLiftObj.lockLiftMotor();
                            }
                            if (robot.capBallLiftObj.isAtDownRange()){
                                liftStateMachine.switchState("Down");
                            } else if (robot.capBallLiftObj.isAtMidRange()){
                                liftStateMachine.switchState("Mid");
                            } else if (robot.capBallLiftObj.isAtUpRange()){
                                liftStateMachine.switchState("Up");
                            }
                        }
                        break;
                    case "Mid":
                        if (robot.driveSystem.getScaleMultiplier() != 0.25){
                            robot.driveSystem.scalePower(0.25);
                        }
                        if (!robot.capBallLiftObj.lockLift) {
                            robot.capBallLiftObj.lockLiftMotor();
                        }
                        if (-curOpMode.gamepad2.right_stick_y != 0.0) {
                            if (robot.capBallLiftObj.lockLift) {
                                robot.capBallLiftObj.unlockLiftMotor();
                            }
                            robot.capBallLiftObj.applyPower(-curOpMode.gamepad2.right_stick_y);
                            liftStateMachine.switchState("Lifting");
                        }
                        if (robot.capBallLiftObj.useEncoders && curOpMode.gamepad2.left_bumper){
                        robot.capBallLiftObj.goToDownPosition();
                        liftStateMachine.switchState("Down");
                    }
                        if (robot.capBallLiftObj.useEncoders && curOpMode.gamepad2.right_bumper){
                            robot.capBallLiftObj.gotToUpPosition();
                            liftStateMachine.switchState("Up");
                        }
                        break;
                    case "Up":
                        if (robot.driveSystem.getScaleMultiplier() != 0.1){
                            robot.driveSystem.scalePower(0.1);
                        }
                        if (!robot.capBallLiftObj.lockLift) {
                            robot.capBallLiftObj.lockLiftMotor();
                        }
                        if (-curOpMode.gamepad2.right_stick_y != 0.0) {
                            if (robot.capBallLiftObj.lockLift) {
                                robot.capBallLiftObj.unlockLiftMotor();
                            }
                            robot.capBallLiftObj.applyPower(-curOpMode.gamepad2.right_stick_y);
                            liftStateMachine.switchState("Lifting");
                        }
                        if (robot.capBallLiftObj.useEncoders && curOpMode.gamepad2.left_bumper){
                            robot.capBallLiftObj.goToMidPosition();
                            liftStateMachine.switchState("Mid");
                        }
                        break;
                }
                switch (driveSysStateMachine.getCurState()) {
                    case "Idle":
                        if ((curOpMode.gamepad1.left_stick_y != 0.0) || (curOpMode.gamepad1.right_stick_x != 0.0)) {
                            robot.driveSystem.drive(Range.clip(curOpMode.gamepad1.left_stick_y, -1, 1),
                                    Range.clip(curOpMode.gamepad1.right_stick_x, -1, 1));
                            driveSysStateMachine.switchState("Driving");
                        } else {
                            robot.driveSystem.drive(0.0f, 0.0f);
                        }
                        break;
                    case "Driving":
                        if ((curOpMode.gamepad1.left_stick_y != 0.0) || (curOpMode.gamepad1.right_stick_x != 0.0)) {
                            robot.driveSystem.drive(Range.clip(curOpMode.gamepad1.left_stick_y, -1, 1),
                                    Range.clip(curOpMode.gamepad1.right_stick_x, -1, 1));
                        } else {
                            robot.driveSystem.drive(0.0f, 0.0f);
                            driveSysStateMachine.switchState("Idle");
                        }
                        break;
                }
                switch (partAccStateMachine.getCurState()) {
                    case "Off":
                        if (curOpMode.gamepad1.dpad_up) {
                            robot.partAccObj.activateParticleAccelerator();
                            partAccStateMachine.switchState("On");
                        }
                        robot.particleObj.keepParticles();
                        break;
                    case "On":
                        if (curOpMode.gamepad1.dpad_down) {
                            robot.partAccObj.deactivateParticleAccelerator();
                            partAccStateMachine.switchState("Off");
                        }
                        if (curOpMode.gamepad1.a) {
                            robot.particleObj.releaseParticles();
                        } else if (curOpMode.gamepad1.y) {
                            robot.particleObj.keepParticles();
                        }
                        break;
                }
                if (curOpMode.gamepad2.x) {
                    robot.beaconClaimObj.pushBeacon();
                } else if (curOpMode.gamepad2.b) {
                    robot.beaconClaimObj.retractBeacon();
                } else {
                    robot.beaconClaimObj.idleBeacon();
                }

                if (curOpMode.gamepad2.dpad_down){
                    robot.harvesterObj.intake();
                } else if (curOpMode.gamepad2.dpad_up){
                    robot.harvesterObj.output();
                } else {
                    robot.harvesterObj.idle();
                }

                if (curOpMode.gamepad1.x){
                    robot.driveSystem.reverseTeleop();
                }
                break;
        }
    }
}
