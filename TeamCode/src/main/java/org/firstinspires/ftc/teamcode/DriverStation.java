package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.FTCRobot;
import org.firstinspires.ftc.teamcode.util.StateMachine;

import java.util.List;
import java.util.StringTokenizer;

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

    public DriverStation(FTCRobot robot, LinearOpMode curOpMode){
        this.robot = robot;
        this.curOpMode = curOpMode;
        timer = new ElapsedTime(ElapsedTime.Resolution.SECONDS);
        drvrStationStates.add("TeleOp");
        drvrStationStates.add("EndGame");
        drvrStationStateMachine = new StateMachine(drvrStationStates);
        driveSysStateMachine.switchState("TeleOp");
        liftStates.add("Closed");
        liftStates.add("Down");
        liftStates.add("Mid");
        liftStates.add("Up");
        liftStates.add("Lifting");
        liftStateMachine = new StateMachine(liftStates);
        liftStateMachine.switchState("Closed");
        driveSysStates.add("Idle");
        driveSysStates.add("Driving");
        driveSysStateMachine = new StateMachine(driveSysStates);
        driveSysStateMachine.switchState("Idle");
        partAccStates.add("Off");
        partAccStates.add("On");
        partAccStateMachine = new StateMachine(partAccStates);
        partAccStateMachine.switchState("Off");
    }

    public void getNextCmd(){
        if (timer.seconds() >= 90){
            drvrStationStateMachine.switchState("EndGame");
        } else if (timer.seconds() < 90){
            drvrStationStateMachine.switchState("TeleOp");
        }

        switch (drvrStationStateMachine.getCurState()) {
            case "TeleOp":
                switch (liftStateMachine.getCurState()) {
                    case "Closed":
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
                            driveSysStateMachine.nextState();
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
                            driveSysStateMachine.prevState();
                        }
                        break;
                }
                switch (partAccStateMachine.getCurState()) {
                    case "Off":
                        if (curOpMode.gamepad1.dpad_up) {
                            robot.partAccObj.activateParticleAccelerator();
                            partAccStateMachine.nextState();
                        }
                        if (curOpMode.gamepad1.y) {
                            robot.particleObj.keepParticles();
                        }
                        break;
                    case "On":
                        if (curOpMode.gamepad1.dpad_down) {
                            robot.partAccObj.deactivateParticleAccelerator();
                            partAccStateMachine.prevState();
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
                switch (liftStateMachine.getCurState()) {
                    case "Closed":
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
                        break;
                    case "Lifting":
                        if (robot.driveSystem.getScaleMultiplier() != 0.0){
                            robot.driveSystem.scalePower(0.0);
                        }
                        if (robot.capBallLiftObj.lockLift) {
                            robot.capBallLiftObj.unlockLiftMotor();
                        }
                        robot.capBallLiftObj.applyPower(-curOpMode.gamepad2.right_stick_y);
                        if (-curOpMode.gamepad2.right_stick_y == 0.0){
                            if (!robot.capBallLiftObj.lockLift) {
                                robot.capBallLiftObj.lockLiftMotor();
                            }
                            if (robot.capBallLiftObj.isAtDownPosition()){
                                liftStateMachine.switchState("Down");
                            } else if (robot.capBallLiftObj.isAtMidPosition()){
                                liftStateMachine.switchState("Mid");
                            } else if (robot.capBallLiftObj.isAtUpPosition()){
                                liftStateMachine.switchState("Up");
                            }
                        }
                        break;
                    case "Mid":
                        if (robot.driveSystem.getScaleMultiplier() != 0.5){
                            robot.driveSystem.scalePower(0.5);
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
                        break;
                    case "Up":
                        if (robot.driveSystem.getScaleMultiplier() != 0.2){
                            robot.driveSystem.scalePower(0.2);
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
                        break;
                }
                switch (driveSysStateMachine.getCurState()) {
                    case "Idle":
                        if ((curOpMode.gamepad1.left_stick_y != 0.0) || (curOpMode.gamepad1.right_stick_x != 0.0)) {
                            robot.driveSystem.drive(Range.clip(curOpMode.gamepad1.left_stick_y, -1, 1),
                                    Range.clip(curOpMode.gamepad1.right_stick_x, -1, 1));
                            driveSysStateMachine.nextState();
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
                            driveSysStateMachine.prevState();
                        }
                        break;
                }
                switch (partAccStateMachine.getCurState()) {
                    case "Off":
                        if (curOpMode.gamepad1.dpad_up) {
                            robot.partAccObj.activateParticleAccelerator();
                            partAccStateMachine.nextState();
                        }
                        if (curOpMode.gamepad1.y) {
                            robot.particleObj.keepParticles();
                        }
                        break;
                    case "On":
                        if (curOpMode.gamepad1.dpad_down) {
                            robot.partAccObj.deactivateParticleAccelerator();
                            partAccStateMachine.prevState();
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
