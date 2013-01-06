
package neur.learning;

import neur.NeuralNetwork;
import neur.data.TrainingSet;

public abstract class Classifier {

    public static final class ClassifierType {

        public static final int 
                CHOOSE_1_OF_2 = 1,
                CHOOSE_1_OF_n = 2
                ;
    }

    
    public abstract int TYPE();
    /**
     * 
     * @param sample
     * @param result
     * @return for classifying tasks, 1 for correct classification and 0 for incorrect
     */
    public abstract int correctness(float[][] sample, float[] result);
    public abstract float[] normalisedClassification(float[][] sample, float[] nnwOutput);
        
    public float[] params = {};
    
    public int correctCount(TrainingSet S, NeuralNetwork N)
    {
        int ok = 0;
        for(float[][] sample : S.set)
            ok += correctness(sample, N.feedf(sample[0]));
        return ok;
    }
    
}
