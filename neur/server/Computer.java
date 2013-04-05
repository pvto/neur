package neur.server;

import neur.learning.LearnRecord;


public interface Computer {
    
    public static final int 

            CLIENT_MSG_1 = 13505
            ,CLIENT_MSG_2 = 54735
            
            ;
            
    
    /** non-blocking 
     * @return true if the task was submitted to execution
     */
    boolean execute(int id, LearnRecord r);
    
    boolean isReserved();
}
