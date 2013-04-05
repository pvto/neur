package neur.server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.util.Log;


public class SubmittingClient implements Callable<int[]> {

    public String host;
    public int port;
    public int WAIT_SECS = 20;
    
    public volatile boolean execute = true;
    private ExecutorService queryPool = Executors.newCachedThreadPool();
    private ConcurrentHashMap queryStatus = new ConcurrentHashMap();
    
    Log log = Log.create.chained(Log.file.bind("remo-exec.log"), Log.cout);
    
    private final Object inNfy = new Object();
    private ConcurrentLinkedQueue<LearnParams> queueIn = new ConcurrentLinkedQueue<>();
    public void queueIn(LearnParams p)
    { 
        queueIn.add(p);
        try
        {
            synchronized(inNfy)
            {
                inNfy.notifyAll();
            }
        }
        catch (Exception e)
        {
            log.log("inNfy error: " + e.getMessage());
        }
    } 
    private List<Tuple> processing = new CopyOnWriteArrayList<>();
    public ConcurrentLinkedQueue<Tuple> queueOutFinished = new ConcurrentLinkedQueue<>();
    public List<Observer> observers = new CopyOnWriteArrayList<>();

    
    public static class Tuple
    {
        int id;
        int numberOfFinished = 0;
        long start = System.currentTimeMillis(), 
                lastUpdate = System.currentTimeMillis(),
                lastRequest = System.currentTimeMillis(),
                avgComputationTime = 0;
        public LearnParams p;
        public LearnRecord r;
    }
    int executed = 0;
    int failed = 0;


    @Override
    public int[] call()
    {
        while(execute)
        {
            if (queueIn.size() > 0)
            {
                submit();
            }
            else
            {
                if (processing.size() > 0)
                {
                    getUpdates();
                }
            }
            try
            {
                synchronized(inNfy)
                {
                    inNfy.wait(1000L);
                }
            }
            catch (Exception e)
            {
                log.log("inNfy wait error: " + e.getMessage());
            }
            
        }
        return new int[]{executed, failed};
    }

    private void submit()
    {
        Socket s = null;
        InputStream in = null;
        OutputStream out = null;
        LearnParams p = queueIn.peek();
        LearnRecord r = null;
        int id = 0;
        try
        {
            log.log("connecting...");
            s = new Socket(host, port);
            log.log("connected");
            s.setSoTimeout(WAIT_SECS * 10000);
            out = s.getOutputStream();
            log.log("got output stream");
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeInt(TcpjServer.CMD_SUBMIT_COMPUTATION);
            oout.writeObject(p);
            oout.flush();
            log.log("written data");
            in = s.getInputStream();
            log.log("got input stream");
            ObjectInputStream oin = new ObjectInputStream(in);
            id = oin.readInt();
            Tuple t = new Tuple();  t.id = id;  t.p = p;
            processing.add(t);
            queueIn.poll();
            log.log("submitted, got id=%d", id);
        }
        catch(Exception ey)
        {
            log.err("cannot submit", ey);
            failed++;
        }
        finally
        {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
            }
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
            }
            try {
                if (s != null)
                    s.close();
            } catch (Exception e) {
            }
            s = null;
            in = null;
            out = null;
        }
    
    }
    

    private void getUpdates()
    {
        for(final Tuple rec : processing)
        {
            if (rec.lastRequest > rec.lastUpdate
                    || System.currentTimeMillis() - rec.lastUpdate < rec.avgComputationTime / 4L)
            {
                continue;
            }
            if (queryStatus.contains(rec.id))
            {
                continue;
            }
            Runnable r = new Runnable()
            {   public void run()
                {
                    getUpdate(rec);
                }
            };
            queryStatus.put(rec.id, rec);
            queryPool.execute(r);
        }
    }
    
    private void getUpdate(Tuple rec)
    {
        Socket s = null;
        InputStream in = null;
        OutputStream out = null;
        try
        {
            log.log("get updates %d %dms since last", rec.id, (System.currentTimeMillis()-rec.lastUpdate));
            s = new Socket(host, port);
            s.setSoTimeout(WAIT_SECS * 1000);
            log.log("connected");
            out = s.getOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeInt(TcpjServer.CMD_QUERY_COMPUTATION_RESULT);
            oout.writeInt(rec.id);
            log.log("wrote %d", rec.id);
            rec.lastRequest = System.currentTimeMillis();
            oout.flush();
            in = s.getInputStream();
            ObjectInputStream oin = new ObjectInputStream(in);
            int checkid = oin.readInt();
            int finished = oin.readInt();
            log.log("got %d, ready=%d, expected=%d", checkid, finished, rec.p.NUMBER_OF_TRAINING_SETS);
            if (checkid != rec.id)
                throw new IllegalArgumentException("server responded with wrong id to query computation result " + rec.id);
            if (finished == -1)
                throw new RuntimeException("server indicated that computation is stopped prematurely for " + rec.id);
            if (finished >= rec.p.NUMBER_OF_TRAINING_SETS)
            {
                rec.r = (LearnRecord)oin.readObject();
                log.log("receive data %d, size=%d", rec.id, rec.r.items.size());
                rec.r.p = rec.p;
                queueOutFinished.add(rec);
                processing.remove(rec);
                for(Observer o : observers)
                {
                    try
                    {
                        o.update(null, rec);
                    }
                    catch (Exception e)
                    {
                        log.err("can't send to observer", e);
                    }
                }
            }
            else
            {
                long tmp = System.currentTimeMillis();
                int newFinished = finished - rec.numberOfFinished;
                if (finished > 0)
                {
                    rec.avgComputationTime = (tmp - rec.start) / finished;
                }
                else
                {
                    rec.avgComputationTime = tmp - rec.start;
                }
                rec.lastUpdate = tmp;
                
            }
            rec.numberOfFinished = finished;
        }
        catch(Exception ey)
        {
            failed++;
            log.err("cannot read update", ey);
        }
        finally
        {
            TcpjServer.closeSocket(in, out, s);
            queryStatus.remove(rec.id);
        }

    }

}
