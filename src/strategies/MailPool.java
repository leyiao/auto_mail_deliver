package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import exceptions.ItemTooHeavyException;
import exceptions.OverdriveCarryException;

public class MailPool implements IMailPool {

	public class Item {
		int priority;
		int destination;
		MailItem mailItem;
		// Use stable sort to keep arrival time relative positions
		public Item(MailItem mailItem) {
			priority = (mailItem instanceof PriorityMailItem) ? ((PriorityMailItem) mailItem).getPriorityLevel() : 1;
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
		}
	}
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.priority < i2.priority) {
				order = 1;
			} else if (i1.priority > i2.priority) {
				order = -1;
			} else if (i1.destination < i2.destination) {
				order = 1;
			} else if (i1.destination > i2.destination) {
				order = -1;
			}
			return order;
		}
	}
	
	private LinkedList<Item> pool;
	private LinkedList<Robot> robots;
	
	public MailPool(int nrobots){
		// Start empty
		pool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}
	
	@Override
	public void step() throws ItemTooHeavyException, OverdriveCarryException {
		try{
			ListIterator<Robot> i = robots.listIterator();
			while (i.hasNext()) loadRobot(i);
		} catch (Exception e) { 
            throw e; 
        } 
	}
	
	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException, OverdriveCarryException {
		Robot robot = i.next();
		assert(robot.isEmpty());
		
		ListIterator<Item> j = pool.listIterator();
		if (pool.size() > 0) {
			robot.addToHand(j.next().mailItem);
			j.remove();

			if (pool.size() > 0 && robot.getIsOverdrive() == false) {
				robot.addToTube(j.next().mailItem);
				j.remove();
			}
			// send the robot off if it has any items to deliver
			robot.dispatch(); 
			// remove from mailPool queue
			i.remove();      
			} 

	}

	@Override
	// assumes won't be there already
	public void registerWaiting(Robot robot) { 
		robots.add(robot);
	}
	public LinkedList<Item> getPool(){
		return pool;
	}

}
