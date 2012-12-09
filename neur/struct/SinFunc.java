package neur.struct;

import neur.struct.ActivationFunction.ActivationFunctionN;

public class SinFunc extends ActivationFunctionN {

    public float get(float val)
    {
        return (float)Math.sin(val);
    }

    @Override
    public float derivative(float val)
    {
        return (float)Math.cos(val);
    }
}