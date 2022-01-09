package urv.imas;

import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class BrokerAgent extends MyAgent {
    private String datapath;
    private int numOfClassifier = 10;
    private int numOfAttributes = 6;
    private int numOfTrain =225;
    private int numOfVal   =75;
    private long seed = 1;
    protected void setup() {
        super.myType="BrokerAgent";
        super.setup();
        datapath = ParseXML("configure.xml","path");
        numOfClassifier = Integer.parseInt(ParseXML("configure.xml","numberofclassifier"));
        numOfAttributes = Integer.parseInt(ParseXML("configure.xml","numberofattributes"));
        numOfTrain = Integer.parseInt(ParseXML("configure.xml","numberoftrain"));
        numOfVal   = Integer.parseInt(ParseXML("configure.xml","numberofval"));
        seed = Long.parseLong(ParseXML("configure.xml","randomseed"));
        for(int i =0; i<numOfClassifier;i++) {
            try {
                AgentController classfiers = getContainerController().createNewAgent("classifier"+i,"urv.imas.ClassifierAgent",new Object[]{numOfAttributes,numOfTrain,numOfVal,seed});

                classfiers.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
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
                        case "NotReady":
                            addBehaviour(new SendMsgBehaviour(datapath,GetReady,ACLMessage.REQUEST,"InformationAgent"));
                        case "GetReady":
                            addBehaviour(new SendMsgBehaviour(datapath,GetReady,ACLMessage.REQUEST,"InformationAgent"));
                            addBehaviour(new SendMsgBehaviour("GetReady",Message,ACLMessage.REQUEST,"ClassifierAgent"));
                            break;
                        case "Train":
                            addBehaviour(new SendMsgBehaviour("Train",Message,ACLMessage.REQUEST,"InformationAgent"));
                            break;
                        case "Test":

                            addBehaviour(new SendMsgBehaviour("Test",Message,ACLMessage.REQUEST,"InformationAgent"));
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
