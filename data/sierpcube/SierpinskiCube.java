package mlplearning;

import java.util.ArrayList;
import java.util.List;

public class SierpinskiCube {

    public static void main(String[] args)
    {
        SierpinskiCube ds = new SierpinskiCube();
        List<float[]> cubeData = new ArrayList<float[]>();
        int level = 2;
        int dsSize = (int)Math.pow(20, level) * 10;
        ds.generateCubes(2, 1f, 0f,0f,0f, cubeData);
        System.out.println(cubeData.size());
    }

    public float[][][] generateDataset(int size, Iterable<float[]> cubes)
    {
        float[][][] ds = new float[size][][];
        float radius = 0.7f;
        for (int i = 0; i < size; i++)
        {
            float
                    x = (float)Math.random()*radius*2f - radius,
                    y = (float)Math.random()*radius*2f - radius,
                    z = (float)Math.random()*radius*2f - radius
                    ;
            int inside = 0;
            for(float[] d : cubes)
            {
                if (x >= d[1] - d[0] && x <= d[1] + d[0]
                        && y >= d[2] - d[0] && y <= d[2] + d[0]
                        && z >= d[3] - d[0] && z <= d[3] + d[0])
                {
                    inside = 1;
                    break;
                }
            }
            ds[i] = new float[][]
                {
                    {x,y,z},
                    {inside,1f-inside}
                };
        }
        return ds;
    }
    
    
    public void generateCubes(int level, float sze, float x, float y, float z, List<float[]> cubes)
    {
        float newSize = sze * 1f/3f;
        if (level == 1)
        {
            cubes.add(new float[]{newSize/2f, x,y,z});
            return;
        }
        for (int k = 0; k < 3; k++) {
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
                    if (k==1&&j==1 || k==1&&i==1 || j==1&&i==1){}
                    else
                        generateCubes(level-1, 
                                newSize,
                                z+(float)(k-1)*sze,
                                y+(float)(j-1)*sze,
                                x+(float)(i-1)*sze,
                                cubes
                                );
                }
            }
        }
        return;
    }
    
    public static float[][] arr(List<float[]> vects)
    {
        float[][] res = new float[vects.size()][];
        int i = 0;  for(float[] r : vects) res[i++] = r;
        return res;
    }
}
