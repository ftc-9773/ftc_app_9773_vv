package org.firstinspires.ftc.teamcode.navigation;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.FTCRobot;
import org.firstinspires.ftc.teamcode.drivesys.DriveSystem;

import java.util.ArrayList;
import java.util.List;

public class NavigationChecks {
    public enum NavChecksSupported {CHECK_OPMODE_INACTIVE, CHECK_ROBOT_TILTING, CHECK_TIMEOUT,
        CHECK_WHITElINE, CHECK_TARGET_YAW_GYRO, CHECK_DISTANCE_TRAVELLED,
        CROSSCHECK_GYRO_WITH_ENCODERS, CHECK_GYRO_IS_WORKING
    }
    LinearOpMode curOpMode;
    FTCRobot robot;
    Navigation navigationObj;
    public List<NavCheckBaseClass> criteriaToCheck = new ArrayList<NavCheckBaseClass>();
    public NavCheckBaseClass stopNavCriterion;

    public class NavCheckBaseClass {
        public NavChecksSupported navcheck;
        public boolean stopNavigation() { return (false);}
        public void reset() { return; }
    }

    public class TimeoutCheck extends NavCheckBaseClass {
        private ElapsedTime timer;
        private long timeoutMillis;

        public TimeoutCheck(long timeoutMillis) {
            this.timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
            this.timer.reset();
            this.timeoutMillis = timeoutMillis;
            navcheck = NavChecksSupported.CHECK_TIMEOUT;
        }

        @Override
        public void reset() {
            this.timer.reset();
        }

        @Override
        public boolean stopNavigation() {
            if (timer.milliseconds() >= timeoutMillis) {
                return (true);
            } else {
                return (false);
            }
        }
    }

    public class CheckGyroIsWorking extends NavCheckBaseClass {
        ElapsedTime timer;
        GyroInterface gyro;
        double prevYaw;
        boolean firstCheck;
        public CheckGyroIsWorking() {
            gyro = navigationObj.gyro;
            timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
            prevYaw = gyro.getYaw();
            firstCheck = true;
            navcheck = NavChecksSupported.CHECK_GYRO_IS_WORKING;
        }

        @Override
        public void reset() {
            timer.reset();
            firstCheck = true;
            prevYaw = gyro.getYaw();
        }

        @Override
        public boolean stopNavigation() {
            double curYaw = gyro.getYaw();
            // If there is no update in 200 milli second, declare navx failure
            // For the first time check though, we may see > 200 msec difference due to the
            // time gap between instantiating this object and actually using it.
            if ((curYaw == prevYaw) && (timer.milliseconds() > 200) && !firstCheck) {
                DbgLog.msg("ftc9773:  CheckGyroIsWorking:  navx got disconnected!");
                return (true);
            } else {
                // navx is working fine; just update the timer and prevYaw so the next
                // check will be done correctly.
                timer.reset();
                prevYaw = curYaw;
                firstCheck = false; // it is not a first time check anymore
                return (false);
            }
        }
    }

    public class CheckGyroTargetYawReached extends NavCheckBaseClass {
        double targetYaw;
        double angleTolerance;
        GyroInterface gyro;
        double prev_speed, prev_yaw;
        double estimatedYaw;
        ElapsedTime timer;
        public CheckGyroTargetYawReached(double targetYaw) {
            this.targetYaw = targetYaw;
            this.gyro = navigationObj.gyro;
            this.angleTolerance = gyro.getAngleTolerance();
            navcheck = NavChecksSupported.CHECK_TARGET_YAW_GYRO;
            timer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
            timer.reset();
            prev_speed = prev_yaw = estimatedYaw = -1;
        }

        @Override
        public void reset() {
            timer.reset();
            prev_speed = prev_yaw = estimatedYaw = -1;
            targetYaw = gyro.getYaw(); // Needs to be changed.
        }

        @Override
        public boolean stopNavigation() {
            double curYaw = gyro.getYaw();
            if (curYaw == prev_yaw) {
                // Estimate the current yaw based on prev_speed, prev_yaw
                estimatedYaw = prev_yaw + (prev_speed * timer.milliseconds());
            } else {
                if (prev_yaw != -1) {
                    prev_speed = navigationObj.distanceBetweenAngles(curYaw, prev_yaw) / timer.milliseconds();
                    prev_speed = (navigationObj.getSpinDirection(prev_yaw, curYaw) == Navigation.SpinDirection.CLOCKWISE) ?
                            prev_speed : -prev_speed;
                }
                prev_yaw = estimatedYaw = curYaw;
                timer.reset();
                DbgLog.msg("ftc9773: prev_speed=%f, prev_yaw=%f", prev_speed, prev_yaw);
            }
            if (navigationObj.distanceBetweenAngles(estimatedYaw, targetYaw) <= angleTolerance) {
                return (true);
            } else {
                return (false);
            }
        }
    }

