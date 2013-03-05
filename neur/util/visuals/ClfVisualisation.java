
package neur.util.visuals;

import java.awt.*;
import neur.MLP;
import neur.data.Dataset;
import neur.learning.LearnRecord;
import neur.util.Arrf;

public class ClfVisualisation extends VisualisationTempl {

    public Color[] COL = new Color[]{Color.BLUE,Color.RED,Color.GREEN,Color.GRAY,Color.MAGENTA,Color.YELLOW,
            Color.BLACK,Color.PINK,Color.ORANGE,Color.CYAN,Color.DARK_GRAY};

    {
        setParameter("X", 0);
        setParameter("Y", 1);
        setParameter("margin", 10);
    }
    
    public void visualise(LearnRecord rec, Graphics g, int x0, int y0, int width, int height)
    {
        if (rec.best == null)
            return;
        MLP net = (MLP)rec.best;
        Dataset D = rec.p.D;
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        int margin = (Integer)parameters.get("margin");
        int xs = x0 + margin;
        int ys = y0 + margin;
        int xe = x0 + width - margin * 2;
        int ye = y0 + height - margin * 2 - (WINDOW_DECO?20:0);
        int 
                yax = (Integer)parameters.get("Y"),
                xax = (Integer)parameters.get("X")
                ;
        float[] X = Arrf.col(D.data, 0, xax);
        float[] Y = Arrf.col(D.data, 0, yax);
        double
                dx1 = Arrf.min(X),
                dx2 = Arrf.max(X),
                dy1 = Arrf.min(Y),
                dy2 = Arrf.max(Y)
                ;

        for (double u = xs; u < xe; u += optimise) {
            for (double v = ys; v < ye; v += optimise)
            {
                double x = dx1 + (dx2 - dx1) * ((u - xs) / (double)(xe - xs));
                double y = dy1 + (dy2 - dy1) * ((v - ys) / (double)(ye - ys));
                float[] data = new float[rec.p.D.data[0][0].length];
                data[xax] = (float)x;
                data[yax] = (float)y;
                g2.setColor(COL[getCol(COL, net, data)]);
                g2.drawLine((int)u,(int)v,(int)(u+optimise),(int)(v+optimise));
            }
        }
        float[][][] data = D.data;
        for (int i = 0; i < data.length; i++)
        {
            double x = (data[i][0][xax] - dx1) / (dx2 - dx1) * (xe - xs) + xs;
            double y = (data[i][0][yax] - dy1) / (dy2 - dy1) * (ye - ys) + ys;
            g2.setColor(Color.BLACK);
            g2.fillOval((int)x - 3, (int)y - 3, 6, 6);
            int max = 0;
            for (int k = 1; k < data[i][1].length; k++)
                if (data[i][1][k] > data[i][1][max])
                    max = k;
            g2.setColor(COL[max]);
            g2.fillOval((int)x - 2, (int)y - 2, 4, 4);
        }
    }

    private int getCol(Color[] COL, MLP mlp, float[] data)
    {
        float[] res = mlp.feedf(data);
        int max = 0;
        for (int i = 0; i < res.length; i++)
            if (res[i] > res[max])
                max = i;
        return max;
    }
        
}
