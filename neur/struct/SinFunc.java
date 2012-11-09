package neur.struct;

public class SinFunc implements ActivationFunction {

    public float k = 3f;

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