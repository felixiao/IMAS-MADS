package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
public class ReasoningAgent extends MyAgent {
    protected void setup() {
        super.myType = "ReasoningAgent";
        super.setup();
        addBehaviour(new WaitAndReply());
    }
    private class WaitAndReply extends CyclicBehaviour {
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
                    switch (content) {
                        case "GetReady":
                            break;
                    }
                }
            }
            if(msgInform!=null){
                String content = msgInform.getContent();
                if (content != null) {
                    switch (content) {
                        case "ImReady":
                            addBehaviour(new SendMsgBehaviour(content,Message,ACLMessage.REQUEST,"UserAgent"));
                            break;
                    }
                }
            }
        }
    }
}
