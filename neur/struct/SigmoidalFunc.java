package neur.struct;

public class SigmoidalFunc extends ActivationFunction.ActivationFunctionN {

    {
        setParameters(new float[]{3f});
    }

    public float get(float val)
    {
        float activation = 
                1f
                / (1f + (float) Math.exp(-params[0] * val));
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
        return params[0] * val * (1f - val) 
                + 0.1f; // fix for the "flat spot problem"
    }
}