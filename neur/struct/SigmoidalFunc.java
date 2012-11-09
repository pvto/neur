package neur.struct;

public class SigmoidalFunc implements ActivationFunction {

    public float k = 3f;

    public float get(float val)
    {
        float activation = 
                1f
                / (1f + (float) Math.exp(-k * val));
        if (activation > 1f) {
            activation = 1f;
        }
        else {
            if (activation < 0f) {
                activation = 0f;
            }
        }
        return activation;
    }

    @Override
    public float derivative(float val)
    {
        return k * val * (1f - val) 
                + 0.1f; // fix for the "flat spot problem"
    }
}