
package neur;

import java.io.Serializable;
import neur.struct.*;

public class Neuron implements Serializable {

    public float netInput = 0f;
    public float activation = 0f;
    public float derivativeActivation = 0f;
    
    private static final long serialVersionUID = 20121016L;

    public Neuron() {}
    public Neuron(int act) { this.activationFunction = act;  ACT(); }
    public Neuron(ActivationFunction f) { 
        this.activationFunction = f.getType();
        this.ACT = f;  this.activationParams = f.getParameters(1); }

    public Neuron copy()
    {
        Neuron b = new Neuron();
        b.netInput = netInput;
        b.activationFunction = activationFunction;
        b.ACT();
        b.activation = activation;
        b.derivativeActivation = derivativeActivation;
        return b;
    }
    
    public float activation()
    {
        return activation = ACT.get(netInput);
    }
    public float derivativeActivation()
    {
        return derivativeActivation = ACT.derivative(activation);
    }
    public int activationFunction;
    public float[] activationParams = {};
    public transient ActivationFunction ACT;

    public ActivationFunction ACT()
    {
        if (ACT == null)
        {
            ACT = ActivationFunction.Types.create(activationFunction, activationParams);
        }
        return ACT;
    }

}
