package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import view.CanvasWorld;
import main.VehicleStatsCollector;

import java.util.Map;
import java.util.Random;

import javax.swing.SwingUtilities;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import behaviours.*;
import intersection.*;
import jade.core.Runtime;

public class IntersectionManagerAgent extends Agent {

	private static final long serialVersionUID 	= 1L;
	private static final String serviceType 	= "IntersectionManager";
	
	// CONFIG
	private static int MAXWORLDX = 800;
	private static int MAXWORLDY = 600;
	private static int CELLSIZE = 40;		// Size of a cell in pixels
	private long turnDelay = 120L; 			// Delay between turns in ms
	// ------------------
	
	private Map<String, P> vehicleAgents = new HashMap<String, P>();
	private boolean open = true;
	private int lastProcessedTurn = -1;
	private FourWayIntersectionSingleLane intersection;
	private Date date = new Date();
	private Random rnd = new Random();
	
	private VehicleStatsCollector statsCollector = new VehicleStatsCollector();
	
	private CanvasWorld canvasWorld;
	private Runtime rt;
	
	
	protected void setup() {
		
		this.rt = (Runtime) this.getArguments()[0];
		
	  	// Register the service
	  	try {
	  		
	  		DFAgentDescription dfd = new DFAgentDescription();
	  		dfd.setName(getAID());
	  		
	  		ServiceDescription sd = new ServiceDescription();
	  		sd.setType(serviceType);
	  		sd.setName(this.getLocalName());
	  		sd.setName(getLocalName());
	  		
	  		dfd.addServices(sd);
	  		
	  		DFService.register(this, dfd);
	  	}
	  	catch (FIPAException fe) {
	  		fe.printStackTrace();
	  	}
	  	
	  	// Build and initialize intersection space
	  	intersection = new FourWayIntersectionSingleLane( MAXWORLDX, MAXWORLDY, CELLSIZE );
	  	
	  	// Initialize Interface	
		IntersectionManagerAgent me = this;

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				canvasWorld = new CanvasWorld(me, MAXWORLDX, MAXWORLDY);
				
				// Add lanes to the canvas
				for ( Lane lane : intersection.getAllLanes() ) {
					canvasWorld.addLane(lane);
				}
				
				// Add cells to the canvas
				canvasWorld.setCellSize(CELLSIZE);
				for ( P cell : intersection.getCells() ) {
					canvasWorld.addCell(cell);
				}
			}
		});
	  	
		
	  	// Give it some time
	  	try {
	  		Thread.sleep(1000L);
	  	} catch (Exception e) {}
	  		  	
	  	// Track vehicle registering requests
	  	addBehaviour( new IMVehicleTrackingBehaviour() );
	  	
	  	// Start the turn rotation
	  	addBehaviour( new IMTurnRotationBehaviour( this, turnDelay ) );
	  	
	}
	
	// Registration/Modification
	public void addVehicle ( String aid, P position ) {
		
		if ( vehicleAgents.containsKey(aid) ) {
			System.out.println("[" + getLocalName() + "] vehicle agent \"" + aid + "\" is already on my list.");
		} else {
			vehicleAgents.put(aid, position);
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					canvasWorld.addVehicle(aid, position);
				}
			});			
			
			//System.out.println("[" + getLocalName() + "] added vehicle agent \"" + aid + "\".");
		}
		
	}
	
	public void updateVehicle( String aid, float x, float y) {
		if ( vehicleAgents.containsKey(aid) ) {
			vehicleAgents.get(aid).set(x, y);
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					canvasWorld.updateVehicle(aid, x, y);			
				}
			});
		}
	}
	
	// Deregistration
	public void removeVehicle ( String aid ) {
		
		if ( vehicleAgents.remove(aid) != null ) {
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					canvasWorld.removeVehicle(aid);
					canvasWorld.updateAverageDelay(statsCollector.getAverageDelay());
				}
			});
			
			//System.out.println("[" + getLocalName() + "] removed vehicle agent \"" + aid + "\".");
		} else {
			System.out.println("[" + getLocalName() + "] cannot remove vehicle agent \"" + aid + "\".");
		}
	}
	
	public String reservePath( String vehicleAgent, String entryLane, String exitLane, int eta, float currentSpeed) {
		//System.out.println("[" + getLocalName() + "] Reservation request: " + vehicleAgent + ", " + entryLane + ", " + exitLane + ", " + eta + ", " + currentSpeed);
		
		// Obtain the requested valid path (ID=entryLane+exitLane)
		Path path = intersection.getPath(entryLane+exitLane);
		
		// Simulate reservations to ensure all the cells involved are available
		boolean reserve = true;
		long currentTime = new Date().getTime();
		long tempArrivalTime = currentTime + (eta * turnDelay);
		double timeInCell = (CELLSIZE / currentSpeed) * turnDelay;
		//System.out.println("currentTime=" + currentTime + ", tempArrivalTime=" + tempArrivalTime + ", diff=" + (tempArrivalTime - currentTime) + "ms");
		
		for ( P cell : path.getCells() ) {
			
			long endTime = (long) (tempArrivalTime + timeInCell);
			reserve = intersection.tryReserve(cell, tempArrivalTime, endTime, vehicleAgent);
			tempArrivalTime = endTime;
			
		}
		
		// If the reservation is possible reserve is commited
		if (reserve) {
			tempArrivalTime = currentTime + (eta * turnDelay);
			String reservationString = "";
			for ( P cell : path.getCells() ) {
				
				long endTime = (long) (tempArrivalTime + timeInCell);
				intersection.reserve(cell, tempArrivalTime, endTime, vehicleAgent);
				tempArrivalTime = endTime;
				
				reservationString += "x=" + cell.getX() + "y=" + cell.getY() + "#";
				
			}
			
			// reservationString contains all the cell's position, exit lane positions needed
			reservationString += "x=" + path.getEndLane().getStartPosition().getX();
			reservationString += "y=" + path.getEndLane().getStartPosition().getY() + "#";
			reservationString += "x=" + path.getEndLane().getEndPosition().getX();
			reservationString += "y=" + path.getEndLane().getEndPosition().getY();
			
			// Reserve is done, now return the string with all the positions
			return reservationString;
			
		} else {
			return null;
		}
		
	}
	
	public void addDelay( long delay) {
		statsCollector.addDelay(delay);
	}
	
	public void shutDown() {
		this.rt.shutDown();
	}
	
	public Lane getRandomIncomingLane() {
		List<Lane> list = intersection.getIncomingLanes();
		int index = rnd.nextInt(list.size());
		return list.get(index);
	}
	
	public Lane getRandomOutgoingLane( String id ) {
		List<Lane> list = intersection.getOutgoingLanes();
		int index = rnd.nextInt(list.size());
		while( list.get(index).getId().substring(0, 1).equals(id) ){
			index = rnd.nextInt(list.size());
		}
		return list.get(index);
	}
	
	// Getters
	public boolean isOpen() {
		return open;
	}
	
	public Map<String, P> getVehicleAgents() {
		return vehicleAgents;
	}
	
	public int getLastProcessedTurn() {
		return lastProcessedTurn;
	}
	
	public FourWayIntersectionSingleLane getIntersection() {
		return intersection;
	}
	
	public long getTime(){
		return this.date.getTime();
	}
	
	
	public void setOpen( boolean value ) {
		this.open = value;
	}
	
	public void incrementLastProcessedTurn() {
		intersection.clearExpiredReservations();
		canvasWorld.updateReservations(intersection.getNumberOfReservations());
		this.lastProcessedTurn++;
	}

}
