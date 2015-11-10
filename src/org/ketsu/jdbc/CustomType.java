package org.ketsu.jdbc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

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
public class CustomType
{
	protected final Map<Integer, CustomTypeItem> val = new TreeMap<Integer, CustomTypeItem>();

	public static class CustomTypeItem
	{
		public final String value;
		public final boolean noQuote;

		public CustomTypeItem(String v, boolean nq)
		{
			value = v;
			noQuote = nq;
		}
	}

	public CustomType()
	{
		//
	}

	public void set(int idx, Object[] arr, boolean noQuote)
	{
		if(arr == null)
		{
			val.put(idx, new CustomTypeItem(null, true));
			return;
		}

		StringBuilder v = new StringBuilder();

		v.append("\"{");

		if(arr.length > 0)
		{
			for(int i = 0; i < arr.length; i++)
			{
				if(arr[i] == null)
				{
					v.append("NULL,");
				}
				else if(noQuote)
				{
					v.append(arr[i].toString());
					v.append(",");
				}
				else
				{
					v.append("\"\"");
					v.append(arr[i].toString().replace("\\", "\\\\\\\\").replace("\"", "\\\\\"\""));
					v.append("\"\",");
				}
			}

			v.deleteCharAt(v.length()-1);
		}

		v.append("}\"");

		val.put(idx, new CustomTypeItem(v.toString(), true));
	}

	public void set(int idx, Object v, boolean noQuote)
	{
		val.put(idx, new CustomTypeItem(v == null ? null : v.toString(), noQuote));
	}

	public void set(int idx, Object[] arr)
	{
		set(idx, arr, false);
	}

	public void set(int idx, Collection<?> arr)
	{
		set(idx, arr == null ? null : arr.toArray(), false);
	}

	public void set(int idx, Object v)
	{
		set(idx, v, false);
	}

	public void set(int idx, Number[] arr)
	{
		set(idx, arr, true);
	}

	public void set(int idx, Number v)
	{
		set(idx, v, true);
	}

	public void set(int idx, CustomTypeArrayBuilder[] arr)
	{
		set(idx, arr, true);
	}

	public void set(int idx, CustomTypeArrayBuilder v)
	{
		set(idx, v.getArray(), true);
	}

	@Override
	public String toString()
	{
		if(val.size() == 0)
		{
			return "NULL";
		}

		StringBuilder ret = new StringBuilder();

		ret.append("(");

		Iterator<CustomTypeItem> iter = val.values().iterator();
		while(iter.hasNext())
		{
			CustomTypeItem v = iter.next();

			if(v.value == null)
			{
				//
			}
			else if(!v.noQuote)
			{
				ret.append("\"");
				ret.append(v.value.replace("\\", "\\\\").replace("\"", "\"\""));
				ret.append("\"");
			}
			else
			{
				ret.append(v.value);
			}

			if(iter.hasNext())
			{
				ret.append(",");
			}
		}

		ret.append(")");

		return ret.toString();
	}
}
