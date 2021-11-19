package urv.imas;

import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

public class BrokerAgent extends MyAgent {
    protected void setup() {
        super.myType="BrokerAgent";
        super.setup();
        addBehaviour(new WaitAndReply());
    }
    private class WaitAndReply extends CyclicBehaviour{
        MessageTemplate filterMsg_Inform = null;
        MessageTemplate filterMsg_Request = null;
        public WaitAndReply(){
            filterMsg_Request = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            filterMsg_Inform = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
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
                        case "GetReady":
                            addBehaviour(new SendMsgBehaviour("GetReady",Message,ACLMessage.REQUEST,"InformationAgent"));
                            addBehaviour(new SendMsgBehaviour("GetReady",Message,ACLMessage.REQUEST,"ClassifierAgent"));
                            break;
                        case "Train":
                            addBehaviour(new SendMsgBehaviour("Train",Message,ACLMessage.REQUEST,"InformationAgent"));
                            break;
                    }
                }
            }
            if(msgInform!=null){
                myLogger.log(Logger.INFO, getInfo()+ " Received <<<<<\n["+GetType(msgInform.getPerformative())+"] \t'"+msgInform.getContent()+"' <<<<< (" + msgInform.getSender().getLocalName()+")");
            }
        }
    }
}
