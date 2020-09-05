package strategies;

import automail.IMailDelivery;
import automail.Robot;

public class Automail {
	      
    public Robot[] robots;
    public IMailPool mailPool;
  //EDIT!
    public boolean overdrive_enabled;
  //EDIT!
    public Automail(IMailPool mailPool, IMailDelivery delivery, int numRobots,boolean overdrive_enabled) {
    	// Swap between simple provided strategies and your strategies here
    	    	
    	//Initialize the MailPool
    	this.mailPool = mailPool;
    	
    	this.overdrive_enabled = overdrive_enabled;
    	//Initialize robots */
    	robots = new Robot[numRobots];
    	for (int i = 0; i < numRobots; i++) robots[i] = new Robot(delivery, mailPool,overdrive_enabled);
    }
    
}
