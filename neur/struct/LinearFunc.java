package neur.struct;

public class LinearFunc implements ActivationFunction {

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