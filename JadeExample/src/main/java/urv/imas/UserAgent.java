package urv.imas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import urv.imas.behaviours.FSMAgent;

public class UserAgent extends Agent {
    /*****************************************************************
     Common code for all agents
     *****************************************************************/
    private final String myType = "UserAgent";
    private String getInfo(){
        return String.format("[%s - %s]:\n",myType,getLocalName());
    }
    private final Logger myLogger = Logger.getMyLogger(getClass().getName());
    protected DFAgentDescription[] SearchAgent(String type) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd  = new ServiceDescription();
        sd.setType( type );
        dfd.addServices(sd);

        DFAgentDescription[] result = new DFAgentDescription[0];
        try {
            result = DFService.search(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        if (result.length>0)
            for (DFAgentDescription re: result) {
                System.out.println("Searched Results: " + re.getName().getLocalName() );
            }
        return result;
    }
    /*****************************************************************
     Agent specific codes
     *****************************************************************/
    private static final String STATE_Start = "Starting";
    private static final String STATE_Check = "Checking";
    private static final String STATE_Prepare = "Preparing";
    private static final String STATE_Ready = "Ready";
    private static final String STATE_Predict = "Predict";
    private static final String STATE_Show = "Showing";
    protected void setup() {
//        super.setup();
        myLogger.log(Logger.INFO,getInfo()+" Start!");
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(myType);
        sd.setName(getName());
        sd.setOwnership("TILAB");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this,dfd);
            addBehaviour(new WaitAndReply());
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, getInfo()+" - Cannot register with DF", e);
            doDelete();
        }
    }

    // Wait and reply
    private class WaitAndReply extends CyclicBehaviour{
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if(msg != null) {
                ACLMessage reply = msg.createReply();
                if(msg.getPerformative()== ACLMessage.INFORM){
                    String content = msg.getContent();
                    if (content != null){
                        myLogger.log(Logger.INFO, getInfo()+ " Received ["+msg.getPerformative()+"] '"+content+"' from (" + msg.getSender().getLocalName()+")");
                        switch (content){
                            case "Start":
                                myAgent.addBehaviour(new CheckingBehaviour());
                                break;
                            case "GetReady":
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent("GetReady-Received");
                                if(msg.getSender().getClass().getName()=="ReasoningAgent"){
                                    myLogger.log(Logger.INFO,"Full Loop! System is Ready!");
                                }
                                break;
                            case "ImReady":
                                myAgent.addBehaviour(new ShowBehaviour());
                                break;
                        }
                        if(reply.getContent()!=null) {
                            send(reply);
                            myLogger.log(Logger.INFO, getInfo() + " Send [" + reply.getPerformative() + "] '" + reply.getContent() + "' to (" + reply.getSender().getLocalName() + ")");
                        }
                    }
                }
            }
        }
    }
    private class ShowBehaviour extends OneShotBehaviour{
        @Override
        public void action() {
            myLogger.log(Logger.INFO,getInfo()+" at State Show!");
        }
    }

    // start behaviour
    private class CheckingBehaviour extends Behaviour{
        public boolean finish;
        @Override
        public void action() {
            myLogger.log(Logger.INFO, getInfo()+ " Change State to Check!");
            ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
            reply.setContent("GetReady");
//            reply.setReplyWith("broker1");
            AID msgTo = SearchAgent("BrokerAgent")[0].getName();
            reply.addReceiver(msgTo);
            send(reply);
            myLogger.log(Logger.INFO,getInfo()+" Send ["+reply.getPerformative()+"] 'GetReady' to ("+msgTo.getLocalName()+")");
            finish=true;
        }

        @Override
        public boolean done() {
            return finish;
        }
    }



}
