package behaviours;

import java.util.ArrayList;
import java.util.List;

import agents.IntersectionManagerAgent;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class IMServeTurnBehaviour extends Behaviour {

	private static final long serialVersionUID = 1L;
	private static final boolean VERBOSE = false;
	
	// Behaviour variables
	private int step, turn;	
	private ACLMessage cfp, propose, acceptProposal, rejectProposal, inform;
	private int nVehicleAgents, nPropose, nInform;
	private List<AID> rejected = new ArrayList<AID>();
	
	// Templates
	private MessageTemplate CFPTpl;
	private MessageTemplate InformTpl = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
	
	
	public IMServeTurnBehaviour( int turn ) {
		
		this.step 			= 0;
		this.nVehicleAgents = 0;
		this.nPropose 		= 0;
		this.nInform 		= 0;
		this.turn 			= turn;
		
	}
	
	@Override
	public void action() {
		
		switch (step) {
		
			case 0:
				// Start CFP with Vehicle Agents
				cfp = new ACLMessage(ACLMessage.CFP);
				
				for ( String aid : ((IntersectionManagerAgent) myAgent).getVehicleAgents().keySet() ) {//FIXME: null pointer si no hay vehiculos
					cfp.addReceiver( new AID(aid, AID.ISLOCALNAME));
					nVehicleAgents++;
				}
				
				cfp.setConversationId("vehicleTurn");
				cfp.setReplyWith( Integer.toString(turn) );
				
				if(VERBOSE) System.out.println("[" + getBehaviourName() + "] CFP sent to " + nVehicleAgents + " vehicle agents.");
				
				myAgent.send(cfp);				
				
				CFPTpl = MessageTemplate.and(
						MessageTemplate.MatchPerformative( ACLMessage.PROPOSE ), 
						MessageTemplate.MatchInReplyTo( Integer.toString(turn) ));
				
				step++;
				break;
				
			case 1:
				// Await and collect responses
				propose = myAgent.receive(CFPTpl);
				
				if( propose != null ){
					
					if(VERBOSE) System.out.println("[" + getBehaviourName() + "] PROPOSE received from VehicleAgent \"" + propose.getSender().getLocalName() + "\"");
					
					if ( !propose.getContent().equals("pass") ) {
																
						// Extract initialization data from registration message
						String cont = propose.getContent();
						final String orig = cont.substring( cont.indexOf("orig=")+5, cont.indexOf("dest=") );
						final String dest = cont.substring( cont.indexOf("dest=")+5, cont.indexOf("eta=") );
						final int eta = Integer.parseInt( cont.substring(cont.indexOf("eta=")+4, cont.indexOf("speed=") ));
						final float currentSpeed = Float.parseFloat( cont.substring(cont.indexOf("speed=")+6, cont.indexOf("end") ));
						
						
						// Issue a reservation request to the intersection manager
						String path = ((IntersectionManagerAgent) myAgent).reservePath( propose.getSender().getLocalName(), orig, dest, eta, currentSpeed);
						
						if ( path != null){
							
							// Send path ID to VehicleAgent
							acceptProposal = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							acceptProposal.addReceiver(propose.getSender());
							acceptProposal.setContent(path);
							
							myAgent.send(acceptProposal);
							
						} else {
							rejected.add(propose.getSender());
						}
					}
					
					nPropose++;
					if ( nPropose >= nVehicleAgents ){
						step++;
					}
				} else block();
				break;
				
			case 2:
								
				// Send reject proposal vehicle agents whose reservation was rejected
				if (rejected.size() > 0) {
					rejectProposal = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
					rejectProposal.setConversationId("vehicleTurn");
					for ( AID aid : rejected ) {
						rejectProposal.addReceiver(aid);
					}
					rejectProposal.setConversationId( Integer.toString(turn) );
					myAgent.send(rejectProposal);
				}
				
				step++;
				break;
				
			case 3:
				// Get updated vehicle position
				inform = myAgent.receive(InformTpl);
				
				if ( inform != null ) {
					
					if(VERBOSE) System.out.println("[" + getBehaviourName() + "] INFORM received from " + inform.getSender().getLocalName());
					
					String cont = inform.getContent();
					final String aid = cont.substring( cont.indexOf("aid=")+4, cont.indexOf("x=") );
					final float x = Float.parseFloat( cont.substring( cont.indexOf("x=")+2, cont.indexOf("y=") ));
					final float y = Float.parseFloat( cont.substring(cont.indexOf("y=")+2, cont.indexOf("end") ));
					
					((IntersectionManagerAgent) myAgent).updateVehicle(aid, x, y);
					
					nInform++;
					if ( nInform >= nVehicleAgents) {
						
						// Increment the last processed turn so that the next one can start
						((IntersectionManagerAgent) myAgent).incrementLastProcessedTurn();

						if(VERBOSE) System.out.println("/*****************************************************/");			
						step++;
					}
				} else block();
				
				break;

		}
		
	}

	@Override
	public boolean done() {
		return (step == 4) ? true : false;
	}

}
