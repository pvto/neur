package neur.struct;

import neur.struct.ActivationFunction.ActivationFunction0;

public class SoftsignFunc extends ActivationFunction0 {


    @Override
    public float get(float val)
    {
        if (val == 0f)
            return 0f;
        float absval = val > 0f ? val : -val;
        return val / (1f + absval);
    }

    @Override
    final public float derivative(float val) {
        return 1f / 
                (1 + 2f*val + val*val);
    }

}
