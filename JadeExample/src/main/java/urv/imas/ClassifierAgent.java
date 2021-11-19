package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import weka.core.Instances;

public class ClassifierAgent extends MyAgent {
    private ClassifyHandler classifier;
    protected void setup() {
        super.myType="ClassifierAgent";
        classifier=new ClassifyHandler();
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
//            ACLMessage msgRequestInstances = myAgent.receive(MessageTemplate.and(filterMsg_Request,MessageTemplate.MatchProtocol(Instances)));
//            if(msgRequestInstances!=null){
//                System.out.println("Receive MsgRequestInstances");
//                addBehaviour(new AutoReplyBehaviour(msgRequestInstances));
//                Instances data = null;
//                try {
//                    data = (Instances)msgRequestInstances.getContentObject();
//                    System.out.println("Received Data!!!!!!!!! Num of Instance: "+data.numInstances()+" Ready to Train!");
//                } catch (UnreadableException e) {
//                    e.printStackTrace();
//                }
//            }
            if(msgRequest != null) {
                System.out.println("Receive MsgRequest");
                addBehaviour(new AutoReplyBehaviour(msgRequest));
                if(msgRequest.getProtocol()==Message){
                    String content = msgRequest.getContent();
                    if (content != null) {
                        switch (content) {
                            case "Train":
                                try {
                                    Instances data = (Instances) msgRequest.getContentObject();
                                    System.out.println("Received Data!!!!!!!!! Num of Instance: "+data.numInstances()+" Ready to Train!");
                                } catch (UnreadableException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case "GetReady":
                                addBehaviour(new SendMsgBehaviour("GetReady",Message,ACLMessage.REQUEST,"ReasoningAgent"));
                                break;
                        }
                    }
                }
                else if(msgRequest.getProtocol()==Instances){
                    Instances data = null;
                    try {
                        data = (Instances) msgRequest.getContentObject();
                        System.out.println("Received Data!!!!!!!!! Num of Instance: "+data.numInstances()+" Ready to Train!");
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(msgInform!=null) {
                String content = msgInform.getContent();
                if (content != null) {
                    switch (content) {
                        case "ImReady":
                            addBehaviour(new SendMsgBehaviour(content,Message,ACLMessage.INFORM,"ReasoningAgent"));
                            break;
                    }
                }
            }
        }
    }

}
