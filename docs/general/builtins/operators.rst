.. highlight:: psql
.. _sql_operators:

Comparison operators
====================

These operations test whether two expressions are equal. Comparison Operators
result in a value of ``true``, ``false`` or ``null``.
The following tables show the usual comparison operators that can be used for
all simple data types:

================  ==================================
Operator          Description
================  ==================================
<                 less than
----------------  ----------------------------------
>                 greater than
----------------  ----------------------------------
<=                less than or equal to
----------------  ----------------------------------
>=                greater than or equal to
----------------  ----------------------------------
=                 equal
----------------  ----------------------------------
<>                not equal
----------------  ----------------------------------
!=                not equal - same as <>
================  ==================================

In addition, when the left operand is an `IP` and the right is `text`
that follows `CIDR notation`_ [ip address/prefix_length], the ``<<``
operator can be used as in:

``'192.160.0.1' << '192.160.0.0/24'``

returning true when the `IP` is included in the range described by the
CIDR.

If used in the :ref:`sql_dql_where_clause` the following operators are valid in
addition.

============================  ==================================
Operator                      Description
============================  ==================================
:ref:`sql_dql_like`           matches a part of the given value
----------------------------  ----------------------------------
:ref:`sql_dql_not`            negates a condition
----------------------------  ----------------------------------
:ref:`sql_dql_is_null`        matches a NULL value
----------------------------  ----------------------------------
:ref:`sql_dql_is_not_null`    matches a Non-NULL value
----------------------------  ----------------------------------
~                             regular expression match
----------------------------  ----------------------------------
!~                            negated regular expression match
----------------------------  ----------------------------------
<<                            IP is included in CIDR
----------------------------  ----------------------------------
between                       ``a BETWEEN x and y``:
                              shortcut for ``a >= x AND a <= y``
============================  ==================================


.. _CIDR notation: https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing#CIDR_notation