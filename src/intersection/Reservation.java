package intersection;

import java.util.ArrayList;
import java.util.List;

public class Reservation{
	
	List<TimeFrame> reservations; 
	
	public Reservation() {
		this.reservations = new ArrayList<TimeFrame>();
	}
	
	public List<TimeFrame> getTimeFrames() {
		return reservations;
	}

	public void addTimeFrame(String vehicleAgent, long start, long end) {
		reservations.add( new TimeFrame( vehicleAgent, start-200L, end+200L ) );
	}
	
	public int size() {
		return reservations.size();
	}
	
	public TimeFrame get( int index ) {
		return reservations.get(index);
	}
	
	public void remove( int index ) {
		reservations.remove(index);
	}

}
