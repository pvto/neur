
package neur.util.visuals;

import java.awt.*;
import neur.MLP;
import neur.learning.LearnRecord;
import neur.util.Arrf;

public class ClfVisualisation extends VisualisationTempl {

    public Color[] COL = new Color[]{Color.BLUE,Color.RED,Color.GREEN,Color.GRAY,Color.MAGENTA,Color.YELLOW,
            Color.BLACK,Color.PINK,Color.ORANGE,Color.CYAN,Color.DARK_GRAY};

    
    public void visualise(LearnRecord rec, Graphics g, int x0, int y0, int width, int height)
    {
        if (rec.best == null)
            return;
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        int margin = 10;
        int xs = x0 + margin;
        int ys = y0 + margin;
        int xe = x0 + width - margin * 2;
        int ye = y0 + height - margin * 2;
        float[] X = Arrf.col(rec.p.D.data, 0, 0);
        float[] Y = Arrf.col(rec.p.D.data, 0, 1);
        double
                dx1 = Arrf.min(X),
                dx2 = Arrf.max(X),
                dy1 = Arrf.min(Y),
                dy2 = Arrf.max(Y)
                ;

        for (double u = xs; u < xe; u += optimise) {
            for (double v = ys; v < ye; v += optimise)
            {
                double x = (dx2 - dx1) * (u / (double)(xe - xs));
                double y = (dy2 - dy1) * (v / (double)(ye - ys));
                float[] data = new float[] {(float)x,(float)y};
                g2.setColor(COL[getCol(COL, rec, data)]);
                g2.drawLine((int)u,(int)v,(int)(u+optimise),(int)(v+optimise));
            }
        }
        float[][][] data = rec.p.D.data;
        for (int i = 0; i < data.length; i++)
        {
            double x = data[i][0][0] / (dx2 - dx1) * (xe - xs) + xs;
            double y = data[i][0][1] / (dy2 - dy1) * (ye - ys) + ys;
            g2.setColor(Color.BLACK);
            g2.fillOval((int)x - 2, (int)y - 2, 4, 4);
            int max = 0;
            for (int k = 0; i < data[i][1].length; i++)
                if (data[i][1][k] > data[i][1][max])
                    max = i;
            g2.setColor(COL[max]);
            g2.fillOval((int)x - 1, (int)y - 1, 2, 2);
        }
        System.out.println("drawn");
    }

    private int getCol(Color[] COL, LearnRecord rec, float[] data)
    {
        MLP mlp = (MLP)rec.best;
        float[] res = mlp.feedf(data);
        int max = 0;
        for (int i = 0; i < res.length; i++)
            if (res[i] > res[max])
                max = i;
        return max;
    }
    
    
    public void drawWeight(Graphics2D g, double x1,double y1, double x2,double y2, Color c)
    {
        for(int i = 0; i < (c.getBlue()+c.getRed()) / 32; i++)
        g.drawLine((int)x1, (int)y1+i, (int)x2, (int)y2+i);
    }
    

}
