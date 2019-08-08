package behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import agents.RadarAgent;
import intersection.P;

public class RadarListenerBehaviour extends CyclicBehaviour {

	private static final long serialVersionUID = 1L;
	private static final boolean VERBOSE = false;
	
	P currentPosition, targetPosition;
	double distanceToCarInFront;
	
	private MessageTemplate radarTpl = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
				MessageTemplate.MatchConversationId("radarRequest"));
	@Override
	public void action() {
		
		// Wait for a radar request from a vehicle
		ACLMessage request = myAgent.receive(radarTpl);
		
		if ( request != null ) {
			if(VERBOSE) System.out.println("[" + getBehaviourName() + "] Radar request received from vehicle ID=" + request.getSender().getLocalName());
			
			// Extract initialization data from request message
			String cont = request.getContent();
			final String vehicleID = cont.substring( cont.indexOf("id=")+3, cont.indexOf("x=") );
			final float currentX = Float.parseFloat( cont.substring( cont.indexOf("x=")+2, cont.indexOf("y=") ) );
			final float currentY = Float.parseFloat( cont.substring( cont.indexOf("y=")+2, cont.indexOf("tx=") ) );
			final float targetX = Float.parseFloat( cont.substring( cont.indexOf("tx=")+3, cont.indexOf("ty=") ) );
			final float targetY = Float.parseFloat( cont.substring( cont.indexOf("ty=")+3, cont.indexOf("end") ) );
			
			currentPosition = new P( currentX, currentY );
			targetPosition  = new P( targetX , targetY  );
			
			((RadarAgent) myAgent).updateCarEntry(vehicleID, targetPosition, currentPosition);
			distanceToCarInFront = ((RadarAgent) myAgent).getDistanceToCarInFront(vehicleID);
			
			ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
			inform.setConversationId("radarRequest");
			inform.addReceiver(request.getSender());
			inform.setContent( String.valueOf(distanceToCarInFront));
			myAgent.send(inform);
			
		} else block();
		
	}

}
