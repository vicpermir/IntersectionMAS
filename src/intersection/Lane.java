package intersection;

import java.util.HashMap;
import java.util.Map;

import jade.core.AID;

public class Lane {

	private P startPosition, endPosition;
	private String id;
	private Map<AID, P> vehicleList;
	
	public Lane(String id, P start, P end) {
		this.setId(id);
		this.setStartPosition(start);
		this.setEndPosition(end);
		this.vehicleList = new HashMap<AID, P>();
	}

	// Add and remove vehicle from lane
	public boolean addVehicle( AID aid, P position) {
		return vehicleList.putIfAbsent(aid, position ) == null;
	}

	public boolean removeVehicle( AID aid ) {
		return vehicleList.remove(aid) != null;
	}
	
	// Getters and setters
	public P getStartPosition() {
		return new P(startPosition);
	}

	public void setStartPosition(P startPosition) {
		this.startPosition = startPosition;
	}

	public P getEndPosition() {
		return new P(endPosition);
	}

	public void setEndPosition(P endPosition) {
		this.endPosition = endPosition;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}
