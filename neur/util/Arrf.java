
package neur.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/** Functions for the manipulation of arrays.
 * 
 * Multidimensional arrays are in this utility class always treated like samples, where
 * the outermost (containing) array is the container of individual records in the sample, 
 * and inner (contained) arrays reflect in their dimensionality (if any) the structure of an individual record.
 * 
 * This is the same policy as that adapted in TrainingSet.java.
 */
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
    public static float[][]                 cols(float[][][] data, int j)
    {
        float[][] ret = new float[data[0][j].length][];
        for (int i = 0; i < ret.length; i++)
            ret[i] = col(data, j, i);
        return ret;
    }
    
    // --- restructuring of data --- //
    
    public static int                       flattenedSize(float[][] data, int subitemCount)
    {
        int count = 0;
        for (int i = 0; i < subitemCount; i++)
            count += data[i].length;
        return count;
    }
    public static int                       flattenedSize(float[][][] data)
    {
        int ret = 0;
        for (int k = 0; k < data.length; k++)
            ret += flattenedSize(data[k], data[k].length);
        return ret;
    }
    public static int                       flatInd(float[][] data, int[] deflatInd)
    {
        return deflatInd[1] + flattenedSize(data, deflatInd[0]);
    }
    public static int[]                     deflatInd(float[][] data, int flatInd)
    {
        int 
                acc = 0,
                i = 0
                ;
        for(;;)
        {
            if (flatInd < acc + data[i].length)
            {
                return new int[]{i,flatInd-acc};
            }
            acc += data[i++].length;
        }
    }
    public static float[]                   flatten(float[][] data)
    {
        float[] ret = new float[flattenedSize(data, data.length)];
        int index = 0;
        for (int k = 0; k < data.length; k++)
        {
            int len = data[k].length;
            System.arraycopy(data[k], 0, ret, index, len);
            index += len;
        }
        return ret;
    }
    public static float[]                   flatten(float[][][] data)
    {
        float[] ret = new float[flattenedSize(data)];
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
    public static float[][][]               copy(float[][][] data)
    {
        float[][][] r = new float[data.length][][];
        for (int k = 0; k < data.length; k++)
        {
            r[k] = new float[data[k].length][];
            for (int j = 0; j < data[k].length; j++)
            {
                r[k][j] = new float[data[k][j].length];
                System.arraycopy(data[k][j], 0, r[k][j], 0, r[k][j].length);
            }
        }
        return r;
    }
    public static int[]                     copy(int[] A)
    {
        return java.util.Arrays.copyOf(A, A.length);
    }
    public static float[]                   concat(float[] ... arrs)
    {
        float[] res = new float[combinedSize(arrs)];
        for (int i = 0, offset = 0; i < arrs.length; offset += arrs[i++].length)
            System.arraycopy(arrs[i], 0, res, offset, arrs[i].length);
        return res;
    }
    public static double[]                  concat(double[] ... arrs)
    {
        double[] res = new double[combinedSize(arrs)];
        for (int i = 0, offset = 0; i < arrs.length; offset += arrs[i++].length)
            System.arraycopy(arrs[i], 0, res, offset, arrs[i].length);
        return res;
    }

    public static <T> Iterable<T>           concatite(final Iterable<T> ... some)
    {
        Iterable<T> ret = new Iterable<T>()
        {
            @Override  public Iterator<T> iterator()
            {
                Object[] more = new Object[some.length];
                int i = 0;
                for(Iterable<T> t : some)
                    more[i++] = t;
                return ite(more);
            }
            private Iterator<T> ite(final Object[] more)
            {
                Iterator<T> ite = new Iterator<T>()
                {
                    int j = 0;
                    Iterator<T> curr = null;
                    T overhead = null;
                    @Override
                    public boolean hasNext()
                    {
                        if (overhead != null)
                            return true;
                        if (j >= more.length)
                            return false;
                        if (curr == null)
                            curr = ((Iterable<T>)more[j]).iterator();
                        if (!curr.hasNext())
                        {
                            j++;
                            curr = null;
                            return hasNext();
                        }
                        overhead = next();
                        return true;
                    }

                    @Override
                    public T next()
                    {
                        if (overhead != null)
                        {
                            T tmp = overhead;
                            overhead = null;
                            return tmp;
                        }
                        if (j >= more.length)
                            return null;
                        if (curr == null)
                            curr = ((Iterable<T>)more[j]).iterator();
                        if (!curr.hasNext())
                        {
                            j++;
                            curr = null;
                            return next();
                        }
                        return curr.next();
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                };
                return ite;
            }
            
        };
        return ret;
    }
    public static int[]                     copy(int val, int count)
    {
        int[] res = new int[count];
        for (int i = 0; i < res.length; i++)
            res[i] = val;
        return res;
    }
    public static float[]                   range(float min, float max, float step)
    {
        float range = max-min;
        float[] res = new float[(int)(range / step)];
        float f = min;
        for(int i = 0; i < res.length; f += step)   // inhibit an extra class due to possible floating point inaccuracy
            res[i++] = f;
        return res;
    }
    /** Clusterises given array so taht each value is truncated to the nearest
    * floor value found in the classes array. 
    * Optimised for class count < 12 */
    public static float[]                   clusterise(float[] data, float[] classes)
    {
        float[] ret = new float[data.length];
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = classes[classes.length - 1];
            for(int r = 0; r < classes.length; r++)
            {
                if (classes[r] > data[i])
                {
                    ret[i] = classes[r - 1];
                    break;
                }
            }
        }
        return ret;
    }
    // --- conversion methods between different types of arrays --- //
    
    public static int[]                     ints(String[] data)
    {
        int[] r = new int[data.length];
        for (int i = 0; i < r.length; i++)
            r[i] = (int)Double.parseDouble(data[i]);
        return r;
    }
    public static float[]                   floats(String[] data)
    {
        float[] r = new float[data.length];
        for (int i = 0; i < r.length; i++)
            r[i] = Float.parseFloat(data[i]);
        return r;
    }
    public static float[][]                 arrayf(List<float[]> data)
    {
        float[][] r = new float[data.size()][];
        for (int i = 0; i < r.length; i++)
        {
            float[] d = data.get(i);
            r[i] = java.util.Arrays.copyOf(d, d.length);
        }
        return r;
    }
    public static float[][][]               arrayff_shallow(List<float[][]> data)
    {
        float[][][] r = new float[data.size()][][];
        for (int i = 0; i < r.length; i++)
            r[i] = data.get(i);
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
    public static int                       sum(int[] data)
    {
        int r = 0;
        for(int i : data)
            r += i;
        return r;
    }
    public static float                     avg(float[] data) {     return sum(data) / Math.max((float)data.length, 1f); }
    public static float[]                   abs(float[] data)
    {
        float[] r = new float[data.length];
        for (int i = 0; i < data.length; i++)
            r[i] = Math.abs(data[i]);
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
    public static boolean                   allEqual(int[] data, int value)
    {
        for (int i = 0; i < data.length; i++)
            if (data[i] != value)
                return false;
        return true;
    }
    public static float                     max(float[] data)  {    return data[indexOfGreatest(data)]; }
    public static int                       max(int[] data)    {    return data[indexOfGreatest(data)]; }
    public static float                     min(float[] data)  {    return data[indexOfLeast(data)]; }

    public static <T> T                     first(T... list)
    {
        for(T t : list)
            if (t != null)
                return t;
        return null;
    }
    public static Float                     nonZero(Float ... vals)
    {
        for(Float f : vals)
            if (f != null && f != 0f)
                return f;
        return null;
    }
    public static int                       indexOfGreatest(float[] data)
    {
        int bestInd = 0;
        float best = data[0];
        for (int i = 1; i < data.length; i++)
            if (data[i] > best)
                best = data[bestInd = i];
        return bestInd;
    }
    public static int                       indexOfGreatest(int[] data)
    {
        int bestInd = 0;
        int best = data[0];
        for (int i = 1; i < data.length; i++)
            if (data[i] > best)
                best = data[bestInd = i];
        return bestInd;
    }
    public static int                       indexOfLeast(float[] data)
    {
        int bestInd = 0;
        float best = data[0];
        for (int i = 1; i < data.length; i++)
            if (data[i] < best)
                best = data[bestInd = i];
        return bestInd;
    }

    public static int                       combinedSize(float[] ... arrs)
    {
        int size = 0;
        for (int i = 0; i < arrs.length; i++)
            size += arrs[i].length;
        return size;
    }
    public static int                       combinedSize(double[] ... arrs)
    {
        int size = 0;
        for (int i = 0; i < arrs.length; i++)
            size += arrs[i].length;
        return size;
    }
    public static float[]                   sqr(float[] data)
    {
        float[] r = new float[data.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = data[i] * data[i];
        }
        return r;
    }
    
    // --- data conversion and normalisation --- //
    
    public static float[]                   normaliseMinmax(float[] data)
    {
        float[] r = new float[data.length];
        float max = max(data);
        float min = min(data);
        for (int i = 0; i < r.length; i++)
            r[i] = (data[i] - min) / (max - min);
        return r;
    }
    public static float[][][]               normaliseMinmax(float[][][] data)
    {
        float[][][] r = copy(data);
        for(int j = 0; j < data[0].length; j++)
        for (int i = 0; i < data[0][j].length; i++)
        {
            float[] norm = normaliseMinmax(col(data, j, i));
            for (int k = 0; k < norm.length; k++)
            {
                r[k][j][i] = norm[k];
            }
        }
        return r;
    }
    public static <T> T[]                   shuffle(T[] data)
    {
        T[] r = java.util.Arrays.copyOf(data, data.length);
        for (int i = 0; i < r.length * 2; i++)
        {
            int x = (int)(Math.random() * r.length);
            int y = (int)(Math.random() * r.length);
            T tmp = r[y];
            r[y] = r[x];
            r[x] = tmp;
        }
        return r;
    }
    
    
    
    // --- data property extraction and compression --- //
    
    public static float[][]                 classes(float[] data)
    {
        float[] ordd = java.util.Arrays.copyOf(data, data.length);
        java.util.Arrays.sort(ordd);
        List<float[]> tmp = new ArrayList<>();
        float[] cur = new float[] {ordd[0], 0};
        tmp.add(cur);
        for (int i = 0; i < ordd.length; i++)
        {
            if (ordd[i] != cur[0])
            {
                tmp.add(cur = new float[] {ordd[i], 1});
                continue;
            }
            cur[1]++;
        }
        return arrayf(tmp);
    }
    public static int[][]                   classes(int[] data)
    {
        int[] ordd = java.util.Arrays.copyOf(data, data.length);
        java.util.Arrays.sort(ordd);
        List<int[]> tmp = new ArrayList<>();
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
    public static int[][]                   intClasses(float[][] classes)
    {
        int[][] ret = new int[classes.length][];
        for (int i = 0; i < ret.length; i++)
            ret[i] = new int[]{i, (int)classes[i][1]};
        return ret;
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
    public static float[]                   add(float[]... list)
    {
        float[] ret = new float[list[0].length];
        for (int j = 0; j < list.length; j++)
            for (int i = 0; i < list[j].length; i++)
                ret[i] += list[j][i];
        return ret;
    }
    public static double[]                  add(double[] A, double[] B)
    {
        double[] R = new double[B.length];
        for (int i = 0; i < R.length; i++)
        {
            R[i] = A[i] + B[i];
        }
        return R;
    }
    public static int[]                     subtract(int from, int[] A)
    {
        int[] r = new int[A.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = from - A[i];
        }
        return r;
    }
    public static float[]                   subtract(float[] from, float val)
    {
        float[] r = new float[from.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = from[i] - val;
        }
        return r;
    }
    public static float[]                   add(float[] from, float val)
    {
        float[] r = new float[from.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = from[i] + val;
        }
        return r;
    }
    public static float[]                   div(float[] data, float divisor)
    {
        float[] r = new float[data.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = data[i] / divisor;
        }
        return r;
    }    
    public static double[]                  div(double[] data, double divisor)
    {
        double[] r = new double[data.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = data[i] / divisor;
        }
        return r;
    }
    public static float[]                   mult(float[] data, float multiplier)
    {
        float[] r = new float[data.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = data[i] * multiplier;
        }
        return r;
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
        return (float)Math.sqrt(evdist_variance(data));
    }

    
    
    // other statistical functions
    public static float                     covariance(float[] A, float[] B)
    {
        cocheck(A,B);
        float 
                amean = evdist_mean(A),
                bmean = evdist_mean(B);
        float sum = 0f;
        for (int i = 0; i < A.length; i++)
        {
            sum += (A[i] - amean) * (B[i] - bmean);
        }
        return sum / (float)A.length;
    }
    public static float                     pearsonCorrelation(float[] A, float[] B)
    {
        cocheck(A,B);
        return covariance(A, B) 
                / (evdist_sd(A) * evdist_sd(B))
                
                ;
    }
    private static void cocheck(float[] A, float[] B)
    {
        if (A.length != B.length)
            throw new RuntimeException("samples of A and B are not of same length");
    }
    
}
