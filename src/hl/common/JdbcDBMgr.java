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
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONObject;

public class JdbcDBMgr {

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
	public int db_conn_pool_size	= 2;
	
	private Stack<Connection> stackConns 		= new Stack<Connection>();
	private Map<String, String> mapSQLtemplate 	= new HashMap<String, String>();
	private static List<String> listNumericType = null;
	private static List<String> listDoubleType 	= null;
	private static List<String> listBooleanType = null;
		
	static{
		listNumericType = new ArrayList<String>();
		listNumericType.add(int.class.getSimpleName());
		listNumericType.add(Integer.class.getSimpleName());
		listNumericType.add(long.class.getSimpleName());
		listNumericType.add(Long.class.getSimpleName());

		listDoubleType = new ArrayList<String>();
		listDoubleType.add(double.class.getSimpleName());
		listDoubleType.add(Double.class.getSimpleName());
		listDoubleType.add(float.class.getSimpleName());
		listDoubleType.add(Float.class.getSimpleName());
		
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
	
	public void setDBConnPoolSize(int aSize)
	{
		db_conn_pool_size = aSize;
	}
	
	public int getDBConnPoolSize()
	{
		return db_conn_pool_size;
	}

	public void setReferenceConfig(Map<String, String> aReferenceMap)
	{
		this.mapReferenceConfig = aReferenceMap;
	}
	
	public Map<String, String> getReferenceConfig()
	{
		return this.mapReferenceConfig;
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
		Connection conn = null;
		
		try {
			conn = getConnection(false);
			
			//Init connection pool
			if(conn!=null && db_conn_pool_size>0)
			{
				for(int i=0; i<db_conn_pool_size; i++)
				{
					Connection connCache = getConnection(false);
					stackConns.push(connCache);
				}
			}
		}finally
		{
			closeQuietly(conn, null, null);
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
				while(stackConns.size()>0 && conn==null)
				{
					conn = stackConns.pop();
					if(conn.isClosed() || !conn.isValid(1))
						conn = null;
				}
			}catch(EmptyStackException ex){}
		}
		
		if(conn==null)
		{
			conn = DriverManager.getConnection (db_url, db_uid, db_pwd);
		}
		
		return conn;
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
				else if(listBooleanType.contains(sParamClassName))
				{
					aStatement.setBoolean(i+1, Boolean.parseBoolean(param.toString()));
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
			throw new SQLException(sqlEx);
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
	
	
	public void closeQuietly(Connection aConn, PreparedStatement aStmt, ResultSet aResultSet ) throws SQLException
	{
		try{
			if(aResultSet!=null)
				aResultSet.close();
		}catch(Exception ex) { }
		//
		try{
			if(aStmt!=null)
				aStmt.close();
		}catch(Exception ex) { }
		//
		if(aConn!=null)
		{
			if(stackConns.size()<db_conn_pool_size)
			{
				stackConns.push(aConn);
			}
			else
			{
				aConn.close();
				stackConns.remove(aConn);
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
