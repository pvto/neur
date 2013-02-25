package neur.struct;

import neur.struct.ActivationFunction.ActivationFunctionN;

public class TanhFunc extends ActivationFunctionN {
    public int getType() { return Types.AFUNC_TANH; }
    
    {
        setParameters(new float[]{3f});
    }

    @Override
    public float get(float val) {
        if (val > 100f) {
            return 1.0f;
        } else if (val < -100f) {
            return -1.0f;
        }

        float E_x = (float) Math.exp(params[0] * val);
        return (E_x - 1f) / (E_x + 1f);
    }

    @Override
    final public float derivative(float val) {
        float out = get(val);
        return (1f - out * out);
    }

}
