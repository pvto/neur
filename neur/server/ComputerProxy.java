package neur.server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import neur.learning.LearnRecord;
import neur.util.Log;



public class ComputerProxy implements Computer {

    Log log = Log.create.chained(Log.file.bind("neur-server.log"), Log.cout);
    
    Socket s_;
    InputStream in_;
    OutputStream out_;
    ObjectInputStream oin_;
    ObjectOutputStream oout_;
    TcpjServer serv;
    
    private boolean reserved = false;
    public boolean isReserved() { return reserved; }
    
    @Override
    public synchronized boolean execute(int id, LearnRecord r)
    {
        if (s_ == null)
            return false;
        reserved = true;
        log.log("%s execute %d", this, id);
        try
        {
            int check = oin_.readInt();
            if (check != Computer.CLIENT_MSG_1)
                throw new IllegalArgumentException("tcp client handshake " + Computer.CLIENT_MSG_1 
                        + " expected, got " + check);
            log.log("got handshake");
            oout_.writeInt(id);
            oout_.flush();
            oout_.writeObject(r.p);
            oout_.writeObject(r);
            oout_.flush();
            log.log("wrote computation task %d", id);
            
            check = oin_.readInt();
            if (check != Computer.CLIENT_MSG_2)
                throw new IllegalArgumentException("tcp client ack " + Computer.CLIENT_MSG_2 
                        + " expected, got " + check);
            TcpjServer.closeSocket(in_, out_, s_);
            
            long time = System.currentTimeMillis();
            long start = time;
            while(!r.isAggregated())
            {
                TcpjServer.sleepq(40L);
                long tmp = System.currentTimeMillis();
                if (tmp - time > 5000L)
                {
                    log.log("waiting for result %d, ready %d", id, r.items.size());
                    time = tmp;
                }
                if (tmp - start > 30L*60L*1000L)
                    throw new IllegalStateException("remote computer died?");
            }
            
        }
        catch (Exception e)
        {
            log.err(""+e.getMessage(), e);
            serv.taskQueue.add(id);
            return false;
        }
        finally
        {
            TcpjServer.closeSocket(in_, out_, s_);
            s_ = null;
            in_ = null;
            out_ = null;
            oin_ = null;
            oout_ = null;
        }
        return true;
    }
    
    
}
