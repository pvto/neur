package neur.struct;

import neur.struct.ActivationFunction.ActivationFunctionN;

public class SoftsignFunc extends ActivationFunctionN {
    public int getType() { return Types.AFUNC_SOFTSIGN; }

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
