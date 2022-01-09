package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetResponder;
import weka.core.Instances;

public class ClassifierAgent extends MyAgent {
    public ClassifyHandler classifier;

    protected void setup() {
        super.myType="ClassifierAgent";

        super.setup();
        Object[] args = getArguments();
        if(args!=null) {
            this.classifier = new ClassifyHandler(super.getLocalName(), (int) args[0], (int) args[1], (int) args[2], (long) args[3]);
            addBehaviour(new WaitAndReply());
        }
        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP) );
        addBehaviour(new ContractNetResponder(this,template){
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
                System.out.println("Agent "+getLocalName()+": CFP received from "+cfp.getSender().getName()+". Action is "+cfp.getContent());
                boolean proposal = false;
                try {
                    proposal = classifier.Fit((int)cfp.getContentObject());
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                if (proposal) {
                    // We provide a proposal
                    System.out.println("Agent "+getLocalName()+": Proposing "+proposal);
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);
                    propose.setContent(String.valueOf(proposal));
                    return propose;
                }
                else {
                    // We refuse to provide a proposal
                    System.out.println("Agent "+getLocalName()+": Refuse");
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.REFUSE);
                    propose.setContent(String.valueOf(proposal));
                    return propose;
                }
            }
        });
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
