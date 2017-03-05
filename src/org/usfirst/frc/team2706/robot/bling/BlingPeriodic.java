package org.usfirst.frc.team2706.robot.bling;

import org.usfirst.frc.team2706.robot.Robot;
import org.usfirst.frc.team2706.robot.controls.StickRumble;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Command;

/**
 * This command should be run with the scheduler during teleop to decide what to display, either
 * distance to gear delivery, if we're ready to get another gear or if it is time to climb/ we're
 * ready to climb.
 */
public class BlingPeriodic extends Command {

    // Will tell us which teleop thing we're displaying
    private static String teleopDisplayState = "";
    
    private static StickRumble rumbler;
    
    public BlingPeriodic() {
        requires(Robot.blingSystem);
    }
    
    private static boolean pegIn = false;

    @Override
    public void initialize() {
        if (DriverStation.getInstance().isAutonomous()) {
            Robot.blingSystem.auto();
        }
        else {
            Robot.blingSystem.teleopInit();
        }
    }

    @Override
    public void execute() {

        int gearState = Robot.gearHandler.gearHandlerState();
        if (DriverStation.getInstance().isAutonomous())
            return;

        double timePassed = timeSinceInitialized();

        /* Wait some seconds from initialization to tell drivers entering teleop.
         * Also don't want to spam the arduino so only run around every 0.5 seconds.
         */
        if (timePassed < 3 || ((timePassed % 0.5) <= 0.1 || (timePassed % 0.5) >= 0.1))
            return;
       
        // Do nothing else if we are displaying low battery.
        if (Robot.blingSystem.getBatteryCriticality()) 
            return;
        

        // Get the average distance from whatever obstacle.
        double distance = (Robot.driveTrain.getRightDistanceToObstacle()
                        + Robot.driveTrain.getLeftDistanceToObstacle()) / 2;
        
        // Need this to determine if we're ready to climb
        double timeLeft = 150 - Timer.getMatchTime();

        // We use the teleopDisplayState to make sure we only call each of these once.
        
        // Basically, if we're in range and have a gear.
        if (distance < 3 && ((1 <= gearState && 3 >= gearState) || gearState == 5)) {
            
            // Basically, if we have the gear, either arm open or closed.
            if (gearState >= 2 && gearState <= 3) {
                
                // Only want to run this the first time.
                if (!pegIn) {
                    StickRumble rumbler = new StickRumble(0.5, 0.5, 1, 0, -1, 1.0);
                    rumbler.start();
                }
                pegIn = true;
                
            }
            else {
                // If peg was in, the rumble was on.
                if (pegIn) {
                    try {
                        rumbler.end();
                    }
                    catch (Exception e) {}
                }
                pegIn = false;
            }
            
            Robot.blingSystem.showDistance(distance, pegIn);
            teleopDisplayState = "distance";
            
          // Basically, if we're ready to get a gear   
        } else if (gearState == 0 && teleopDisplayState != "gear") {    

            Robot.blingSystem.showReadyToReceiveGear(true);
            teleopDisplayState = "gear";
           
          // Basically, if we must climb  
        } else if ((timeLeft <= 30 || timeSinceInitialized() >= 135) && teleopDisplayState != "climb") {
          
            Robot.blingSystem.showReadyToClimb(true);
            teleopDisplayState = "climb";
        }
    }

    @Override
    protected boolean isFinished() {
        return false;
    }

    @Override
    public void end() {
        Robot.blingSystem.clear();
    }
    
    @Override
    public void interrupted() {
        end();
    }
}
