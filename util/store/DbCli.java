package util.store;

import java.sql.Connection;
import java.sql.SQLException;

public interface DbCli {
    
    public Connection   getConnection()                                 throws SQLException;
    public void         close()                                         throws SQLException;
    public void         execute(String sql)                             throws SQLException;
    public Object[]     execute(String sql, String[] generatedIdCols)   throws SQLException;
    public Q            query(String sql)                               throws SQLException;
    public Object[]     singleResult(Object[] into, String sql)         throws SQLException;            
    public int          count(Q q)                                      throws SQLException;
    public int          count(String tableName, String where)           throws SQLException;    

    
    public static class x
    {
        private static DbClimp cli = null;
        private static DbClimp _cli(String jdbcUrl)
        {
            return cli == null ? (cli = new DbClimp(jdbcUrl)) : cli;
        }
        public static DbCli cli()
        {
            return _cli("jdbc:hsqldb:hsql://localhost:9001/neur");
        }
    }
    
}
