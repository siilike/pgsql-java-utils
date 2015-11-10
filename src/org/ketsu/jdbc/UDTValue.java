package org.ketsu.jdbc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
public abstract class UDTValue implements Iterable<UDTValue>
{
	public static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
		  .appendValue(ChronoField.HOUR_OF_DAY, 2)
		  .appendLiteral(':')
		  .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
		  .optionalStart()
		  .appendLiteral(':')
		  .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
		  .toFormatter();

  public static final DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder()
  	  .parseCaseInsensitive()
  	  .append(DateTimeFormatter.ISO_LOCAL_DATE)
  	  .optionalStart()
  	  .appendLiteral(' ')
  	  .append(TIME_FORMATTER)
  	  .toFormatter();

	public static final boolean FORCE_COPYING = true;

	public static final UDTValue NULL = new CopyingUDTValue(null);
	public static final UDTValue EMPTY = new CopyingUDTValue("");

	protected boolean parsed = false;
	protected List<UDTValue> values;
	protected Type type;

	public static UDTValue create(ResultSet rset, String col) throws SQLException
	{
		return create(rset.getString(col));
	}

	public static UDTValue create(ResultSet rset, int col) throws SQLException
	{
		return create(rset.getString(col));
	}

	public static UDTValue create(String x)
	{
		if(x == null)
		{
			return NULL;
		}

		if(x.isEmpty() || x.equals("{}") || x.equals("()"))
		{
			return EMPTY;
		}

		if(FORCE_COPYING)
		{
			return new CopyingUDTValue(x);
		}

		if(x.charAt(0) == '(' && x.charAt(x.length() - 1) == ')')
		{
			return new CopyingUDTValue(x);
		}

		return null; // new NoCopyUDTValue(x);
	}

	protected abstract void parse();

	protected void checkParsed()
	{
		if(!parsed)
		{
			parse();
		}
	}

	public List<UDTValue> getValues()
	{
		checkParsed();

		return values;
	}

	public String toDebugString()
	{
		return toString();
	}

	@Override
	public abstract String toString();

	public int toInt()
	{
		if(isNull())
		{
			return 0;
		}

		checkParsed();

		return Integer.parseInt(toString());
	}

	public long toLong()
	{
		if(isNull())
		{
			return 0;
		}

		checkParsed();

		return Long.parseLong(toString());
	}

	public double toDouble()
	{
		if(isNull())
		{
			return 0;
		}

		checkParsed();

		return Double.parseDouble(toString());
	}

	public boolean toBoolean()
	{
		if(isNull())
		{
			return false;
		}

		return toString().equals("t") ? true : false;
	}

