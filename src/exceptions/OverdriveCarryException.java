package exceptions;

public class OverdriveCarryException extends Exception {
	
    public OverdriveCarryException(){
        super("Can't carry an item in the tube while in overdrive mode!");
    }

}
