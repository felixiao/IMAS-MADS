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
                            boolean success = info.LoadData("audit_risk.arff");
                            if(success)
                                addBehaviour(new SendMsgBehaviour("ImReady",Message,ACLMessage.INFORM,"ClassifierAgent"));
                            else
                                addBehaviour(new SendMsgBehaviour("NotReady",Message,ACLMessage.INFORM,"BrokerAgent"));
                            break;
                        case "Train":
                            for(int i=0;i<10;i++) {
                                Instances data = info.GetTrainData(300);
                                addBehaviour(new SendMsgBehaviour(data, "Train", Train, ACLMessage.REQUEST, "ClassifierAgent",i));
                            }
                            break;
                        case "Test":
                            int testAttrs = info.GetTestAttrs();
                            addBehaviour(new SendMsgBehaviour(testAttrs,"PreTest",PreTest,ACLMessage.REQUEST,"ClassifierAgent"));
                            break;
                        case "TestReady":
                            Instances testData = info.GetTestData(15);
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
            if(msgInform!=null){
                myLogger.log(Logger.INFO, getInfo()+ " Received <<<<<\n["+GetType(msgInform.getPerformative())+"] \t'"+msgInform.getContent()+"' <<<<< (" + msgInform.getSender().getLocalName()+")");
            }
        }
    }
}
