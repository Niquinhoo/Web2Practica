*Versioned migrations *are applied to a target database in order exactly once. The [*flyway_schema_history* table](https://documentation.red-gate.com/fd/flyway-schema-history-table-273973417.html) tracks which *versioned migrations* have already been applied and stores the *checksum* to make sure they haven't changed. It is best practice not to make changes to existing   *versioned migrations*   once they have been applied to a permanent downstream environment. It's best to create a new   *versioned migration*   script and roll forward any necessary changes.

Versioned migrations are typically used for:

- Creating/altering/dropping tables/indexes/foreign keys/enums/UDTs/...

- Reference data updates

- User data corrections

## Naming

Each versioned migration has a *version*, a *description* and a *checksum*. eg., V001.002__NewTwitterColumn.sql

Each versioned migration must be assigned a **unique version**.

Versioned migrations are applied in the order of their versions. Versions are sorted numerically as you would normally expect.

Any version is valid as long as it conforms to the usual dotted notation or an underscore separated notation. For most cases a simple increasing integer padded with zeros should be all you need, though Flyway Desktop generates a timestamp as part of the version by default when generating migrations as it helps to avoid conflicts if two team members simultaneously add migrations with no conflicting SQL.

The description is purely informative for you to be able to remember what each migration does.

The checksum is there to detect accidental changes.

Relevant configuration:

- [SQL migration prefix](#)

- [SQL migration separator](#)

- [SQL migration suffixes](#)

## Example

```
CREATE   TABLE   car   (
id   INT   NOT   NULL   PRIMARY   KEY  ,
license_plate   VARCHAR   NOT   NULL  ,
color   VARCHAR   NOT   NULL
);

ALTER   TABLE   owner   ADD   driver_license_id   VARCHAR  ;

INSERT   INTO   brand   (  name  )   VALUES   (  'DeLorean'  );

```
