
package util.store;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Paavo Toivanen
 */
public final class Q <T extends DbCli>
{
    public ResultSet rs;
    Statement st;
    
    public Q(String sql, T cli) throws SQLException
    {
        st = cli.getConnection().createStatement();
        rs = st.executeQuery(sql);
    }

    public void close() throws SQLException {
        rs.close();
        st.close();
    }
    
    
    
    public static Builder insertInto(String table){return new Builder().insertInto(table);}
    public static class Builder {
        private StringBuilder 
                b = new StringBuilder(),
                p = new StringBuilder();
        private boolean colintro = false, insert = false;
        public Builder(){};
        public Builder insertInto(String table){insert=true; b.append("INSERT INTO ").append(table).append(" ("); return this;}
        public Builder a(String col, Object val, String c2, Object v2, Object... rest){
            a(col,val);  a(c2,v2);  for(int i=0;i<rest.length;){a((String)rest[i++],rest[i++]);}  return this;}
        public Builder a(String col, Object val){return a(col,val,null);}
        public Builder a(String col, Object val, String fmt){
            colintro = true; b.append(col); return f(val, "%s");}
        public Builder f(Object val, String fmt){
            boolean hyphens = true;
            if (val == null || val instanceof Integer || val instanceof Long)
                hyphens = false;
            if (hyphens) p.append('\'');
            if(fmt == null){
                if (!hyphens){p.append(val);}
                else if (Number.class.isAssignableFrom(val.getClass())){p.append(String.format("'%.4f'",val));}
                else if (val instanceof Date){p.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(val));}
                else{p.append('\'').append(val.toString()).append('\'');}
            }
            else{
                p.append(String.format(fmt, val));
            }
            if (hyphens) p.append('\'');
            return this;
        }
        public String z(){return b.toString()+(insert?(colintro?") VALUES(":""):"")+p.toString()+(insert?")":"");}
    }
}