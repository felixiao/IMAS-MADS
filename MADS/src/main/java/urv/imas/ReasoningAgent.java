package urv.imas;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import weka.classifiers.Evaluation;

import java.util.*;

public class ReasoningAgent extends MyAgent {
    private int numClassifier;
    private int countImReady = 0;
    private int countTrained = 0;
    private String weightMessure =null;
    private double votingThreshold = 0;
    private HashMap<String, Evaluation> trainMetrics = new HashMap<String, Evaluation>();
    private HashMap<String,double[]> testResults=new HashMap<String, double[]>();
    protected void setup() {
        super.myType = "ReasoningAgent";
        super.setup();
        numClassifier = Integer.parseInt(ParseXML("configure.xml","numberofclassifier"));
        weightMessure = ParseXML("configure.xml","votingmessure");
        votingThreshold = Double.parseDouble(ParseXML("configure.xml","votingthreshold"));
        addBehaviour(new WaitAndReply());
    }
    private String MeasureResult(String measure, double[] results,double[] gt, double sum){
        int TP=0,FP=0,TN=0,FN=0;
        for(int i =0;i<results.length;i++){
            results[i] /=sum;
            if (results[i] >= votingThreshold) results[i] = 1;
            else results[i] = 0;

            if(gt[i]==1){
                if(results[i]==1) TP++; // GT 1 Y 1
                else FN++; // GT 1 Y 0
            }else{
                if(results[i]==1) FP++;// GT 0 Y 1
                else TN++; // GT 0 Y 0
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("----------------------------------------------------------------------------\n");
        sb.append(measure+"\t"+Arrays.toString(results)+"\n");
        sb.append("Metrics   \tTP\t\tTN\t\tFP\t\tFN\t\t\n");
        sb.append("          \t"+TP+"\t\t"+TN+"\t\t"+FP+"\t\t"+FN+"\t\t\n");
        System.out.print(sb);
        return sb.toString();
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
                }else if(msgRequest.getProtocol()==TrainedSuccess){
                    countTrained++;
                    System.out.println("Classifier Trained Count "+countTrained);
                    try {
                        Evaluation eval = (Evaluation) msgRequest.getContentObject();
                        if(!trainMetrics.containsKey(msgRequest.getSender().getLocalName())){
                            trainMetrics.put(msgRequest.getSender().getLocalName(),eval);
                        }
                        if(countTrained==numClassifier) {
                            addBehaviour(new SendMsgBehaviour("TrainedSuccess", Message, ACLMessage.REQUEST, "UserAgent"));
                        }
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }else if(msgRequest.getProtocol()==Result){
                    try {
                        double[] testR = (double[]) msgRequest.getContentObject();
                        double[] gt =new double[testR.length/2];
                        double[] testRes = new double[testR.length/2];
                        for(int i =0;i<testR.length/2;i++){
                            testRes[i] = testR[i];
                            gt[i] = testR[i+testR.length/2];
                        }
                        System.out.println(msgRequest.getSender().getLocalName()+" Result "+Arrays.toString(testRes));
                        if(!testResults.containsKey(msgRequest.getSender().getLocalName()))
                            testResults.put(msgRequest.getSender().getLocalName(),testRes);
                        System.out.println("\n---------------------Voting Result-----------------------\nAgent\t\t\t\t\t\t\t\t\t\tResult\t\t\t\t\t\t\t\t\t\t\tTPR\t\tFPR\tPrecision\t\tRecall\tFMeasure\tAccuracy");
                        double[] finalRUniform = new double[testRes.length];
                        double[] finalRTPR = new double[testRes.length];
                        double[] finalRFPR = new double[testRes.length];
                        double[] finalRPrecision = new double[testRes.length];
                        double[] finalRRecall = new double[testRes.length];
                        double[] finalRFMeasure = new double[testRes.length];
                        double[] finalRAccuracy = new double[testRes.length];

                        double sumweightUniform=0;
                        double sumweightTPR=0;
                        double sumweightFPR=0;
                        double sumweightPrecision=0;
                        double sumweightRecall=0;
                        double sumweightFMeasure=0;
                        double sumweightAccuracy=0;

                        for (String k:testResults.keySet()) {
                            double[] r = testResults.get(k);

                            sumweightUniform += 1;
                            for(int i=0;i<testRes.length;i++){
                                finalRUniform[i]+=r[i];
                            }
                            sumweightTPR += trainMetrics.get(k).weightedTruePositiveRate();
                            for(int i=0;i<testRes.length;i++){
                                finalRTPR[i]+=r[i]*trainMetrics.get(k).weightedTruePositiveRate();
                            }
                            sumweightFPR += trainMetrics.get(k).weightedFalsePositiveRate();
                            for(int i=0;i<testRes.length;i++){
                                finalRFPR[i]+=r[i]*trainMetrics.get(k).weightedFalsePositiveRate();
                            }
                            sumweightPrecision += trainMetrics.get(k).weightedPrecision();
                            for(int i=0;i<testRes.length;i++){
                                finalRPrecision[i]+=r[i]*trainMetrics.get(k).weightedPrecision();
                            }
                            sumweightRecall += trainMetrics.get(k).weightedRecall();
                            for(int i=0;i<testRes.length;i++){
                                finalRRecall[i]+=r[i]*trainMetrics.get(k).weightedRecall();
                            }
                            sumweightFMeasure += trainMetrics.get(k).weightedFMeasure();
                            for(int i=0;i<testRes.length;i++){
                                finalRFMeasure[i]+=r[i]*trainMetrics.get(k).weightedFMeasure();
                            }
                            sumweightAccuracy += 1-trainMetrics.get(k).errorRate();
                            for(int i=0;i<testRes.length;i++){
                                finalRAccuracy[i]+=r[i]*(1-trainMetrics.get(k).errorRate());
                            }

                            System.out.println(k+"\t"+Arrays.toString(testResults.get(k))+
                                    "\t"+String.format("%1$.3f",trainMetrics.get(k).weightedTruePositiveRate())+
                                    "\t"+String.format("%1$.3f",trainMetrics.get(k).weightedFalsePositiveRate())+
                                    "\t"+String.format("%1$.3f",trainMetrics.get(k).weightedPrecision())+
                                    "\t\t"+String.format("%1$.3f",trainMetrics.get(k).weightedRecall())+
                                    "\t"+String.format("%1$.3f",trainMetrics.get(k).weightedFMeasure())+
                                    "\t"+String.format("%1$.3f",1-trainMetrics.get(k).errorRate()));

                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("----------------------------------------------------------------------------\n");
                        sb.append("GT        \t"+Arrays.toString(gt)+"\n");
                        System.out.println(sb);

                        String sUniform =  MeasureResult("Uniform   ",finalRUniform,gt,sumweightUniform);
                        String sTPR =  MeasureResult("TPR       ",finalRTPR,gt,sumweightTPR);
                        String sFPR =  MeasureResult("FPR       ",finalRFPR,gt,sumweightFPR);
                        String sPrecision =  MeasureResult("Precision ",finalRPrecision,gt,sumweightPrecision);
                        String sRecall =  MeasureResult("Recall    ",finalRRecall,gt,sumweightRecall);
                        String sFMeasure =  MeasureResult("FMeasure  ",finalRFMeasure,gt,sumweightFMeasure);
                        String sAccuracy =  MeasureResult("Accuracy  ",finalRAccuracy,gt,sumweightAccuracy);
                        switch (weightMessure){
                            case "Uniform":
                                sb.append(sUniform);
                                break;
                            case "TPR":
                                sb.append(sTPR);
                                break;
                            case "FPR":
                                sb.append(sFPR);
                                break;
                            case "Precision":
                                sb.append(sPrecision);
                                break;
                            case "Recall":
                                sb.append(sRecall);
                                break;
                            case "FMeasure":
                                sb.append(sFMeasure);
                                break;
                            case "Accuracy":
                                sb.append(sAccuracy);
                                break;
                        }
                        addBehaviour(new SendMsgBehaviour(sb.toString(),Result,ACLMessage.REQUEST,"UserAgent"));
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
                            if(countImReady==numClassifier){
                                addBehaviour(new SendMsgBehaviour(content,Message,ACLMessage.REQUEST,"UserAgent"));
                            }
                            break;
                    }
                }
            }
        }
    }
}
