use mtdb
GO

DROP TABLE T_ATOKEN
GO

CREATE TABLE T_ATOKEN
(
    TENANT_ID NUMERIC(7) NOT NULL,
    T_TYPE VARCHAR(32) NOT NULL,
    U_KEY VARCHAR(128) NOT NULL,
    SERIES VARCHAR(128) NOT NULL,
    TOKEN VARCHAR(128) NOT NULL,
    POL_NAME VARCHAR(128),
    S_DATE DATETIME2(3),
    T_INFO VARBINARY(MAX),
    CONSTRAINT T_ATOKEN_PK PRIMARY KEY (TENANT_ID, T_TYPE, SERIES)
)
ON PS_MTDB (TENANT_ID)
GO

CREATE INDEX T_ATOKEN_INDEX1 ON T_ATOKEN (TENANT_ID, T_TYPE, U_KEY)
GO

CREATE INDEX T_ATOKEN_INDEX2 ON T_ATOKEN (TENANT_ID, U_KEY)
GO
