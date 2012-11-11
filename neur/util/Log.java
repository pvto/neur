
package neur.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public interface Log {

    
    void log(String fmtStr, Object ... params);
    
    
    void err(String str, Throwable t);
    
    
    
    
    // --- some log implementations follow --- //
    
    public static Log log = new Log()
    {
        public void log(String f, Object ... params) {
            System.out.println(String.format(f, params));
        }
        public void err(String str, Throwable t) {
            System.out.println(str);
            t.printStackTrace();
        }
    };

    
    
    
    public static class file
    {
        public static Map<String,Log> fileLogs = new HashMap<String,Log>();
        
        public static synchronized Log          bind(String filename)
        {
            filename = new File(filename).getAbsolutePath();
            Log log = fileLogs.get(filename);
            if (log != null) 
                return log;
            
            log = new Log()
            {
                private SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss' '");
                private OutputStream out;
                {
                    try {
                        out = new BufferedOutputStream(new FileOutputStream(new File("gen-srch.log")));
                    } 
                    catch(IOException e) { e.printStackTrace(); }
                }
                @Override
                public void log(String f, Object ... params)
                {
                    String s = String.format(f, params);
                    System.out.println(s);
                    try {
                        out.write(df.format(new Date()).getBytes());
                        out.write(s.getBytes());  out.write("\r\n".getBytes());
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                @Override
                public void err(String str, Throwable t)
                {
                    try {
                        out.write(str.getBytes());
                        for(StackTraceElement e : t.getStackTrace()) {
                            out.write("\r\n\t".getBytes());  
                            out.write(e.toString().getBytes());
                        }
                        out.write('\r');  out.write('\n');
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            };
            fileLogs.put(filename, log);
            return log;
        }
    }

}
