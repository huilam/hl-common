/*
 Copyright (c) 2017 onghuilam@gmail.com
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 The Software shall be used for Good, not Evil.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 
 */

package hl.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import hl.common.thread.ThreadMgr;

public class JdbcDBMgr {

	private static Logger logger = Logger.getLogger(JdbcDBMgr.class.getName());
	
	public static String KEY_DB_CLASSNAME 	= "db.jdbc.classname";
	public static String KEY_DB_URL 		= "db.url";
	public static String KEY_DB_UID 		= "db.uid";
	public static String KEY_DB_PWD			= "db.pwd";
	public static String KEY_DB_CONNPOOL	= "db.pool.size";
	
	public Map<String, String> mapReferenceConfig	= null;
	
	public String db_classname 		= null;
	public String db_url 			= null;
	public String db_uid 			= null;
	public String db_pwd 			= null;
	
	public int db_conn_fetchsize	= 0;
	public int db_conn_pool_size	= 2;
	public int db_conn_max			= 0;
	//
	public long conn_wait_interval_ms	= 50; // wait when conn reach db_conn_max count
	public long conn_timeout_ms 		= 5000; // 5 secs
	
	private Stack<Connection> stackConnPool 	= new Stack<Connection>();
	private Map<Connection, Long> mapConnInUse 	= new ConcurrentHashMap<Connection, Long>();
	
	private Map<String, String> mapSQLtemplate 	= new ConcurrentHashMap<String, String>();
	private static List<String> listNumericType = null;
	private static List<String> listDoubleType 	= null;
	private static List<String> listFloatType32bit 	= null;
	private static List<String> listBooleanType = null;
	
	private String database_server_info	= "";
		
	static{
		listNumericType = new ArrayList<String>();
		listNumericType.add(int.class.getSimpleName());
		listNumericType.add(Integer.class.getSimpleName());
		listNumericType.add(long.class.getSimpleName());
		listNumericType.add(Long.class.getSimpleName());
		listNumericType.add(BigInteger.class.getSimpleName());

		listDoubleType = new ArrayList<String>();
		listDoubleType.add(double.class.getSimpleName());
		listDoubleType.add(Double.class.getSimpleName());
		listDoubleType.add(BigDecimal.class.getSimpleName());
		
		listFloatType32bit = new ArrayList<String>();
		listFloatType32bit.add(float.class.getSimpleName());
		listFloatType32bit.add(Float.class.getSimpleName());
		
		listBooleanType = new ArrayList<String>();
		listBooleanType.add(boolean.class.getSimpleName());
		listBooleanType.add(Boolean.class.getSimpleName());

	}
	
