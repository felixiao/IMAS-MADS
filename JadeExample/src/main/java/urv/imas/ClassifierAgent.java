package urv.imas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import weka.core.Utils;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.converters.ConverterUtils.DataSource;
import java.io.File;

public class ClassifierAgent extends Agent {
    /*****************************************************************
     Common code for all agents
     *****************************************************************/
    private final String myType = "ClassifierAgent";
    private Classifier classifier;
    private String getInfo(){
        return String.format("[%s - %s]:\n",myType,getLocalName());
    }
    private final Logger myLogger = Logger.getMyLogger(getClass().getName());
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
    protected void setup() {
//        super.setup();
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
            addBehaviour(new WaitAndReplyBehaviour());
            classifier=new Classifier();
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, getInfo()+" - Cannot register with DF", e);
            doDelete();
        }
    }
    /*****************************************************************
     Agent specific codes
     *****************************************************************/
    private class WaitAndReplyBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if(msg != null) {
                ACLMessage reply = msg.createReply();
                if(msg.getPerformative()== ACLMessage.INFORM){
                    String content = msg.getContent();
                    if (content != null){
                        myLogger.log(Logger.INFO, getInfo() + " - Received ["+msg.getPerformative()+"] '"+content+"' from (" + msg.getSender().getLocalName()+")");
                        switch (content){
                            case "GetReady":
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent("GetReady-Received");
                                // wait for 1 sec to send msg to info agent and classifier agent
                                addBehaviour(new WakerBehaviour(myAgent,1000) {
                                    @Override
                                    protected void onWake() {
                                        ACLMessage _msg = new ACLMessage(ACLMessage.INFORM);
                                        _msg.setContent("GetReady");
                                        AID msgTo = SearchAgent("ReasoningAgent")[0].getName();
                                        _msg.addReceiver(msgTo);
                                        send(_msg);
                                        classifier.LoadData("audit_risk.arff");
                                        myLogger.log(Logger.INFO,getInfo()+" Send ["+_msg.getPerformative()+"] 'GetReady' to ("+msgTo.getLocalName()+")");
                                    }
                                });
                                break;
                        }
                        if(reply.getContent()!=null) {
                            send(reply);
                            myLogger.log(Logger.INFO, getInfo() + " Send [" + reply.getPerformative() + "] '" + reply.getContent() + "' to (" + reply.getSender().getLocalName() + ")");
                        }
                    }
                }
            }
        }
    }
    /*****************************************************************
     Classifier class
     *****************************************************************/
    public class Classifier{
        public Classifier(){
            System.out.println("Create Classifier!");
        }

        public void LoadData(String filename){
            try {
                System.out.println("Loading data from :"+filename);
                DataSource source = new DataSource(filename);
//                Instances data = DataSource.read(filename);
                Instances data = source.getDataSet();
                // uses the last attribute as class attribute
                if (data.classIndex() == -1)
                    data.setClassIndex(data.numAttributes() - 1);
                System.out.println("Num Instances: "+data.numInstances()+"\nNum Class: "+data.numClasses()+"\nNum Attrs: "+data.numAttributes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