	public LocalDate toDate()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		try
		{
			return LocalDate.parse(toString().substring(0, 10));
		}
		catch (Exception e)
		{
			throw new RuntimeException("Parsing date string failed", e);
		}
	}

	public LocalTime toTime()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		try
		{
			return LocalTime.parse(toString());
		}
		catch (Exception e)
		{
			throw new RuntimeException("Parsing date string failed", e);
		}
	}

	public Date toTimestamp()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		try
		{
			return new Date(LocalDateTime.parse(toString().substring(0, 19), DATETIME_FORMATTER).toEpochSecond(ZoneOffset.UTC) * 1000);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Parsing date string failed", e);
		}
	}

	public Instant toInstant()
	{
		if(isNull())
		{
			return null;
		}

		return toTimestamp().toInstant();
	}

	/**
	 * @source internet, have lost the link
	 */
	public byte[] toBytes()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		String v = toString();

		if(v.startsWith("\\x"))
		{
			String hex = v.substring(2);

			int len = hex.length();
			ByteArrayOutputStream bytes = new ByteArrayOutputStream(len / 2);

			int idx = 0;
			while(idx < len)
			{
				int hexDigit = hex.charAt(idx++);
				int byteValue = (hexValue(hexDigit) << 4);

				if(idx == len)
				{
					break;
				}

				hexDigit = hex.charAt(idx++);
				byteValue += hexValue(hexDigit);

				bytes.write(byteValue);
			}

      return bytes.toByteArray();
		}

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		int len = v.length();
		int idx = 0;
		while(idx < len)
		{
			int ch = v.charAt(idx++);

			if(ch == '\\')
			{
				ch = v.charAt(idx++);

				if(ch == '\\')
				{
					bytes.write('\\');
					continue;
				}

				int val = octalValue(ch);

				ch = v.charAt(idx++);

				val <<= 3;
				val += octalValue(ch);

				ch = v.charAt(idx++);

				val <<= 3;
				val += octalValue(ch);

				bytes.write(val);
			}
			else
			{
				bytes.write(ch);
			}
		}

		return bytes.toByteArray();
	}

	public BigInteger toBigInteger()
	{
		if(isNull())
		{
			return null;
		}

		return new BigInteger(toString());
	}

	public BigDecimal toBigDecimal()
	{
		if(isNull())
		{
			return null;
		}

		return new BigDecimal(toString());
	}

	public List<String> toStringCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<String> ret = new ArrayList<String>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toString());
		}

		return ret;
	}

	public List<Integer> toIntegerCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<Integer> ret = new ArrayList<Integer>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toInt());
		}

		return ret;
	}

	public List<Long> toLongCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<Long> ret = new ArrayList<Long>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toLong());
		}

		return ret;
	}

	public List<Double> toDoubleCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<Double> ret = new ArrayList<Double>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toDouble());
		}

		return ret;
	}

	public List<Boolean> toBooleanCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<Boolean> ret = new ArrayList<Boolean>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toBoolean());
		}

		return ret;
	}

	public List<LocalDate> toDateCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<LocalDate> ret = new ArrayList<LocalDate>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toDate());
		}

		return ret;
	}

	public List<LocalTime> toTimeCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<LocalTime> ret = new ArrayList<LocalTime>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toTime());
		}

		return ret;
	}

	public List<Date> toTimestampCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<Date> ret = new ArrayList<Date>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toTimestamp());
		}

		return ret;
	}

	public List<Instant> toInstantCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<Instant> ret = new ArrayList<Instant>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toInstant());
		}

		return ret;
	}

	public List<byte[]> toBytesCollection()
	{
		if(isNull())
		{
			return null;
		}

		checkParsed();

		List<byte[]> ret = new ArrayList<byte[]>(values.size());

		for(UDTValue v : values)
		{
			ret.add(v.toBytes());
		}

		return ret;
	}

	public UDTValue getValue(int which)
	{
		if(getSize() < which)
		{
			return null;
		}

		return values.get(which - 1);
	}

	public boolean isNull(int which)
	{
		UDTValue v = getValue(which);

		return v == null || v.isNull();
	}

	public String getString(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toString();
	}

	public int getInt(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return 0;
		}

		return v.toInt();
	}

	public long getLong(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return 0;
		}

		return v.toLong();
	}

	public double getDouble(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return 0;
		}

		return v.toDouble();
	}

	public boolean getBoolean(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return false;
		}

		return v.toBoolean();
	}

	public LocalDate getDate(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toDate();
	}

	public Date getTimestamp(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toTimestamp();
	}

	public Instant getInstant(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toInstant();
	}

	public byte[] getBytes(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toBytes();
	}

	public BigInteger getBigInteger(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toBigInteger();
	}

	public BigDecimal getBigDecimal(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toBigDecimal();
	}

	public List<String> getStringCollection(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toStringCollection();
	}

	public List<Integer> getIntegerCollection(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toIntegerCollection();
	}

	public List<Long> getLongCollection(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toLongCollection();
	}

	public List<Double> getDoubleCollection(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toDoubleCollection();
	}

	public List<LocalDate> getDateCollection(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toDateCollection();
	}

	public List<LocalTime> getTimeCollection(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toTimeCollection();
	}

	public List<Date> getTimestampCollection(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toTimestampCollection();
	}

	public List<Instant> getInstantCollection(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toInstantCollection();
	}

	public List<byte[]> getBytesCollection(int which)
	{
		UDTValue v = getValue(which);

		if(v == null)
		{
			return null;
		}

		return v.toBytesCollection();
	}

	public boolean hasValues()
	{
		if(isNull())
		{
			return false;
		}

		checkParsed();

		return values != null && !values.isEmpty();
	}

	public abstract boolean isNull();

	public boolean isArray()
	{
		checkParsed();

		return type == Type.ARRAY;
	}

	public boolean isType()
	{
		checkParsed();

		return type == Type.TYPE;
	}

	public boolean isSimpleValue()
	{
		checkParsed();

		return type == Type.NORMAL;
	}

	public Type getType()
	{
		checkParsed();

		return type;
	}

	public int getSize()
	{
		checkParsed();

		return values == null ? 0 : values.size();
	}

	@Override
	public Iterator<UDTValue> iterator()
	{
		return new Iterator<UDTValue>()
		{
			protected int current = 0;

			@Override
			public boolean hasNext()
			{
				return current < getSize();
			}

			@Override
			public UDTValue next()
			{
				return values.get(current++);
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	protected void addValue(UDTValue v)
	{
		if(values == null)
		{
			values = new ArrayList<UDTValue>();
		}

		values.add(v);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj == this)
		{
			return true;
		}

		if(!(obj instanceof UDTValue))
		{
			return false;
		}

		UDTValue v = (UDTValue) obj;

		boolean nv = v.isNull();
		boolean nc = isNull();

		if(nv || nc)
		{
			return nv && nc;
		}

		return v.toString().equals(toString());
	}

	@Override
	public int hashCode()
	{
		if(isNull())
		{
			return 0;
		}

		return toString().hashCode();
	}

	public static enum Type
	{
		TYPE,
		ARRAY,
		NORMAL
	}

	protected static int hexValue(final int hexDigit)
	{
		if(hexDigit >= '0' && hexDigit <= '9')
		{
			return hexDigit - '0';
		}
		else if(hexDigit >= 'a' && hexDigit <= 'f')
		{
			return hexDigit - 'a' + 10;
		}
		else if(hexDigit >= 'A' && hexDigit <= 'F')
		{
			return hexDigit - 'A' + 10;
		}

		throw new RuntimeException("Unknown format: " + hexDigit);
	}

	protected static int octalValue(final int octalDigit)
	{
		if(octalDigit < '0' || octalDigit > '7')
		{
			throw new RuntimeException("Unknown format: " + octalDigit);
		}

		return octalDigit - '0';
	}
}
