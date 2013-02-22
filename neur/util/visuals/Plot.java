package neur.util.visuals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 *
 * @author Paavo Toivanen
 */
public class Plot {
    
    private float width, height;
    
    public Color[] colors = new Color[]{Color.GREEN, Color.BLACK, Color.YELLOW, Color.BLUE, Color.ORANGE, Color.MAGENTA};
    public Color GRID = Color.BLACK;
    public Color BG = Color.WHITE;
    private int X = 1;
    
    public Plot(int width, int height)
    {
        this.width = width;
        this.height = height;
    }
    
    public BufferedImage plot(float[][][] data, int xInd, int yOff, int[] yInds)
    {
        BufferedImage buf = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buf.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);        
        g.setColor(BG);
        g.fillRect(0, 0, (int)width, (int)height);
        
        float minx = Float.MAX_VALUE,
                miny = Float.MAX_VALUE,
                maxx = Float.MIN_VALUE,
                maxy = Float.MIN_VALUE;
        
        float margin = 10,
                iwidth = width - 2 * margin,
                iheight = height - 2 * margin;
        
        for(float[][] d : data)
        {
            if (d[xInd][0] < minx) { minx = d[xInd][0]; }
            if (d[xInd][0] > maxx) { maxx = d[xInd][0]; }
            for(int yInd : yInds)
            {
                if (d[yOff][yInd] < miny) { miny = d[yOff][yInd]; }
                if (d[yOff][yInd] > maxy) { maxy = d[yOff][yInd]; }
            }
        }
        g.setColor(GRID);
        g.drawLine((int)margin, (int)margin, (int)(margin + iwidth), (int)margin);
        g.drawLine((int)margin, (int)(margin+iheight), (int)(margin + iwidth), (int)(margin+iheight));
        g.drawLine((int)margin, (int)margin, (int)(margin), (int)(margin+iheight));
        g.drawLine((int)(margin+iwidth), (int)margin, (int)(margin+iwidth), (int)(margin+iheight));
        for(float[][] d : data)
        {
            float scale = (iwidth / (maxx - minx));
            float x0 = (d[xInd][0] - minx) * scale
                    + margin;
            int color = 0;
            for(int yInd : yInds)
            {
                g.setColor(colors[color]);
                float y1 = height - margin - 
                        (d[yOff][yInd] - miny) * scale;
                g.drawLine((int)x0-X, (int)y1, (int)x0+X, (int)y1);
                g.drawLine((int)x0, (int)y1-X, (int)x0, (int)y1+X);
                color++;
            }
        }
        return buf;
    }

    public Plot cross(int i) {
        this.X = i;
        return this;
    }
}
