
package neur.util.dataio;

import java.sql.SQLException;
import util.store.DbCli;
import util.store.Q;


public final class DbSamples {

    private final DbCli cli;

    public DbSamples(DbCli dbClient)
    {
        this.cli = dbClient;
    }


    public void saveSample(int datasetId, String datasetName, String sampleName, float[][][] data) throws SQLException
    {
        cli.getConnection().setAutoCommit(false);
        if (cli.count("d_dataset", "dataset_id = " + datasetId) == 0)
        {
            cli.execute(Q.insertInto("d_dataset").a("dataset_id",datasetId,"name",datasetName,"description","").z());
        }
        String sa = Q.insertInto("d_sample").a("dataset_id",datasetId,"name",sampleName,"size",data.length).z();
        Object[] said = cli.execute(sa, new String[]{"SAMPLE_ID"});
        int i = 0;
        for(float[][] specimen : data)
        {
            String sp = Q.insertInto("d_specimen").a("sample_id",said[0],"sp_number",i).z();
            Object[] spid = cli.execute(sp, new String[]{"SPECIMEN_ID"});
            for (int x = 0; x < specimen.length; x++)
            {
                for (int j = 0; j < specimen[x].length; j++)
                {
                    cli.execute(Q.insertInto("d_dataitem")
                            .a("specimen_id",spid[0], "index",j, "xgroup",x).a("data",specimen[x][j],"%f").z());
                }
            }
            i++;
        }
        cli.getConnection().commit();
        cli.getConnection().setAutoCommit(true);
    }



    public float[][][] loadSample(int dataset, String sample) throws SQLException
    {
        int size = (Integer) 
            cli.singleResult(new Object[1], 
                "select s.size from d_sample s,d_dataset d"
                +" where d.dataset_id=s.dataset_id and d.dataset_id="+dataset+" and s.name='"+sample+"'"
                )[0];
        float[][][] data = new float[size][][];
        Q q = cli.query(
                "select * from sample s where s.dataset_id="+dataset+" and s.sample='"+sample+"'"
                +" order by s.number desc, s.xgroup desc, s.index desc");
        while(q.rs.next())
        {
            int number = q.rs.getInt("NUMBER");
            int xg = q.rs.getInt("XGROUP");
            int ind = q.rs.getInt("INDEX");
            if (data[number] == null)
            {
                data[number] = new float[xg + 1][];
            }
            if (data[number][xg] == null)
            {
                data[number][xg] = new float[ind + 1];
            }
            data[number][xg][ind] = q.rs.getFloat("DATA");
        }
        q.close();
        return data;
    }

}
