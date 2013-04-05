package neur.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.util.Log;

public class TcpjServer implements Callable<Integer> {

    public static final int
            CMD_SUBMIT_COMPUTATION = 1
            ,CMD_QUERY_COMPUTATION_RESULT = 2
            ,CMD_QUERY_COMPUTATION_RESULT_FORCE = 4
            ,CMD_SUBMIT_COMPUTATION_RESULT = 8
            ,CMD_REGISTER_EXECUTING_CLIENT = 16
            
            ;
    
    boolean ADD_VM = true;
    public volatile boolean keepOpen = true;
    ServerSocket ss;
    AtomicInteger taskId = new AtomicInteger(1);
    Map<Integer, LearnRecord> tasks = new ConcurrentHashMap<>();
    List<Computer> executing = new CopyOnWriteArrayList<>();
    ConcurrentLinkedQueue<Integer> taskQueue = new ConcurrentLinkedQueue<>();
    private ExecutorService servicePool = Executors.newCachedThreadPool();
    
    Log log = Log.create.chained(Log.file.bind("neur-server.log"), Log.tcout);
    
    private static final Object pollNfy = new Object();
    
    
    public TcpjServer(int port) throws IOException
    {
        ss = new ServerSocket(port);
        ss.setReuseAddress(true);
        ss.setSoTimeout(0);
        log.log("server socket opened at port %d", port);
    }


    
    
    
    private void handle(Socket s)
    {
        InputStream in = null;
        OutputStream out = null;
        boolean closeSocket = true;
        try
        {
            in = s.getInputStream();
            out = s.getOutputStream();
            final ObjectInputStream oin = new ObjectInputStream(in);
            final ObjectOutputStream oout = new ObjectOutputStream(out);
            int command = oin.readInt();
            log.log("  cmd=%d", command);
            switch(command)
            {
                case CMD_SUBMIT_COMPUTATION:
                    LearnParams p = (LearnParams)oin.readObject();
                    int outid = taskId.getAndAdd(1);
                    oout.writeInt(outid);
                    log.log("queuing task %d", outid);
                    LearnRecord lrec0 = new LearnRecord(p);
                    tasks.put(outid, lrec0);
                    taskQueue.add(outid);
                    try
                    {
                        synchronized(pollNfy)
                        {
                            pollNfy.notifyAll();
                        }
                    }
                    catch (Exception e)
                    {
                        log.err("pollNfy.notifyAll", e);
                    }
                    break;
                case CMD_QUERY_COMPUTATION_RESULT:
                case CMD_QUERY_COMPUTATION_RESULT_FORCE:
                    int inid = oin.readInt();
                    oout.writeInt(inid);
                    LearnRecord r = tasks.get(inid);
                    if (r == null)
                    {
                        oout.writeInt(-1);
                        break;
                    }
                    long time = System.currentTimeMillis();
                    while (System.currentTimeMillis() - time < 9000L && r.items.size() < r.p.NUMBER_OF_TRAINING_SETS)
                        sleepq(40L);
                    log.log("q:%d; ready=%d", inid, r.items.size());
                    oout.writeInt(r.items.size());
                    if (r.isAggregated() || command == CMD_QUERY_COMPUTATION_RESULT_FORCE)
                    {
                        oout.writeObject(r);
                    }
                    break;
                case CMD_SUBMIT_COMPUTATION_RESULT:
                    int insid = oin.readInt();
                    LearnRecord ri = (LearnRecord)oin.readObject();
                    submitResults(insid, ri);
                    oout.writeInt(insid);
                    break;
                case CMD_REGISTER_EXECUTING_CLIENT:
                    log.log("registering new executing client");
                    ComputerProxy proxy = new ComputerProxy();
                    proxy.s_ = s;
                    proxy.in_ = in;
                    proxy.out_ = out;
                    proxy.oin_ = oin;
                    proxy.oout_ = oout;
                    proxy.serv = this;
                    executing.add(proxy);
                    closeSocket = false;
                    break;
            }
            oout.flush();
        }
        catch (Exception e)
        {
            log.err("error processing client input", e);
        }
        finally
        {
            if (closeSocket)
            {
                closeSocket(in, out, s);
            }
        }
    }

    
    @Override
    public Integer call() throws Exception
    {
        // start a thread for dequeuing and submitting computations
        Runnable poller = new Runnable()
        {
            int offset = 0;
            @Override
            public void run()
            {
                log.log("started server poller thread");
                for(;;)
                {
                    if (!taskQueue.isEmpty())
                    {
                        while (executing.size() == 0)
                        {
                            sleepq(40L);
                            continue;
                        }
                        final int id = taskQueue.poll();
                        log.log("dequed task %d", id);

                        Runnable r = new Runnable()
                        { public void run() 
                        {
                            for(int next = offset++; ; next++)
                            {
                                int ind = next % executing.size();
                                Computer exi = executing.get(ind);
                                if (exi.isReserved())
                                {
                                    if (ind == 0 && next > 0)
                                    {
                                        sleepq(40L);
                                        System.out.print(".");
                                    }
                                    continue;
                                }
                                log.log("submit    %d to %s", id, exi);
                                if (exi.execute(id, tasks.get(id)))
                                {
                                    if (exi instanceof VmComputer)
                                    {}
                                    else
                                    {
                                        executing.remove(ind);
                                    }
                                    log.log("finished  %d in %s", id, exi);
                                    break;
                                }
                            }
                        }};
                        servicePool.execute(r);
                    }
                    try
                    {
                        synchronized(pollNfy)
                        {
                            pollNfy.wait(1000L);
                        }
                    }
                    catch (Exception e) {
                        log.err("error on wait, pollNfy,", e);
                    }
                }
            }
        };
        Thread pollerTh = new Thread(poller);
        pollerTh.setDaemon(true);
        pollerTh.start();
        
        
        // add one vm executor to make server functional 
        if (ADD_VM)
        {
            executing.add(new VmComputer(this));
            log.log("added a vm executor");
        }
        
        // start server main loop
        while(keepOpen)
        {
            Socket s = null;
            try
            {
                final Socket s_ = s = ss.accept();
                log.log("new cli %s", s.getRemoteSocketAddress());
                Runnable r = new Runnable() { @Override public void run() { handle(s_); } };
                servicePool.execute(r);
            }
            catch (Exception e)
            {
                log.err("error accepting client", e);
                try {
                    if (s != null)
                        s.close();
                } catch (Exception f) {
                }
            }
        }
        return 0;
    }

