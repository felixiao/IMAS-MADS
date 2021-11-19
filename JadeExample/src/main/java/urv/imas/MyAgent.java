package urv.imas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;
import weka.core.Instances;
import java.io.IOException;

public class MyAgent extends Agent {
    public final static String Message ="Message";
    public final static String Instances ="Instances";

    public String GetType(int type){
        switch (type){
            case ACLMessage.INFORM:
                return "INFORM";
            case ACLMessage.REQUEST:
                return "REQUEST";
            case ACLMessage.CFP:
                return "CFP";
            case ACLMessage.FAILURE:
                return "FAILURE";
            case ACLMessage.PROPOSE:
                return "PROPOSE";
            case ACLMessage.AGREE:
                return "AGREE";
            case ACLMessage.REFUSE:
                return "REFUSE";
            case ACLMessage.ACCEPT_PROPOSAL:
                return "ACCEPT_PROPOSAL";
            case ACLMessage.REJECT_PROPOSAL:
                return "REJECT_PROPOSAL";
            case ACLMessage.CANCEL:
                return "CANCEL";
            case ACLMessage.CONFIRM:
                return "CONFIRM";
            case ACLMessage.DISCONFIRM:
                return "DISCONFIRM";
            case ACLMessage.INFORM_IF:
                return "INFORM_IF";
            case ACLMessage.INFORM_REF:
                return "INFORM_REF";
            case ACLMessage.NOT_UNDERSTOOD:
                return "NOT_UNDERSTOOD";
            case ACLMessage.PROPAGATE:
                return "PROPAGATE";
            case ACLMessage.PROXY:
                return "PROXY";
            case ACLMessage.QUERY_IF:
                return "QUERY_IF";
            case ACLMessage.QUERY_REF:
                return "QUERY_REF";
            case ACLMessage.REQUEST_WHEN:
                return "REQUEST_WHEN";
            case ACLMessage.REQUEST_WHENEVER:
                return "REQUEST_WHENEVER";
            case ACLMessage.SUBSCRIBE:
                return "SUBSCRIBE";
            case ACLMessage.UNKNOWN:
                return "UNKNOWN";
        }
        return "TYPE";
    }
    protected String myType = "MyAgent";
    protected String getInfo(){
        return String.format("[%s - %s]:",myType,getLocalName());
    }
    protected final Logger myLogger = Logger.getMyLogger(getClass().getName());
    protected DFAgentDescription[] SearchAgent(String type) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd  = new ServiceDescription();
        sd.setType( type );
        dfd.addServices(sd);

        DFAgentDescription[] result = new DFAgentDescription[0];
        try {
            result = DFService.search(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        if (result.length>0)
            for (DFAgentDescription re: result) {
                System.out.println("Searched Results: " + re.getName().getLocalName() );
            }
        return result;
    }
    protected class SendMsgBehaviour extends OneShotBehaviour {
        String m_content;
        int m_type;
        String m_to;
        Instances m_data;
        String m_protocol;
        public SendMsgBehaviour(String Content,String protocol, int ACLMessageType,String to){
            m_content= Content;
            m_type = ACLMessageType;
            m_to = to;
            m_protocol=protocol;
        }
        public SendMsgBehaviour(Instances data,String Content,String protocol, int ACLMessageType, String to){
            m_content= Content;
            m_type = ACLMessageType;
            m_to = to;
            m_data=data;
            m_protocol=protocol;
        }
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(m_type);
            msg.setContent(m_content);
            msg.setProtocol(m_protocol);
            if (m_data!=null) {
                try {
                    msg.setContentObject(m_data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            AID msgTo = SearchAgent(m_to)[0].getName();
            msg.addReceiver(msgTo);
            send(msg);
            myLogger.log(Logger.INFO,getInfo()+" Send >>>>>\n["+GetType(msg.getPerformative())+"] \t'"+m_content+"' >>>>> ("+msgTo.getLocalName()+")");
        }
    }
    protected class AutoReplyBehaviour extends OneShotBehaviour{
        ACLMessage m_reply;
        public AutoReplyBehaviour(ACLMessage msg) {
            m_reply = msg.createReply();
            m_reply.setPerformative(ACLMessage.INFORM);
            if(m_reply.getProtocol()==MyAgent.Message){
                m_reply.setContent(msg.getContent()+"-Received!");
                myLogger.log(Logger.INFO, getInfo()+ " Received <<<<<\n["+GetType(msg.getPerformative())+"] \t'"+msg.getContent()+"' <<<<< (" + msg.getSender().getLocalName()+")");
            }else if(m_reply.getProtocol()==MyAgent.Instances){
                m_reply.setContent("Data-Received!");
                myLogger.log(Logger.INFO, getInfo() + " Received <<<<<\n[" + GetType(msg.getPerformative()) + "] \tData <<<<< (" + msg.getSender().getLocalName() + ")");
            }
        }
        @Override
        public void action() {
            send(m_reply);
            myLogger.log(Logger.INFO, getInfo() + " Send >>>>>\n[" + GetType(m_reply.getPerformative()) + "] \t'" + m_reply.getContent() + "' >>>>> (" + m_reply.getSender().getLocalName() + ")");
        }
    }


    @Override
    protected void setup() {
        myLogger.log(Logger.INFO,getInfo()+" Start!");
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(myType);
        sd.setName(getName());
        sd.setOwnership("TILAB");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this,dfd);
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, getInfo()+" - Cannot register with DF", e);
            doDelete();
        }
        super.setup();
    }
}
