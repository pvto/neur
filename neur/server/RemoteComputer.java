package neur.server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import neur.MLP;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.learning.Teachers;
import neur.util.Log;


public class RemoteComputer implements Callable<int[]> {

    public String host;
    public int port;
    public int WAIT_SECS = 60;
    
    public volatile boolean execute = true;
    
    Log log = Log.create.chained(Log.file.bind("remo-exec.log"), Log.tcout);
    
    @Override
    public int[] call() throws Exception
    {
        log.log("start remote, host=%s:%d", host, port);
        int executed = 0;
        int failed = 0;
        while(execute)
        {
            Socket s = null;
            InputStream in = null;
            OutputStream out = null;
            LearnRecord r = null;
            int id = 0;
            try
            {
                log.log("connecting %s:%d", host, port);
                s = new Socket(host, port);
                s.setSoTimeout(WAIT_SECS * 1000);
                out = s.getOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(out);
                oout.writeInt(TcpjServer.CMD_REGISTER_EXECUTING_CLIENT);
                oout.writeInt(Computer.CLIENT_MSG_1);
                oout.flush();
                log.log("written intro");
                in = s.getInputStream();
                ObjectInputStream oin = new ObjectInputStream(in);
                id = oin.readInt();
                LearnParams p = (LearnParams)oin.readObject();
                r = (LearnRecord)oin.readObject();
                log.log("read id=%d,p,n=%d", id, p.NUMBER_OF_TRAINING_SETS);
                r.p = p;
                oout.writeInt(Computer.CLIENT_MSG_2);
                oout.flush();
                log.log("written ack");
            }
            catch(SocketTimeoutException ste)
            {
                log.log("socket timeout, reconnecting");
                continue;
            }
            catch(ConnectException ce)
            {
                TcpjServer.sleepq(5000L);
                continue;
            }
            catch(Exception ey)
            {
                log.err("error fetching task", ey);
                failed++;
                continue;
            }
            finally
            {
                TcpjServer.closeSocket(in, out, s);
                s = null;
                in = null;
                out = null;
            }
            
            try
            {
                int check = 0;
                while(r.p.getNumberOfPendingTrainingSets(r) > 0)
                {
                    r.p.nnw = new MLP(r.p);
                    r.p.D.initTrain_Test_Sets(r.p.TESTSET_SIZE, r.p.DATASET_SLICING);
                    new Teachers().tabooBoxAndIntensification(r.p, r, log);
                    if (check++ > 20 && r.p.getNumberOfPendingTrainingSets(r) == r.p.NUMBER_OF_TRAINING_SETS)
                    {
                        log.log("no result");
                    }
                }
                r.aggregateResults();
                
                s = new Socket(host, port);
                out = s.getOutputStream();
                log.log("writing result %d, n=%d", id, r.items.size());
                ObjectOutputStream oout = new ObjectOutputStream(out);
                oout.writeInt(TcpjServer.CMD_SUBMIT_COMPUTATION_RESULT);
                oout.writeInt(id);
                oout.writeObject(r);
                oout.flush();
                log.log("written");
                in = s.getInputStream();
                ObjectInputStream oin = new ObjectInputStream(in);
                check = oin.readInt();
                if (check != id)
                    throw new IllegalArgumentException("expecting server ack-id " + id + ", got " + check);
                log.log("got ack");
                executed++;
            }
            catch(Exception e)
            {
                log.err("failed to execute learning ", e);
                failed++;
            }
            finally
            {
                TcpjServer.closeSocket(in, out, s);
            }

            
        }
        return new int[]{executed, failed};
    }
    
    
    public static void main(String[] args) throws Exception
    {
        String host = args.length > 0 ? args[0] : "localhost";
        Integer port = args.length > 1 ? Integer.parseInt(args[1]) : 11111;
        RemoteComputer je = new RemoteComputer();
        je.host = host;
        je.port = port;
        je.call();
    }
}