    public void submitResults(int insid, LearnRecord ri)
    {
        LearnRecord ro = tasks.get(insid);
        if (ro == ri)
        {
            log.log("result %d, ok", insid);
            return;
        }
        log.log("register result %d, oldsize %d, adding %d", insid, ro.items.size(), ri.items.size());
        ro.items.addAll(ri.items);
        if (ro.items.size() >= ro.p.NUMBER_OF_TRAINING_SETS)
        {
            ro.aggregateResults();
            log.log("aggregated %d", insid);
        }
    }

    
    
    public static void main(String[] args) throws Exception
    {
        int PORT = 11111;
        boolean NO_VM = false;
        for(int i = 0; i < args.length; i++)
        {
            if (args[i].matches("[1-9]\\d{0,5}"))
                PORT = Integer.parseInt(args[0]);
            else if ("NOVM".equals(args[i]))
                NO_VM = true;
                
        }
        TcpjServer s = new TcpjServer(PORT);
        s.ADD_VM = !NO_VM;
        s.call();
    }

    static void sleepq(long l)
    {
        try
        {
            Thread.sleep(l);
        } catch (Exception e) {}
    }

    static void closeSocket(InputStream in, OutputStream out, Socket s)
    {
        try {   if (in != null)     in.close();     } catch (Exception e) { }
        try {   if (out != null)    out.close();    } catch (Exception e) { }
        try {   if (s != null)      s.close();      } catch (Exception e) { }
    }


    
    
}
