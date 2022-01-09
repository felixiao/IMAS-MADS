package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import weka.core.Instances;

import java.io.IOException;

public class InformationAgent extends MyAgent {
    InfoHandler info;
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
