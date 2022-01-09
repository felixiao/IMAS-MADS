package urv.imas;
// 300 train -> 225 train 75 validation -> 6 / 24 attrs
// divide train and validate set
// set attributes
// train
// evaluation

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.supervised.instance.*;
import weka.filters.Filter;
import weka.core.Attribute;

import java.util.*;

public class ClassifyHandler {
    private long m_seed =1;
    private double m_trainPercent = 75;
    private Instances m_dataTrain;
    private Instances m_dataValidation;
    public Evaluation eval;
    private int m_attrInd = 0;// reversed order, right = 23 (24th attribute)
    Classifier m_cls = new J48();
    private String m_id;

    public ClassifyHandler(String name,int numberofAttrs){
        m_id = name;
        m_attrInd = 0;
        List<Integer> intList = Arrays.asList(23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0);
        Collections.shuffle(intList);
        for(int i= 0;i<numberofAttrs;i++){
//          System.out.println(m_id+" ["+b[ind]+"]");
            m_attrInd += 1<<(23-intList.get(i));
        }
        System.out.println("Create "+ m_id+ " Attrs = "+intToString(m_attrInd,4));
    }

    public static String intToString(int number, int groupSize) {
        StringBuilder result = new StringBuilder();

        for(int i = 23; i >= 0 ; i--) {
            int mask = 1 << i;
            result.append((number & mask) != 0 ? "1" : "0");

            if (i % groupSize == 0)
                result.append(" ");
        }
        result.replace(result.length() - 1, result.length(), "");

        return result.toString();
    }
    public void LoadData(Instances data){

        int tempAtrI= m_attrInd;
        for(int i=23;i>=0;i--){
            if( (tempAtrI & 1) ==0) data.deleteAttributeAt(i);
            tempAtrI = tempAtrI >>> 1;
        }
        System.out.println(m_id+" Load Data!!!!!!!!! Class Index = " +data.classIndex()+" Name = "+data.classAttribute().name());
        System.out.println(m_id+" Remain Attributes Data Summary\n"+data.toSummaryString());

        data.randomize(data.getRandomNumberGenerator(m_seed));
        m_dataTrain      = new Instances(data,0,225);
        m_dataValidation = new Instances(data,225,75);
        System.out.println("Train Size = "+ m_dataTrain.numInstances()+"\nVal Size = "+ m_dataValidation.numInstances());

    }
    public boolean Train(){
        try {
            m_cls.buildClassifier(m_dataTrain);
            eval = new Evaluation(m_dataTrain);
            eval.evaluateModel(m_cls, m_dataValidation);

            System.out.println(eval.toMatrixString("\n----------"+m_id+" Validation Matrix----------\n"));
            System.out.println(eval.toSummaryString("\n----------"+m_id+" Validation Results----------\n", true));
            System.out.println(eval.toClassDetailsString("\n------------"+m_id+" Validation Class Detail Results------------\n"));
            System.out.println((m_id+" Train Class Attr = "+m_dataTrain.instance(0).classAttribute().name()));
            System.out.println(m_id+" Summary Result "+m_dataTrain.toSummaryString());
            System.out.println(m_id+"         Result "+m_dataTrain.instance(0).toString());
            System.out.println((m_id+" Val Class Attr = "+m_dataValidation.instance(0).classAttribute().name()));
            System.out.println(m_id+" Summary Result "+m_dataValidation.toSummaryString());
            System.out.println(m_id+"         Result "+m_dataValidation.instance(0).toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean Fit(int attrs){
        System.out.println(m_id+ " Attrs =\t"+intToString(m_attrInd,4) +" = "+m_attrInd+"\n\t\t\t\t\t"+intToString(attrs,4)+"\n\t\t\t\t\t"+intToString((attrs & m_attrInd),4)+" = "+(attrs & m_attrInd));

        return ((attrs & m_attrInd) == m_attrInd);
    }
    public double[] Predict(Instances data){
        try {
            int tempAtrI= m_attrInd;
            for(int i=23;i>=0;i--){
                if( (tempAtrI & 1) ==0) data.deleteAttributeAt(i);
                tempAtrI = tempAtrI >>> 1;
            }
            System.out.println("Predict "+m_id);
            int ind = 0;
            for (Enumeration<Attribute> e = data.enumerateAttributes(); e.hasMoreElements();) {
                System.out.println("[" + ind + "] " + e.nextElement().name());
                ind++;
            }
            eval = new Evaluation(m_dataTrain);
            eval.evaluateModel(m_cls, data);

            double[] predR = new double[data.numInstances()*2];
            ind =0;
            for (Instance i:data ){
                predR[ind++] = eval.evaluateModelOnce(m_cls,i);
            }
            System.out.println(eval.toMatrixString("\n----------"+m_id+" Test Matrix----------\n"));
//            System.out.println(eval.toSummaryString("\n----------"+m_id+" Test Results----------\n", true));
            System.out.println(eval.toClassDetailsString("\n------------"+m_id+" Test Class Detail Results------------\n"));
            System.out.println((m_id+" Test Class Attr = "+data.instance(0).classAttribute().name()));
            System.out.println(m_id+" Summary Result "+data.toSummaryString());

            System.out.println(m_id+" Predict Result "+Arrays.toString(predR));
            double[] gtLables = new double[data.numInstances()];
            for(int i=0;i<data.numInstances();i++){
                gtLables[i] = (double)data.get(i).classValue();
                predR[i+data.numInstances()] = gtLables[i];
            }
            System.out.println(m_id+" GT      Result "+Arrays.toString(gtLables));
            return predR;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
