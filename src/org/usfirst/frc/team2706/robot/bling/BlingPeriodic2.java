package org.usfirst.frc.team2706.robot.bling;

import org.usfirst.frc.team2706.robot.Robot;
import org.usfirst.frc.team2706.robot.controls.StickRumble;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/***
 * 
 * @author eAUE (Kyle Anderson)
 * @see <a href = "https://docs.google.com/spreadsheets/d/1zb40w_BbmzKFPKRT7XTrjqsIKUw0Jn152sTJA3ppQCs/edit?usp=sharing">
 * Bling Subsystem Map </a>. 
 *
 */
public class BlingPeriodic2 extends Command {
    
    
    // Distances for lining up to receive a gear in inches.
    
    // Too close if we're below this.
    protected final int lowerDistanceThreshold = 11;
    
    // Too far if we're above this.
    protected final int upperDistanceThreshold = 20;
    
    // Don't bother displaying the pattern if we're outside of this.
    protected final int outsideDistanceThreshold = 40;
    
    /* True if we were just lining up for gear.
     * To avoid showing the peg line-up pattern right after gear pickup.
     */
    protected static boolean gearPickupPattern = false;
    protected static double timePointGearPickup = 0;
    protected static double timePassedGearPickup = 0;
    
    protected static double timePoint = Timer.getFPGATimestamp();
    
    public BlingPeriodic2() {
        requires (Robot.blingSystem);
    }
    
    protected static StickRumble rumbler = null; 
    
    protected void initialize () {
        if (DriverStation.getInstance().isAutonomous() && Robot.blingSystem.getSpecialState() == "autoTrue") {
            Robot.blingSystem.auto();
        }
    }
    
    protected void execute () {
        
        double timePassed = Timer.getFPGATimestamp() - timePoint;
        
        if (timePassed < 0.2)
            return;
        
        timePoint = Timer.getFPGATimestamp();
        
        // Used to make sure we do not interrupt patterns with other patterns.
        boolean busy = false;
        
        // Autonomous display modes
        if (DriverStation.getInstance().isAutonomous())
            return;
        
        // Teleop display modes
        else if (DriverStation.getInstance().isOperatorControl() && Robot.blingSystem.getSpecialState() == "") {
            
            
            // Required measurements. 
            double distance = Robot.driveTrain.getDistanceToObstacle(); 
            
            /*
             * Description of gearHandler states (from GearHandler subsystem)<p>
             * 0 = Arms closed with no gear.<p>
             * 1 = arms closed with a gear.<p>
             * 2 = arms closed with gear and peg in.<p>
             * 3 = Arms open with a gear and peg in. <p>
             * 4 = Arms open with no gear.<p>
             * 5 = Arms open with a gear.<p>
             * 6 = Arms open with no gear and peg in.<p>
             * 7 = Arms closed with no gear and peg in.
             */
            int gearState = Robot.gearHandler.gearHandlerState();
            boolean climbing = Robot.climber.isClimbing();
            
            
            /* Turn off the rumbler at an appropriate time 
             * (when the peg is no longer in or arms are open).
             */
            if ((rumbler != null) && (gearState < 2 || gearState > 3)) {
                rumbler.end();
                rumbler = null;
            }
            
            // Displaying peg line up
            if (!gearPickupPattern && distance < outsideDistanceThreshold && ((1 <= gearState && gearState <= 3) || gearState == 5)) {
                busy = true;
                
                // Get some vibration going, but only if there is already none.
                if (2 <= gearState && gearState <= 3 && rumbler == null) {
                    rumbler = new StickRumble(0.7, 0.3, 2, 0.2, -1, 1.0, 0);
                    rumbler.start();
                }
                
                Robot.blingSystem.showDistance(2 <= gearState && gearState <= 3);
            }
            
            // Displaying pickup line up
            else if ((distance < outsideDistanceThreshold) && (gearState == 0 || gearState == 4) && !busy) {
                busy = true;
                gearPickupPattern = true;
                
                // Just right (arms closed and in nice distance)
                if ((lowerDistanceThreshold <= distance) && (distance <= upperDistanceThreshold) && gearState == 0)
                    Robot.blingSystem.showReadyToReceiveGear(1);
                
                // Arms open)
                else if (gearState == 4)
                    Robot.blingSystem.showReadyToReceiveGear(0);
                
                // Too far or arms open
                else
                    Robot.blingSystem.showReadyToReceiveGear(2);
            }
            
            // We are climbing!
            else if (climbing && !busy) {
                busy = true;
                Robot.blingSystem.climbingDisplay();
            }
            
            // Time to climb
            else if (timeSinceInitialized() > 120 && !busy) {
                gearPickupHandler(gearState);
                busy = true;
                Robot.blingSystem.showReadyToClimb(true);
            }
            
            // 
            else if (gearState == 1 && !busy) {
                gearPickupHandler(gearState);
                busy = true;
                Robot.blingSystem.funDisplay();                
            }
            
            else if (!busy) {
                Robot.blingSystem.clear();
            }
        }
    }
    
    /**
     * Used to make sure that when we back up from picking up a gear,
     * there is a delay before the peg line up pattern can be displayed.
     */
    protected void gearPickupHandler(int gearState) {
        if (!gearPickupPattern || gearState != 1)
            return;
        
        else if (timePointGearPickup == 0) {
            timePointGearPickup = Timer.getFPGATimestamp();
        }
        timePassedGearPickup = Timer.getFPGATimestamp() - timePointGearPickup;
        
        if (timePassedGearPickup > 1.5) {
            timePassedGearPickup = 0;
            timePointGearPickup = 0;
            gearPickupPattern = false;
        }
    }
    
    @Override
    protected boolean isFinished() {
        return false;
    }
    
    protected void end () {
        Robot.blingSystem.clear();
    }
    
    protected void interrupted () {
        end();
    }
}
