
package util.store;

import java.sql.*;

public class DbClimp implements DbCli {
    
    public Connection c;
    private String connectionString;
    
    public DbClimp(String con)
    {
        this.connectionString = con;
    }
    
    public Connection getConnection() throws SQLException
    {
        if (c == null)
        {
            c = DriverManager.getConnection(connectionString);
        }
        return c;
    }
    
    public void execute(String sql) throws SQLException
    {
        getConnection();
        Statement st = c.createStatement();
        st.execute(sql);
        st.close();
    }
    public Object[] execute(String sql, String[] generatedIdCols) throws SQLException
    {
        getConnection();
        Statement st = c.createStatement();
        st.execute(sql, generatedIdCols);
        ResultSet rs = st.getGeneratedKeys();
        Object[] ret = null;
        if (rs != null)
        {
            ret = new Object[generatedIdCols.length];
            if (rs.next())
            for(int i = 0; i < ret.length; i++)
            {
                ret[i] = rs.getObject(i+1);
            }
            rs.close();
        }
        st.close();
        return ret;
    }

    public void close() throws SQLException
    {
        if (c != null)
            c.close();
    }


    

    public Q query(String sql) throws SQLException
    {
        return new Q<DbClimp>(sql, this);
    }
    
    public Object[] singleResult(Object[] into, String sql) throws SQLException
    {
        Q q = query(sql);
        try
        {
            if (!q.rs.next())
                throw new RuntimeException("result set is empty for: " + sql);
            for (int i = 0; i < into.length; i++) {
                into[i] = q.rs.getObject(i+1);
            }
        }
        finally
        {
            q.close();
        }
        return into;
    }

    public int count(Q q) throws SQLException
    {
        int i = 0;
        while(q.rs.next()) i++;
        q.close();
        return i;
    }

    public int count(String tableName, String where) throws SQLException
    {
        return count(query(
                "select null from "+tableName
                +(where!=null?" where "+where:"")));
    }

}
