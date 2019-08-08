package intersection;

public class TimeFrame  {
	
	private long startTime;
	private long endTime;
	private String vehicleID;
	
	public TimeFrame ( String vehicleID) {
		this.setVehicleID(vehicleID);
		this.startTime = 0;
		this.endTime = 0;
	}
	
	public TimeFrame (String vehicleID, long start, long end ) {
		this.setVehicleID(vehicleID);
		this.startTime = start;
		this.endTime = end;
	}
	
	
	// Getters and setters 
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public String getVehicleID() {
		return vehicleID;
	}

	public void setVehicleID(String vehicleID) {
		this.vehicleID = vehicleID;
	}
	
	// toString
	public String toString() {
		return "TimeFrame: ( vID="+this.vehicleID+", start="+this.startTime+", end=" + this.endTime + " )";
	}
	
}
