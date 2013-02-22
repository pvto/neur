
package neur.util.visuals;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JPanel;
import neur.MLP;
import neur.Neuron;
import neur.learning.LearnRecord;
import neur.struct.SigmoidalFunc;

public class MLPVisualisation {

    
    public MLPVisualisation(){}
    
    private LearnRecord rec;
    private Runnable init;
    private Runnable updt;
    
    public MLPVisualisation createFrame(final LearnRecord rec, final int w, final int h, final int updateFrequency)
    {
        final JFrame fr = new JFrame();
        fr.setLayout(new BorderLayout());
        final JPanel panel = new JPanel()
        {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (rec.current != null)
                {
                    g.clearRect(0, 0, w, h);
                    new MLPVisualisation().visualise((MLP)rec.current, g, 0, 0, fr.getWidth(), fr.getHeight());
                }
            }
            
        };
        fr.add(panel, BorderLayout.CENTER);
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        init = new Runnable()
        {
            public void run()
            {
                fr.setSize(w,h);
                fr.setVisible(true);
            }
        };
        updt = new Runnable(){public void run(){ for(;;){sleep(1000/updateFrequency);fr.repaint();}}};
        return this;
    }
    
    public MLPVisualisation run()
    {
        javax.swing.SwingUtilities.invokeLater(init);
        new Thread(updt).start();
        return this;
    }
    
    
    public void visualise(MLP net, Graphics g, int x0, int y0, int width, int height)
    {
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        int margin = 10;
        int xs = x0 + margin;
        int ys = y0 + margin;
        int xe = x0 + width - margin * 2;
        int ye = y0 + height - margin * 2;
        double xdiv = (xe - xs) / (net.getLayerSizes().length - 1);
        double size = Math.max(2, Math.min(10, xdiv / 4.0));
        double ycenter = y0 + (height) / 2.0;
        for (int lr = 0; lr < net.getLayerSizes().length; lr++) {
            int lrl = net.layers[lr].length;
            double ydiv = Math.min((ye-ys) / (4.0), (ye - ys) / (double)(net.getLayerSizes()[lr] + 1));
            double ystart = ycenter - (lrl - 1) / 2.0 * ydiv;
            for(int n = 0; n < net.layers[lr].length; n++)
            {
                if (lr < net.getLayerSizes().length - 1)
                {
                    int lrl2 = net.layers[lr+1].length;
                    double ydiv2 = Math.min((ye-ys) / (4.0), (ye - ys) / (double)(net.getLayerSizes()[lr+1] + 1));
                    double ystart2 = ycenter - (lrl2 - 1) / 2.0 * ydiv2;
                    for(int p = 0; p < net.weights[lr][n].length; p++)
                    {
                        double w = net.weights[lr][n][p];
                        float colval = (float)Math.min(Math.abs(w), 1.0);
                        Color c =  new Color(w<0?colval:0f,0f,w>=0?colval:0f,colval);
                                //Color.getHSBColor((w < 0)?0.0f:0.6f, 0.8f, (float)Math.min(Math.abs(w), 1.0));
                        g.setColor(c);
                        drawWeight(g2, xs + lr * xdiv, ystart + n * ydiv, xs + (lr + 1) * xdiv, ystart2 + (p) * ydiv2, c);
                    }
                }
                drawNeuron(g2, xs + lr * xdiv, ystart + n * ydiv, size, net.layers[lr][n]);
            }
        }
    }
    
    public void drawNeuron(Graphics2D g, double x, double y, double size, Neuron n)
    {
        g.setColor(Color.BLACK);
        g.drawOval((int)(x - size/2), (int)(y - size/2), (int)(size), (int)(size));
        float c = (float)Math.max(0.0,Math.min(1.0,Math.abs(n.activation)));
        g.setColor(new Color(0f,c,0f,1f));
        g.fillOval((int)(x - size/2) + 2, (int)(y - size/2) + 2, (int)(size) - 2, (int)(size) - 2);
    }
    public void drawWeight(Graphics2D g, double x1,double y1, double x2,double y2, Color c)
    {
        for(int i = 0; i < (c.getBlue()+c.getRed()) / 32; i++)
        g.drawLine((int)x1, (int)y1+i, (int)x2, (int)y2+i);
    }
    
    
    
    
    private static void sleep(long t)
    {
        try {
            Thread.sleep(t);
        } catch (Exception e) {
        }
    }

    
    public static void main(String[] args) 
    {
        final MLP mlp = new MLP(new int[]{3,80,2}, new SigmoidalFunc());
        final JFrame fr = new JFrame();
        fr.setLayout(new BorderLayout());
        final int w = 800, h = 600;
        final JPanel panel = new JPanel()
        {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.clearRect(0, 0, w, h);
                new MLPVisualisation().visualise(mlp, g, 0, 0, fr.getWidth(), fr.getHeight());
            }
            
        };
        fr.add(panel, BorderLayout.CENTER);
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Runnable r = new Runnable()
        {
            public void run()
            {
                fr.setSize(w,h);
                fr.setVisible(true);
            }
        };
        javax.swing.SwingUtilities.invokeLater(r);
    }
}
