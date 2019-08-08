package behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import agents.IntersectionManagerAgent;
import intersection.Lane;

public class IMVehicleTrackingBehaviour extends CyclicBehaviour {

	private static final long serialVersionUID = 1L;
	private static final boolean VERBOSE = false;
	
	private String lastLaneId = "";
	
	private MessageTemplate vehicleRegisterTpl = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
				MessageTemplate.or(
					MessageTemplate.MatchConversationId("vehicleDelete"),
					MessageTemplate.MatchConversationId("vehicleRegister")));
	
	@Override
	public void action() {
		
		// Wait for a registering message from a vehicle
		ACLMessage msg = myAgent.receive(vehicleRegisterTpl);
		
		if ( msg != null ) {

			if (msg.getConversationId().equals("vehicleRegister")) {
			
				if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] processing VehicleRegister request from agent " + msg.getSender().getLocalName() + ".");
				
				if ( ((IntersectionManagerAgent) myAgent).isOpen() ) {
					
					ACLMessage reply = new ACLMessage( ACLMessage.AGREE );
					
					reply.addReceiver( msg.getSender() );
					reply.setConversationId( msg.getConversationId() );
					
					// Obtain a random Lane from the intersection manager
					Lane randomLane = ((IntersectionManagerAgent) myAgent).getRandomIncomingLane();
					
					while( randomLane.getId().equals(lastLaneId)) {
						randomLane = ((IntersectionManagerAgent) myAgent).getRandomIncomingLane();
					}
					
					lastLaneId = randomLane.getId();
					
					// Populate reply content with origin and destination coordinates
					// for the random lane selected
					String replyContent = "";
					replyContent += "orig=" + randomLane.getId();
					replyContent += "xO=" + randomLane.getStartPosition().getX();
					replyContent += "yO=" + randomLane.getStartPosition().getY();
					replyContent += "xD=" + randomLane.getEndPosition().getX();
					replyContent += "yD=" + randomLane.getEndPosition().getY();
					replyContent += "dest=" + ((IntersectionManagerAgent) myAgent).getRandomOutgoingLane(randomLane.getId().substring(0, 1)).getId();
					replyContent += "end";
					
					// Add reply content
					reply.setContent( replyContent );
					
					myAgent.send(reply);
					
					// Add vehicle to the IM vehicleAgent list
					((IntersectionManagerAgent) myAgent).addVehicle( msg.getSender().getLocalName(),  randomLane.getStartPosition());
					
				} else {
					
					ACLMessage reply = new ACLMessage( ACLMessage.REFUSE );
					
					reply.addReceiver( msg.getSender() );
					reply.setConversationId( msg.getConversationId() );
									
					myAgent.send(reply);
					
				}
			
			} else if ( msg.getConversationId().equals("vehicleDelete")) {

				long delay = Long.parseLong(msg.getContent());
				((IntersectionManagerAgent) myAgent).addDelay(delay);
				
				if(VERBOSE) System.out.println("[" + myAgent.getLocalName() + "] processing VehicleDelete request from agent " + msg.getSender().getLocalName() + ".");
				
				((IntersectionManagerAgent) myAgent).removeVehicle(msg.getSender().getLocalName());
				
			} else {
				if(VERBOSE) System.out.println("[" + getBehaviourName() + "] ERROR: Unexpected vehicle tracking message.");
			}
			
			
		} else block();
	}

}
