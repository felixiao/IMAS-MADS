package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.*;

public class ReasoningAgent extends MyAgent {
    private int countImReady = 0;
    private int countTrained = 0;
    private HashMap<String,int[]> testResults=new HashMap<String, int[]>();
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
                if(msgRequest.getProtocol()==Message){
                    String content = msgRequest.getContent();
                    if (content != null) {
                        switch (content) {
                            case "GetReady":
                                break;
                        }
                    }
                }else if(msgRequest.getProtocol()==Result){
                    try {
                        int[] testResult = (int[]) msgRequest.getContentObject();
                        if(!testResults.containsKey(msgRequest.getSender().getLocalName()))
                            testResults.put(msgRequest.getSender().getLocalName(),testResult);
                        System.out.println(testResults.toString());
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }

            }
            if(msgInform!=null){
                String content = msgInform.getContent();
                if (content != null) {
                    switch (content) {
                        case "ImReady":
                            countImReady++;
                            System.out.println("Classifier Ready count "+countImReady);
                            if(countImReady==10){
                                addBehaviour(new SendMsgBehaviour(content,Message,ACLMessage.REQUEST,"UserAgent"));
                            }
                            break;
                        case "TrainedSuccess":
                            countTrained++;
                            System.out.println("Classifier Trained Count "+countTrained);
                            if(countTrained==10) {
                                addBehaviour(new SendMsgBehaviour(content, Message, ACLMessage.REQUEST, "UserAgent"));
                            }
                            break;
                    }
                }
            }
        }
    }
}
