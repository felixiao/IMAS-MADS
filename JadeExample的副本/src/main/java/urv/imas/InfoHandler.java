package urv.imas;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

public class InfoHandler {
    private Instances m_data;
    private long m_seed=1;
    private int m_numOfTrain;
    private int m_numOfVal;
    private int m_numOfTest;
    private int m_numOfEval;
    private Instances m_trainData;
    private Instances m_testData;
    private int m_attrTest = 0b101110111111101110111011;
    public int GetTestAttrs(){return m_attrTest;}

    public InfoHandler(int numOfTrain,int numOfVal,int numOfTest,int numOfEval,int attrTest,long seed){
        m_seed = seed;
        m_numOfTrain= numOfTrain;
        m_numOfVal  = numOfVal;
        m_numOfTest = numOfTest;
        m_numOfEval = numOfEval;
        m_attrTest  = attrTest;
        System.out.println("Create InfoHandler!");
    }

    public boolean LoadData(String filename){
        try {
            System.out.println("Loading data from :"+filename);
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(filename);
            m_data = source.getDataSet();
            // uses the last attribute as class attribute
            if (m_data.classIndex() == -1)
                m_data.setClassIndex(m_data.numAttributes() - 1);
            System.out.println("Num Instances: "+m_data.numInstances()+"\nNum Class: "+m_data.numClasses()+"\nNum Attrs: "+m_data.numAttributes());

            m_testData = new Instances(m_data,0,m_numOfTest);
            m_testData.randomize(m_testData.getRandomNumberGenerator(m_seed));

            m_trainData= new Instances(m_data,m_numOfTest,m_data.numInstances()-m_numOfTest);

            System.out.println("Train Size = "+ m_trainData.numInstances()+"\nTest Size = "+ m_testData.numInstances());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public Instances GetTrainData(){
        m_trainData.randomize(m_trainData.getRandomNumberGenerator(m_seed++));
        Instances d = new Instances(m_trainData,0,m_numOfTrain+m_numOfVal);
        System.out.println("Random Select: "+d.numInstances()+" Train Data\n");
        return d;
    }
    public Instances GetTestData(){
        m_testData.randomize(m_testData.getRandomNumberGenerator(m_seed++));
        Instances d = new Instances(m_testData,0,m_numOfEval);
        System.out.println("Random Select: "+d.numInstances()+" Train Data\n");
        return d;
    }

}
