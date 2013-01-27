package neur.struct;

import java.util.Arrays;
import neur.learning.LearnParams;

public interface ActivationFunction {

    
    public static final class Types {
        public static final int AFUNC_LINEAR = 0;
        public static final int AFUNC_SIGMOID = 1;
        public static final int AFUNC_SIN = 2;
        public static final int AFUNC_TANH = 3;
        public static final int AFUNC_SOFTSIGN = 4;
        
        public static String asString(int func)
        {
            return create(func,null).getClass().getSimpleName();
        }

        public static ActivationFunction create(int activationFunction, float[] params) {
            ActivationFunction ACT = null;
            switch (activationFunction)
            {
            case AFUNC_LINEAR:
                ACT = new LinearFunc();
                break;
            case AFUNC_SIGMOID:
                ACT = new SigmoidalFunc();
                break;
            case AFUNC_SIN:
                ACT = new SinFunc();
                break;
            case AFUNC_TANH:
                ACT = new TanhFunc();
                break;
            case AFUNC_SOFTSIGN:
                ACT = new SoftsignFunc();
                break;
            }
            // copy any parameters to the target function
            if (params != null)
            {
                float[] p = ACT.getParameters(params.length);
                for (int i = 0; i < params.length; i++)
                {
                    p[i] = params[i];
                }
            }
            return ACT;
        }

        /** a shorthand for create() */
        public static ActivationFunction get(LearnParams p)
        {
            return create(p.NNW_AFUNC, p.NNW_AFUNC_PARAMS);
        }
    }
    
    public float get(float val);

    public float derivative(float val);
    
    
    /**Returns an array of parameters for the activation function of size n.
     * 
     * The idea is that if user client more parameters through this than the function hitherto holds,
     * it grows its internal array of parameters to the given length.
     * 
     * @param n
     * @return 
     */
    public float[] getParameters(int n);
    
    
    public abstract class ActivationFunction0 implements ActivationFunction {

        @Override
        public float[] getParameters(int n)
        {
            return new float[0];
        }
        
    }

    public abstract class ActivationFunctionN implements ActivationFunction {

        public float[] params = {};

        @Override
        public synchronized float[] getParameters(int n)
        {
            if (n <= params.length)
                return params;
            return params = Arrays.copyOf(params, n);
        }
        
        public synchronized void setParameters(float[] p)
        {
            params = p;
        }
    }

}