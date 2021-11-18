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
import jade.lang.acl.MessageTemplate;
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
    protected class SendMsgBehaviour extends OneShotBehaviour{
        String m_content;
        int m_type;
        String m_to;
        public SendMsgBehaviour(String Content,int ACLMessageType,String to){
            m_content= Content;
            m_type = ACLMessageType;
            m_to = to;
        }
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(m_type);
            msg.setContent(m_content);
            AID msgTo = SearchAgent(m_to)[0].getName();
            msg.addReceiver(msgTo);
            send(msg);
            myLogger.log(Logger.INFO,getInfo()+" Send ["+msg.getPerformative()+"] '"+m_content+"' to ("+msgTo.getLocalName()+")");
        }
    }
    protected class AutoReplyBehaviour extends OneShotBehaviour{
        ACLMessage m_reply;
        public AutoReplyBehaviour(ACLMessage msg){
            m_reply = msg.createReply();
            m_reply.setContent(msg.getContent()+"-Received!");
            m_reply.setPerformative(ACLMessage.INFORM);
            myLogger.log(Logger.INFO, getInfo()+ " Received ["+msg.getPerformative()+"] '"+msg.getContent()+"' from (" + msg.getSender().getLocalName()+")");
        }
        @Override
        public void action() {
            send(m_reply);
            myLogger.log(Logger.INFO, getInfo() + " Send [" + m_reply.getPerformative() + "] '" + m_reply.getContent() + "' to (" + m_reply.getSender().getLocalName() + ")");
        }
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
        MessageTemplate filterMsg_Inform = null;
        MessageTemplate filterMsg_Request = null;
        public WaitAndReply(){
            filterMsg_Request = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchLanguage("English"));
            filterMsg_Inform = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchLanguage("English"));
        }

        @Override
        public void action() {
            ACLMessage msgInform = myAgent.receive(filterMsg_Inform);
            ACLMessage msgRequest = myAgent.receive(filterMsg_Request);
            if(msgRequest != null) {
                addBehaviour(new AutoReplyBehaviour(msgRequest));
                String content = msgRequest.getContent();
                if (content != null) {
                    switch (content){
                        case "Start":
                            myAgent.addBehaviour(new CheckingBehaviour());
                            break;
                        case "ImReady":
                            myLogger.log(Logger.INFO,"Full Loop! System is Ready!");
                            myAgent.addBehaviour(new ShowBehaviour());
                            myAgent.addBehaviour(new SendMsgBehaviour("Train",ACLMessage.REQUEST,"BrokerAgent"));
                            break;
                    }
                }
            }
            if(msgInform!=null){
                myLogger.log(Logger.INFO, getInfo()+ " Received ["+msgInform.getPerformative()+"] '"+msgInform.getContent()+"' from (" + msgInform.getSender().getLocalName()+")");
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
    private class CheckingBehaviour extends OneShotBehaviour{
        public boolean finish;
        @Override
        public void action() {
            addBehaviour(new SendMsgBehaviour("GetReady",ACLMessage.REQUEST,"BrokerAgent"));
        }

    }

}
