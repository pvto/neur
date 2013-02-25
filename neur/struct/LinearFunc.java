package neur.struct;

import neur.struct.ActivationFunction.ActivationFunction0;

public class LinearFunc extends ActivationFunction0 {
    public int getType() { return Types.AFUNC_LINEAR; }
    
    @Override
    public float get(float val)
    {
        return val;
    }

    @Override
    public float derivative(float val)
    {
        return 1f;
    }
}