    public class CrossCheckNavxWhileTurning extends NavCheckBaseClass {
        double degreesToCheck;
        DriveSystem.ElapsedEncoderCounts elapsedCounts;
        double navxYaw;
        GyroInterface gyro;
        public CrossCheckNavxWhileTurning(double degreesToCheck) {
            this.degreesToCheck = degreesToCheck;
            elapsedCounts = robot.driveSystem.getNewElapsedCountsObj();
            elapsedCounts.reset();
            gyro = navigationObj.gyro;
            navxYaw = gyro.getYaw();
            navcheck = NavChecksSupported.CROSSCHECK_GYRO_WITH_ENCODERS;
        }

        @Override
        public void reset() {
            elapsedCounts.reset();
            navxYaw = gyro.getYaw();
        }

        @Override
        public boolean stopNavigation() {
            double encoder_degreesTurned = Math.abs(elapsedCounts.getDegreesTurned());
            double navx_degreesTurned = navigationObj.distanceBetweenAngles(navxYaw,
                    gyro.getYaw());
            double diff = Math.abs(encoder_degreesTurned - navx_degreesTurned);
            if (diff > Math.abs(degreesToCheck)) {
                DbgLog.msg("ftc9773: encoder degrees: %f, navx degrees: %f", encoder_degreesTurned, navx_degreesTurned);
                return (true);
            } else {
                return (false);
            }
        }
    }

    public class CheckRobotTilting extends NavCheckBaseClass {
        double pitchDegrees;
        GyroInterface gyro;
        double navxPitch;
        // TODO: 12/29/16 Investigate the feasibility of using phone's builtin sensors to detect tilting

        public CheckRobotTilting(double pitchDegrees) {
            this.pitchDegrees = pitchDegrees;
            gyro = navigationObj.gyro;
            navxPitch = gyro.getPitch();
            navcheck = NavChecksSupported.CHECK_ROBOT_TILTING;
        }

        @Override
        public void reset() {
            return;
        }

        @Override
        public boolean stopNavigation() {
            if (Math.abs(gyro.getPitch() - navxPitch) > pitchDegrees) {
                return (true);
            } else {
                return (false);
            }
        }
    }

    public class OpmodeInactiveCheck extends NavCheckBaseClass {
        @Override
        public boolean stopNavigation() {
            if (curOpMode.opModeIsActive()) {
                return (false);
            } else {
                return (true);
            }
        }

        @Override
        public void reset() {
            return;
        }

    }

    /**
     * Check if the required distance in inches has been travelled
     */
    public class EncoderCheckForDistance extends NavCheckBaseClass {
        double distanceInInches;
        DriveSystem.ElapsedEncoderCounts elapsedCounts;

        public EncoderCheckForDistance(double distanceInInches) {
            this.distanceInInches = Math.abs(distanceInInches);
            elapsedCounts = robot.driveSystem.getNewElapsedCountsObj();
            elapsedCounts.reset();
            navcheck = NavChecksSupported.CHECK_DISTANCE_TRAVELLED;
        }

        @Override
        public boolean stopNavigation() {
            double distanceTravelled = elapsedCounts.getDistanceTravelledInInches();
            // Check for both magnitude and sign
            // Note: sign is always +ve anyways... need to change this code later
            if (Math.abs(distanceTravelled) >= Math.abs(distanceInInches)) {
                DbgLog.msg("ftc9773: distanceTravelled = %f", distanceTravelled);
                elapsedCounts.printCurrentEncoderCounts();
                return (true);
            } else {
                return (false);
            }
        }

        @Override
        public void reset() {
            elapsedCounts.reset();
        }
    }

    public class CheckForWhiteLine extends NavCheckBaseClass {
        @Override
        public boolean stopNavigation() {
            if (navigationObj.lf.onWhiteLine()) {
                return (true);
            } else {
                return (false);
            }
        }

        @Override
        public void reset() {
            return;
        }
    }

    public NavigationChecks(FTCRobot robot, LinearOpMode curOpMode, Navigation navigationObj) {
        this.robot = robot;
        this.curOpMode = curOpMode;
        this.navigationObj = navigationObj;
        stopNavCriterion = null;
    }

    public boolean stopNavigation() {
        for (NavCheckBaseClass e: this.criteriaToCheck) {
            if (e.stopNavigation()) {
                this.stopNavCriterion = e;
                return (true);
            }
        }
        return (false);
    }
    public void reset() {
        for (NavCheckBaseClass e: this.criteriaToCheck) {
            e.reset();
        }
    }

    public void addNewCheck(NavigationChecks.NavCheckBaseClass navCheck) {
        this.criteriaToCheck.add(navCheck);
    }

    public void removeCheck(NavCheckBaseClass navCheck) {
        this.criteriaToCheck.remove(navCheck);
    }
}
