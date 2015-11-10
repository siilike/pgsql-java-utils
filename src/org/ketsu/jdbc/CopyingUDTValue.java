package org.ketsu.jdbc;

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
public class CopyingUDTValue extends UDTValue
{
	protected final String value;

	public CopyingUDTValue(String value)
	{
		this.value = value;
	}

	@Override
	protected void parse()
	{
		if(parsed)
		{
			throw new IllegalStateException("Already parsed!");
		}

		parsed = true;

		if(value == null)
		{
			return;
		}

		StringBuilder parsedValue = null;
		boolean directValue = true;

		State state = null;

		int i = 0;
		int max = value.length();

		if(i == max)
		{
			type = Type.NORMAL;
			return;
		}

		int elementStart = -1;

		main:
		while(i < max)
		{
			if(state == null)
			{
				char c = value.charAt(i);
				char l = value.charAt(max - 1);

				if(c == '(' && l == ')')
				{
					type = Type.TYPE;

					if(i == max - 2) // ()
					{
						addValue(UDTValue.NULL);
						break;
					}
				}
				else if(c == '{' && l == '}')
				{
					type = Type.ARRAY;
				}
				else
				{
					type = Type.NORMAL;

					// nothing to do
					break;
				}

				state = State.ELEMENT_START;

				i++;
			}
			else if(state == State.ELEMENT_START)
			{
				char c = value.charAt(i);

				if(c == ',') // NULL == ,,
				{
					addValue(UDTValue.NULL);

					i++;
				}
				else if(c != '"')
				{
					// array null == NULL
					if(c == 'N' && value.charAt(i+1) == 'U' && value.charAt(i+2) == 'L' && value.charAt(i+3) == 'L')
					{
						addValue(UDTValue.NULL);

						i += 4 + 1;
					}
					else
					{
						state = State.UNQUOTED;
					}

					elementStart = i;
				}
				else
				{
					state = State.QUOTED;

					elementStart = i;

					if(parsedValue == null)
					{
						parsedValue = new StringBuilder(max - i);
					}
					else
					{
						parsedValue.setLength(0);
					}
				}

				directValue = true;
			}
			else if(state == State.UNQUOTED || state == State.QUOTED)
			{
				int arrays = 0;
				boolean inQuotes = false;

				while(i <= max)
				{
					char c = i == max ? '\0' : value.charAt(i);

					if(!inQuotes && type == Type.ARRAY)
					{
						if(c == '{')
						{
							arrays++;
						}
						else if(c == '}')
						{
							arrays--;
						}
					}

					if(c == '\\')
					{
						if(!inQuotes)
						{
							throw new IllegalStateException("inQuotes="+inQuotes+", c="+c+", i="+i);
						}

						int dashes = 1;
						while(value.charAt(i + dashes) == '\\')
						{
							dashes++;
						}

						if(arrays == 0 && directValue)
						{
							if(parsedValue == null)
							{
								parsedValue = new StringBuilder(max - elementStart);
							}
							else
							{
								parsedValue.setLength(0);
							}

							parsedValue.append(value, elementStart, i);

							directValue = false;
						}

						if(dashes == 1) // \ + something more
						{
							if(arrays == 0)
							{
								parsedValue.append(value.charAt(i + dashes));
							}

							i++;
						}
						else if(dashes % 2 == 0) // \
						{
							if(arrays == 0)
							{
								for(int k = 0, j = dashes / 2; k < j; k++)
								{
									parsedValue.append('\\');
								}
							}
						}
						else // \\\\ + something more
						{
							if(arrays == 0)
							{
								for(int k = 0, j = (dashes - 1) / 2; k < j; k++)
								{
									parsedValue.append('\\');
								}

								parsedValue.append(value.charAt(i + dashes));
							}

							i++;
						}

						i += dashes;
					}
					else if(c == '"')
					{
						if(type == Type.TYPE)
						{
							int quotes = 1;
							while(value.charAt(i + quotes) == '"')
							{
								quotes++;
							}

							if(quotes == 1) // beginning or end
							{
								inQuotes = !inQuotes;

								if(!directValue)
								{
									parsedValue.append('"');
								}
							}
							else
							{
								if(directValue)
								{
									if(parsedValue == null)
									{
										parsedValue = new StringBuilder(max - elementStart);
									}
									else
									{
										parsedValue.setLength(0);
									}

									parsedValue.append(value, elementStart, i);

									directValue = false;
								}

								if(quotes % 2 == 0) // escaped
								{
									// only quotes
									if(i == elementStart && (value.charAt(i + quotes) == ',' || i + quotes == max - 1))
									{
										parsedValue.append('"');

										for(int k = 0, j = (quotes - 2) / 2; k < j; k++)
										{
											parsedValue.append('"');
										}

										parsedValue.append('"');
									}
									else // escaped quotes + other chars
									{
										for(int k = 0, j = quotes / 2; k < j; k++)
										{
											parsedValue.append('"');
										}
									}
								}
								else // escaped + beginning or end
								{
									for(int k = 0, j = (quotes - 1) / 2; k < j; k++)
									{
										parsedValue.append('"');
									}

									parsedValue.append('"');

									inQuotes = !inQuotes;
								}
							}

							i += quotes;
						}
						else if(type == Type.ARRAY)
						{
							inQuotes = !inQuotes;

							if(!directValue)
							{
								parsedValue.append('"');
							}

							i++;
						}
					}
					else if(!inQuotes && arrays <= 0 && (c == ',' || i == max - 1)) // if max-1 == }, then arrays == -1
					{
						int elementEnd = i;

						String v;

						if(directValue)
						{
							if(state == State.QUOTED)
							{
								v = value.substring(elementStart + 1, elementEnd - 1);
							}
							else
							{
								v = value.substring(elementStart, elementEnd);

								// unquoted empty string is null, e.g. ,,
								if(v.isEmpty())
								{
									v = null;
								}
							}
						}
						else if(parsedValue.length() == 0)
						{
							v = null;
						}
						else
						{
							v = parsedValue.substring(1, parsedValue.length() - 1);
						}

						addValue(v == null ? UDTValue.NULL : new CopyingUDTValue(v));

						if(i == max - 1)
						{
							state = null;
							break main;
						}

						if(c == ',')
						{
							state = State.ELEMENT_START;

							elementStart = -1;

							i++;
						}
						else
						{
							throw new IllegalStateException();
						}

						break;
					}
					else if(!directValue)
					{
						parsedValue.append(c);
						i++;
					}
					else
					{
						i++;
					}
				}
			}
			else
			{
				throw new IllegalStateException("state="+state);
			}
		}
	}

	@Override
	public boolean isNull()
	{
		checkParsed();

		return value == null && values == null;
	}

	@Override
	public String toString()
	{
		checkParsed();

		if(isNull())
		{
			return null;
		}

		return value;
	}

	@Override
	public int hashCode()
	{
		if(value == null)
		{
			return 0;
		}

		return value.hashCode();
	}

	protected static enum State
	{
		ELEMENT_START,
		UNQUOTED,
		QUOTED
	}
}
