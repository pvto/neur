package neur.struct;

import neur.struct.ActivationFunction.ActivationFunction0;

public class LinearFunc extends ActivationFunction0 {

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