	public JdbcDBMgr(String aResourcePath) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		Properties prop = new Properties();
		InputStream in = null;
		try {
			
			File file = new File(aResourcePath);
			if(file.isFile())
			{
				in = new FileInputStream(file);
			}
			else
			{
				in = JdbcDBMgr.class.getResourceAsStream(aResourcePath);
			}
			prop.load(in);
		}
		finally
		{
			if(in!=null)
				in.close();
		}
		initDB(prop);
	}
	
	public void clearSQLtemplates()
	{
		mapSQLtemplate.clear();
	}
	
	public String getSQLtemplates(String aSQLTemplateName)
	{
		return mapSQLtemplate.get(aSQLTemplateName);
	}
	
	public void addSQLtemplates(String aSQLTemplateName, String aSQL)
	{
		mapSQLtemplate.put(aSQLTemplateName, aSQL);
	}	
	
	public JdbcDBMgr(Properties aProp) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		initDB(aProp);
	}
	
	public JdbcDBMgr(String aClassName, String aDBUrl, String aDBUid, String aDBPwd) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		Properties prop = new Properties();
		prop.put(KEY_DB_CLASSNAME, aClassName);
		prop.put(KEY_DB_URL, aDBUrl);
		prop.put(KEY_DB_UID, aDBUid);
		prop.put(KEY_DB_PWD, aDBPwd);
		initDB(prop);
	}
	
	public void setDBFetchSize(int aSize)
	{
		db_conn_fetchsize = aSize;
	}
	
	public int getDBFetchSize()
	{
		return db_conn_fetchsize;
	}
	
	public void setDBConnPoolSize(int aSize)
	{
		db_conn_pool_size = aSize;
	}
	
	public int getDBConnPoolSize()
	{
		return db_conn_pool_size;
	}
	
	public void setMaxDBConn(int aMaxSize)
	{
		db_conn_max = aMaxSize;
	}
	
	public int getMaxDBConn()
	{
		return db_conn_max;
	}
	
	public void setWaitingIntervalMs(long aWaitIntervalMs)
	{
		conn_wait_interval_ms = aWaitIntervalMs;
	}
	
	public long getWaitingIntervalMs()
	{
		return conn_wait_interval_ms;
	}
		
	
	public void setTimeoutMs(long aIntervalMs)
	{
		conn_timeout_ms = aIntervalMs;
	}
	
	public long getTimeoutMs()
	{
		return conn_timeout_ms;
	}
		
	public void setReferenceConfig(Map<String, String> aReferenceMap)
	{
		this.mapReferenceConfig = aReferenceMap;
	}
	
	public Map<String, String> getReferenceConfig()
	{
		return this.mapReferenceConfig;
	}
	
	public String getDatabaseServerVersion()
	{
		return this.database_server_info;
	}
	
	public String getDatabaseDriverClassName()
	{
		return this.db_classname;
	}
	
	private void initDB(Properties aProp) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		db_classname = aProp.getProperty(KEY_DB_CLASSNAME);
		db_url = aProp.getProperty(KEY_DB_URL);
		db_uid = aProp.getProperty(KEY_DB_UID);
		db_pwd = aProp.getProperty(KEY_DB_PWD);
		
		String sConnSize = aProp.getProperty(KEY_DB_CONNPOOL);
		if(sConnSize!=null)
		{
			try{
				db_conn_pool_size = Integer.parseInt(sConnSize);
			}
			catch(NumberFormatException ex){}
		}
		
		//Init JDBC class
		Class.forName (db_classname).newInstance();
		
		//Test connection
		boolean isGetConnSuccess = false;
		
		Connection connTest = null;
		try {
			//test connection
			connTest = getConnection(false);
			isGetConnSuccess = (connTest!=null);
			
			if(isGetConnSuccess)
			{
				DatabaseMetaData meta = connTest.getMetaData();
				this.database_server_info = meta.getDatabaseProductName()+" "+meta.getDatabaseProductVersion();
			}
		}finally
		{
			closeQuietly(connTest, null, null);
		}
			
		//Init connection pool
		if(isGetConnSuccess && db_conn_pool_size>0)
		{
			for(int i=0; i<db_conn_pool_size; i++)
			{
				stackConnPool.push(getConnection(false));
			}
		}
		
	}
		
	public Connection getConnection() throws SQLException
	{
		return getConnection(true);
	}
	
	public Connection getConnection(boolean isGetFromConnPool) throws SQLException
	{
		Connection conn = null;
		
		if(isGetFromConnPool)
		{
			try{
				while(stackConnPool.size()>0 && conn==null)
				{
					conn = stackConnPool.pop();
					if(conn.isClosed() || !conn.isValid(1))
						conn = null;
				}
			}catch(EmptyStackException ex){}
		}
		
		if(conn==null)
		{
			//if there's a MAX connection defined
			if(db_conn_max>0)
			{
				long lFreeConn = getTotalAvailConn();
				long totalWaitMs = 0;
				while(lFreeConn<=0)
				{
					lFreeConn = getTotalAvailConn();
					try {
						totalWaitMs += conn_wait_interval_ms;
						Thread.sleep(conn_wait_interval_ms);
						if(totalWaitMs >= conn_timeout_ms)
						{
							throw new SQLException("Timeout for getting a database connection. db_conn_max:"+db_conn_max+", conn_in_used:"+mapConnInUse.size());
						}
					} catch (InterruptedException e) {
					}
				}
			}
			conn = DriverManager.getConnection (db_url, db_uid, db_pwd);
		}
		
		if(conn!=null)
		{
			mapConnInUse.put(conn, System.currentTimeMillis());
		}
			
		
		return conn;
	}
	
	protected long getTotalAvailConn()
	{
		if(db_conn_max>0)
		{
			return db_conn_max - getTotalConnInUse();
		}
		else
		{
			//if no max specified 
			return 10000;
		}
	}
	
	public long getTotalConnInUse()
	{
		return mapConnInUse.size();
	}
	
	public void closeAllConnInUse()
	{
		for(Connection conn : mapConnInUse.keySet())
		{
			mapConnInUse.remove(conn);
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static PreparedStatement setParams(PreparedStatement aStatement, List<Object> aParamList ) throws NumberFormatException, SQLException
	{
		if(aParamList!=null)
			return setParams(aStatement, aParamList.toArray(new Object[aParamList.size()]));
		else
			return setParams(aStatement, new Object[]{});
	}
	
	public static PreparedStatement setParams(PreparedStatement aStatement, Object[] aParams ) throws NumberFormatException, SQLException
	{
		if(aParams!=null && aParams.length>0 && aStatement!=null)
		{
			for(int i=0; i<aParams.length; i++)
			{
				Object param = aParams[i];
				
				String sParamClassName = param.getClass().getSimpleName();
				
				if(param == null || param == JSONObject.NULL)
				{
					aStatement.setObject(i+1, null);
				}
				else if(String.class.getSimpleName().equals(sParamClassName))
				{
					aStatement.setString(i+1, param.toString());
				}
				else if(listNumericType.contains(sParamClassName))
				{
					aStatement.setLong(i+1, Long.parseLong(param.toString()));
				}
				else if(Date.class.getSimpleName().equals(sParamClassName))
				{
					aStatement.setDate(i+1, (Date)param);
				}
				else if(Timestamp.class.getSimpleName().equals(sParamClassName))
				{
					aStatement.setTimestamp(i+1, (Timestamp)param);
				}
				else if(listDoubleType.contains(sParamClassName))
				{
					aStatement.setDouble(i+1, Double.parseDouble(param.toString()));
				}
				else if(listFloatType32bit.contains(sParamClassName))
				{
					aStatement.setFloat(i+1, Float.parseFloat(param.toString()));
				}
				else if(listBooleanType.contains(sParamClassName))
				{
					aStatement.setBoolean(i+1, Boolean.parseBoolean(param.toString()));
				}
				else
				{
					if(param!=null && param.getClass().isArray())
					{
						String sArrayType = "VARCHAR";
						String sClassName = param.getClass().getSimpleName();
						
						if(sClassName.endsWith("[]"))
						{
							sClassName = sClassName.substring(0, sClassName.length()-2);
						}
						else
						{
							sClassName = sClassName.substring(2, sClassName.length()-1);
						}
						
						if(listNumericType.contains(sClassName))
						{
							sArrayType = "bigint";
						}
						else if(listDoubleType.contains(sClassName) || listFloatType32bit.contains(sClassName))
						{
							sArrayType = "float8";
						}
						else if(listBooleanType.contains(sClassName))
						{
							sArrayType = "boolean";
						}

						Connection conn = aStatement.getConnection();
						Array array = conn.createArrayOf(sArrayType, (Object[])param);
						param = array;
					}
					
					if(param instanceof Array)
					{
						aStatement.setArray(i+1, (Array) param);
					}
					else
					{
						throw new SQLException("Unsupported datatype - "+sParamClassName+" : "+String.valueOf(param));
					}
				}
				
			}
		}		
		return aStatement;
	}

	public JSONArray executeUpdate(String aSQL, List<Object> aParamList) throws SQLException
	{
		return executeUpdate(aSQL, aParamList.toArray(new Object[aParamList.size()]));
	}
	
	public JSONArray executeUpdate(String aSQL, Object[] aParams ) throws SQLException
	{
		JSONArray jArrReturn 	= new JSONArray();
		Connection conn 		= null;
		PreparedStatement stmt	= null;
		ResultSet rs			= null;
		
		long lAffectedRows 		= 0;
		try{
			conn 	= getConnection();
			stmt 	= conn.prepareStatement(aSQL, 1);
			stmt 	= setParams(stmt, aParams);
			
			lAffectedRows = stmt.executeUpdate();
			
			if(lAffectedRows>0)
			{
				rs = stmt.getGeneratedKeys();
				ResultSetMetaData meta = rs.getMetaData();
				while(rs.next())
				{
					JSONObject json = new JSONObject();
					for(int i=1; i<=meta.getColumnCount(); i++)
					{
						json.put(meta.getColumnName(i), rs.getObject(i));
					}
					jArrReturn.put(json);
				}
			}
		}
		catch(SQLException sqlEx)
		{
			throw sqlEx;
		}
		finally
		{
			closeQuietly(conn, stmt, rs);
		}
		return jArrReturn;
	}
	
	public long executeBatchUpdate(String aSQL, List<Object[]> aParamsList) throws SQLException
	{
		Connection conn 		= null;
		PreparedStatement stmt	= null;
		
		long lAffectedRows 		= 0;
		try{
			conn 	= getConnection();
			
			stmt 	= conn.prepareStatement(aSQL);
			for(Object[] aParams : aParamsList)
			{
				stmt.clearParameters();
				stmt = setParams(stmt, aParams);
				lAffectedRows += stmt.executeUpdate();
			}
		}finally
		{
			closeQuietly(conn, stmt, null);
		}
		return lAffectedRows;
	}	

	public long getQueryCount(String aSQL, Object[] aParams) throws SQLException
	{
		long lCount = 0;
		
		Connection conn 		= null;
		PreparedStatement stmt	= null;
		ResultSet rs 			= null;
		try{
			conn 	= getConnection();
			stmt 	= conn.prepareStatement(aSQL);
			stmt 	= setParams(stmt, aParams);
			rs 		= stmt.executeQuery();
			
			//Result
			while(rs.next())
			{
				lCount++;
			}
		}finally
		{
			closeQuietly(conn, stmt, rs);
		}
		
		return lCount;
	}
	
	
	public void closeQuietly(Connection aConn, PreparedStatement aStmt, ResultSet aResultSet ) 
	{
		String sSQL = "";
		
		Class classCaller = null;
		
		if(aResultSet!=null)
		{
			classCaller = ThreadMgr.getCallerClass(aResultSet.getClass());
		}
		else if(aStmt!=null)
		{
			classCaller = ThreadMgr.getCallerClass(aStmt.getClass());
		}
		else if(aConn!=null)
		{
			classCaller = ThreadMgr.getCallerClass(aConn.getClass());
		}

		if(aResultSet!=null)
		{
			try{
				Statement stmtRs = aResultSet.getStatement();
				if(stmtRs!=null)
				{
					sSQL = stmtRs.toString(); 
				}
				aResultSet.close();
			}catch(Exception ex) { 
				logger.log(Level.WARNING, ex.getMessage()+"\nSQL:"+sSQL);
			}
		}		
		///////////////////
		if(aStmt!=null)
		{	
			try {
				ResultSet rs = aStmt.getResultSet();
				if(rs!=null && !rs.isClosed())
				{
					Statement stmtRs = aResultSet.getStatement();
					if(stmtRs!=null)
					{
						sSQL = stmtRs.toString(); 
					}
					rs.close();
				}
			} catch (SQLException ex) {
				logger.log(Level.WARNING, ex.getMessage()+"\nSQL:"+sSQL);
			}
			
			try{
				aStmt.close();
			}catch(Exception ex) { 
				logger.log(Level.WARNING, ex.getMessage()+"\nSQL:"+sSQL);
			}
		}
		///////////////////
		if(aConn!=null)
		{
			Long LStartTime = mapConnInUse.remove(aConn);
			if(LStartTime!=null)
			{
				long lElapsedMs = System.currentTimeMillis()-LStartTime.longValue();
				
				if(lElapsedMs > conn_timeout_ms)
				{
					String sCallerName = null;
					
					if(classCaller!=null)
						sCallerName = classCaller.getName();
					
					//Log warning
					logger.log(Level.WARNING, "[long SQL] "+lElapsedMs+"ms callerClass:"+sCallerName+" -"+sSQL);
				}
			}
			
			if(stackConnPool.size()<db_conn_pool_size)
			{
				stackConnPool.push(aConn);
			}
			else
			{
				if(aConn!=null)
				{
					try {
						aConn.close();
					} catch (SQLException ex) {
						logger.log(Level.WARNING, ex.getMessage()+"\nSQL:"+sSQL);
					}
				}
			}
		}
	}
	
	
	public String toString()
	{
		JSONObject json = new JSONObject();
		for(String sKey : mapReferenceConfig.keySet())
		{
			json.put(sKey, mapReferenceConfig.get(sKey));
		}
		return json.toString();
	}
}
