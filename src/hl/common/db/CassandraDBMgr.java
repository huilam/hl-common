package hl.common.db;

import java.net.InetSocketAddress;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.*;


public class CassandraDBMgr {

	public final static int _CQL_DEF_PORT			= 9042;
	
	
	public final static String _SCHEMA_SYSTEM 		= "system_schema";
	public final static String _CQL_WHERE 			= "WHERE";
	public final static String _CQL_ALLOW_FILTERING = "ALLOW FILTERING";
	
	public final static String _DATATYPE_BIGINT = "bigint";
	public final static String _DATATYPE_DOUBLE = "double";
	public final static String _DATATYPE_TEXT 	= "text";
	
	private CqlSessionBuilder cqlSessionbuilder = null;
	private List<CqlSession> listCqlSession = new ArrayList<CqlSession>();
	private int min_cqlsession_size = 10;
	
	private static List<String> listNumericType 		= null;
	private static List<String> listDoubleType 			= null;
	private static List<String> listFloatType_32bits 	= null;
	
	private static Logger logger = Logger.getLogger(CassandraDBMgr.class.getName());
	
	static{
		listNumericType = new ArrayList<String>();
		listNumericType.add(int.class.getSimpleName());
		listNumericType.add(Integer.class.getSimpleName());
		listNumericType.add(long.class.getSimpleName());
		listNumericType.add(Long.class.getSimpleName());

		listDoubleType = new ArrayList<String>();
		listDoubleType.add(double.class.getSimpleName());
		listDoubleType.add(Double.class.getSimpleName());
		
		listFloatType_32bits = new ArrayList<String>();
		listFloatType_32bits.add(float.class.getSimpleName());
		listFloatType_32bits.add(Float.class.getSimpleName());
		
	}
	
	
	public CassandraDBMgr()
	{
		String sIP = "127.0.0.1";//"172.30.92.6";
		
		this.cqlSessionbuilder = CqlSession.builder();
		//this.cqlSessionbuilder.addContactPoint(new InetSocketAddress(sIP, _CQL_DEF_PORT));
		
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
	
	public JSONArray queryVersion()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT ");
		//sb.append("   * ");
		sb.append("   host_id, cluster_name, release_version as server_version, cql_version ");
		sb.append(" FROM ");
		sb.append("   system.local");
		
		return executeCql(sb.toString(), null);
	}
	
	public JSONArray listKeyspaces()
	{
		List<Object> listParam = new ArrayList<Object>();
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT ");
		sb.append("   keyspace_name ");
		sb.append(" FROM ");
		sb.append(_SCHEMA_SYSTEM).append(".keyspaces");
		
		return executeCql(sb.toString(),
				listParam.toArray(new Object[listParam.size()]));	
	}
	
	public JSONArray listTables(String aKeyspaceName)
	{
		List<Object> listParam = new ArrayList<Object>();
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT ");
		sb.append("   table_name, keyspace_name ");
		sb.append(" FROM ");
		sb.append(_SCHEMA_SYSTEM).append(".tables");
		if(aKeyspaceName!=null && aKeyspaceName.trim().length()>0)
		{
			sb.append(" WHERE keyspace_name = ?");
			listParam.add(aKeyspaceName);
		}
		return executeCql(sb.toString(), 
				listParam.toArray(new Object[listParam.size()]));	
	}
	
	public JSONArray listColumns(String aTableName)
	{
		List<Object> listParam = new ArrayList<Object>();
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT ");
		sb.append("   column_name, type, table_name ");
		sb.append(" FROM ");
		sb.append(_SCHEMA_SYSTEM).append(".columns");
		if(aTableName!=null && aTableName.trim().length()>0)
		{
			sb.append(" WHERE table_name = ? ");
			listParam.add(aTableName);
		}
		return executeCql(sb.toString(), 
				listParam.toArray(new Object[listParam.size()]));	
	}
	
