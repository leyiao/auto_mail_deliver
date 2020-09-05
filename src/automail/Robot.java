package automail;

import exceptions.*;
import strategies.IMailPool;
import java.util.Map;
import java.util.TreeMap;

/**
 * The robot delivers mail!
 */
public class Robot {
	//// if there is a mail whit weight excess 2000, implies a simulation bug
    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;

    IMailDelivery delivery;
    protected final String id;
    
    // Possible states the robot can be in 
    // Regard overdrive mode as one of states
    public enum RobotState { DELIVERING, WAITING, RETURNING,OVERDRIVE,COOLINGDOWN }
    public RobotState current_state;
    //Time for cooling down after robot delivering under overdrive mode
    static public final int COOLING_DOWN_STEP = 5;
    private int cooling_step = 0;
    
    private int current_floor;
    private int destination_floor;
    private IMailPool mailPool;
    //get sign from mailPool whether robot could start delivering or not
    private boolean receivedDispatch;
    
    private MailItem deliveryItem = null;
    private MailItem tube = null;
    // how many items that the robot has been delivered in one delivery
    private int deliveryCounter;

    //robot moves 2 steps at a time when in overdrive mode
    private final int OVERDRIVE_STEP = 2;
    //only if the priority of mail excess 50, the robot can start overdrive mode
    private final int OVERDRIVE_PRIORITY = 50;
    private boolean isOverdrive= false;
    private boolean overdrive_enabled;

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */

    public Robot(IMailDelivery delivery, IMailPool mailPool,boolean overdrive_enabled){
    	id = "R" + hashCode();
    	current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
        this.overdrive_enabled = overdrive_enabled;
    }
    

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException {    	
    	switch(current_state) {
    		//This state is triggered when the robot is returning to the mailroom after a delivery
    		case RETURNING:
    			//If its current position is at the mailroom, then the robot should change state 
           if(current_floor == Building.MAILROOM_LOCATION){
                	if (tube != null) {
                		mailPool.addToPool(tube);
                    System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), tube.toString());
                    tube = null;
                	}
        			//Tell the sorter the robot is ready 
        			mailPool.registerWaiting(this);
                	changeState(RobotState.WAITING);
                } else {
                	//If the robot is not at the mailroom floor yet, then move towards it!
                    moveTowards(Building.MAILROOM_LOCATION);
                	break;
                }
    		case WAITING:
            //If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery 
            if(!isEmpty() && receivedDispatch){
                	receivedDispatch = false;
                	// reset delivery counter
                	deliveryCounter = 0; 
        			setRoute();
	        			if(!isOverdrive){
	        				changeState(RobotState.DELIVERING);
	        			}else{
	        				changeState(RobotState.OVERDRIVE);}
                }
                break;
    		case DELIVERING:
    			// If already here drop off either way
            //Delivery complete, report this to the simulator!
    			if(current_floor == destination_floor){ 
    				   // false means overdrive mode is off
                    delivery.deliver(deliveryItem, false);
                
                    deliveryItem = null;
                    deliveryCounter++;
                    // Implies a simulation bug
                    if(deliveryCounter > 2){  
                    		throw new ExcessiveDeliveryException();
                    }
                    //Check if want to return, i.e. if there is no item in the tube
                    if(tube == null){
                    		changeState(RobotState.RETURNING);
                    }
                    else{
                        //If there is another item, set the robot's route to the location to deliver the item
                        deliveryItem = tube;
                        tube = null;
                        setRoute();
                        changeState(RobotState.DELIVERING);
                    }
    			} else {
	        		//The robot is not at the destination yet, move towards it!
	            moveTowards(destination_floor);
    			}
                break;
                
