package hl.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DBMgr {

	public static String KEY_DB_CLASSNAME 	= "db.jdbc.classname";
	public static String KEY_DB_URL 		= "db.url";
	public static String KEY_DB_UID 		= "db.uid";
	public static String KEY_DB_PWD			= "db.pwd";
	
	public String db_classname 	= null;
	public String db_url 		= null;
	public String db_uid 		= null;
	public String db_pwd 		= null;
	
	private Map<String, String> mapSQLtemplate = new HashMap<String, String>();
	private static List<String> listNumericType = null;
	private static List<String> listDoubleType 	= null;
	
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
		
	}
	
	public DBMgr(String aResourcePath) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
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
				in = DBMgr.class.getResourceAsStream(aResourcePath);
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
	
	public DBMgr(Properties aProp) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		initDB(aProp);
	}
	
	public DBMgr(String aClassName, String aDBUrl, String aDBUid, String aDBPwd) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		Properties prop = new Properties();
		prop.put(KEY_DB_CLASSNAME, aClassName);
		prop.put(KEY_DB_URL, aDBUrl);
		prop.put(KEY_DB_UID, aDBUid);
		prop.put(KEY_DB_PWD, aDBPwd);
		initDB(prop);
	}
	
	private void initDB(Properties aProp) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		db_classname = aProp.getProperty(KEY_DB_CLASSNAME);
		db_url = aProp.getProperty(KEY_DB_URL);
		db_uid = aProp.getProperty(KEY_DB_UID);
		db_pwd = aProp.getProperty(KEY_DB_PWD);
		
		//Init JDBC class
		Class.forName (db_classname).newInstance();
		
		//Test connection
		Connection conn = null;
		try{
			conn = getConnection();
		}finally
		{
			closeQuietly(conn, null, null);
		}
	}
		
	public Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection (db_url, db_uid, db_pwd);
	}
	
	public static PreparedStatement setParams(PreparedStatement aStatement, List<Object> aParamList ) throws NumberFormatException, SQLException
	{
		return setParams(aStatement, aParamList.toArray(new Object[aParamList.size()]));
	}
	public static PreparedStatement setParams(PreparedStatement aStatement, Object[] aParams ) throws NumberFormatException, SQLException
	{
		if(aParams!=null && aParams.length>0 && aStatement!=null)
		{
			for(int i=0; i<aParams.length; i++)
			{
				Object param = aParams[i];
				
				String sParamClassName = param.getClass().getSimpleName();
				if(String.class.getSimpleName().equals(sParamClassName))
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
			}
		}		
		return aStatement;
	}

	public long executeUpdate(String aSQL, List aParamList) throws SQLException
	{
		return executeUpdate(aSQL, aParamList.toArray(new Object[aParamList.size()]));
	}
	public long executeUpdate(String aSQL, Object[] aParams ) throws SQLException
	{
		Connection conn 		= null;
		PreparedStatement stmt	= null;
		
		long lAffectedRows 		= 0;
		try{
			conn 	= getConnection();
			stmt 	= conn.prepareStatement(aSQL);
			stmt 	= setParams(stmt, aParams);
			
			lAffectedRows = stmt.executeUpdate();
		}finally
		{
			closeQuietly(conn, stmt, null);
		}
		return lAffectedRows;
	}
	
	private List<Object> setObjectsFromResultSet(ResultSet aResultSet, Object aObj) throws NoSuchMethodException, SecurityException, SQLException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		if(aResultSet==null || aObj==null)
			return null;
		List<Object> listResult = new ArrayList<Object>();
		
		Class<? extends Object> c = aObj.getClass();
		ResultSetMetaData meta = aResultSet.getMetaData();
		
		Map<String,Method> mapSetters = getSetterMethods(meta, c);
		
		if(mapSetters.size()>0)
		{
			while(aResultSet.next())
			{
				Object o = c.newInstance();
			
				for(String sColName : mapSetters.keySet())
				{
					Method m = mapSetters.get(sColName);
					o = m.invoke(o, aResultSet.getObject(sColName));
					listResult.add(o);
				}
				
			}
		}
		return listResult;
	}
	
	private static Map<String,Method> getSetterMethods(ResultSetMetaData meta, Class<? extends Object> aClass) throws SQLException, NoSuchMethodException, SecurityException
	{
		Map<String,Method> mapSetterMethods = new HashMap<String, Method>();
		int iColCount = meta.getColumnCount();
		for(int i=1; i<=iColCount; i++)
		{
			Method m 				= null;
			String sColName 		= meta.getColumnName(i);
			
		
			String sSetterMethod 	= "set"+getInitialCap(sColName);
			
			switch (meta.getColumnType(i))
			{
				case Types.INTEGER : 
					if(m==null)
					{
						m = aClass.getDeclaredMethod(sSetterMethod, int.class);
						if(m==null)
						{
							m = aClass.getDeclaredMethod(sSetterMethod, Integer.class);
						}
					}
				case Types.DOUBLE : 
					if(m==null)
					{
						m = aClass.getDeclaredMethod(sSetterMethod, double.class);
						if(m==null)
						{
							m = aClass.getDeclaredMethod(sSetterMethod, Double.class);
						}
					}
				case Types.BIGINT :
					if(m==null)
					{
						m = aClass.getDeclaredMethod(sSetterMethod, long.class);
						if(m==null)
						{
							m = aClass.getDeclaredMethod(sSetterMethod, Long.class);
						}
					}
					break;
				case Types.FLOAT : 
					if(m==null)
					{
						m = aClass.getDeclaredMethod(sSetterMethod, float.class);
						if(m==null)
						{
							m = aClass.getDeclaredMethod(sSetterMethod, Float.class);
						}
					}
					break;
				case Types.DATE : 
				case Types.TIMESTAMP : 
				case Types.TIME : 
	
					if(m==null)
					{
						m = aClass.getDeclaredMethod(sSetterMethod, java.util.Date.class);
						if(m==null)
						{
							m = aClass.getDeclaredMethod(sSetterMethod, Timestamp.class);
						}
					}
					break;
				default :
					if(m==null)
					{
						m = aClass.getDeclaredMethod(sSetterMethod, String.class);
					}
			}
			
			if(m!=null)
			{
				System.out.println("Adding "+m.getName()+"("+m.getParameterTypes()[0].getName()+")");
				mapSetterMethods.put(sColName, m);
			}
		}
		return mapSetterMethods;
	}
	
	private static String getInitialCap(final String aString)
	{
		if(aString!=null && aString.length()>2)
		{
			return aString.substring(0,1).toUpperCase()
					+aString.substring(1).toLowerCase();
		}
		return aString;
	}

	public List<List<String>> executeQuery(String aSQL, List aParamList) throws SQLException
	{
		return executeQuery(aSQL, aParamList.toArray(new Object[aParamList.size()]));
	}
	public List<List<String>> executeQuery(String aSQL, Object[] aParams) throws SQLException
	{
		List<List<String>> listData = new ArrayList<List<String>>();
		List<String> listCols = null;
		
		Connection conn 		= null;
		PreparedStatement stmt	= null;
		ResultSet rs 			= null;
		try{
			conn 	= getConnection();
			stmt 	= conn.prepareStatement(aSQL);
			stmt 	= setParams(stmt, aParams);
			
			rs 		= stmt.executeQuery();
			
			//Column Name
			ResultSetMetaData meta = rs.getMetaData();
			int iTotalCols = meta.getColumnCount();
			listCols = new ArrayList<String>();
			for(int i=0; i<iTotalCols; i++)
			{
				listCols.add(meta.getColumnName(i+1));
			}
			listData.add(listCols);

			//Result
			while(rs.next())
			{
				listCols = new ArrayList<String>();
				for(int i=0; i<iTotalCols; i++)
				{
					listCols.add(rs.getString(i+1));
				}
				listData.add(listCols);
			}
			
		}finally
		{
			closeQuietly(conn, stmt, rs);
		}
		
		return listData;
	}
	
	public List<String> executeTemplateQueryToJson(String aSQLTemplateName, Object[] aParams ) throws SQLException
	{
		List<String> listData = new ArrayList<String>();
		
		Connection conn 		= null;
		PreparedStatement stmt	= null;
		ResultSet rs 			= null;
		String sSQL 			= getSQLtemplates(aSQLTemplateName);
		
		try{
			conn 	= getConnection();
			stmt 	= conn.prepareStatement(sSQL);
			stmt 	= setParams(stmt, aParams);
			
			rs 		= stmt.executeQuery();
			
			//Column Name
			ResultSetMetaData meta = rs.getMetaData();

			StringBuffer sb = new StringBuffer();
			//Result
			while(rs.next())
			{
				sb.setLength(0);
				for(int i=0; i<meta.getColumnCount(); i++)
				{
					int iColType 		= meta.getColumnType(i);
					String sColName 	= meta.getColumnName(i);   
					String sColValue 	= rs.getString(sColName);
					
					if(i>0)
						sb.append(",");
					
					sb.append("\"").append(sColName).append("\"").append("=");
					
					boolean isNum = false;
					switch(iColType)
					{
						case Types.BIGINT 	: 
						case Types.DECIMAL 	: 
						case Types.INTEGER 	: 
						case Types.FLOAT 	: 
						case Types.NUMERIC 	:
						case Types.DOUBLE 	:
							break;
						default :
							isNum = true;
					}
					
					if(!isNum)
						sb.append("\"");
					
					sb.append(sColValue);
					
					if(!isNum)
						sb.append("\"");
				}
				
				listData.add(sb.toString());
			}
			
		}finally
		{
			closeQuietly(conn, stmt, rs);
		}
		
		return listData;
	}
	
	public static void closeQuietly(Connection aConn, PreparedStatement aStmt, ResultSet aResultSet ) throws SQLException
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
			aConn.close();
	}
	
	public static void main(String args[]) throws Exception
	{

		System.out.println(DBMgr.getInitialCap("figglFFame"));
		
		/*
		Properties prop = new Properties();
		prop.setProperty(KEY_DB_CLASSNAME, "com.mysql.jdbc.Driver");
		
		prop.setProperty(KEY_DB_URL, "jdbc:mysql://192.168.0.100:3306/sys?useSSL=false");
		prop.setProperty(KEY_DB_UID, "root");
		prop.setProperty(KEY_DB_PWD, "password$1");
		String sSQL = "SELECT * FROM sys.sys_config where variable like ? and set_time <= ?";
		@SuppressWarnings("deprecation")
		Object[] oParams = new Object[]{"dia%", new Timestamp(2015,05,21,20,20,44,0)};
		
		DBMgr dbmgr = new DBMgr(prop);
		List<List<String>> list = dbmgr.executeQuery(sSQL, oParams);
		
		for(int i=0; i<list.size(); i++)
		{
			List<String> listRow = list.get(i);
			System.out.println();
			for(int j=0; j<listRow.size(); j++)
			{
				if(j>0)
					System.out.print(", ");
				System.out.print(listRow.get(j));
			}
		}
		*/
	}
	

}
