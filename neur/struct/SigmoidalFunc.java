package neur.struct;

public class SigmoidalFunc extends ActivationFunction.ActivationFunctionN {
    public int getType() { return Types.AFUNC_SIGMOID; }
    
    {
        setParameters(new float[]{3f});
    }

    public float get(float val)
    {
        if (val > 100f)
            return 1.0f;
        if (val < -100f)
            return 0f;
        float activation = 
                1f
                / (1f + (float) Math.exp(-params[0] * val));
        return activation;
    }

    @Override
    public float derivative(float val)
    {
        return params[0] * val * (1f - val) 
                + 0.1f; // fix for the "flat spot problem"
    }
}