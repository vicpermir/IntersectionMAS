package behaviours;

import agents.IntersectionManagerAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

public class IMTurnRotationBehaviour extends TickerBehaviour {
	
	private static final long serialVersionUID = 1L;
	private static final boolean VERBOSE = false;
	private int currentTurn;
	
	public IMTurnRotationBehaviour(Agent a, long period) {
		super(a, period);
		
		currentTurn = 0;
		
	}


	@Override
	protected void onTick() {
		
		if (((IntersectionManagerAgent) myAgent).getVehicleAgents().size() > 0){
			
			if ( ((IntersectionManagerAgent) myAgent).getLastProcessedTurn() == (currentTurn-1) ) {
				if(VERBOSE) System.out.println("/*****************************************************/");
				if(VERBOSE) System.out.println("[" + getBehaviourName() + "] Serving simulation TURN = " + currentTurn + ".");
				
				myAgent.addBehaviour( new IMServeTurnBehaviour(currentTurn) );
			
				currentTurn++;
			} else block();
			
		}

	}

}