	public JSONArray executeCql(String aCqlString, Object[] params)
	{
		JSONArray jsonArrs 	= new JSONArray();
		
		CqlSession session = null;
		try {
			session = getCqlSession();
		
			String aUpperCql = aCqlString.toUpperCase();
			
			
			if(aUpperCql.indexOf(_CQL_WHERE)>-1 
					&& aUpperCql.indexOf(_CQL_ALLOW_FILTERING)==-1)
			{
				aCqlString += " "+_CQL_ALLOW_FILTERING;
			}
			
			PreparedStatement stmt = session.prepare(aCqlString);
			BoundStatement bstmt = setParams(stmt, params);
			
			ResultSet rs = null;
			try {
				rs = session.execute(bstmt);
				for(Row row : rs.all())
				{
					JSONObject jsonData = new JSONObject();
					for(ColumnDefinition col : row.getColumnDefinitions())
					{
						jsonData.put(col.getName().toString(), 
								row.getObject(col.getName()));
					}
					jsonArrs.put(jsonData);
				}
			}catch(Exception ex)
			{
				System.err.println("cql:"+aCqlString);
				System.err.println("params:"+ String.join(",", Arrays.copyOf(params, params.length, String[].class)));
				throw ex;
			}
		}
		finally
		{
			closeCqlSession(session);
		}
		
		return jsonArrs;
	}
	
	public static BoundStatement setParams(PreparedStatement aStatement, List<Object> aParamList ) throws NumberFormatException, SQLException
	{
		if(aParamList!=null)
			return setParams(aStatement, aParamList.toArray(new Object[aParamList.size()]));
		else
			return setParams(aStatement, new Object[]{});
	}
	
	public static BoundStatement setParams(PreparedStatement aStatement, Object[] aParams ) 
	{
		BoundStatementBuilder bound = null;
		if(aStatement!=null)
		{
			bound = new BoundStatementBuilder(aStatement.bind());
			if(aParams!=null && aParams.length>0)
			{
				for(int i=0; i<aParams.length; i++)
				{
					Object param = aParams[i];
					
					String sParamClassName = param.getClass().getSimpleName();
					if(String.class.getSimpleName().equals(sParamClassName))
					{
						bound.setString(i, param.toString());
					}
					else if(listNumericType.contains(sParamClassName))
					{
						bound.setLong(i, Long.parseLong(param.toString()));
					}
					else if((Date.class.getSimpleName().equals(sParamClassName))
							|| (Timestamp.class.getSimpleName().equals(sParamClassName)))
					{
						//store as epoch
						bound.setLong(i, ((Date)param).getTime());
					}
					else if(listDoubleType.contains(sParamClassName))
					{
						bound.setDouble(i, Double.parseDouble(param.toString()));
					}
					else if(listFloatType_32bits.contains(sParamClassName))
					{
						bound.setFloat(i, Float.parseFloat(param.toString()));
					}
					else
					{
						logger.log(Level.SEVERE, 
								"Unsupported datatype : "+sParamClassName+" - "+String.valueOf(param));
					}
				}
			}
		}
		
		if(bound==null)
			return null;
		
		return bound.build();
	}
	
	
	public static void main(String args[]) throws Exception
	{
		CassandraDBMgr m = new CassandraDBMgr();
		System.out.println("version:"+m.queryVersion());
		
		JSONArray jsonArrKeyspaces = null;
		
		jsonArrKeyspaces = m.listKeyspaces();
		
		for(int i=1; i<=jsonArrKeyspaces.length(); i++)
		{
			String sKeySpaceName = jsonArrKeyspaces.getJSONObject(i-1).getString("keyspace_name");
			System.out.println("  "+i+". [keyspace]"+sKeySpaceName+" ====================");
			System.out.println();
			JSONArray jsonArrTables = m.listTables(sKeySpaceName);
			for(int i2=1; i2<=jsonArrTables.length(); i2++)
			{
				String sTableName = jsonArrTables.getJSONObject(i2-1).getString("table_name");
				System.out.println("  "+i+"."+i2+" [table]"+sTableName);
				
				JSONArray jsonArrCols = m.listColumns(sTableName);
				for(int i3=1; i3<=jsonArrCols.length(); i3++)
				{
					String sColName = jsonArrCols.getJSONObject(i3-1).getString("column_name");
					System.out.println("  "+i+"."+i2+"."+i3+" "+sColName);
				}
				System.out.println();
			}
		}
		
		
		m.close();
	}
    
}
