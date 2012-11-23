
package neur.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** functions for the manipulation of arrays */
public final class Arrf {

    
    // --- column extraction methods from multidimensional arrays --- //
    
    public static float[]                   col(float[][][] data, int j, int i)
    {
        float[] r = new float[data.length];
        int k = 0;
        for(float[][] d : data)
            r[k++] = d[j][i];
        return r;
    }
    public static float[]                   col(float[][] data, int i)
    {
        float[] r = new float[data.length];
        int j = 0;
        for(float[] d : data)
            r[j++] = d[i];
        return r;
    }
    public static float[]                   col(List<float[][]> data, int j, int i)
    {
        float[] r = new float[data.size()];
        int k = 0;
        for(float[][] d : data)
            r[k++] = d[j][i];
        return r;
    }    
    public static String[]                  col(List<String[]> data, int i)
    {
        String[] r = new String[data.size()];
        int j = 0;
        for(String[] d : data)
            r[j++] = d[i];
        return r;
    }
    
    // --- restructuring of data --- //
    
    public static float[]                   flatten(float[][][] data)
    {
        int count = 0;
        for (int k = 0; k < data.length; k++)
            for (int j = 0; j < data[k].length; j++)
                count += data[k][j].length;
        float[] ret = new float[count];
        int index = 0;
        for (int k = 0; k < data.length; k++)
            for (int j = 0; j < data[k].length; j++)
            {
                int len = data[k][j].length;
                System.arraycopy(data[k][j], 0, ret, index, len);
                index += len;
            }
        return ret;
    }
    public static int[]                     copy(int[] A)
    {
        return java.util.Arrays.copyOf(A, A.length);
    }
    
    
    // --- conversion methods between different types of arrays --- //
    
    public static int[]                     ints(String[] data)
    {
        int[] r = new int[data.length];
        for (int i = 0; i < r.length; i++)
            r[i] = Integer.parseInt(data[i]);
        return r;
    }
    public static float[]                   floats(String[] data)
    {
        float[] r = new float[data.length];
        for (int i = 0; i < r.length; i++)
            r[i] = Float.parseFloat(data[i]);
        return r;
    }
    public static int[][]                   array(List<int[]> data)
    {
        int[][] r = new int[data.size()][];
        for (int i = 0; i < r.length; i++)
        {
            int[] d = data.get(i);
            r[i] = java.util.Arrays.copyOf(d, d.length);
        }
        return r;
    }
    
    
    // --- statistical or aggregate methods on series of data --- //
    
    public static float                     sum(float[] data)
    {
        float r = 0f;
        for(float f : data)
            r += f;
        return r;
    }
    public static <T> boolean               contains(T[] data, T val)
    {
        for(T t : data)
            if (t == val)
                return true;
        return false;
    }
    public static boolean                   contains(int[] data, int val)
    {
        for(int t : data)
            if (t == val)
                return true;
        return false;
    }
    public static int maxInd(float[] result)
    {
        int bestInd = 0;
        float best = result[0];
        for (int i = 1; i < result.length; i++)
            if (result[i] > best)
                best = result[bestInd = i];
        return bestInd;
    }
    
    
    // --- data conversion and normalisation --- //
    
    public static float[]                   normalise(float[] data)
    {
        float[] r = new float[data.length];
        float mean = sum(data);
        if (mean == 0f) mean = 1f;  // can't div by zero
        for (int i = 0; i < r.length; i++)
            r[i] = (data[i] / mean);
        return r;
    }
    public static void                      normalise(float[][][] data)
    {
        for(int j = 0; j < data[0].length; j++)
        for (int i = 0; i < data[0][j].length; i++)
        {
            float[] norm = normalise(col(data, j, i));
            for (int k = 0; k < norm.length; k++)
                data[k][j][i] = norm[k];
        }
    }

    
    
    // --- data property extraction --- //
    
    public static int[][]                   classes(int[] data)
    {
        int[] ordd = java.util.Arrays.copyOf(data, data.length);
        java.util.Arrays.sort(ordd);
        List<int[]> tmp = new ArrayList<int[]>();
        int[] cur = new int[] {ordd[0], 0};
        tmp.add(cur);
        for (int i = 0; i < ordd.length; i++)
        {
            if (ordd[i] != cur[0])
            {
                tmp.add(cur = new int[] {ordd[i], 1});
                continue;
            }
            cur[1]++;
        }
        return array(tmp);
    }
    public static TreeMap<String,Integer>   classes(String[] data)
    {
        TreeMap<String,Integer> r = new TreeMap<String,Integer>();
        String[] tmp = java.util.Arrays.copyOf(data, data.length);
        java.util.Arrays.sort(tmp);
        String cur = tmp[0];
        int counter = 1;
        for (int i = 1; i < tmp.length; i++)
        {
            if (!cur.equals(tmp[i]))
            {
                r.put(cur, counter);
                counter = 0;
                cur = tmp[i];
            }
            counter++;
        }
        r.put(cur, counter);
        return r;
    }
    
    
    // --- vector algebra --- //
    
    public static float[]                   subtract(float[] A, float[] B)
    {
        float[] r = new float[A.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = A[i] - B[i];
        }
        return r;
    }
    public static float[]                   add(float[] A, float[] B)
    {
        float[] R = new float[B.length];
        for (int i = 0; i < R.length; i++)
        {
            R[i] = A[i] + B[i];
        }
        return R;
    }    
    

    // --- statistical functions for even distributions --- //
    
    public static float                     evdist_mean(float[] data)
    {
        float sum = 0f;
        for(float d : data)
        {
            sum += d;
        }
        return sum / (float)data.length;
    }
    public static float                     evdist_variance(float[] data)
    {
        float mean = evdist_mean(data);
        float sum = 0f;            
        for(float d : data)
        {
            float v = d - mean;
            sum += v * v;
        }
        return sum;
    }
    public static float                     evdist_sd(float[] data)
    {
        return (float)Math.sqrt((float)evdist_variance(data));
    }

    

}
