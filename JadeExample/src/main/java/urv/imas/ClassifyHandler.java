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
    Evaluation eval;
    private int m_attrInd = 0;// reversed order, right = 23 (24th attribute)
    Classifier m_cls = new J48();
    private String m_id;

    public ClassifyHandler(String name){
        m_id = name;
        m_attrInd = 0;
        List<Integer> intList = Arrays.asList(23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0);
        Collections.shuffle(intList);
        for(int i= 0;i<6;i++){
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
        System.out.println("Load Data!!!!!!!!! Class Index = " +data.classIndex());
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);
        int ind = 0;

        int tempAtrI= m_attrInd;
        for(int i=23;i>=0;i--){
            if( (tempAtrI & 1) ==0) data.deleteAttributeAt(i);
            tempAtrI = tempAtrI >>> 1;
        }
        System.out.println("Remain Attributes");

        ind = 0;
        for (Enumeration<Attribute> e = data.enumerateAttributes(); e.hasMoreElements();) {
            System.out.println("[" + ind + "] " + e.nextElement().name());
            ind++;
        }
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean Fit(int attrs){
        System.out.println(m_id+ " Attrs = "+intToString(m_attrInd,4) +" = "+m_attrInd);
        System.out.println(m_id+ " Compr = "+intToString(attrs,4));
        System.out.println(m_id+ " Resul = "+intToString((attrs & m_attrInd),4)+" = "+(attrs & m_attrInd));
        System.out.println(m_id+ " Resut = "+((attrs & m_attrInd) == m_attrInd));
        return ((attrs & m_attrInd) == m_attrInd);
    }
    public int[] Predict(Instances data){
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

            int[] predR = new int[data.numInstances()];
            ind =0;
            for (Instance i:data ){
                predR[ind++] = (int)m_cls.classifyInstance(data.instance(0));
            }
            System.out.println(eval.toMatrixString("\n----------"+m_id+" Test Matrix----------\n"));
            System.out.println(eval.toSummaryString("\n----------"+m_id+" Test Results----------\n", true));
            System.out.println(eval.toClassDetailsString("\n------------"+m_id+" Test Class Detail Results------------\n"));
            return predR;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
