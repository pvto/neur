package neur.struct;

public class TanhFunc implements ActivationFunction {

    public float slope = 3f;

    @Override
    public float get(float val) {
        if (val > 100f) {
            return 1.0f;
        } else if (val < -100f) {
            return -1.0f;
        }

        float E_x = (float) Math.exp(this.slope * val);
        return (E_x - 1f) / (E_x + 1f);
    }

    @Override
    final public float derivative(float val) {
        float out = get(val);
        return (1f - out * out);
    }

}