    		case OVERDRIVE:
    			//Mail has been delivered if the robot arrive destination floor
    			if(current_floor == destination_floor) {
    				 //Delivery complete, report this to the simulator!
 				 // true means overdrive mode is on
                 delivery.deliver(deliveryItem, true);
                 
                 deliveryItem = null;
                 deliveryCounter++;
                 // Implies a simulation bug
                 if(deliveryCounter > 1){  
                 	throw new ExcessiveDeliveryException();
                 }
                 changeState(RobotState.COOLINGDOWN);
    			}
    			// different movement from normal mode
    			else {
    				overdriveMove(destination_floor);
    			}
    			
    			break;
    			
    		case COOLINGDOWN:
    			if(cooling_step < COOLING_DOWN_STEP) {
    				System.out.printf("T: %3d > %7s-> Cooling Down Step %2d\n", Clock.Time(), getIdTube(),cooling_step+1);
    				cooling_step ++;
    			}else {
    				changeState(RobotState.RETURNING);
    				deactivateOverdrive();
    				System.out.printf("T: %3d > %7s-> Overdrive mode has turn off\n", Clock.Time(), getIdTube());
    				cooling_step = 0;
    			}
    	}
    }
    
    public void dispatch() {
		receivedDispatch = true;
}
    /**
     * Sets the route for the robot
     */
    private void setRoute() {
        //Set the destination floor
        destination_floor = deliveryItem.getDestFloor();
    }

    /**
     * function that moves the robot towards the destination when the overdrive mode is off
     * @param destination: the floor towards which the robot is moving
     */
    private void moveTowards(int destination) { 	
    		// when the robot is beyond destination 
        if(current_floor < destination){
        		current_floor++;  
        } 
        // when the robot is above the destination
        else {
        		current_floor--;
        } 
    }
    
    /**
     * function that moves the robot towards the destination when the overdrive mode is on
     * @param destination: the floor towards which the robot is moving
     */
    private void overdriveMove(int destination) {
    		int dist = Math.abs(destination - current_floor);
    		
    		// when the robot is beyond destination 
    		if(current_floor < destination) {
    			if(dist > 1) {
    				current_floor += OVERDRIVE_STEP;
    			}else {
    				current_floor++;
    			}
    		}
    		// when the robot is above the destination
    		else {
    			if(dist > 1) {
    				current_floor -= OVERDRIVE_STEP;
    			}else {
    				current_floor--;
    			}
    		}
    	
    }
    
    private String getIdTube() {
    	return String.format("%s(%1d)", id, (tube == null ? 0 : 1));
    }
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(RobotState nextState){
    	assert(!(deliveryItem == null && tube != null));
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
    	current_state = nextState;
    	if(nextState == RobotState.DELIVERING){
            System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
    	}
    }

	public MailItem getTube() {
		return tube;
	}
    
	static private int count = 0;
	static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

	@Override
	public int hashCode() {
		Integer hash0 = super.hashCode();
		Integer hash = hashMap.get(hash0);
		if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
		return hash;
	}

	public boolean isEmpty() {
		return (deliveryItem == null && tube == null);
	}

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException {
		
		assert(deliveryItem == null);
		
		deliveryItem = mailItem;
		
		if (deliveryItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
		
		if (deliveryItem instanceof PriorityMailItem && overdrive_enabled) {
			
			if (((PriorityMailItem) deliveryItem).getPriorityLevel() > OVERDRIVE_PRIORITY && tube == null) {
				activateOverdrive();	
			}
		}
		
	}

	public void addToTube(MailItem mailItem) throws ItemTooHeavyException, OverdriveCarryException {
		
		assert(tube == null);
		
		if (isOverdrive && deliveryItem != null) {
			throw new OverdriveCarryException();
		}
		
		tube = mailItem;
		if (tube.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
		
	}
	
	public void activateOverdrive() {
		this.isOverdrive = true;
	}
	
	public void deactivateOverdrive() {
		this.isOverdrive = false;
	}
	public boolean getIsOverdrive() {
		return isOverdrive;
	}
	
	public RobotState getState() {
		return current_state;
	}
}
