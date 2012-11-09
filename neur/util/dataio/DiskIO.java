
package neur.util.dataio;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DiskIO {

    public void save(String filename, Serializable mlp) throws IOException
    {
        File f = new File(filename);
        FileOutputStream out = new FileOutputStream(f);
        ObjectOutputStream oo = new ObjectOutputStream(out);
        oo.writeObject(mlp);
        oo.flush();
        oo.close();
    }


    public List<String[]> loadCSV(String filename, String separatorRegexp) throws IOException
    {
        FileInputStream in = null;
        try
        {
            in = new FileInputStream(new File(filename));
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            List<String[]> ret = new ArrayList<String[]>();
            String line;
            while((line = r.readLine()) != null)
            {
                ret.add(line.split(separatorRegexp));
            }
            return ret;
        }
        finally
        {
            if (in != null)
            {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
