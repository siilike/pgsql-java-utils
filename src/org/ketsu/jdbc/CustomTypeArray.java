package org.ketsu.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Multimap;

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014, Lauri Keel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class CustomTypeArray implements java.sql.Array
{
	/*
	 * name without schema as setArray() prepends _ to names, but doesn't check for schema
	 */
	protected final String name;

	protected final String stringValue;

	public CustomTypeArray(String n, Collection<CustomType> t)
	{
		name = n;
		stringValue = toString(n, t);
	}

	@Override
	public String toString()
	{
		return stringValue;
	}

	public static String toString(String n, Collection<CustomType> t)
	{
		if(t == null)
		{
			return "NULL";
		}

		StringBuilder b = new StringBuilder();

		b.append("{");

		if(!t.isEmpty())
		{
			Iterator<CustomType> iter = t.iterator();
			while(iter.hasNext())
			{
				CustomType tt = iter.next();

				String s = tt.toString();

				if(s == null)
				{
					b.append("NULL");
				}
				else
				{
					b.append("\"");
					b.append(s.replace("\\", "\\\\").replace("\"", "\\\""));
					b.append("\"");
				}

				if(iter.hasNext())
				{
					b.append(",");
				}
			}
		}

		b.append("}");

		return b.toString();
	}

	@Override
	public int getBaseType() throws SQLException
	{
		return java.sql.Types.ARRAY;
	}

	@Override
	public String getBaseTypeName() throws SQLException
	{
		return name;
	}

	public static <K, V> java.sql.Array toArray(String typeName, Map<K, V> m)
	{
		if(m == null)
		{
			return null;
		}

		Collection<CustomType> b = new ArrayList<CustomType>();

		for(Map.Entry<K, V> entry : m.entrySet())
		{
			CustomType t = new CustomType();

			t.set(1, entry.getKey());
			t.set(2, entry.getValue());

			b.add(t);
		}

		return new CustomTypeArray(typeName, b);
	}

	public static <K, V> java.sql.Array toArray(String typeName, Multimap<K, V> m)
	{
		if(m == null)
		{
			return null;
		}

		Collection<CustomType> b = new ArrayList<CustomType>();

		for(Map.Entry<K, Collection<V>> entry : m.asMap().entrySet())
		{
			CustomType t = new CustomType();

			t.set(1, entry.getKey());
			t.set(2, entry.getValue().toArray());

			b.add(t);
		}

		return new CustomTypeArray(typeName, b);
	}

	@Override
	public Object getArray() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getArray(Map<String, Class<?>> map) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getArray(long index, int count) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getResultSet() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getResultSet(long index, int count) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void free() throws SQLException
	{
		//
	}
}
