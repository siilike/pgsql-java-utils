Java utilities for PostgreSQL composite types
=============================================

Utilities to simplify sending and receiving composite types with JDBC.

Getting data to PostgreSQL
==========================

Simple usage
------------

    CustomType ct = new CustomType();
    ct.set(1, 204);
    ct.set(2, Arrays.asList(3, 5, 9));
    
    // (204, "{3, 5, 9}")
    String ret = ct.toString();

Real usage
----------

In the real world you will probably want to send arrays of composite types, so you can either do:

    java.sql.Array arr = new CustomTypeArray("users_type", Arrays.asList(ct, ct2, ...));

or go with a more sane way of using a builder, e.g:

    public class UsersArrayBuilder extends CustomTypeArrayBuilder
    {
      public UsersArrayBuilder()
      {
        super("users_type");
      }
    
      public void add(User user)
      {
        CustomType t = new CustomType();
        t.set(1, user.getId());
        t.set(2, user.getName());
    
        addItem(t);
      }
    }
    
    UsersArrayBuilder b = new UsersArrayBuilder();
    
    // returns an empty array, not null
    java.sql.Array ret = b.getArray();
    
    b.add(u1);
    b.add(u2);
    
    // {(1, Jane), (2, Mary)}
    java.sql.Array ret = b.getArray();

`new CustomTypeArray("users_type", null)` returns `NULL`, empty builders return an empty array.

For maps and multimaps you can also use `CustomTypeArray.toArray(type, map)`.

Getting data from PostgreSQL
============================

    // {(1, Mary, 59, {3, 4}), (2, Jane, NULL, NULL)}
    UDTValue users = UDTValue.create(rset.getString("users"));
    
    for(UDTValue user : users)
    {
      // 1
      user.getInt(1);
    
      // Mary
      user.getString(2);
      
      // 59; 0 for Jane like in JDBC
      user.getInt(3);
    
      // List<Integer>, free to modify; null for Jane
      user.getIntegerCollection(4);
    
      // List<UDTValue>, direct value, do not modify; null for Jane
      user.getValues();
    
      // nonexistent field, returns null, no exception is thrown
      user.getValue(5);
    
      // nonexistent field, 0
      user.getInt(6);
    }

The parser unescapes the value for every level recursively, for the above example twice. There is also a non-copying (except for the final string values) parser that is 25% faster and more memory efficient, but it has a few bugs with some edge cases like `{{},{}}`, so currently it is not published, hence the `UDTValue.create()` usage to replace it in the future if needed.

You can have any amount of nesting you want, just bear in mind that the amount of escape characters grows exponentially, e.g for level 5 there are 32 backslashes or quotes to escape a single character.

The `get{Int,Double,Boolean,Date,...}` methods for NULL values are consistent with JDBC, so `getInt` for NULL returns 0, etc.

Currently range values are not supported and milliseconds and timezones are truncated.

Depending on the use case it could make a lot sense to use `enum`s that are automatically generated from the SQL type defintions for the `get*` operations, so when you add or remove a field from the definition, you will not have to change any code.
