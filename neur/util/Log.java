
package neur.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public interface Log {
    
    void log(String fmtStr, Object ... pars);
 
    
    
    
    
    
    public static Log log = new Log()
    {
        private SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        public StringBuilder bd = new StringBuilder();
        public void log(String f, Object ... fs)
        {
            String s = String.format(f, fs);
            bd.append(df.format(new Date())).append(" ")
                    .append(s).append('\n');
            System.out.println(s);
        }
    };

}
