package urv.imas;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.util.Logger;
import weka.core.Instances;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

public class InformationAgent extends MyAgent {
    InfoHandler info;
    int nResponders;
    protected void setup() {
        super.myType="InformationAgent";
        int numOftrain= Integer.parseInt(ParseXML("configure.xml","numberoftrain"));
        int numOfval  = Integer.parseInt(ParseXML("configure.xml","numberofval"));
        int numOftest = Integer.parseInt(ParseXML("configure.xml","numberoftest"));
        int numOfeval = Integer.parseInt(ParseXML("configure.xml","numberofeval"));
        int testAttr  = Integer.parseInt(ParseXML("configure.xml","testattrs"),2);
        System.out.println("TestAttr "+testAttr);
        long seed     = Long.parseLong(ParseXML("configure.xml","randomseed"));
        info=new InfoHandler(numOftrain,numOfval,numOftest,numOfeval,testAttr,seed);
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
                if(msgRequest.getProtocol()==GetReady){
                    String datapath = msgRequest.getContent();
                    if (datapath != null) {
                        boolean success = info.LoadData(datapath);
                        if(success)
                            addBehaviour(new SendMsgBehaviour("ImReady",Message,ACLMessage.INFORM,"ClassifierAgent"));
                        else
                            addBehaviour(new SendMsgBehaviour("NotReady",Message,ACLMessage.INFORM,"BrokerAgent"));
                    }
                }
                else if(msgRequest.getProtocol()==Message) {
                    String content = msgRequest.getContent();
                    if (content != null) {
                        switch (content) {
                            case "Train":
                                Instances data = info.GetTrainData();
                                addBehaviour(new SendMsgBehaviour(data, "Train", Train, ACLMessage.REQUEST, "ClassifierAgent"));
                                break;
                            case "Test":
                                int testAttrs = info.GetTestAttrs();
                                DFAgentDescription[] agentsearched = SearchAgent("ClassifierAgent");
                                nResponders = agentsearched.length;
                                ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                // We want to receive a reply in 10 secs
                                msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                                msg.setContent("PreTest");
                                try {
                                    msg.setContentObject(testAttrs);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                addBehaviour(new ContractNetInitiator(this.getAgent(),msg){
                                    protected void handlePropose(ACLMessage propose, Vector v) {
                                        System.out.println("Agent "+propose.getSender().getName()+" proposed "+propose.getContent());
                                    }

                                    protected void handleRefuse(ACLMessage refuse) {
                                        System.out.println("Agent "+refuse.getSender().getName()+" refused");
                                    }

                                    protected void handleFailure(ACLMessage failure) {
                                        if (failure.getSender().equals(myAgent.getAMS())) {
                                            // FAILURE notification from the JADE runtime: the receiver
                                            // does not exist
                                            System.out.println("Responder does not exist");
                                        }
                                        else {
                                            System.out.println("Agent "+failure.getSender().getName()+" failed");
                                        }
                                        // Immediate failure --> we will not receive a response from this agent
                                        nResponders--;
                                    }

                                    protected void handleAllResponses(Vector responses, Vector acceptances) {
                                        if (responses.size() < nResponders) {
                                            // Some responder didn't reply within the specified timeout
                                            System.out.println("Timeout expired: missing "+(nResponders - responses.size())+" responses");
                                        }
                                        // Evaluate proposals.
                                        AID bestProposer = null;
                                        ACLMessage accept = null;
                                        Enumeration e = responses.elements();
                                        while (e.hasMoreElements()) {
                                            ACLMessage msg = (ACLMessage) e.nextElement();
                                            if (msg.getPerformative() == ACLMessage.PROPOSE) {
                                                ACLMessage reply = msg.createReply();
                                                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                                acceptances.addElement(reply);
                                                boolean proposal = Boolean.parseBoolean(msg.getContent());
                                                if (proposal) {
                                                    bestProposer = msg.getSender();
                                                    accept = reply;
                                                    System.out.println("Accepting proposal from responder "+bestProposer.getName());
                                                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                                }
                                            }
                                        }
                                    }

                                    protected void handleInform(ACLMessage inform) {
                                        System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
                                    }
                                });
                                addBehaviour(new SendMsgBehaviour(testAttrs,"PreTest",PreTest,ACLMessage.REQUEST,"ClassifierAgent"));
                                break;
                            case "TestReady":
                                Instances testData = info.GetTestData();
                                addBehaviour(new OneShotBehaviour() {
                                    @Override
                                    public void action() {
                                        ACLMessage m_reply = msgRequest.createReply();
                                        m_reply.setPerformative(ACLMessage.REQUEST);
                                        m_reply.setProtocol(Test);
                                        m_reply.setContent("Test");
                                        try {
                                            m_reply.setContentObject(testData);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        send(m_reply);
                                    }
                                });
                                break;
                        }
                    }
                }
            }
            if(msgInform!=null){
                myLogger.log(Logger.INFO, getInfo()+ " Received <<<<<\n["+GetType(msgInform.getPerformative())+"] \t'"+msgInform.getContent()+"' <<<<< (" + msgInform.getSender().getLocalName()+")");
            }
        }
    }
}
