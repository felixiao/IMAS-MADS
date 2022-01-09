package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

public class UserAgent extends MyAgent {
    private boolean m_autotest=false;
    private boolean m_autostart = false;
    private boolean m_readyToTest= false;
    protected void setup() {
        super.myType="UserAgent";
        super.setup();
        m_autotest = Boolean.parseBoolean(ParseXML("configure.xml","autotest"));
        m_autostart = Boolean.parseBoolean(ParseXML("configure.xml","autostart"));
        System.out.println("m_autotest = "+m_autotest);
        addBehaviour(new WaitAndReply());
        if(m_autostart)addBehaviour(new SendMsgBehaviour("GetReady",Message,ACLMessage.REQUEST,"BrokerAgent"));
    }

    // Wait and reply
    private class WaitAndReply extends CyclicBehaviour{
        MessageTemplate filterMsg_Inform = null;
        MessageTemplate filterMsg_Request = null;
        public WaitAndReply(){
            filterMsg_Request = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            filterMsg_Inform = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        }
        @Override
        public void action() {

            ACLMessage msgInform = receive(filterMsg_Inform);
            ACLMessage msgRequest = receive(filterMsg_Request);
            if(msgRequest != null) {
                addBehaviour(new AutoReplyBehaviour(msgRequest));
                String content = msgRequest.getContent();
                if (content != null) {
                    switch (content){
                        case "Start":
                            addBehaviour(new SendMsgBehaviour("GetReady",Message,ACLMessage.REQUEST,"BrokerAgent"));
                            break;
                        case "ImReady":
                            myLogger.log(Logger.INFO,"Full Loop! System is Ready!");
                            addBehaviour(new ShowBehaviour());
                            addBehaviour(new SendMsgBehaviour("Train",Message,ACLMessage.REQUEST,"BrokerAgent"));
                            break;
                        case "TrainedSuccess":
                            myLogger.log(Logger.INFO,"System is Trained! Ready to Test");
                            m_readyToTest= true;
                            System.out.println("m_autotest = "+m_autotest);
                            if(m_autotest) addBehaviour(new SendMsgBehaviour("Test",Message,ACLMessage.REQUEST,"BrokerAgent"));
                            break;
                        case "Test":
                            if(m_readyToTest)
                                addBehaviour(new SendMsgBehaviour("Test",Message,ACLMessage.REQUEST,"BrokerAgent"));
                            break;
                    }
                }
            }
            if(msgInform!=null){
                myLogger.log(Logger.INFO, getInfo()+ " Received <<<<<\n["+GetType(msgInform.getPerformative())+"] \t'"+msgInform.getContent()+"' <<<<< (" + msgInform.getSender().getLocalName()+")");
            }
        }
    }
    private class ShowBehaviour extends OneShotBehaviour{
        @Override
        public void action() {
            myLogger.log(Logger.INFO,getInfo()+" at State Show!");
        }
    }


}
