package agents;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import behaviours.RadarListenerBehaviour;
import intersection.P;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class RadarAgent extends Agent {

	private static final long serialVersionUID  = 1L;
	private static final String serviceType 	= "Radar";

	private Map<String, VehicleRadarEntry> vehicleMap = new HashMap<String, VehicleRadarEntry>();
	
	protected void setup() {
				
		try {
	  		
	  		DFAgentDescription dfd = new DFAgentDescription();
	  		dfd.setName(getAID());
	  		
	  		ServiceDescription sd = new ServiceDescription();
	  		sd.setType(serviceType);
	  		sd.setName(getLocalName());
	  		
	  		dfd.addServices(sd);
	  		
	  		DFService.register(this, dfd);
	  	}
	  	catch (FIPAException fe) {
	  		fe.printStackTrace();
	  	}
	  	  	
	  	// Register this vehicle to the intersection
	  	addBehaviour( new RadarListenerBehaviour() );
		
	}
	
	public void updateCarEntry( String vehicleID, P targetPosition, P currentPosition) {
		vehicleMap.put(vehicleID, new VehicleRadarEntry( targetPosition, currentPosition ) );
	}
	
	public double getDistanceToCarInFront( String vehicleID ) {
		P myPosition, myTarget;
		double myDistanceToTarget;
		
		// Check if the radar agent has any information about the requester
		if ( !vehicleMap.containsKey(vehicleID) ) {
			return 0.0;
		} else {
			myPosition			= vehicleMap.get(vehicleID).getCurrentPosition();
			myTarget 			= vehicleMap.get(vehicleID).getTargetPosition();
			myDistanceToTarget  = myPosition.dist(myTarget);
		}
		
		
		// Check if there is a vehicle moving towards the same target
		double distanceToClosest = 999999;
		double tempDistance, candidateDistanceToTarget;
		P candidatePosition, candidateTarget;
		
		for ( Entry<String, VehicleRadarEntry> entry : vehicleMap.entrySet() ) {
			candidatePosition = entry.getValue().getCurrentPosition();
			candidateTarget   = entry.getValue().getTargetPosition();
			candidateDistanceToTarget = candidatePosition.dist(candidateTarget);
			
			// Ignore myself
			if ( !entry.getKey().equals(vehicleID)) {
								
				// Only interested in vehicles on my same target (i.e. same lane or cell)
				if( candidateTarget.equals(myTarget) ) {
					
					// Calculate my distance to the candidate vehicle
					tempDistance = ( myDistanceToTarget - candidateDistanceToTarget );
					
					// It has to be positive, otherwise the vehicle is behind me
					if( ( tempDistance > 0 ) && ( tempDistance < distanceToClosest ) ) {
						distanceToClosest = tempDistance;
					}
					
				}
				
			}
			
		}
		return distanceToClosest;
	}
	
	class VehicleRadarEntry {
		
		private P targetPosition;
		private P currentPosition;
		
		public VehicleRadarEntry( P targetPosition, P currentPosition ) {
			this.setTargetPosition(targetPosition);
			this.setCurrentPosition(currentPosition);
		}

		public P getTargetPosition() {
			return targetPosition;
		}

		public void setTargetPosition(P targetPosition) {
			this.targetPosition = targetPosition;
		}

		public P getCurrentPosition() {
			return currentPosition;
		}

		public void setCurrentPosition(P currentPosition) {
			this.currentPosition = currentPosition;
		}
		
		public double getDistanceToTarget() {
			return currentPosition.dist(targetPosition);
		}
		
	}
	
}
