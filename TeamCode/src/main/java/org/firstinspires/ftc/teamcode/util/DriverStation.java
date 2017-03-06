package org.firstinspires.ftc.teamcode.util;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.FTCRobot;

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
        liftStates.add("Unfolded_Down");
        liftStates.add("Unfolded_Lifting");
        liftStates.add("Unfolded_Up");
        liftStateMachine = new StateMachine(liftStates);
        liftStateMachine.switchState("Closed");
        driveSysStates.add("Idle");
        driveSysStates.add("Driving");
        driveSysStateMachine = new StateMachine(driveSysStates);
        driveSysStateMachine.switchState("Idle");
    }

    public void getNextCmd(){
        switch (drvrStationStateMachine.getCurState()){
            case "TeleOp":
                switch (liftStateMachine.getCurState()){
                    case "Closed":
                        if (curOpMode.gamepad2.a){
                            robot.capBallLiftObj.autoPlacement();
                            liftStateMachine.nextState();
                        }else if (curOpMode.gamepad2.y){
                            robot.capBallLiftObj.foldFork();
                        }
                        break;
                    case "Unfolded_Down":
                        if (curOpMode.gamepad2.y){
                            robot.capBallLiftObj.foldFork();
                            liftStateMachine.prevState();
                        }
                        break;
                }

                switch (driveSysStateMachine.getCurState()){
                    case "Idle":
                        if ((curOpMode.gamepad1.left_stick_y != 0.0) || (curOpMode.gamepad1.right_stick_x != 0.0)){
                            robot.driveSystem.drive(curOpMode.gamepad1.left_stick_y, curOpMode.gamepad1.right_stick_x);
                            driveSysStateMachine.nextState();
                        }
                        break;
                    case "Driving":
                        if ((curOpMode.gamepad1.left_stick_y != 0.0) || (curOpMode.gamepad1.right_stick_x != 0.0)){
                            robot.driveSystem.drive(curOpMode.gamepad1.left_stick_y, curOpMode.gamepad1.right_stick_x);
                        } else {
                            robot.driveSystem.drive(0.0f, 0.0f);
                            driveSysStateMachine.prevState();
                        }
                        break;
                }
                break;
            case "EndGame":
                switch (liftStateMachine.getCurState()){
                    case "Closed":
                        if (curOpMode.gamepad2.a){
                            robot.capBallLiftObj.autoPlacement();
                            liftStateMachine.nextState();
                        }else if (curOpMode.gamepad2.y){
                            robot.capBallLiftObj.foldFork();
                        }
                        break;
                    case "Unfolded_Down":
                        if (curOpMode.gamepad2.y){
                            robot.capBallLiftObj.foldFork();
                            liftStateMachine.prevState();
                        }

                        if (curOpMode.gamepad2.right_stick_y != 0.0){
                            robot.capBallLiftObj.applyPower(curOpMode.gamepad2.right_stick_y);
                            liftStateMachine.nextState();
                        }
                        break;
                    case "Unfolded_Lifting":
                        if (curOpMode.gamepad2.y){
                            robot.capBallLiftObj.foldFork();
                            liftStateMachine.switchState("Closed");
                        }

                        if (curOpMode.gamepad2.right_stick_y != 0.0){
                            robot.capBallLiftObj.applyPower(curOpMode.gamepad2.right_stick_y);
                            liftStateMachine.nextState();
                        }
                }
                switch (driveSysStateMachine.getCurState()){
                    case "Idle":
                        if ((curOpMode.gamepad1.left_stick_y != 0.0) || (curOpMode.gamepad1.right_stick_x != 0.0)){
                            robot.driveSystem.drive(curOpMode.gamepad1.left_stick_y, curOpMode.gamepad1.right_stick_x);
                            driveSysStateMachine.nextState();
                        }
                        break;
                    case "Driving":
                        if ((curOpMode.gamepad1.left_stick_y != 0.0) || (curOpMode.gamepad1.right_stick_x != 0.0)){
                            robot.driveSystem.drive(curOpMode.gamepad1.left_stick_y, curOpMode.gamepad1.right_stick_x);
                        } else {
                            robot.driveSystem.drive(0.0f, 0.0f);
                            driveSysStateMachine.prevState();
                        }
                        break;
                }
        }
    }
}
