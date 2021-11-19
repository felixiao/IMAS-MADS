package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import weka.core.Instances;

public class InformationAgent extends MyAgent {
    InfoHandler info;
    protected void setup() {
        super.myType="InformationAgent";
        info=new InfoHandler();
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
                            info.LoadData("audit_risk.arff");
                            addBehaviour(new SendMsgBehaviour("ImReady",Message,ACLMessage.INFORM,"ClassifierAgent"));
                            break;
                        case "Train":
                            Instances data = info.GetDataInstances(300);
                            addBehaviour(new SendMsgBehaviour(data,"Train",Instances,ACLMessage.REQUEST,"ClassifierAgent"));
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
