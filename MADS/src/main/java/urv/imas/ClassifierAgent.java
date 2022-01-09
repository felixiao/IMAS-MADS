package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import weka.core.Instances;

public class ClassifierAgent extends MyAgent {
    public ClassifyHandler classifier;

    protected void setup() {
        super.myType="ClassifierAgent";

        super.setup();
        this.classifier=new ClassifyHandler(super.getLocalName(),(int)getArguments()[0],(int)getArguments()[1],(int)getArguments()[2],(long)getArguments()[3]);
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
                addBehaviour(new AutoReplyBehaviour(msgRequest));
                if(msgRequest.getProtocol()==Message){
                    String content = msgRequest.getContent();
                    if (content != null) {
                        switch (content) {
                            case "GetReady":
                                addBehaviour(new SendMsgBehaviour("GetReady",Message,ACLMessage.REQUEST,"ReasoningAgent"));
                                break;
                        }
                    }
                }
                else if(msgRequest.getProtocol()==Train) {
                    try {
                        Instances data = (Instances) msgRequest.getContentObject();
                        classifier.LoadData(data);
                        System.out.println("Received Data!!!!!!!!! Num of Instance: " + data.numInstances() + " Ready to Train!");
                        boolean success = classifier.Train();
                        if (success) addBehaviour(new SendMsgBehaviour(classifier.eval,"TrainedSuccess", TrainedSuccess, ACLMessage.REQUEST, "ReasoningAgent"));
                        else System.out.println("Not Match!");
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }
                else if(msgRequest.getProtocol()==PreTest) {
                    try {
                        int attrs = (int) msgRequest.getContentObject();
                        boolean fit = classifier.Fit(attrs);
                        if(fit)
                            addBehaviour(new SendMsgBehaviour("TestReady",Message,ACLMessage.REQUEST,"InformationAgent"));
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }
                else if(msgRequest.getProtocol()==Test) {
                    try {
                        Instances testData = (Instances) msgRequest.getContentObject();
                        double[] predictResult = classifier.Predict(testData);
                        if(predictResult!=null)
                            addBehaviour(new SendMsgBehaviour(predictResult,"TestSuccess",Result,ACLMessage.REQUEST,"ReasoningAgent"));
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
