package data;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import neur.util.Arrf;
import neur.util.dataio.DiskIO;

public class PngColl {



    /** Extracts a data set from a folder of png files. The data set is returned as an array of samples,
     * 
     * Each png file in folder "folder" creates one row in the file. The strategy is to stack rgba of each pixel into 
     * four successive columns in the supervision vector. The supervision vector contains an integer for the class attribute,
     * exteracted by an user-provided regexp from the filename.
     * 
     * @param folder the source folder for png files
     * @param classPattern pattern to extract supervision class name from the filename.  First (not zeroth)
     * capturing group in the regular expression is used to capture the class name.
     * @param classMap if null, this is not used; otherwise, it must be mutable!
     * A class is mapped to an id using the provided map; if a class is not found in it, an incremental strategy 
     * is used to create a new id, and the new mapping is added.
     * 
     * @return dset = [sample[][], sample[][], sample[][]] ..., where each sample 
     * contains a data vector and a supervision vector that contains an id for a class,
     * sample=[supervision-columns[n], class-id-column[0]], where n is the number of pixels in the sample * 4.
     */
    public float[][][] extract(String folder, Pattern classPattern, Map<String, Integer> classMap)
    {
        List<float[][]> list = new ArrayList<float[][]>();
        Map<String, Integer> usedClasses;
        if (classMap != null)
        {
            usedClasses = classMap;
        }
        else
        {
            usedClasses = new HashMap<String,Integer>();
        }
        
        Integer currentArbitraryID = 1;
        for(File png : new File(folder).listFiles())
        {
            if (!png.getName().toLowerCase().endsWith(".png"))
            {
                continue;
            }
            if (png.isDirectory())
            {
                continue;
            }
            try
            {
                BufferedImage img = ImageIO.read(png);
                Raster r = img.getData();
                float[] tmp = new float[4];
                float[] col = new float[r.getWidth() * 4 * r.getHeight()];
                int ind = 0;
                for (int y = 0; y < r.getHeight(); y++)
                {
                    for (int x = 0; x < r.getWidth(); x++)
                    {
                        r.getPixel(x, y, tmp);
                        System.arraycopy(tmp, 0, col, ind, 4);
                        ind += 4;
                    }
                }
                Matcher m = classPattern.matcher(png.getName());
                
                String className = (m.find() ? m.group(1) : png.getName().replaceFirst("\\.(png|PNG)$", ""));
                while(usedClasses.containsValue(currentArbitraryID))
                {
                    currentArbitraryID++;
                }
                Integer classId = getOrPutNew(className, usedClasses, currentArbitraryID);
                list.add(new float[][] {col, 
                                        new float[]{classId}});
            }
            catch (Exception e)
            {
                System.out.println("can't read " + png);
            }
        }
        return Arrf.arrayff(list);
    }
    
    private <T,U> U getOrPutNew(T className, Map<T, U> usedClasses, U nextVal)
    {
        U classId = usedClasses.get(className);
        if (classId == null)
        {
            usedClasses.put(className, classId = nextVal);
        }
        return classId;
    }

    
    
    /**  create an inverse grayscale copy of supervision image data */
    
    public static float[][][] inverseGreyscale(float[][][] data)
    {
        // 
        float[][][] ret = new float[data.length][][];
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = new float[][] { new float[data[i][0].length / 4], data[i][1]};
        }
        for (int i = 0; i < ret[0][0].length; i+=4)
        {
            float[] r = Arrf.col(data, 0, i);
            float[] g = Arrf.col(data, 0, i+1);
            float[] b = Arrf.col(data, 0, i+2);
            float[] a = Arrf.col(data, 0, i+3);
            float[] max = Arrf.dot(a, Arrf.maxarr(r,g,b));
            max = Arrf.subtract(1f, max);
            Arrf.setCol(ret, max, 0, i /4);
        }
        return ret;
    }

    
    
    private static void extractToMap(String string, String split, String keyvalSplit, Map<String, Integer> classMap)
    {
        for(String keyval : string.split(split))
        {
            String[] kv = keyval.split(keyvalSplit);
            try {
                classMap.put(kv[0], Integer.parseInt(kv[1]));
            } catch (Exception e) {
                System.out.println("class id must be integral, found '" + kv[1] + "'");
            }

        }
    }

    public static void usage(PrintStream out)
    {
        out.println("Usage: PngColl <png-folder> <target-file> <class-pattern> [class-map]");
        out.println("    class-pattern is a java regexp with first capturing group specifying the class name to catch");
        out.println("    class-map :=  key ':' val [',' classMap]");
    }
    
    public static void main(String[] args) throws IOException {
        args = new String[]{"src_copy/data/pattern/ABCD_16x17", "abcd_16x17.data", "([a-zA-Z]+)", "A:0,B:1,C:2,D:3"};
        if (args.length < 3)
        {
            usage(System.out);
            return;
        }
        Map<String,Integer> classMap = new TreeMap<String,Integer>();
        if (args.length > 3)
        {
            extractToMap(args[3], ",\\s*", ":\\s*", classMap);
        }

        float[][][] data = new PngColl().extract(args[0], Pattern.compile(args[2]), classMap);
        // convert r/g/b/a [0..255] to [0..1]
        for (int i = 0; i < data[0][0].length; i++)
        {
            Arrf.setCol(data, Arrf.div(Arrf.col(data, 0, i), 255f), 0, i);
        }
        // create an inverse grey scale image
        data = inverseGreyscale(data);
        // blur along y-axis
//        for(int k = 0; k < data.length; k++)
//        {
//            for(int i = 0; i < data[k][0].length-32; i++)
//                data[k][0][i] += data[k][0][i+32] / 2f;
//        }
        
        
        System.out.println("class-map: " + classMap.toString().replaceAll(",", "\n,"));
        new DiskIO().saveSampleCSV(args[1], Arrf.doubles(data), 3);
        System.out.println("saved: " + args[1]);
    }
}
