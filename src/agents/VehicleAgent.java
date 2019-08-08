package agents;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import intersection.P;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class VehicleAgent extends Agent {

	private static final long serialVersionUID 	= 1L;
	private static final String serviceType 	= "Vehicle";
	
	// Configuration and main attributes
	private String myIntersectionManager, radarAgent;
	Random r = new Random();
	
	private static int speedLimit = 4; // 10=fast, 4=moderate
	private float currentSpeed = 0;
	
	private P spawnPosition;
	private P currentPosition;
	private P targetPosition;
	
	private String currentLane = "";
	private String targetLane  = "";
	private int targetPositionIndex = 0;
	
	private boolean reserveDone = false;
	private float brakingFactor = 0.80f;
	private float pickupFactor = 2.00f;
	
	private long radarRequestPeriod = 250L;
	private double distanceToVehicleInFront = 0.0;		// pixels
	private double safeDistanceBetweenVehicles = 40;	// pixels
	
	private long spawnTime, finishTime;
	
	private List<P> myPath = new ArrayList<P>();
	
	
	// Setup()
	protected void setup() {
		
		setMyIntersectionManager( (String) this.getArguments()[0] );
		setRadarAgent( (String) this.getArguments()[1] );
		
	  	// Register the service
	  	//System.out.println("[" + getLocalName() + "] registering service of type \""+serviceType+"\"");
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
	  	addBehaviour( new VehicleRegisterBehaviour() );	  	

	}


	public double getDistanceToTarget() {
		return currentPosition.dist(targetPosition);
	}
	public List<P> getMyPath() {
		return myPath;
	}
	
	public void addToPath( P p) {
		myPath.add(p);
	}
	
	public boolean nextTarget() {
		targetPositionIndex++;
		
		// Check if I have a next target
		if (targetPositionIndex >= myPath.size()) {
			if(!intersectionPassed()) {
				setCurrentSpeed(0);
				System.out.println("[" + getLocalName() + "] ERROR: Failed to reserve before reaching the intersection. Stopping.");
			}
			return false;
		}
				
		// If I still have targets to reach I move to the next one
		targetPosition = myPath.get(targetPositionIndex);
		return true;
		
	}
	
	public void resetReservation() {
		myPath = new ArrayList<P>();
		addToPath(spawnPosition);
		addToPath(targetPosition);
		targetPositionIndex = 1;
		setReserveDone(false);
	}
	
	public long getDelay() {
		finishTime = new Date().getTime();
		
		double dist = 0;
		for ( int i=0; i < getMyPath().size() - 1; i++) {
			dist += getMyPath().get(i).dist(getMyPath().get(i+1));
		}
		long idealTime = (long) ( dist / speedLimit) * 120L;
		
		return (finishTime - spawnTime) - idealTime;
			
	}
	
	public void finish() {
		killAgent();
	}
	
	public void killAgent() {
		
		//Deregister the agent
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(this.getAID());
		
		try {
			DFService.deregister(this,  dfd);
		} catch (FIPAException fe) { 
			fe.printStackTrace(); 
		}
				
	}
	
	public void applyRandomBrakingFactor() {
		float min = 0.85f;
		float max = 0.95f;
		float randomBrakingFactor = r.nextFloat() * (max - min) + min;
		setCurrentSpeed(currentSpeed * randomBrakingFactor);
	}

	public void applyRandomPickupFactor() {
		float min = 1.60f;
		float max = 2.40f;
		float randomPickupFactor = r.nextFloat() * (max - min) + min;
		setCurrentSpeed(currentSpeed * randomPickupFactor);
	}
	// -------------------------------
	// 			Behaviours
	// -------------------------------
	
	/**
	 * VehicleRegisterBehaviour
	 * 
	 * Este comportamiento se encarga de registrar el vehículo
	 * en el agente intersección. Envía una petición REQUEST
	 * y espera una respuesta del agente intersección con sus
	 * carriles de origen y destino.
	 *
	 */	
	public class VehicleRegisterBehaviour extends Behaviour {

		private static final long serialVersionUID = 1L;
		private static final boolean VERBOSE = false;
		
		private DFAgentDescription   im 	 = null;
		private DFAgentDescription[] results = null;
		private int step = 0;
		
		
		private MessageTemplate registerConfirmTpl = MessageTemplate.and(
					MessageTemplate.or(
							MessageTemplate.MatchPerformative(ACLMessage.AGREE),
							MessageTemplate.MatchPerformative(ACLMessage.REFUSE)),
					MessageTemplate.MatchConversationId("vehicleRegister"));

		@Override
		public void action() {
			switch (step) {
			case 0:
				
				// Search for services of type "weather-forecast"
			  	if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] searching for my intersection manager (" + myIntersectionManager  + ").");
			  	
			  	// Build the description used as template for the search
			  	DFAgentDescription template = new DFAgentDescription();
			  	ServiceDescription templateSd = new ServiceDescription();
			  	templateSd.setName(myIntersectionManager);
			  	templateSd.setType("IntersectionManager");
			  	template.addServices(templateSd);
			    	
			  	SearchConstraints sc = new SearchConstraints();
			  	// We want to receive 10 results at most
			  	sc.setMaxResults(new Long(10));
			  	
			  	try {	
			  		results	= DFService.searchUntilFound( myAgent, myAgent.getDefaultDF(), template, sc, 5000);
			  		
			  		if ( results.length > 0 ) {
			  			
			  			im = results[0];
			  			
			  			step++;
			  		}	
			  		
			  	}
			  	catch (FIPAException fe) {
			  		fe.printStackTrace();
			  	}
				
				break;

			case 1:
				
				// Send a vehicle register request
				ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
				request.addReceiver( im.getName() );
				request.setConversationId("vehicleRegister");
				
				myAgent.send( request );
				
				if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] vehicleRegister REQUEST sent.");
				
				step++;			
				break;
			
			case 2:
				
				ACLMessage reply = myAgent.blockingReceive( registerConfirmTpl );
				
				if ( reply != null ) {
					
					if ( reply.getPerformative() == ACLMessage.AGREE ){
						
						if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] received AGREE from agent " + reply.getSender().getLocalName() + ".");
						
						String cont = reply.getContent();
						
						// Extract initialization data from registration message
						final String orig = cont.substring( cont.indexOf("orig=")+5, cont.indexOf("xO=") );
						final float xO = Float.parseFloat( cont.substring(cont.indexOf("xO=")+3, cont.indexOf("yO=") ));
						final float yO = Float.parseFloat( cont.substring(cont.indexOf("yO=")+3, cont.indexOf("xD=") ));
						final float xD = Float.parseFloat( cont.substring(cont.indexOf("xD=")+3, cont.indexOf("yD=") ));
						final float yD = Float.parseFloat( cont.substring(cont.indexOf("yD=")+3, cont.indexOf("dest=") ));
						final String dest = cont.substring( cont.indexOf("dest=")+5, cont.indexOf("end") );
						
						// Initialize
						setCurrentLane(orig);
						setCurrentPosition( new P( xO, yO) );
						setSpawnPosition( new P( xO, yO) );
						addToPath( new P( xO, yO) );
						
						setTargetLane(dest);
						addToPath( new P( xD, yD) );
						nextTarget();
						
						// Placeholder, spawn speed
						setCurrentSpeed(getSpeedLimit());
											  	
					  	// Process turn requests
					  	myAgent.addBehaviour( new VehicleProcessTurnBehaviour() );
					  		  	
					  	// Start the radar tracking behaviour
					  	myAgent.addBehaviour( new VehicleRadarTrackingBehavour( myAgent, radarRequestPeriod) );
						
						// System.out.println("[" + myAgent.getLocalName() + "] Message received: " + cont);
						step++;
						
					} else if ( reply.getPerformative() == ACLMessage.REFUSE ) {
						
						if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] received REFUSE from agent " + reply.getSender().getLocalName() + ".");
						
						// Registration denied, try again
						step--;
						
					} else {
						
						if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] received UNEXPECTED message from agent " + reply.getSender().getLocalName() + ".");
						
						// Unexpected message, kill this agent?
						finish();
					}
					
				}
							
				break;
			}

		}

		@Override
		public boolean done() {
			if (step == 3) {
				// Set the spawn time
				spawnTime = new Date().getTime();
				return true;
			} else {
				return false;
			}
		}

	}
	
	
	/**
	 * VehicleRadarTrackingBehaviour
	 * 
	 * Este comportamiento simplemente envia peticiones
	 * de informacion al agente radar, unicamente con el
	 * fin de conocer la distancia al vehículo que tiene delante
	 *
	 */	
	public class VehicleRadarTrackingBehavour extends TickerBehaviour {

		private static final long serialVersionUID = 1L;
		
		private int step = 0;
		private ACLMessage request, inform;
		private double distance;
		
		private MessageTemplate radarTpl = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.INFORM),
					MessageTemplate.MatchConversationId("radarRequest"));

		public VehicleRadarTrackingBehavour(Agent a, long period) {
			super(a, period);
		}

		@Override
		public void onTick() {

			switch (step) {
			
				case 0:
					
					request = new ACLMessage(ACLMessage.REQUEST);
					request.setConversationId("radarRequest");
					request.addReceiver( new AID( getRadarAgent(), AID.ISLOCALNAME) );
					
					String cont = "id=" + myAgent.getLocalName();
					cont += "x=" + currentPosition.getX();
					cont += "y=" + currentPosition.getY();
					cont += "tx=" + targetPosition.getX();
					cont += "ty=" + targetPosition.getY();
					cont += "end";
					
					request.setContent(cont);
					
					myAgent.send(request);
					
					step++;
					break;
		
				case 1:
					
					inform = myAgent.receive(radarTpl);
					
					if( inform != null ) {
						
						distance = Double.parseDouble( inform.getContent() );
						setDistanceToVehicleInFront(distance);
												
					} else block();
					
					step--;
					break;
					
			}
			
		}
		
	}
	
	
	
	/**
	 * VehicleProcessTurnBehaviour
	 * 
	 * Este es el comportamiento principal que implementa
	 * el contract net con el agente interseccion. Se encarga
	 * de gestionar la peticion de turno, enviarle los datos
	 * necesarios para que nos de una reserva, etc.
	 *
	 */	
	public class VehicleProcessTurnBehaviour extends Behaviour {

		private static final long serialVersionUID = 1L;
		private static final boolean VERBOSE = false;
		
		// Behaviour variables
		private int step = 0;
		private ACLMessage cfp, propose, proposal, inform;
		private String replyWith;
		
		// Templates
		private MessageTemplate CFPTpl = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.CFP),
					MessageTemplate.MatchConversationId("vehicleTurn"));
		
		private MessageTemplate reservationTpl = MessageTemplate.or(
					MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
					MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));
		
		
		@Override
		public void action() {
			
			switch (step) {
			
				case 0:
					
					cfp = myAgent.receive(CFPTpl);
					if ( cfp != null ) {
						
						if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] Turn CFP received.");
						
						replyWith = cfp.getReplyWith();
						propose = new ACLMessage(ACLMessage.PROPOSE);
						propose.addReceiver(cfp.getSender());
						propose.setInReplyTo(replyWith);
												
						// Do I have a reservation?
						if ( !isReserveDone() && !intersectionPassed() ) {
							
							// Estimated time of arrival
							double eta = getDistanceToTarget() / getCurrentSpeed();
							
							String cont = "";
							cont += "orig=" + getCurrentLane();
							cont += "dest=" + getTargetLane();
							cont += "eta=" + (int) (eta);
							cont += "speed=" + getCurrentSpeed();
							cont += "end";

							propose.setContent(cont);
							
							myAgent.send(propose);
							
							step++;
							
						} else {
							
							propose.setContent("pass");
							myAgent.send(propose);
							
							// I already have a reservation, jump to just update my position
							// without asking for a new one
							step += 2;
							
						}
					} else block();
					
					break;
		
				case 1:
					
					// Process reservation request response
					proposal = myAgent.receive(reservationTpl);
					
					if ( proposal != null ) {
						
						if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] PROPOSAL received from " + proposal.getSender().getLocalName());
						
						int performative = proposal.getPerformative();
						
						// Reservation success
						if ( performative == ACLMessage.ACCEPT_PROPOSAL ) {
							
							//System.out.println("[" + myAgent.getLocalName() + "] Reservation SUCCESS !");
							
							// If reservation is accepted the message will contain a list of all the points in my path
							String path = proposal.getContent();
							String[] points = path.split("#");
							
							for (String point : points ) {
								final float x = Float.parseFloat( point.substring( point.indexOf("x=")+2, point.indexOf("y=") ));
								final float y = Float.parseFloat( point.substring( point.indexOf("y=")+2 ));
								addToPath( new P(x, y));
								//if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] Added new point (" + x + ", " + y + ") to my path.");
							}
							setReserveDone(true);
							
						// Reservation denied	
						} else if ( performative == ACLMessage.REJECT_PROPOSAL ) {
							
							//System.out.println("[" + myAgent.getLocalName() + "] Reservation DENIED, decelerating.");
							applyRandomBrakingFactor();
						
						// Unexpected message	
						} else {
							if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] Unexpected reservation response.");
						}
						
						step++;
					}
					
					break;
					
				case 2:
										
					// Move
					double increment = getCurrentSpeed();
									
					// Initialize
					float carX  = getCurrentPosition().getX();
					float carY  = getCurrentPosition().getY();
					float destX = getTargetPosition().getX();
					float destY = getTargetPosition().getY();
									
					// Distance to target
					double distanceToTarget = getDistanceToTarget();
									
					// Check if vehicle is closing on target
					if ( increment > distanceToTarget ) {
						// The vehicle is too close, we have to change target position
						// to the next step in the path
						
						if ( !nextTarget() ) {
							//System.out.println("[" + myAgent.getLocalName() + "] Destination reached, agent terminated.");
							
							ACLMessage vehicleDelete = new ACLMessage(ACLMessage.REQUEST);
							vehicleDelete.setConversationId("vehicleDelete");
							AID im = new AID( getMyIntersectionManager(), AID.ISLOCALNAME );
							vehicleDelete.addReceiver(im);
							vehicleDelete.setContent("" + getDelay());
							myAgent.send(vehicleDelete);
							
							finish();
						}
						
					} else {
						// Vehicle has enough room to move
						if ( intersectionPassed() ){
							setCurrentSpeed(currentSpeed * pickupFactor);
							//System.out.println("[" + myAgent.getLocalName() + "] Intersection traversed, accelerating.");
						} else {
							// Check if vehicle ahead and adjust speed if needed
							if ((getDistanceToVehicleInFront() > 0) && (getDistanceToVehicleInFront() < 999999)){
								if ( getDistanceToVehicleInFront() < getSafeDistanceBetweenVehicles() ) {
									applyRandomBrakingFactor();
								} else {
									applyRandomPickupFactor();
								}
							}
						}
					}
					
					// Do move
					float proportion = (float) (currentSpeed / distanceToTarget);
									
					float newX = ((1 - proportion) * carX + proportion * destX);
					float newY = ((1 - proportion) * carY + proportion * destY);
					
					
					setCurrentPosition(new P(newX, newY));
					
					step++;
					break;
					
				case 3:
					// Send inform with vehicle's new position
					inform = new ACLMessage(ACLMessage.INFORM);
					inform.addReceiver(cfp.getSender());
					
					String cont = "";
					cont += "aid=" + myAgent.getLocalName();
					cont += "x=" + getCurrentPosition().getX();
					cont += "y=" + getCurrentPosition().getY();
					cont += "end";
					
					inform.setContent(cont);
					
					if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] Position update INFORM sent.");
					
					myAgent.send(inform);
															
					// Get ready for next turn
					myAgent.addBehaviour( new VehicleProcessTurnBehaviour() );
					
					step++;
					break;
			}
		}

		@Override
		public boolean done() {
			return (step == 4) ? true : false;
		}

	}
	
	
	
	
	
	
	
	
	
	// -------------------------------
	// 		Getters and setters
	// -------------------------------
	
	public P getCurrentPosition() {
		return currentPosition;
	}

	public void setCurrentPosition(P currentPosition) {
		this.currentPosition = currentPosition;
	}
	
	public void setX( float x ) {
		currentPosition.setX(x);
	}
	
	public void setY( float y ) {
		currentPosition.setY(y);
	}
	
	public float getX() {
		return getCurrentPosition().getX();
	}
	
	public float getY() {
		return getCurrentPosition().getY();
	}
	
	public P getTargetPosition() {
		return targetPosition;
	}

	public void setTargetPosition(P targetPosition) {
		this.targetPosition = targetPosition;
	}

	public float getCurrentSpeed() {
		return currentSpeed;
	}

	public void setCurrentSpeed(float speed) {
		if ( speed <= 0 ) {
			this.currentSpeed = 0.1f;
		} else if (speed > speedLimit) {
			this.currentSpeed = speedLimit;
		} else {
			this.currentSpeed = speed;
		}
	}

	public String getTargetLane() {
		return targetLane;
	}

	public void setTargetLane(String targetLane) {
		this.targetLane = targetLane;
	}

	public String getCurrentLane() {
		return currentLane;
	}

	public void setCurrentLane(String currentLane) {
		this.currentLane = currentLane;
	}
	
	public int getSpeedLimit() {
		return speedLimit;
	}

	public boolean isReserveDone() {
		return reserveDone;
	}

	public void setReserveDone(boolean reserveDone) {
		this.reserveDone = reserveDone;
	}
	

	public float getBrakingFactor() {
		return brakingFactor;
	}

	public void setBrakingFactor(float brakingFactor) {
		this.brakingFactor = brakingFactor;
	}

	public float getPickupFactor() {
		return pickupFactor;
	}

	public void setPickupFactor(float pickupFactor) {
		this.pickupFactor = pickupFactor;
	}
	
	public String getMyIntersectionManager() {
		return myIntersectionManager;
	}
	
	public void setMyIntersectionManager( String intersectionManager ) {
		this.myIntersectionManager = intersectionManager;
	}
	
	public boolean intersectionPassed() {
		return (targetPositionIndex > 1) ? true : false;
	}

	public String getRadarAgent() {
		return radarAgent;
	}

	public void setRadarAgent(String radarAgent) {
		this.radarAgent = radarAgent;
	}

	public long getRadarRequestPeriod() {
		return radarRequestPeriod;
	}

	public void setRadarRequestPeriod(long radarRequestPeriod) {
		this.radarRequestPeriod = radarRequestPeriod;
	}	

	public double getDistanceToVehicleInFront() {
		return distanceToVehicleInFront;
	}

	public void setDistanceToVehicleInFront(double distanceToVehicleInFront) {
		this.distanceToVehicleInFront = distanceToVehicleInFront;
	}
	
	public double getSafeDistanceBetweenVehicles() {
		return safeDistanceBetweenVehicles;
	}


	public void setSafeDistanceBetweenVehicles(double safeDistanceBetweenVehicles) {
		this.safeDistanceBetweenVehicles = safeDistanceBetweenVehicles;
	}

	public P getSpawnPosition() {
		return spawnPosition;
	}

	public void setSpawnPosition(P spawnPosition) {
		this.spawnPosition = spawnPosition;
	}
	
}
