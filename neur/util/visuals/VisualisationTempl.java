
package neur.util.visuals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JPanel;
import neur.learning.LearnRecord;

public abstract class VisualisationTempl {

    private LearnRecord rec;
    private Runnable init;
    private Runnable updt;
    public double optimise = 1.0;
    public JFrame frame;
    public int w,h;
    public Map<String,Object> parameters = new HashMap<String,Object>();
    public volatile boolean doRun = true;
    
    public <T extends VisualisationTempl> T 
            createFrame(final LearnRecord rec, final int w, final int h, final double updateFrequency)
    {
        this.w = w;  this.h = h;
        final JFrame fr = frame = new JFrame();
        frame.setFocusableWindowState(false);
        fr.setLayout(new BorderLayout());
        final JPanel panel = new JPanel()
        {
            long chg = 0;
            double fps = 0;
            @Override
            protected void paintComponent(Graphics g)
            {
                long start = System.currentTimeMillis();
                super.paintComponent(g);
                if (rec.current == null)
                    return;
                g.clearRect(0, 0, w, h);
                try
                {
                    visualise(rec, g, 0, 0, fr.getWidth(), fr.getHeight());
                } catch(Exception ex) { ex.printStackTrace(); }
                long end = System.currentTimeMillis();
                long time = (end-start);
                g.setColor(Color.green);
                if (end - chg > 3000)
                {
                    fps = 1000.0/time;
                    chg = end;
                }
                char[] ch = String.format("%.2ffps", fps).toCharArray();
                g.drawChars(ch, 0, ch.length, 1, 20);
                if (time > 80L)
                {
                    optimise *= 2.0;
                }
                else
                {
                    optimise -= 1.0;
                    if (optimise < 1.0)
                        optimise = 1.0;
                }
                optimise = optimise;
            }
            
        };
        fr.add(panel, BorderLayout.CENTER);
        //fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        init = new Runnable()
        {
            public void run()
            {
                fr.setSize(w,h + 20); // upper bar takes space
                fr.setVisible(true);
            }
        };
        updt = new Runnable(){public void run(){ while(doRun){sleep((long)(1000.0/updateFrequency));fr.repaint();}}};
        return (T)this;
    }
    
    public <T extends VisualisationTempl> T run()
    {
        javax.swing.SwingUtilities.invokeLater(init);
        new Thread(updt).start();
        return (T)this;
    }
    
    public <T extends VisualisationTempl> T setScreenPos(String pos)
    {
        String CONF = "\\d\\d\\s+\\d\\d";
        if (!pos.matches(CONF))
            throw new RuntimeException("Give screen position in form [XY] [xy] where X,Y are grid width and height and x,y frame x,y in grid");
        char[] wh = pos.split("\\s+")[0].toCharArray();
        char[] pos_ = pos.split("\\s+")[1].toCharArray();
        int xd = (int)(wh[0] - '0');
        int yd = (int)(wh[1] - '0');
        int x = (int)(pos_[0] - '0');
        int y = (int)(pos_[1] - '0');
        Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int xstep = (d.width - w) / Math.max(1,xd - 1);
        int ystep = (d.height - h) / Math.max(1, yd - 1);
        frame.setBounds((x - 1)*xstep, (y - 1)*ystep, w, Math.min(h, ystep));
        return (T)this;
    }
    
    public <T extends VisualisationTempl> T setParameter(String key, Object val)
    {
        parameters.put(key, val);
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
