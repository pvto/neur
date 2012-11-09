package neur.struct;

public interface ActivationFunction {

    
    public static final class Types {
        public static final int AFUNC_LINEAR = 0;
        public static final int AFUNC_SIGMOID = 1;
        public static final int AFUNC_SIN = 2;
        public static final int AFUNC_TANH = 3;
        
        public static String asString(int func)
        {
            return create(func,0f).getClass().getSimpleName();
        }

        public static ActivationFunction create(int activationFunction, float param1) {
            ActivationFunction ACT = null;
            switch (activationFunction)
            {
            case AFUNC_LINEAR:
                ACT = new LinearFunc();
                break;
            case AFUNC_SIGMOID:
                SigmoidalFunc f = new SigmoidalFunc();
                f.k = param1;
                ACT = f;
                break;
            case AFUNC_SIN:
                ACT = new SinFunc();
                break;
            case AFUNC_TANH:
                ACT = new TanhFunc();
                break;
            }
            return ACT;
        }
    }
    
    public float get(float val);

    public float derivative(float val);
}