package urv.imas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.concurrent.CyclicBarrier;

public class BrokerAgent extends Agent {
    /*****************************************************************
     Common code for all agents
     *****************************************************************/
    private final String myType = "BrokerAgent";
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
            addBehaviour(new WaitAndReplyBehaviour());
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, getInfo()+" - Cannot register with DF", e);
            doDelete();
        }
    }
    /*****************************************************************
     Agent specific codes
     *****************************************************************/
    private class WaitAndReplyBehaviour extends CyclicBehaviour{
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if(msg != null) {
                ACLMessage reply = msg.createReply();
                if(msg.getPerformative()== ACLMessage.INFORM){
                    String content = msg.getContent();
                    if (content != null){
                        myLogger.log(Logger.INFO, getInfo() + " - Received ["+msg.getPerformative()+"] '"+content+"' from (" + msg.getSender().getLocalName()+")");
                        switch (content){
                            case "GetReady":
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent("GetReady-Received");
                                // wait for 1 sec to send msg to info agent and classifier agent
                                addBehaviour(new WakerBehaviour(myAgent,1000) {
                                    @Override
                                    protected void handleElapsedTimeout() {
                                        System.out.println("HandleElapsedTimeout !");
                                    }

                                    @Override
                                    protected void onWake() {
                                        ACLMessage _msg = new ACLMessage(ACLMessage.INFORM);
                                        _msg.setContent("GetReady");
                                        AID msgTo = SearchAgent("InformationAgent")[0].getName();
                                        _msg.addReceiver(msgTo);
                                        send(_msg);
                                        myLogger.log(Logger.INFO,getInfo()+" Send ["+_msg.getPerformative()+"] 'GetReady' to ("+msgTo.getLocalName()+")");
                                    }
                                });
                                addBehaviour(new OneShotBehaviour() {
                                    @Override
                                    public void action() {
                                        ACLMessage _msg = new ACLMessage(ACLMessage.INFORM);
                                        _msg.setContent("GetReady");
                                        AID msgTo = SearchAgent("ClassifierAgent")[0].getName();
                                        _msg.addReceiver(msgTo);
                                        send(_msg);
                                        myLogger.log(Logger.INFO,getInfo()+" Send ["+_msg.getPerformative()+"] 'GetReady' to ("+msgTo.getLocalName()+")");
                                    }
                                });
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
}
