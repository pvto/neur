
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
        private static long time = System.currentTimeMillis();
        private static void flush(boolean force) throws IOException
        {
            long next = System.currentTimeMillis();
            if (!force && next - time < 1000L)
                return;
            synchronized(file.class)
            {
                for(OutputStream out : outs)
                    out.flush();
            }
            time = next;
        }
        private static void close() throws IOException
        {
            for (int i = 0; i < outs.size();) {
                outs.remove(0).close();
            }
        }
        private static int init = 0;
        private static void init()
        {
            if (init++ > 0)
                return;
            Runnable logflush = new Runnable()
            {
                @Override
                public void run() {
                    for(;;)
                    {
                        try {
                            synchronized(lock)
                            {
                                lock.wait(15000L);
                            }
                            flush(false);
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread flush = new Thread(logflush);
            flush.setDaemon(true);
            flush.setName("neur-log");
            flush.start();
            Runnable fine = new  Runnable()
            {
                public void run()
                {
                    try { 
                        flush(true);
                    } catch(Exception ex)
                    { ex.printStackTrace(); }
                    System.out.println(":fine");
                }
            };
            Runtime.getRuntime().addShutdownHook(new Thread(fine));
        }
        private static void nfy()
        {
            try { 
                synchronized(lock) { lock.notify(); } } 
            catch(Exception ex) { }
        }
        public static synchronized Log          bind(String filename)
        {
            init();
            final String filename_ = new File(filename).getAbsolutePath();
            Log log = fileLogs.get(filename_);
            if (log != null) 
                return log;
            
            log = new Log()
            {
                private SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss' '");
                private OutputStream out;
                {
                    try {
                        out = new BufferedOutputStream(new FileOutputStream(new File(filename_), true));
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
