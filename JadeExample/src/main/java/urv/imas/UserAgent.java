package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

public class UserAgent extends MyAgent {
    protected void setup() {
        super.myType="UserAgent";
        super.setup();
        addBehaviour(new WaitAndReply());
    }

    // Wait and reply
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
                        case "Start":
                            myAgent.addBehaviour(new SendMsgBehaviour("GetReady",Message,ACLMessage.REQUEST,"BrokerAgent"));
                            break;
                        case "ImReady":
                            myLogger.log(Logger.INFO,"Full Loop! System is Ready!");
                            myAgent.addBehaviour(new ShowBehaviour());
                            myAgent.addBehaviour(new SendMsgBehaviour("Train",Message,ACLMessage.REQUEST,"BrokerAgent"));
                            break;
                    }
                }
            }
            if(msgInform!=null){
                myLogger.log(Logger.INFO, getInfo()+ " Received <<<<<\n["+GetType(msgInform.getPerformative())+"] \t'"+msgInform.getContent()+"' <<<<< (" + msgInform.getSender().getLocalName()+")");
            }
        }
    }
    private class ShowBehaviour extends OneShotBehaviour{
        @Override
        public void action() {
            myLogger.log(Logger.INFO,getInfo()+" at State Show!");
        }
    }


}
