
package neur.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;


public interface Log {

    
    void log(String fmtStr, Object ... params);
    
    
    void err(String str, Throwable t);
    
    
    
    
    // --- some log implementations follow --- //
    
    public static Log cout = new Log()
    {
        public void log(String f, Object ... params) {
            System.out.println(String.format(f, params));
        }
        public void err(String str, Throwable t) {
            System.out.println(str);
            t.printStackTrace();
        }
    };

    
    public static class create
    {
        public static Log chained(final Log... logs) {
            return new Log()
            {
                public void log(String fmtStr, Object... params) {
                    for(Log log : logs)
                        log.log(fmtStr, params);
                }

                @Override
                public void err(String str, Throwable t) {
                    for(Log log : logs)
                        log.err(str, t);
                }
            };
        } 
    }
    
    public static class file
    {
        public static Map<String,Log> fileLogs = new ConcurrentHashMap<String,Log>();
        private static List<OutputStream> outs = new ArrayList<OutputStream>();
        private static final Object lock = new Object();
        {
            Runnable logflush = new Runnable()
            {
                @Override
                public void run() {
                    long time = System.currentTimeMillis();
                    for(;;)
                    {
                        try {
                            System.out.print("1");
                            synchronized(lock)
                            {
                                lock.wait(15000L);
                            }
                            System.out.print("2");
                            long next = System.currentTimeMillis();
                            if (next - time < 1000L)
                                continue;
                            for(OutputStream out : outs)
                                out.flush();
                            System.out.print("3");
                            time = next;
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            //new Thread(logflush).start();
        }
        private static void nfy()
        {
            try { 
                synchronized(lock) { lock.notify(); } } 
            catch(Exception ex) { }
        }
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
                        out = new BufferedOutputStream(new FileOutputStream(new File("gen-srch.log"), true));
                        outs.add(out);
                    } 
                    catch(IOException e) { e.printStackTrace(); }
                }
                @Override
                public void log(String f, Object ... params)
                {
                    String s = String.format(f, params);
//                    System.out.println(s);
                    try {
                        out.write(df.format(new Date()).getBytes());
                        out.write(s.getBytes());  out.write("\r\n".getBytes());
                                            nfy();
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
                        nfy();
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
