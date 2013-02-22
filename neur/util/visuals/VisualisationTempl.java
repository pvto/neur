
package neur.util.visuals;

import java.awt.BorderLayout;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import neur.learning.LearnRecord;

public abstract class VisualisationTempl {

    private LearnRecord rec;
    private Runnable init;
    private Runnable updt;
    public double optimise = 1.0;
    
    public <T extends VisualisationTempl> T 
            createFrame(final LearnRecord rec, final int w, final int h, final double updateFrequency)
    {
        final JFrame fr = new JFrame();
        fr.setLayout(new BorderLayout());
        final JPanel panel = new JPanel()
        {

            @Override
            protected void paintComponent(Graphics g) {
                long start = System.currentTimeMillis();
                super.paintComponent(g);
                if (rec.current != null)
                {
                    g.clearRect(0, 0, w, h);
                    visualise(rec, g, 0, 0, fr.getWidth(), fr.getHeight());
                }
                long time = (System.currentTimeMillis()-start);
                System.out.println(getClass().getSimpleName() + " repaint " + time + "ms");
                if (time > 40L)
                {
                    optimise += 1.0;
                }
                else
                {
                    optimise *= 0.95;
                }
                optimise = Math.max(1.0, optimise);
            }
            
        };
        fr.add(panel, BorderLayout.CENTER);
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        init = new Runnable()
        {
            public void run()
            {
                fr.setSize(w,h + 20); // upper bar takes space
                fr.setVisible(true);
            }
        };
        updt = new Runnable(){public void run(){ for(;;){sleep((long)(1000/updateFrequency));fr.repaint();}}};
        return (T)this;
    }
    
    public <T extends VisualisationTempl> T run()
    {
        javax.swing.SwingUtilities.invokeLater(init);
        new Thread(updt).start();
        return (T)this;
    }
    
    
    public abstract void visualise(LearnRecord rec, Graphics g, int x0, int y0, int width, int height);

    
    private static void sleep(long t)
    {
        try {
            Thread.sleep(t);
        } catch (Exception e) {
        }
    }
}
