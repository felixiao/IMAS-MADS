package urv.imas;

import weka.core.Instances;
import weka.core.converters.ConverterUtils;

public class InfoHandler {
    private Instances m_data;
    public InfoHandler(){
        System.out.println("Create InfoHandler!");
    }

    public void LoadData(String filename){
        try {
            System.out.println("Loading data from :"+filename);
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(filename);
            m_data = source.getDataSet();
            // uses the last attribute as class attribute
            if (m_data.classIndex() == -1)
                m_data.setClassIndex(m_data.numAttributes() - 1);
            System.out.println("Num Instances: "+m_data.numInstances()+"\nNum Class: "+m_data.numClasses()+"\nNum Attrs: "+m_data.numAttributes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Instances GetDataInstances(int size){
//            List<Instance> datas= m_data.subList(0,size);
        Instances d = new Instances(m_data,0,size);
        System.out.println("GetDataInstances First of Num Instances: "+m_data.numInstances()+"\nNum Class: "+m_data.numClasses()+"\nNum Attrs: "+m_data.numAttributes());
        return d;
    }
}
