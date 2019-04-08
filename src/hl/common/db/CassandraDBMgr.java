package hl.common.db;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.*;


public class CassandraDBMgr {

	public final static String _DATATYPE_BIGINT = "bigint";
	public final static String _DATATYPE_DOUBLE = "double";
	public final static String _DATATYPE_TEXT 	= "text";
	
	private CqlSessionBuilder cqlSessionbuilder = null;
	private List<CqlSession> listCqlSession = new ArrayList<CqlSession>();
	private int min_cqlsession_size = 10;
	
	
	public CassandraDBMgr()
	{
		this.cqlSessionbuilder = CqlSession.builder();
		CqlSession ss = this.cqlSessionbuilder.build();
		if(ss!=null && !ss.isClosed())
		{
			ss.close();
		}
	}
	
	public void initCqlSessionPool()
	{
		CqlSession ss = null;
	
		try {
			for(int i=0;i<this.min_cqlsession_size;i++)
			{
				ss = cqlSessionbuilder.build();
				listCqlSession.add(ss);
			}
		}
		finally
		{
			
		}
	}
	
	public void closeCqlSession(CqlSession aCqlSession)
	{
		if(aCqlSession!=null)
		{
			if(listCqlSession.size()<this.min_cqlsession_size)
			{
				listCqlSession.add(aCqlSession);
			}
			else
			{
				aCqlSession.close();
			}
		}
	}
	
	public CqlSession getCqlSession()
	{
		CqlSession ss = null;
		
		if(listCqlSession.size()>0)
			ss = listCqlSession.remove(0);
		else
			ss = cqlSessionbuilder.build();
		
		return ss;
	}
	
	public void close()
	{
		for(CqlSession s : listCqlSession)
		{
			if(!s.isClosed())
			{
				s.closeAsync();
			}
		}
	}
	
	public JSONObject queryVersion()
	{
		return executeCql("select release_version as cassandra_version, cql_version from system.local");
	}
	
	public JSONObject listKeyspaces()
	{
		return executeCql("SELECT keyspace_name FROM system_schema.keyspaces");
	}
	
	public JSONObject listTables(String aKeyspaceName)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT table_name, keyspace_name FROM system_schema.tables ");
		if(aKeyspaceName!=null && aKeyspaceName.trim().length()>0)
		{
			sb.append("WHERE keyspace_name = ").append(aKeyspaceName);
		}
		return executeCql(sb.toString());
	}
	
	public JSONObject listColumns(String aTableName)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT column_name, type, table_name FROM system_schema.columns ");
		if(aTableName!=null && aTableName.trim().length()>0)
		{
			sb.append("WHERE table_name = ").append(aTableName);
		}
		return executeCql(sb.toString());
	}
	
	public JSONObject executeCql(String aCqlString)
	{
		JSONObject jsonData = new JSONObject();
		
		CqlSession session = null;
		try {
				session = getCqlSession();
				ResultSet rs = session.execute(aCqlString);
				Row row = rs.one();
				if(row!=null)
				{
					for(ColumnDefinition col : row.getColumnDefinitions())
					{
						jsonData.put(col.getName().toString(), 
								row.getObject(col.getName()));
					}
				}
		}
		finally
		{
			closeCqlSession(session);
		}
		
		return jsonData;
	}
	
	
	public static void main(String args[]) throws Exception
	{
		CassandraDBMgr m = new CassandraDBMgr();
		System.out.println("version:"+m.queryVersion());
		
		System.out.println(m.listKeyspaces());
		System.out.println(m.listTables(null));
		System.out.println(m.listColumns(null));
		m.close();
	}
    
}
